package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid = "animatedinventory", value = Dist.CLIENT)
public final class ClientValidation {
    private static boolean booted, done;
    private static int ticks, step;
    private static final long START = System.nanoTime();
    private static final List<String> RESULTS = new ArrayList<>();
    private static ContainerScreen screen;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Boolean.getBoolean("animatedinventory.clientValidation") || done) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.mouseHandler.releaseMouse();
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            require(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(), GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE, "validation window stays hidden");
            if (System.nanoTime() - START > 240_000_000_000L) throw new IllegalStateException("Client validation timed out");
            if (!booted && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                if (Boolean.getBoolean("animatedinventory.sophisticatedValidation")) {
                    require(com.cappleapple.animatedinventory.client.compat.sophisticated.SophisticatedView.class.isAssignableFrom(
                        Class.forName("net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase")), "Sophisticated screen adapter applies");
                    require(com.cappleapple.animatedinventory.client.compat.sophisticated.SophisticatedMenu.class.isAssignableFrom(
                        Class.forName("net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase")), "Sophisticated menu adapter applies");
                }
                booted = true; mc.options.pauseOnLostFocus = false; mc.options.framerateLimit().set(60);
                mc.createWorldOpenFlows().createFreshLevel("animatedinventory-validation-" + System.currentTimeMillis(),
                    new LevelSettings("Animated Inventory validation", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (mc.player == null || mc.level == null || mc.getOverlay() != null || ++ticks < 20) return;
            ticks = 0;
            ClientRuntime runtime = ClientRuntime.INSTANCE;
            switch (step++) {
                case 0 -> {
                    ClientConfig.MOVE_MS.set(1000); ClientConfig.PICKUP_MS.set(1000); ClientConfig.QUICK_MS.set(1000);
                    mc.player.getInventory().clearContent();
                    ChestMenu menu = ChestMenu.threeRows(81, mc.player.getInventory());
                    screen = new ContainerScreen(menu, mc.player.getInventory(), Component.literal("Forge port validation"));
                    mc.player.containerMenu = menu; mc.setScreen(screen);
                }
                case 1 -> {
                    var menu = screen.getMenu();
                    var items = NonNullList.withSize(menu.slots.size(), ItemStack.EMPTY);
                    items.set(0, new ItemStack(Items.DIAMOND, 32));
                    items.set(1, new ItemStack(Items.DIAMOND, 52));
                    mc.getConnection().handleContainerContent(new ClientboundContainerSetContentPacket(menu.containerId, 1, items, ItemStack.EMPTY));
                    runtime.afterInteraction();
                    require(runtime.animations.active().isEmpty(), "initial synchronized contents do not animate arrivals");
                    require(runtime.snapshot().items().get("slot:0").stack().getCount() == 32, "initial snapshot captures synchronized stack");
                }
                case 2 -> {
                    var menu = screen.getMenu();
                    runtime.beforeInteraction(screen, menu.getSlot(0), ClickType.PICKUP);
                    menu.clicked(0, 0, ClickType.PICKUP, mc.player); runtime.afterInteraction();
                    require(menu.getCarried().getCount() == 32, "pickup state remains immediate");
                    require(runtime.animations.active().stream().anyMatch(a -> "cursor".equals(a.transition.destinationId())), "pickup creates cursor animation");
                }
                case 3 -> {
                    var menu = screen.getMenu();
                    runtime.beforeInteraction(screen, menu.getSlot(1), ClickType.PICKUP);
                    menu.clicked(1, 0, ClickType.PICKUP, mc.player); runtime.afterInteraction();
                    require(menu.getSlot(1).getItem().getCount() == 64 && menu.getCarried().getCount() == 20, "partial merge preserves vanilla quantities");
                    require(runtime.enabled(), "native slot rendering and compositor remain enabled");
                }
                case 4 -> {
                    var menu = screen.getMenu(); menu.setCarried(ItemStack.EMPTY); runtime.invalidate(screen, false);
                    menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT, 32)); runtime.invalidate(screen, false);
                    runtime.beforeInteraction(screen, menu.getSlot(0), ClickType.QUICK_MOVE);
                    menu.clicked(0, 0, ClickType.QUICK_MOVE, mc.player); runtime.afterInteraction();
                    require(menu.getSlot(0).getItem().isEmpty(), "quick move preserves vanilla transfer");
                    require(!runtime.animations.active().isEmpty(), "quick move creates visual transition");
                }
                case 5 -> {
                    long owner = runtime.owner(screen);
                    screen.resize(mc, screen.width, screen.height);
                    require(owner != runtime.owner(screen), "resize renews animation ownership");
                    require(runtime.animations.active().isEmpty(), "resize clears stale visual ownership");
                    mc.setScreen(new ClientConfigScreen(screen));
                }
                case 6 -> {
                    require(mc.screen instanceof ClientConfigScreen, "Forge config screen opens and renders");
                    finish(mc, null);
                }
            }
        } catch (Throwable error) { finish(mc, error); }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
        String result = "PASS " + message; if (!RESULTS.contains(result)) RESULTS.add(result);
    }
    private static void finish(Minecraft mc, Throwable error) {
        done = true;
        if (error != null) { RESULTS.add("FAIL " + error); error.printStackTrace(); }
        else RESULTS.add("PASS validation completed");
        try { Files.write(Path.of("client-validation.txt"), RESULTS); }
        catch (Exception writeError) { throw new IllegalStateException(writeError); }
        mc.stop();
    }
}
