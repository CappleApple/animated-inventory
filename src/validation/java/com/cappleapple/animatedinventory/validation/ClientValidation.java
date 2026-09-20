package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.AnimatedInventory;
import com.cappleapple.animatedinventory.api.animation.TransitionType;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.validation.mixin.*;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Automated input and rendering checks against a real integrated server. */
@Mod.EventBusSubscriber(modid="animatedinventory", value=Dist.CLIENT)
public final class ClientValidation {
    private static boolean booted, done, tooltipPending;
    private static int ticks, step, delay=10, configPage, startupTicks;
    private static final long START=System.nanoTime();
    private static final List<String> RESULTS=new CopyOnWriteArrayList<>();
    private static CompletableFuture<?> pending;
    private static Runnable serverAction;
    private static CompletableFuture<Void> serverFuture;
    private static int earliestServerTick, serverDeadline;
    private static final class ValidationAssertion extends IllegalStateException {
        ValidationAssertion(String message) { super(message); }
    }
    private static AbstractContainerScreen<?> screen;
    private static long owner;
    private static final Path EVIDENCE=Path.of("validation-evidence");
    private static Minecraft mc() { return Minecraft.getInstance(); }
    private static ClientRuntime runtime() { return ClientRuntime.INSTANCE; }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !Boolean.getBoolean("animatedinventory.clientValidation") || done) return;
        Minecraft mc=mc();
        try {
            if (!booted && startupTicks++ % 100 == 0) System.out.println("Animated Inventory startup screen=" + mc.screen + " overlay=" + mc.getOverlay());
            mc.mouseHandler.releaseMouse();
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            require(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_VISIBLE)==GLFW.GLFW_FALSE,"native window remains hidden");
            require(!mc.mouseHandler.isMouseGrabbed(),"mouse remains released");
            if(System.nanoTime()-START>900_000_000_000L) throw new IllegalStateException("Client validation timed out at phase "+step);
            if(pending!=null) { if(!pending.isDone()) return; pending.join(); pending=null; ticks=0; }
            if(!booted && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
                require(net.minecraftforge.versions.forge.ForgeVersion.getVersion().equals("47.4.10"),"recommended Forge 47.4.10 is loaded");
                String source=AnimatedInventory.class.getProtectionDomain().getCodeSource().getLocation().toString();
                RESULTS.add("SOURCE "+source);
                if(Boolean.getBoolean("animatedinventory.productionValidation")) require(source.contains("animatedinventory-forge-1.20.1-1.1.1.jar"),"production release jar supplies mod classes");
                auditLanguage("en_us");
                booted=true; mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE); mc.options.pauseOnLostFocus=false; mc.options.framerateLimit().set(60);
                mc.createWorldOpenFlows().createFreshLevel("animatedinventory-audit-"+System.currentTimeMillis(),
                    new LevelSettings("Animated Inventory audit",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if(mc.player==null || mc.level==null || mc.getOverlay()!=null || mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen || ++ticks<delay) return;
            ticks=0; delay=10;
            System.out.println("Animated Inventory validation phase "+step);
            phase(step++);
        } catch(Throwable failure) { finish(failure); }
    }

    private static void phase(int phase) throws Exception {
        Minecraft mc=mc(); ClientRuntime r=runtime();
        switch(phase) {
            case 0 -> {
                ClientConfig.ENABLED.set(true); ClientConfig.REDUCE_MOTION.set(false);
                ClientConfig.MOVE_MS.set(1000); ClientConfig.PICKUP_MS.set(1000); ClientConfig.PLACE_MS.set(1000); ClientConfig.QUICK_MS.set(1000); ClientConfig.CRAFT_MS.set(1000);
                ClientConfig.OPEN_MS.set(1000); ClientConfig.CLOSE_MS.set(1000); ClientConfig.HOTBAR_MS.set(1000);
                server(player->{
                    player.getInventory().clearContent();
                    SimpleContainer contents=new SimpleContainer(27);
                    contents.setItem(0,new ItemStack(Items.DIAMOND,32)); contents.setItem(1,new ItemStack(Items.DIAMOND,52));
                    contents.setItem(4,new ItemStack(Items.EMERALD,7)); contents.setItem(6,new ItemStack(Items.IRON_INGOT,32));
                    player.openMenu(new SimpleMenuProvider((id,inventory,p)->ChestMenu.threeRows(id,inventory,contents),Component.literal("Validation chest")));
                });
            }
            case 1 -> {
                require(mc.screen instanceof ContainerScreen,"server opens native chest screen"); screen=(AbstractContainerScreen<?>)mc.screen;
                require(stack(0).getCount()==32 && stack(1).getCount()==52,"server contents synchronize into native slots");
                require(r.animations.active().isEmpty(),"initial synchronization creates no arrival animations");
                require(field(r.screens,"live")!=null,"opening screen captures a real render target");
                mouseSlot(0); capture("01-open-inventory");
            }
            case 2 -> {
                int x=screen.getMenu().getSlot(0).x,y=screen.getMenu().getSlot(0).y;
                int highlight=RenderTrace.calls.indexOf("H:"+x+":"+y),item=RenderTrace.calls.indexOf("I:"+x+":"+y);
                require(highlight>=0 && item>highlight,"native hover highlight renders before the item");
                require(r.emphasis.scale("slot:0",true,System.nanoTime())>1,"native hovered item receives emphasis");
                capture("02-hover-highlight");
                mouseClick(0,0);
                require(carried().getCount()==32,"real mouse pickup predicts immediately");
                require(hasDestination("cursor"),"real mouse pickup creates a cursor animation");
                server(player->require(player.containerMenu.getCarried().getCount()==32 && player.containerMenu.getSlot(0).getItem().isEmpty(),"integrated server confirms mouse pickup"));
            }
            case 3 -> {
                click(1,0,ClickType.PICKUP);
                require(stack(1).getCount()==64 && carried().getCount()==20,"partial merge preserves client quantities");
                server(player->require(player.containerMenu.getSlot(1).getItem().getCount()==64 && player.containerMenu.getCarried().getCount()==20,"integrated server confirms partial merge"));
            }
            case 4 -> {
                click(2,1,ClickType.PICKUP);
                require(stack(2).getCount()==1 && carried().getCount()==19,"right placement moves one item");
                click(3,0,ClickType.PICKUP);
                require(stack(3).getCount()==19 && carried().isEmpty(),"left placement moves remaining cursor stack");
                server(player->require(player.containerMenu.getSlot(2).getItem().getCount()==1 && player.containerMenu.getSlot(3).getItem().getCount()==19 && player.containerMenu.getCarried().isEmpty(),"server confirms single and full placement"));
            }
            case 5 -> {
                click(1,1,ClickType.PICKUP);
                require(stack(1).getCount()==32 && carried().getCount()==32,"right pickup splits a stack in half");
                click(4,0,ClickType.PICKUP);
                require(stack(4).is(Items.DIAMOND) && carried().is(Items.EMERALD),"cursor swap preserves both item types");
                click(5,0,ClickType.PICKUP);
                server(player->require(player.containerMenu.getSlot(1).getItem().getCount()==32 && player.containerMenu.getSlot(4).getItem().getCount()==32 && player.containerMenu.getSlot(5).getItem().getCount()==7,"server confirms split and swap"));
            }
            case 6 -> {
                click(5,0,ClickType.SWAP);
                require(mc.player.getInventory().getItem(0).is(Items.EMERALD) && stack(5).isEmpty(),"number-key swap moves into hotbar");
                server(player->require(player.getInventory().getItem(0).getCount()==7 && player.containerMenu.getSlot(5).getItem().isEmpty(),"server confirms hotbar swap"));
            }
            case 7 -> {
                click(6,0,ClickType.QUICK_MOVE);
                require(stack(6).isEmpty() && !r.animations.active().isEmpty(),"native quick move transfers and animates");
                server(player->require(player.containerMenu.getSlot(6).getItem().isEmpty() && player.getInventory().countItem(Items.IRON_INGOT)==32,"server confirms quick move"));
            }
            case 8 -> {
                click(2,0,ClickType.PICKUP); click(2,0,ClickType.PICKUP_ALL);
                require(carried().getCount()==64,"double-click collection fills the cursor stack");
                server(player->require(player.containerMenu.getCarried().getCount()==64,"server confirms collect-all"));
            }
            case 9 -> server(player->{ player.containerMenu.setCarried(new ItemStack(Items.DIAMOND,12)); player.containerMenu.getSlot(7).set(ItemStack.EMPTY); player.containerMenu.getSlot(8).set(ItemStack.EMPTY); player.containerMenu.broadcastChanges(); });
            case 10 -> {
                require(carried().getCount()==12,"server cursor update reaches client");
                click(-999,0,ClickType.QUICK_CRAFT); click(7,1,ClickType.QUICK_CRAFT); click(8,1,ClickType.QUICK_CRAFT); click(-999,2,ClickType.QUICK_CRAFT);
                require(stack(7).getCount()==6 && stack(8).getCount()==6 && carried().isEmpty(),"drag distribution splits the cursor evenly");
                server(player->require(player.containerMenu.getSlot(7).getItem().getCount()==6 && player.containerMenu.getSlot(8).getItem().getCount()==6 && player.containerMenu.getCarried().isEmpty(),"server confirms drag distribution"));
            }
            case 11 -> {
                click(7,0,ClickType.THROW);
                require(stack(7).getCount()==5,"throw removes one item through native input");
                server(player->require(player.containerMenu.getSlot(7).getItem().getCount()==5,"server confirms throw"));
            }
            case 12 -> { mc.player.getInventory().selected=4; delay=2; }
            case 13 -> {
                require(RenderTrace.selectorFrames>0 && Math.abs(RenderTrace.selectorVisual-RenderTrace.selectorLogical)>.05,"actual hotbar selector renders between old and new slots");
                capture("03-hotbar-in-motion");
                server(player->{ player.setGameMode(GameType.CREATIVE); player.containerMenu.getSlot(0).set(new ItemStack(Items.STONE,8)); player.containerMenu.broadcastChanges(); });
            }
            case 14 -> {
                click(0,0,ClickType.CLONE); require(carried().is(Items.STONE) && carried().getCount()==64,"creative clone preserves native full-stack behavior");
                server(player->{ require(player.containerMenu.getCarried().getCount()==64,"server confirms creative clone"); player.containerMenu.setCarried(ItemStack.EMPTY); player.setGameMode(GameType.SURVIVAL); player.closeContainer(); player.getInventory().clearContent(); player.inventoryMenu.getSlot(1).set(new ItemStack(Items.OAK_LOG,2)); player.inventoryMenu.slotsChanged(player.inventoryMenu.getSlot(1).container); player.inventoryMenu.broadcastChanges(); });
            }
            case 15 -> { mc.setScreen(new InventoryScreen(mc.player)); screen=(AbstractContainerScreen<?>)mc.screen; }
            case 16 -> {
                require(stack(0).is(Items.OAK_PLANKS) && stack(0).getCount()==4,"server recipe computes actual 2x2 crafting output");
                click(0,0,ClickType.PICKUP);
                require(carried().is(Items.OAK_PLANKS) && carried().getCount()==4 && stack(1).getCount()==1,"native 2x2 crafting consumes exactly one ingredient");
                require(hasType(TransitionType.CRAFT),"native 2x2 result creates ingredient-flow animation");
                capture("04-crafting-flow");
                server(player->require(player.inventoryMenu.getSlot(1).getItem().getCount()==1 && player.inventoryMenu.getCarried().getCount()==4,"server confirms 2x2 craft quantities"));
            }
            case 17 -> {
                click(0,0,ClickType.QUICK_MOVE);
                require(stack(1).isEmpty() && hasType(TransitionType.CRAFT),"shift crafting consumes remaining ingredient and animates");
                server(player->require(player.inventoryMenu.getSlot(1).getItem().isEmpty() && player.getInventory().countItem(Items.OAK_PLANKS)==4,"server confirms shift-craft output"));
            }
            case 18 -> server(player->{
                player.inventoryMenu.setCarried(ItemStack.EMPTY); player.getInventory().clearContent();
                var position=player.blockPosition().offset(1,0,0); player.serverLevel().setBlockAndUpdate(position,Blocks.CRAFTING_TABLE.defaultBlockState());
                player.openMenu(new SimpleMenuProvider((id,inventory,p)->new CraftingMenu(id,inventory,ContainerLevelAccess.create(player.serverLevel(),position)),Component.literal("Validation crafting")));
                for(int slot=1;slot<=3;slot++) player.containerMenu.getSlot(slot).set(new ItemStack(Items.WHEAT));
                player.containerMenu.slotsChanged(player.containerMenu.getSlot(1).container); player.containerMenu.broadcastChanges();
            });
            case 19 -> {
                require(mc.screen instanceof CraftingScreen,"server opens native crafting-table screen"); screen=(AbstractContainerScreen<?>)mc.screen;
                require(stack(0).is(Items.BREAD),"server computes the real three-wide bread recipe");
                click(0,0,ClickType.QUICK_MOVE);
                require(hasType(TransitionType.CRAFT),"3x3 shift crafting creates ingredient-flow animation");
                server(player->{ require(player.getInventory().countItem(Items.BREAD)==1 && player.containerMenu.getSlot(1).getItem().isEmpty() && player.containerMenu.getSlot(2).getItem().isEmpty() && player.containerMenu.getSlot(3).getItem().isEmpty(),"server confirms 3x3 recipe consumption and output"); player.closeContainer(); player.inventoryMenu.getSlot(9).set(new ItemStack(Items.DIAMOND_HELMET)); player.inventoryMenu.broadcastChanges(); });
            }
            case 20 -> { mc.setScreen(new InventoryScreen(mc.player)); screen=(AbstractContainerScreen<?>)mc.screen; }
            case 21 -> {
                click(9,0,ClickType.QUICK_MOVE);
                require(stack(5).is(Items.DIAMOND_HELMET),"native quick move equips matching armor");
                server(player->require(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET),"server confirms equipment transfer"));
            }
            case 22 -> { ClientConfig.ENABLED.set(false); }
            case 23 -> {
                require(!r.enabled() && r.animations.active().isEmpty(),"disabling animation clears visual ownership");
                click(5,0,ClickType.PICKUP);
                require(carried().is(Items.DIAMOND_HELMET) && r.animations.active().isEmpty(),"disabled mode preserves real input without visual copies");
                server(player->require(player.inventoryMenu.getCarried().is(Items.DIAMOND_HELMET),"server confirms input while animations are disabled"));
                ClientConfig.ENABLED.set(true); ClientConfig.REDUCE_MOTION.set(true);
            }
            case 24 -> {
                click(5,0,ClickType.PICKUP);
                require(stack(5).is(Items.DIAMOND_HELMET),"re-enabled reduced-motion input remains native");
                require(!r.animations.active().isEmpty() && r.animations.active().stream().allMatch(a->a.duration<=60_000_000L && a.source.equals(a.destination)),"reduced motion uses short stationary item fades");
                mc.player.getInventory().selected=1; delay=2;
            }
            case 25 -> {
                require(Math.abs(RenderTrace.selectorVisual-RenderTrace.selectorLogical)<.01,"reduced motion removes hotbar travel");
                require(r.enabled(),"native compositor remains enabled after transaction coverage");
                ClientConfig.REDUCE_MOTION.set(false); owner=r.owner(screen); screen.resize(mc,screen.width,screen.height);
                require(owner!=r.owner(screen) && r.animations.active().isEmpty(),"resize replaces ownership and clears stale animations");
            }
            case 26 -> {
                require(field(r.screens,"live")!=null,"screen transition captures refreshed viewport");
                mc.setScreen(null);
                require(field(r.screens,"exit")!=null,"closing transfers a GPU image into exit animation");
                delay=2;
            }
            case 27 -> { capture("05-closing-transition"); delay=25; }
            case 28 -> {
                require(field(r.screens,"exit")==null,"closing image is released after its duration");
                mc.getToasts().clear(); mc.gui.getChat().clearMessages(true); auditLanguage("en_us"); mc.setScreen(new ClientConfigScreen(null)); mouse(0,0); configPage=0;
            }
            case 29 -> {
                if (tooltipPending) {
                    capture("config-tooltip"); mouse(0,0); tooltipPending=false;
                    button("animatedinventory.configuration.next").onPress(); step--; break;
                }
                require(mc.screen instanceof ClientConfigScreen,"translated config screen remains open");
                for(var child:mc.screen.children()) if(child instanceof Button button)
                    require(!button.getMessage().getString().contains("animatedinventory.configuration."),"config buttons resolve translated text");
                capture(String.format(Locale.ROOT,"config-%02d",configPage++));
                if(configPage==1) {
                    mouse(22,62); tooltipPending=true; step--; break;
                }
                Button next=button("animatedinventory.configuration.next");
                if(next.active) { next.onPress(); step--; } else { require(configPage>=14,"all configuration sections render"); mc.setScreen(new ClientConfigScreen(null)); }
            }
            case 30 -> {
                EditBox speed=mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(b->b.getMessage().getString().equals(Component.translatable("animatedinventory.configuration.animation_speed_multiplier").getString())).findFirst().orElseThrow();
                speed.setValue("not-a-number"); require(!button("gui.done").active && !button("animatedinventory.configuration.next").active,"invalid numeric input blocks save and navigation");
                speed.setValue("2.25"); require(button("gui.done").active,"valid numeric input re-enables save"); button("gui.done").onPress();
                require(ClientConfig.SPEED.get()==2.25,"Done applies edited config value");
                Path file=Path.of("config/animatedinventory-client.toml"); String text=Files.readString(file);
                require(text.contains("animation_speed_multiplier = 2.25"),"Done persists config value to TOML");
                Files.writeString(file,text.replace("animation_speed_multiplier = 2.25","animation_speed_multiplier = 1.75"));
                CommentedFileConfig loaded=CommentedFileConfig.of(file); loaded.load(); ClientConfig.SPEC.acceptConfig(loaded); ClientConfig.SPEC.afterReload();
                require(ClientConfig.SPEED.get()==1.75,"Forge config reload reads the changed disk value");
                mc.setScreen(new ClientConfigScreen(null));
            }
            case 31 -> {
                EditBox speed=mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(b->b.getValue().equals("1.75")).findFirst().orElseThrow();
                speed.setValue("3.0"); button("gui.cancel").onPress(); require(ClientConfig.SPEED.get()==1.75,"Cancel discards the edited draft");
                mc.getLanguageManager().setSelected("fr_fr"); mc.options.languageCode="fr_fr"; pending=mc.reloadResourcePacks();
            }
            case 32 -> { auditLanguage("fr_fr fallback"); mc.setScreen(new ClientConfigScreen(null)); }
            case 33 -> { capture("config-french-fallback"); mc.getLanguageManager().setSelected("en_us"); mc.options.languageCode="en_us"; pending=mc.reloadResourcePacks(); }
            case 34 -> { require(r.enabled(),"client remains healthy after language reloads and config edits"); finish(null); }
            default -> throw new IllegalStateException("Unexpected phase "+phase);
        }
    }

    private static void server(Consumer<ServerPlayer> action) {
        var server=Objects.requireNonNull(mc().getSingleplayerServer()); var id=mc().player.getUUID();
        var completion=new CompletableFuture<Void>(); pending=completion;
        server.execute(()->{
            serverAction=()->action.accept(Objects.requireNonNull(server.getPlayerList().getPlayer(id)));
            serverFuture=completion; earliestServerTick=server.getTickCount()+2; serverDeadline=server.getTickCount()+100;
        });
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || serverAction==null || event.getServer().getTickCount()<earliestServerTick) return;
        try { serverAction.run(); serverAction=null; serverFuture.complete(null); }
        catch(ValidationAssertion assertion) {
            if(event.getServer().getTickCount()>=serverDeadline) { serverAction=null; serverFuture.completeExceptionally(assertion); }
        } catch(Throwable failure) { serverAction=null; serverFuture.completeExceptionally(failure); }
    }
    private static void click(int index,int button,ClickType type) {
        screen=(AbstractContainerScreen<?>)mc().screen;
        ((ContainerInputAccess)screen).validation$click(index<0?null:screen.getMenu().getSlot(index),index,button,type);
    }
    private static void mouseClick(int index,int button) {
        var slot=screen.getMenu().getSlot(index); double x=screen.getGuiLeft()+slot.x+8,y=screen.getGuiTop()+slot.y+8;
        screen.mouseClicked(x,y,button); screen.mouseReleased(x,y,button);
    }
    private static ItemStack stack(int index) { return screen.getMenu().getSlot(index).getItem(); }
    private static ItemStack carried() { return screen.getMenu().getCarried(); }
    private static boolean hasDestination(String id) { return runtime().animations.active().stream().anyMatch(a->id.equals(a.transition.destinationId())); }
    private static boolean hasType(TransitionType type) { return runtime().animations.active().stream().anyMatch(a->a.transition.type()==type); }
    private static void mouseSlot(int index) { var slot=screen.getMenu().getSlot(index); mouse(screen.getGuiLeft()+slot.x+8,screen.getGuiTop()+slot.y+8); }
    private static void mouse(double x,double y) {
        var window=mc().getWindow(); var access=(MousePositionAccess)mc().mouseHandler;
        access.validation$x(x*window.getScreenWidth()/window.getGuiScaledWidth()); access.validation$y(y*window.getScreenHeight()/window.getGuiScaledHeight());
    }
    private static Object field(Object object,String name) throws Exception { Field field=object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
    private static Button button(String key) { String text=Component.translatable(key).getString(); return mc().screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().getString().equals(text)).findFirst().orElseThrow(); }
    private static void capture(String name) throws Exception {
        Files.createDirectories(EVIDENCE);
        try(var image=Screenshot.takeScreenshot(mc().getMainRenderTarget())) { image.writeToFile(EVIDENCE.resolve(name+".png")); }
        RESULTS.add("CAPTURE "+name+".png");
    }
    private static void auditLanguage(String language) throws Exception {
        Set<String> keys=new TreeSet<>(); int settings=0;
        for(Field field:ClientConfig.class.getFields()) if(field.get(null) instanceof ForgeConfigSpec.ConfigValue<?> value) {
            var path=value.getPath(); String key="animatedinventory.configuration."+path.get(path.size()-1);
            keys.add(key); keys.add(key+".tooltip"); keys.add("animatedinventory.configuration."+path.get(0)); settings++;
            if(value.get() instanceof Enum<?> option) for(Object choice:option.getDeclaringClass().getEnumConstants()) keys.add(ClientConfigScreen.enumKey((Enum<?>)choice));
        }
        keys.addAll(List.of("animatedinventory.configuration.title","animatedinventory.configuration.previous","animatedinventory.configuration.next","animatedinventory.configuration.page","animatedinventory.configuration.invalid_number","animatedinventory.configuration.unavailable","options.on","options.off","gui.done","gui.cancel"));
        List<String> resolved=new ArrayList<>();
        for(String key:keys) { require(Language.getInstance().has(key),"language key exists: "+key); String text=Component.translatable(key,1,2).getString(); require(!text.equals(key),"language key resolves: "+key); resolved.add(key+" = "+text); }
        Files.createDirectories(EVIDENCE); Files.write(EVIDENCE.resolve("language-"+language.replace(' ','-')+".txt"),resolved);
        require(settings==57,"all 57 config settings have resolved labels and tooltips in "+language);
        RESULTS.add("LANGUAGE "+language+" "+keys.size()+" resolved keys");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new ValidationAssertion(message); String line="PASS "+message; if(!RESULTS.contains(line)) RESULTS.add(line); }
    private static void finish(Throwable failure) {
        done=true;
        if(failure!=null) { RESULTS.add("FAIL phase "+(step-1)+": "+failure); failure.printStackTrace(); }
        else RESULTS.add("PASS validation completed");
        try { Files.write(Path.of("client-validation.txt"),RESULTS); } catch(Exception error) { throw new IllegalStateException(error); }
        mc().stop();
    }
}
