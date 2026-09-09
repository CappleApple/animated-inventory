package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.api.AnimatedInventoryApi;
import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.attachment.AttachmentType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import static com.cappleapple.animatedinventory.validation.ClientValidation.require;

/** Real integrated-server menus and packets, including BNS's independently synchronized inventory. */
final class TransferValidation {
    private static int scenario, phase, delay = 35;
    private static final String[] CASES = {"storage", "backpack", "vanilla-chest"};
    private static CompletableFuture<Void> task;
    private static AbstractContainerScreen<?> screen;
    private static Slot source, hotbar;
    private static Bounds original;
    private static String sourceId;

    interface ServerAction { void run(ServerPlayer player) throws Exception; }
    static void tick(Minecraft mc) throws Exception {
        if (task != null) {
            if (!task.isDone()) return;
            task.join(); task = null;
        }
        if (delay-- > 0) return;
        if (scenario == CASES.length) { StashRecipeValidation.tick(mc); return; }
        var runtime = ClientRuntime.INSTANCE;
        switch (phase) {
            case 0 -> {
                ClientConfig.MOVE_MS.set(900); ClientConfig.QUICK_MS.set(900); ClientConfig.PICKUP_MS.set(900);
                ClientConfig.PLACE_MS.set(900); ClientConfig.OPEN.set(ClientConfig.ScreenEffect.NONE);
                ClientConfig.CLOSE.set(ClientConfig.ScreenEffect.NONE); ClientConfig.REDUCE_MOTION.set(false);
                server(mc, player -> open(player));
                delay = 12;
            }
            case 1 -> {
                require(mc.screen instanceof AbstractContainerScreen<?>, "real server opens " + CASES[scenario]);
                screen = (AbstractContainerScreen<?>)mc.screen;
                require(BundledCompatibility.serverRevision() >= 0, "authoritative BNS baseline installed");
                source = screen.getMenu().getSlot(0);
                hotbar = screen.getMenu().slots.stream().filter(s -> s.container == mc.player.getInventory() && s.getContainerSlot() == 0).findFirst().orElseThrow();
                require(source.getItem().getCount() == 32, "server storage stack synchronized");
                sourceId = VanillaInventoryProvider.id(source);
                runtime.invalidate(screen, false);
                original = runtime.snapshot().items().get(sourceId).bounds();
                TrashSlotValidation.prepare(screen);
                delay = 3;
            }
            case 2 -> {
                click(source, ClickType.QUICK_MOVE);
                TrashSlotValidation.pending(screen, source);
                require(runtime.animations.active().stream().noneMatch(a -> a.transition.sourceId() != null), "prediction creates no intermediate-slot flight");
                delay = 4;
            }
            case 3 -> {
                require(hotbar.getItem().is(Items.DIAMOND) && hotbar.getItem().getCount() == 32 && source.getItem().isEmpty(),
                        "server routes new identity directly to hotbar: " + CASES[scenario]);
                movement(sourceId, VanillaInventoryProvider.id(hotbar), 32);
                require(runtime.animations.destination(VanillaInventoryProvider.id(hotbar), System.nanoTime()).transition.source().equals(original),
                        "hotbar arrival retains original container source");
                TrashSlotValidation.verify(screen, "hotbar arrival");
                capture("hotbar-arrival");
                server(mc, player -> {
                    if (player.getInventory().getItem(0).getCount() != 32 || !player.containerMenu.getSlot(0).getItem().isEmpty())
                        throw new AssertionError("Authoritative server transfer mismatch");
                });
                delay = 3;
            }
            case 4 -> {
                runtime.invalidate(screen, false);
                TrashSlotValidation.retainedItem();
                click(hotbar, ClickType.QUICK_MOVE);
                TrashSlotValidation.verify(screen, "pending return");
                delay = 4;
            }
            case 5 -> {
                require(hotbar.getItem().isEmpty() && source.getItem().getCount() == 32, "BNS returns stack to real container: " + CASES[scenario]);
                movement(VanillaInventoryProvider.id(hotbar), sourceId, 32);
                TrashSlotValidation.verify(screen, "return flight");
                capture("return-to-storage");
                delay = 3;
            }
            case 6 -> {
                server(mc, player -> {
                    player.containerMenu.getSlot(0).set(new ItemStack(Items.EMERALD, 48));
                    player.containerMenu.getSlot(1).set(ItemStack.EMPTY);
                    player.getInventory().setItem(9, new ItemStack(Items.EMERALD, 32));
                    player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
                });
                delay = 8;
            }
            case 7 -> {
                Slot grid = playerSlot(mc, 9);
                require(grid.getItem().getCount() == 32, "BNS main grid is server synchronized");
                runtime.invalidate(screen, false); click(grid, ClickType.QUICK_MOVE);
                require(runtime.pendingStack(screen, grid, grid.getItem()).getCount() == 32, "pending transfer preserves original source visual");
                TrashSlotValidation.verify(screen, "pending split merge");
                delay = 4;
            }
            case 8 -> {
                movement(VanillaInventoryProvider.id(playerSlot(mc, 9)), sourceId, 16);
                movement(VanillaInventoryProvider.id(playerSlot(mc, 9)), "slot:1", 16);
                require(screen.getMenu().getSlot(0).getItem().getCount() == 64 && screen.getMenu().getSlot(1).getItem().getCount() == 16,
                        "BNS main-grid quick move distributes exact quantities");
                require(AnimatedInventoryApi.normalRenderStack(screen, sourceId, source.getItem()).getCount() == 48,
                        "partial destination retains its original 48 during incoming merge");
                capture("main-grid-split");
                delay = 3;
            }
            case 9 -> { delay = 0; }
            case 10 -> {
                server(mc, player -> {
                    var menu = player.containerMenu;
                    menu.setCarried(ItemStack.EMPTY);
                    for (int i = 0; i < 4; i++) menu.getSlot(i).set(new ItemStack(Items.DIAMOND, new int[]{16,12,20,40}[i]));
                    menu.broadcastChanges();
                });
                delay = 8;
            }
            case 11 -> {
                runtime.invalidate(screen, false);
                click(source, ClickType.PICKUP);
                require(screen.getMenu().getCarried().getCount() == 16, "real pickup precedes double-click collection");
                delay = 2;
            }
            case 12 -> {
                click(source, ClickType.PICKUP_ALL);
                require(screen.getMenu().getCarried().getCount() == 64, "double click collects to cursor capacity");
                donors();
                capture("double-click");
                delay = 3;
            }
            case 13 -> {
                donors();
                require(screen.getMenu().getSlot(3).getItem().getCount() == 24, "partial donor retains exact remainder");
                require(AnimatedInventoryApi.normalRenderStack(screen, "cursor", screen.getMenu().getCarried()).getCount() == 16,
                        "cursor keeps original 16 while all 48 incoming items travel");
                TrashSlotValidation.verify(screen, "double-click flight");
                capture("double-click-flight");
                server(mc, player -> {
                    if (player.containerMenu.getCarried().getCount() != 64 || player.containerMenu.getSlot(3).getItem().getCount() != 24)
                        throw new AssertionError("Authoritative double-click quantity mismatch");
                });
                delay = 2;
            }
            case 14 -> {
                server(mc, player -> {
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    player.containerMenu.getSlot(5).set(new ItemStack(Items.GOLD_INGOT, 17));
                    setDestination(player, "STOWED_FIRST");
                    player.containerMenu.broadcastChanges();
                }); delay = 8;
            }
            case 15 -> {
                runtime.invalidate(screen, false); click(screen.getMenu().getSlot(5), ClickType.QUICK_MOVE); delay = 4;
            }
            case 16 -> {
                var stowed = runtime.animations.active().stream().filter(a -> a.transition.type() == TransitionType.STOW).toList();
                require(stowed.size() == 1 && stowed.getFirst().transition.stack().getCount() == 17,
                        "synchronized container transfer reaches BNS stowed grid edge: " + CASES[scenario]);
                require("slot:5".equals(stowed.getFirst().transition.sourceId()), "stowed transfer retains container source");
                TrashSlotValidation.verify(screen, "stowed flight");
                capture("stowed-edge"); delay = 2;
            }
            case 17 -> {
                TrashSlotValidation.clear();
                mc.player.closeContainer();
                require(runtime.animations.active().isEmpty(), "closing clears transfer ownership");
                scenario++; phase = -1; delay = 8;
                if (scenario == CASES.length) return;
            }
        }
        phase++;
    }
    private static void donors() {
        var animations = ClientRuntime.INSTANCE.animations.active().stream().filter(a -> "cursor".equals(a.transition.destinationId())).toList();
        require(animations.size() == 3, "every donor animates: " + CASES[scenario] + " count=" + animations.size());
        require(animations.stream().map(a -> a.transition.stack().getCount()).sorted().toList().equals(List.of(12,16,20)), "donor quantities are 12, 16 and 20");
        require(animations.stream().noneMatch(a -> a.inline), "multiple arrivals use detached model copies");
    }
    private static Slot playerSlot(Minecraft mc, int index) {
        return screen.getMenu().slots.stream().filter(s -> s.container == mc.player.getInventory() && s.getContainerSlot() == index).findFirst().orElseThrow();
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setDestination(ServerPlayer player, String name) throws Exception {
        var attachment = (Supplier<AttachmentType<Object>>)Class.forName("com.cappleapple.bundlednotsiloed.data.ModAttachments").getField("PLAYER_DATA").get(null);
        Object data = player.getData(attachment);
        Object destination = Enum.valueOf((Class)Class.forName("com.cappleapple.bundlednotsiloed.inventory.NewItemDestination"), name);
        data.getClass().getMethod("setNewItemDestination", destination.getClass()).invoke(data, destination);
        Class.forName("com.cappleapple.bundlednotsiloed.network.ModNetwork").getMethod("sendMetadata", ServerPlayer.class).invoke(null, player);
    }
    private static void movement(String from, String to, int count) {
        var animation = ClientRuntime.INSTANCE.animations.active().stream().filter(a -> from.equals(a.transition.sourceId()) && to.equals(a.transition.destinationId())).findFirst();
        require(animation.isPresent(), "synchronized movement " + from + " -> " + to + ": " + CASES[scenario]
                + " active=" + ClientRuntime.INSTANCE.animations.active().stream().map(a -> a.transition.sourceId() + "->" + a.transition.destinationId()).toList());
        require(animation.get().transition.stack().getCount() == count, "synchronized movement retains quantity");
    }
    private static void capture(String name) { ClientValidation.captureSophisticated(screen, "transfer-" + CASES[scenario] + "-" + name); }
    private static void server(Minecraft mc, ServerAction action) {
        UUID id = mc.player.getUUID();
        task = mc.getSingleplayerServer().submit(() -> {
            try { action.run(mc.getSingleplayerServer().getPlayerList().getPlayer(id)); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static void open(ServerPlayer player) throws Exception {
        var attachment = (Supplier<AttachmentType<Object>>)Class.forName("com.cappleapple.bundlednotsiloed.data.ModAttachments").getField("PLAYER_DATA").get(null);
        Object data = player.getData(attachment);
        Object inventory = data.getClass().getMethod("inventory").invoke(data);
        inventory.getClass().getMethod("clear").invoke(inventory);
        Object destination = Enum.valueOf((Class)Class.forName("com.cappleapple.bundlednotsiloed.inventory.NewItemDestination"), "HOTBAR_FIRST");
        data.getClass().getMethod("setNewItemDestination", destination.getClass()).invoke(data, destination);
        Class.forName("com.cappleapple.bundlednotsiloed.network.ModNetwork").getMethod("sendMetadata", ServerPlayer.class).invoke(null, player);
        if (scenario == 0) {
            BlockPos pos = player.blockPosition().offset(2, 0, 0);
            player.serverLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("sophisticatedstorage:chest")).defaultBlockState(), 3);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.serverLevel(), pos));
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> construct("net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu",
                    new Class[]{int.class,Player.class,BlockPos.class}, id,p,pos), Component.literal("Synchronized storage validation")), buffer -> buffer.writeBlockPos(pos));
        } else if (scenario == 1) {
            player.getInventory().setItem(8, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("sophisticatedbackpacks:backpack"))));
            Class<?> contextType = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext");
            String handler = (String)Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider").getField("MAIN_INVENTORY").get(null);
            Object context = Class.forName(contextType.getName() + "$Item").getConstructor(String.class, int.class).newInstance(handler, 8);
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> {
                var menu = construct("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer",
                        new Class[]{int.class,Player.class,contextType}, id,p,context);
                // The newly created backpack (including its generated UUID) must exist on the client before opening.
                try { Class.forName("com.cappleapple.bundlednotsiloed.network.ModNetwork").getMethod("sendInitial", ServerPlayer.class).invoke(null, player); }
                catch (Exception error) { throw new RuntimeException(error); }
                return menu;
            }, Component.literal("Synchronized backpack validation")),
                    buffer -> { try { contextType.getMethod("toBuffer", FriendlyByteBuf.class, Player.class).invoke(context, buffer, player); } catch (Exception e) { throw new RuntimeException(e); } });
        } else {
            SimpleContainer inventorySlots = new SimpleContainer(27);
            player.openMenu(new SimpleMenuProvider((id,inv,p) -> ChestMenu.threeRows(id, inv, inventorySlots), Component.literal("Synchronized chest validation")));
        }
        player.containerMenu.getSlot(0).set(new ItemStack(Items.DIAMOND, 32));
        player.containerMenu.broadcastChanges();
    }
    private static AbstractContainerMenu construct(String name, Class<?>[] types, Object... args) {
        try { return (AbstractContainerMenu)Class.forName(name).getConstructor(types).newInstance(args); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
    private static void click(Slot slot, ClickType type) throws Exception {
        Class<?> declaring = scenario < 2 ? Class.forName("net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase") : AbstractContainerScreen.class;
        Method method = declaring.getDeclaredMethod("slotClicked", Slot.class, int.class, int.class, ClickType.class);
        method.setAccessible(true); method.invoke(screen, slot, slot.index, 0, type);
    }
    private TransferValidation() { }
}
