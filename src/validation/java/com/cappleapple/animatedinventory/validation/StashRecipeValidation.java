package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.api.AnimatedInventoryApi;
import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.transaction.RecipeViewerOrigins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import static com.cappleapple.animatedinventory.validation.ClientValidation.require;

final class StashRecipeValidation {
    private static int phase, delay, recipeCase;
    private static CompletableFuture<Void> task;
    private static AbstractContainerScreen<?> screen;
    private static final ResourceLocation CATEGORY = ResourceLocation.parse("animatedinventory:validation_gold");
    private static Object recipe, jeiRuntime, category, emiRecipe;
    interface Action { void run(ServerPlayer player) throws Exception; }
    static void tick(Minecraft mc) throws Exception {
        if (task != null) { if (!task.isDone()) return; task.join(); task = null; }
        if (delay-- > 0) return;
        var runtime = ClientRuntime.INSTANCE;
        switch (phase) {
            case 0 -> {
                recipeCase = net.neoforged.fml.ModList.get().isLoaded("emi") ? 1 : 0;
                ClientConfig.MOVE_MS.set(900); ClientConfig.QUICK_MS.set(900); ClientConfig.PICKUP_MS.set(900);
                ClientConfig.OPEN.set(ClientConfig.ScreenEffect.NONE); ClientConfig.CLOSE.set(ClientConfig.ScreenEffect.NONE);
                server(mc,p -> {
                    p.closeContainer(); clear(p); put(p,9,new ItemStack(Items.APPLE,8)); put(p,36,new ItemStack(Items.GOLD_INGOT,17));
                    Object data=data(p), categories=data.getClass().getMethod("categories").invoke(data);
                    Class<?> rule=Class.forName("com.cappleapple.bundlednotsiloed.category.CategoryRule");
                    Object itemRule=rule.getConstructor(Class.forName(rule.getName()+"$Type"),ResourceLocation.class)
                            .newInstance(enumeration(rule.getName()+"$Type","ITEM"),ResourceLocation.parse("minecraft:gold_ingot"));
                    Class<?> definition=Class.forName("com.cappleapple.bundlednotsiloed.category.CategoryDefinition");
                    Object gold=definition.getConstructors()[0].newInstance(CATEGORY,"Validation gold",ResourceLocation.parse("minecraft:gold_ingot"),
                            100,List.of(itemRule),List.of(),-1L,sort(),true,false);
                    categories.getClass().getMethod("upsert",definition).invoke(categories,gold);
                    sync(p);
                }); delay=7;
            }
            case 1 -> { mc.setScreen(new InventoryScreen(mc.player)); screen=(AbstractContainerScreen<?>)mc.screen; delay=8; }
            case 2 -> { runtime.invalidate(screen,false); preferences(CATEGORY); delay=5; }
            case 3 -> {
                arrival(Items.GOLD_INGOT,17,"category switch retrieves stashed gold");
                require(runtime.animations.active().stream().anyMatch(a -> a.transition.type()==TransitionType.STOW && a.transition.stack().is(Items.APPLE)),
                        "category switch also stows excluded apples");
                capture("stash-category"); delay=4;
            }
            case 4 -> { runtime.invalidate(screen,false); preferences(null); delay=5; }
            case 5 -> { arrival(Items.APPLE,8,"sorting all items retrieves stashed apples"); capture("stash-sort"); delay=4; }
            case 6 -> {
                mc.player.closeContainer();
                server(mc,p -> {
                    p.closeContainer(); clear(p);
                    int visible=recipeCase<2?2:3, hidden=recipeCase<2?2:5;
                    put(p,9,new ItemStack(Items.OAK_PLANKS,visible)); put(p,36,new ItemStack(Items.OAK_PLANKS,hidden)); sync(p);
                    if(recipeCase>=2) {
                        BlockPos pos=p.blockPosition().offset(1,0,0); p.serverLevel().setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
                        p.openMenu(new SimpleMenuProvider((id,inv,player)->new CraftingMenu(id,inv,ContainerLevelAccess.create(p.serverLevel(),pos)),
                                Component.literal("Recipe transfer validation")));
                    }
                }); delay=8;
            }
            case 7 -> {
                if(recipeCase<2) mc.setScreen(new InventoryScreen(mc.player));
                require(mc.screen instanceof AbstractContainerScreen<?>,"real crafting screen open for recipe fill");
                screen=(AbstractContainerScreen<?>)mc.screen; delay=8;
            }
            case 8 -> {
                require(runtime.snapshot().items().values().stream().anyMatch(i->i.offscreenSource()&&i.stack().is(Items.OAK_PLANKS)),
                        "stash recipe source tracked before viewer opens");
                ResourceLocation id=ResourceLocation.parse(recipeCase<2?"minecraft:crafting_table":"minecraft:chest");
                recipe=mc.level.getRecipeManager().byKey(id).orElseThrow();
                if(recipeCase%2==0) {
                    jeiRuntime=Class.forName("mezz.jei.common.Internal").getMethod("getJeiRuntime").invoke(null);
                    Object recipeType=Class.forName("mezz.jei.api.constants.RecipeTypes").getField("CRAFTING").get(null);
                    Object manager=call("mezz.jei.api.runtime.IJeiRuntime",jeiRuntime,"getRecipeManager");
                    category=Class.forName("mezz.jei.api.recipe.IRecipeManager").getMethod("getRecipeCategory",Class.forName("mezz.jei.api.recipe.RecipeType")).invoke(manager,recipeType);
                    Object gui=call("mezz.jei.api.runtime.IJeiRuntime",jeiRuntime,"getRecipesGui");
                    Class.forName("mezz.jei.api.runtime.IRecipesGui").getMethod("showTypes",List.class).invoke(gui,List.of(recipeType));
                } else {
                    Object manager=Class.forName("dev.emi.emi.api.EmiApi").getMethod("getRecipeManager").invoke(null);
                    emiRecipe=Class.forName("dev.emi.emi.api.recipe.EmiRecipeManager").getMethod("getRecipe",ResourceLocation.class).invoke(manager,id);
                    require(emiRecipe!=null,"real EMI recipe exists");
                    Class.forName("dev.emi.emi.api.EmiApi").getMethod("displayRecipe",Class.forName("dev.emi.emi.api.recipe.EmiRecipe")).invoke(null,emiRecipe);
                }
                require(mc.screen!=screen&&RecipeViewerOrigins.viewer(mc.screen),"actual recipe viewer opens: "+mc.screen.getClass().getName());
                delay=5;
            }
            case 9 -> {
                if(recipeCase%2==0) {
                    Object manager=call("mezz.jei.api.runtime.IJeiRuntime",jeiRuntime,"getRecipeTransferManager");
                    Object handler=((Optional<?>)Class.forName("mezz.jei.api.recipe.transfer.IRecipeTransferManager")
                            .getMethod("getRecipeTransferHandler",AbstractContainerMenu.class,Class.forName("mezz.jei.api.recipe.category.IRecipeCategory"))
                            .invoke(manager,screen.getMenu(),category)).orElseThrow();
                    require(handler.getClass().getName().contains("bundlednotsiloed"),"JEI selected registered BNS recipe handler");
                    Object error=Class.forName("mezz.jei.api.recipe.transfer.IRecipeTransferHandler").getMethod("transferRecipe",AbstractContainerMenu.class,
                            Object.class,Class.forName("mezz.jei.api.gui.ingredient.IRecipeSlotsView"),net.minecraft.world.entity.player.Player.class,boolean.class,boolean.class)
                            .invoke(handler,screen.getMenu(),recipe,null,mc.player,false,true);
                    require(error==null,"JEI recipe fill accepted");
                    mc.setScreen(screen);
                } else {
                    Class<?> type=Class.forName("dev.emi.emi.api.recipe.handler.EmiCraftContext$Type"), destination=Class.forName("dev.emi.emi.api.recipe.handler.EmiCraftContext$Destination");
                    boolean filled=(boolean)Class.forName("dev.emi.emi.registry.EmiRecipeFiller").getMethod("performFill",
                            Class.forName("dev.emi.emi.api.recipe.EmiRecipe"),AbstractContainerScreen.class,type,destination,int.class)
                            .invoke(null,emiRecipe,screen,enumeration(type.getName(),"FILL_BUTTON"),enumeration(destination.getName(),"NONE"),1);
                    require(filled,"EMI real fill operation accepted");
                    if(mc.screen!=screen) mc.setScreen(screen);
                }
                delay=5;
            }
            case 10 -> {
                int expected=recipeCase<2?4:8, hidden=recipeCase<2?2:5;
                var moving=runtime.animations.active().stream().filter(a->a.transition.stack().is(Items.OAK_PLANKS)).toList();
                require(moving.stream().mapToInt(a->a.transition.stack().getCount()).sum()==expected,
                        "recipe ingredients travel from their sources: case "+recipeCase+" active="+moving.stream().map(a->a.transition.sourceId()+"->"+a.transition.destinationId()).toList());
                require(moving.stream().filter(a->a.transition.type()==TransitionType.RETRIEVE).mapToInt(a->a.transition.stack().getCount()).sum()==hidden,
                        "recipe fill attributes exact stashed ingredient count");
                require(moving.stream().filter(a->a.transition.sourceId().startsWith("slot:")).mapToInt(a->a.transition.stack().getCount()).sum()==expected-hidden,
                        "recipe fill attributes exact visible ingredient count");
                require(screen.getMenu().getSlot(0).hasItem(),"recipe preview present without taking result");
                require(runtime.animations.active().stream().noneMatch(a->a.transition.type()==TransitionType.CRAFT),"filling does not pretend to consume recipe");
                capture("recipe-fill-"+recipeCase); delay=3;
            }
            case 11 -> {
                recipeCase+=2; phase=5;
                if(recipeCase>=4) { mc.player.closeContainer(); ClientValidation.finish(mc,null); return; }
            }
        }
        phase++;
    }
    private static Object call(String type,Object target,String method) throws Exception { return Class.forName(type).getMethod(method).invoke(target); }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static Object enumeration(String type,String name) throws Exception { return Enum.valueOf((Class)Class.forName(type),name); }
    private static Object sort() throws Exception { return enumeration("com.cappleapple.bundlednotsiloed.category.SortMode","NAME_ASCENDING"); }
    private static void preferences(ResourceLocation category) throws Exception {
        Object mode=sort();
        var payload=(CustomPacketPayload)Class.forName("com.cappleapple.bundlednotsiloed.network.InventoryViewPreferencesPayload")
                .getConstructor(mode.getClass(),ResourceLocation.class).newInstance(mode,category);
        PacketDistributor.sendToServer(payload);
    }
    @SuppressWarnings("unchecked")
    private static Object data(net.minecraft.world.entity.player.Player p) throws Exception {
        return p.getData((Supplier<AttachmentType<Object>>)Class.forName("com.cappleapple.bundlednotsiloed.data.ModAttachments").getField("PLAYER_DATA").get(null));
    }
    private static void clear(ServerPlayer p) throws Exception {
        Object data=data(p), inventory=data.getClass().getMethod("inventory").invoke(data);
        inventory.getClass().getMethod("clear").invoke(inventory);
        data.getClass().getMethod("setSelectedCategoryPreference",ResourceLocation.class).invoke(data,(Object)null);
        for(int i=0;i<5;i++) p.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
        p.inventoryMenu.setCarried(ItemStack.EMPTY);
    }
    private static void put(ServerPlayer p,int slot,ItemStack stack) throws Exception {
        Object inventory=data(p).getClass().getMethod("inventory").invoke(data(p));
        inventory.getClass().getMethod("replaceSyntheticSlotFromItemUse",int.class,ItemStack.class).invoke(inventory,slot,stack);
    }
    private static void sync(ServerPlayer p) throws Exception {
        Class.forName("com.cappleapple.bundlednotsiloed.network.ModNetwork").getMethod("sendInitial",ServerPlayer.class).invoke(null,p);
        p.inventoryMenu.broadcastChanges();
    }
    private static void server(Minecraft mc,Action action) {
        UUID id=mc.player.getUUID();
        task=mc.getSingleplayerServer().submit(()->{try{action.run(mc.getSingleplayerServer().getPlayerList().getPlayer(id));}catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void arrival(Item item,int count,String message) {
        require(ClientRuntime.INSTANCE.animations.active().stream().anyMatch(a->a.transition.type()==TransitionType.RETRIEVE
                && a.transition.stack().is(item)&&a.transition.stack().getCount()==count),message);
    }
    private static void capture(String name) { ClientValidation.captureSophisticated(screen,name); }
    private StashRecipeValidation() {}
}
