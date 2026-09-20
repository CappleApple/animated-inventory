package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.api.animation.TransitionType;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.animation.HotbarAnimation;
import com.cappleapple.animatedinventory.mixin.ContainerScreenAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Set;

/** Native client menus in a disposable integrated world; local menu mutations are not network assertions. */
public final class GameplayValidation {
    private static InventoryScreen screen;
    private static int step;
    private static long next, previewHandle;
    private static java.util.concurrent.CompletableFuture<Void> serverWork;
    private static final ClientRuntime RUNTIME = ClientRuntime.INSTANCE;

    public static boolean tick() throws Exception {
        var mc = Minecraft.getInstance();
        if (System.nanoTime() < next || serverWork != null && !serverWork.isDone()) return false;
        if (serverWork != null) { serverWork.join(); serverWork = null; }
        if (step > 0 && Boolean.getBoolean("animatedinventory.captureValidation")) net.minecraft.client.Screenshot.grab(mc, false);
        switch (step++) {
            case 0 -> {
                ClientConfig.MOVE_MS.set(1000); ClientConfig.PICKUP_MS.set(1000); ClientConfig.PLACE_MS.set(1000);
                ClientConfig.QUICK_MS.set(1000); ClientConfig.CRAFT_MS.set(1000);
                screen = new InventoryScreen(mc.player); mc.gui.setScreen(screen);
                clear(); slot(9).set(new ItemStack(Items.APPLE, 13)); rebase();
            }
            case 1 -> {
                require(RUNTIME.canAnimateSlot(screen, slot(9)), "expanded native slot ownership");
                interact(slot(9), 1, ContainerInput.PICKUP);
                require(slot(9).getItem().getCount() == 6 && menu().getCarried().getCount() == 7, "right pickup splits odd stack 6/7 immediately");
                animated("split pickup");
            }
            case 2 -> {
                interact(slot(10), 1, ContainerInput.PICKUP);
                require(slot(10).getItem().getCount() == 1 && menu().getCarried().getCount() == 6, "right placement moves one item immediately");
                animated("single placement");
            }
            case 3 -> {
                clear(); slot(9).set(new ItemStack(Items.APPLE, 3)); menu().setCarried(new ItemStack(Items.IRON_INGOT, 4)); rebase();
                interact(slot(9), 0, ContainerInput.PICKUP);
                require(slot(9).getItem().is(Items.IRON_INGOT) && slot(9).getItem().getCount() == 4
                    && menu().getCarried().is(Items.APPLE) && menu().getCarried().getCount() == 3, "cursor swap preserves both identities and counts");
                animated("cursor swap");
            }
            case 4 -> {
                clear(); slot(9).set(new ItemStack(Items.APPLE, 8)); rebase();
                interact(slot(9), 0, ContainerInput.QUICK_MOVE);
                require(slot(9).getItem().isEmpty() && inventory(0).getCount() == 8, "quick move transfers inventory stack into hotbar");
                animated("quick move");
            }
            case 5 -> {
                clear(); slot(9).set(new ItemStack(Items.DIAMOND, 3)); mc.player.getInventory().setItem(0, new ItemStack(Items.STONE, 5)); rebase();
                interact(slot(9), 0, ContainerInput.SWAP);
                require(slot(9).getItem().is(Items.STONE) && slot(9).getItem().getCount() == 5
                    && inventory(0).is(Items.DIAMOND) && inventory(0).getCount() == 3, "hotbar-key swap preserves both stacks");
                animated("hotbar swap");
            }
            case 6 -> {
                clear(); slot(10).set(new ItemStack(Items.APPLE, 8)); menu().setCarried(new ItemStack(Items.APPLE, 12)); rebase();
                interact(slot(10), 1, ContainerInput.PICKUP);
                var arriving = RUNTIME.animations.destination(VanillaInventoryProvider.id(slot(10)), System.nanoTime());
                require(arriving != null, "preview fixture has an active destination ownership claim"); previewHandle = arriving.handle;
                field(AbstractContainerScreen.class, "isQuickCrafting").setBoolean(screen, true);
                field(AbstractContainerScreen.class, "quickCraftingType").setInt(screen, 0);
                field(AbstractContainerScreen.class, "quickCraftingRemainder").setInt(screen, 1);
                quickSlots().add(slot(9)); quickSlots().add(slot(10));
                mouse(slot(10));
            }
            case 7 -> {
                require(slot(9).getItem().isEmpty() && slot(10).getItem().getCount() == 9 && menu().getCarried().getCount() == 11,
                    "rendered quick-craft previews leave real slots and cursor unchanged");
                require(RUNTIME.animations.active().stream().anyMatch(a -> a.handle == previewHandle), "quick-craft preview rendering preserves existing destination claim");
                field(AbstractContainerScreen.class, "isQuickCrafting").setBoolean(screen, false); quickSlots().clear();
                interact(null, AbstractContainerMenu.getQuickcraftMask(0, 0), ContainerInput.QUICK_CRAFT);
                interact(slot(9), AbstractContainerMenu.getQuickcraftMask(1, 0), ContainerInput.QUICK_CRAFT);
                interact(slot(10), AbstractContainerMenu.getQuickcraftMask(1, 0), ContainerInput.QUICK_CRAFT);
                interact(null, AbstractContainerMenu.getQuickcraftMask(2, 0), ContainerInput.QUICK_CRAFT);
                require(slot(9).getItem().getCount() == 5 && slot(10).getItem().getCount() == 14 && menu().getCarried().getCount() == 1,
                    "native quick-craft commit distributes 5/5 and preserves remainder");
                animated("quick-craft distribution");
            }
            case 8 -> {
                clear(); craft(); rebase(); interact(slot(0), 0, ContainerInput.PICKUP);
                require(menu().getCarried().is(Items.CRAFTING_TABLE) && menu().getCraftSlots().isEmpty(), "native crafting consumes four inputs and takes one result");
                require(RUNTIME.animations.active().stream().filter(a -> a.transition.type() == TransitionType.CRAFT).count() == 4,
                    "native result take creates four ingredient-to-product animations");
            }
            case 9 -> {
                clear(); craft(); rebase(); interact(slot(0), 0, ContainerInput.QUICK_MOVE);
                require(menu().getCraftSlots().isEmpty() && menu().getCarried().isEmpty()
                    && mc.player.getInventory().countItem(Items.CRAFTING_TABLE) == 1, "shift crafting sends product to inventory with immediate counts");
                require(RUNTIME.animations.active().stream().anyMatch(a -> a.transition.type() == TransitionType.CRAFT), "shift crafting animates ingredient consumption");
            }
            case 10 -> {
                ClientConfig.ENABLED.set(false); RUNTIME.tick(); clear(); slot(9).set(new ItemStack(Items.APPLE, 6)); rebase();
                interact(slot(9), 0, ContainerInput.PICKUP);
                require(menu().getCarried().getCount() == 6 && slot(9).getItem().isEmpty(), "disabled mode leaves native pickup functional");
                require(RUNTIME.animations.active().isEmpty(), "disabled mode creates no animation or suppression claims");
            }
            case 11 -> {
                ClientConfig.ENABLED.set(true); ClientConfig.REDUCE_MOTION.set(true); RUNTIME.tick();
                clear(); slot(9).set(new ItemStack(Items.APPLE, 6)); rebase(); interact(slot(9), 0, ContainerInput.PICKUP);
                animated("reduced-motion pickup");
                require(RUNTIME.animations.active().stream().allMatch(a -> a.duration <= 60_000_000L
                    && a.source.equals(a.destination)), "reduced motion uses stationary fades at most 60ms");
            }
            case 12 -> {
                ClientConfig.REDUCE_MOTION.set(false); clear(); slot(9).set(new ItemStack(Items.APPLE, 12)); slot(10).set(new ItemStack(Items.DIAMOND, 3)); rebase();
                ClientConfig.HOVER_SCALE.set(1.5); ClientConfig.HOVER_Z.set(10.0); mouse(slot(9));
            }
            case 13 -> {
                var a = (ContainerScreenAccess) screen;
                double scale = RUNTIME.emphasis.scale(VanillaInventoryProvider.id(slot(9)), true, System.nanoTime());
                require(scale > 1.2 && slot(9).getItem().getCount() == 12, "hover enlarges native item without changing stack state");
                require(RUNTIME.enabled(), "elevated hover and behind-item highlight render without runtime failure");
                ClientConfig.HOVER_Z.set(0.0);
            }
            case 14 -> {
                require(RUNTIME.enabled(), "zero hover depth renders without runtime failure");
                ClientConfig.HOVER_SCALE.set(1.08); ClientConfig.HOVER_Z.set(10.0);
                mc.gui.setScreen(null); mc.player.getInventory().setSelectedSlot(0); HotbarAnimation.reset();
                ClientConfig.HOTBAR_MS.set(1000);
            }
            case 15 -> {
                mc.player.getInventory().setSelectedSlot(4);
                long now = System.nanoTime(); int width = mc.getWindow().getGuiScaledWidth();
                int left = width / 2 - 91 - 1;
                double visual = HotbarAnimation.position(left + 80, width, now);
                require(visual < left + 80, "hotbar selector starts between old and new logical selection");
            }
            case 16 -> {
                require(RUNTIME.enabled(), "animated hotbar selection rendered in-world");
                ClientConfig.REDUCE_MOTION.set(true);
                int width = mc.getWindow().getGuiScaledWidth(); int logical = width / 2 - 91 - 1 + 80;
                require(HotbarAnimation.position(logical, width, System.nanoTime()) == logical, "reduced motion snaps hotbar selector to logical slot");
                ClientConfig.REDUCE_MOTION.set(false);
                screen = new InventoryScreen(mc.player); mc.gui.setScreen(screen);
            }
            case 17 -> {
                long owner = RUNTIME.owner(screen); screen.init(screen.width + 16, screen.height);
                require(RUNTIME.owner(screen) != owner && RUNTIME.animations.active().isEmpty(), "resize invalidates presentation ownership and active claims");
                screen.init(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
            case 18 -> {
                require(RUNTIME.enabled(), "inventory rendered after resize");
                clear(); rebase();
                onServer(player -> {
                    resetServer(player);
                    player.getInventory().setItem(9, new ItemStack(Items.APPLE, 20));
                    player.inventoryMenu.broadcastFullState();
                });
            }
            case 19 -> {
                require(slot(9).getItem().getCount() == 20, "integrated server inventory seed synchronized before quick move");
                realClick(slot(9), true);
                require(slot(9).getItem().isEmpty() && inventory(0).getCount() == 20, "real shift-click predicts quick-move counts immediately");
                animated("real shift-click");
            }
            case 20 -> onServer(player -> {
                serverRequire(player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(0).getCount() == 20,
                    "integrated server confirmed real shift-click result");
                resetServer(player);
                player.inventoryMenu.getInputGridSlots().forEach(slot -> slot.set(new ItemStack(Items.OAK_PLANKS)));
                serverRequire(player.inventoryMenu.getResultSlot().getItem().is(Items.CRAFTING_TABLE), "server recipe manager computed crafting-table output");
                player.inventoryMenu.broadcastFullState();
            });
            case 21 -> {
                require(slot(0).getItem().is(Items.CRAFTING_TABLE), "real crafting output synchronized from server recipe");
                realClick(slot(0), false);
                require(menu().getCarried().is(Items.CRAFTING_TABLE) && menu().getCraftSlots().isEmpty(), "real result-slot click immediately consumes ingredients");
                require(RUNTIME.animations.active().stream().filter(a -> a.transition.type() == TransitionType.CRAFT).count() == 4,
                    "real recipe pickup routes four consumed ingredients into animations");
            }
            case 22 -> onServer(player -> {
                serverRequire(player.inventoryMenu.getCarried().is(Items.CRAFTING_TABLE) && player.inventoryMenu.getCraftSlots().isEmpty(),
                    "integrated server confirmed real crafting pickup");
                resetServer(player);
                player.inventoryMenu.getInputGridSlots().forEach(slot -> slot.set(new ItemStack(Items.OAK_PLANKS)));
                player.inventoryMenu.broadcastFullState();
            });
            case 23 -> {
                require(slot(0).getItem().is(Items.CRAFTING_TABLE), "second real recipe synchronized for shift crafting");
                realClick(slot(0), true);
                require(menu().getCraftSlots().isEmpty() && menu().getCarried().isEmpty()
                    && mc.player.getInventory().countItem(Items.CRAFTING_TABLE) == 1, "real shift-click crafting predicts inventory result");
                require(RUNTIME.animations.active().stream().anyMatch(a -> a.transition.type() == TransitionType.CRAFT), "real shift crafting creates ingredient animations");
            }
            case 24 -> onServer(player -> serverRequire(player.inventoryMenu.getCraftSlots().isEmpty()
                && player.getInventory().countItem(Items.CRAFTING_TABLE) == 1, "integrated server confirmed real shift crafting"));
            case 25 -> { pass("expanded gameplay complete"); return true; }
            default -> { return true; }
        }
        next = System.nanoTime() + (step >= 19 ? 450_000_000L : 180_000_000L);
        return false;
    }
    private static void realClick(Slot slot, boolean shift) throws Exception {
        mouse(slot);
        var a = (ContainerScreenAccess) screen;
        var event = new net.minecraft.client.input.MouseButtonEvent(a.animatedinventory$left() + slot.x + 8,
            a.animatedinventory$top() + slot.y + 8, new net.minecraft.client.input.MouseButtonInfo(
                com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, shift ? com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT : 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static void onServer(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        var mc = Minecraft.getInstance(); var id = mc.player.getUUID();
        serverWork = java.util.concurrent.CompletableFuture.runAsync(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)), mc.getSingleplayerServer());
    }
    private static void resetServer(net.minecraft.server.level.ServerPlayer player) {
        for (Slot slot : player.inventoryMenu.slots) slot.set(ItemStack.EMPTY);
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
    }
    private static void serverRequire(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        try { pass(message); } catch (Exception error) { throw new RuntimeException(error); }
    }
    private static InventoryMenu menu() { return screen.getMenu(); }
    private static Slot slot(int index) { return menu().slots.get(index); }
    private static ItemStack inventory(int index) { return Minecraft.getInstance().player.getInventory().getItem(index); }
    private static void clear() {
        for (Slot slot : menu().slots) slot.set(ItemStack.EMPTY);
        menu().setCarried(ItemStack.EMPTY);
    }
    private static void rebase() { RUNTIME.invalidate(screen, false); }
    private static void craft() {
        menu().getInputGridSlots().forEach(slot -> slot.set(new ItemStack(Items.OAK_PLANKS)));
        // Result is normally server-synchronized. Native ResultSlot.onTake still performs real ingredient consumption.
        menu().getResultSlot().set(new ItemStack(Items.CRAFTING_TABLE));
    }
    private static void interact(Slot slot, int button, ContainerInput input) {
        RUNTIME.beforeInteraction(screen, slot, input);
        menu().clicked(slot == null ? -999 : slot.index, button, input, Minecraft.getInstance().player);
        RUNTIME.afterInteraction();
    }
    @SuppressWarnings("unchecked") private static Set<Slot> quickSlots() throws Exception {
        return (Set<Slot>) field(AbstractContainerScreen.class, "quickCraftSlots").get(screen);
    }
    private static void mouse(Slot slot) throws Exception {
        var mc = Minecraft.getInstance(); var access = (ContainerScreenAccess) screen;
        double x = access.animatedinventory$left() + slot.x + 8;
        double y = access.animatedinventory$top() + slot.y + 8;
        field(mc.mouseHandler.getClass(), "xpos").setDouble(mc.mouseHandler, x * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth());
        field(mc.mouseHandler.getClass(), "ypos").setDouble(mc.mouseHandler, y * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight());
    }
    private static Field field(Class<?> type, String name) throws Exception { var f = type.getDeclaredField(name); f.setAccessible(true); return f; }
    private static void animated(String name) throws Exception { require(!RUNTIME.animations.active().isEmpty(), name + " creates animation"); }
    private static void require(boolean value, String message) throws Exception { if (!value) throw new AssertionError(message); pass(message); }
    private static void pass(String message) throws Exception { Files.writeString(Path.of("validation.txt"), "PASS " + message + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
    private GameplayValidation() { }
}