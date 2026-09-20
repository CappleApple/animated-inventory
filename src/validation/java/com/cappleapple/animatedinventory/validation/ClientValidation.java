package com.cappleapple.animatedinventory.validation;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.mixin.ContainerScreenAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.*;
/** Exercises a disposable local world; no production classes or artifacts include this fixture. */
public final class ClientValidation {
    private static final long STARTED = System.nanoTime();
    private static int stage, age, heartbeat;
    private static InventoryScreen screen;
    private static Slot source, target;
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (stage == 99) return;
        if (stage == 0 && ++heartbeat % 200 == 0) System.out.println("Validation waiting: screen=" + mc.gui.screen() + ", overlay=" + mc.gui.overlay());
        try {
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            mc.mouseHandler.releaseMouse();
            if (System.nanoTime() - STARTED > 480_000_000_000L) throw new AssertionError("Client validation timeout");
            if (stage == 0 && mc.gui.screen() instanceof TitleScreen && mc.gui.overlay() == null) {
                if (Boolean.getBoolean("animatedinventory.productionValidation")) {
                    String origin = ClientRuntime.class.getProtectionDomain().getCodeSource().getLocation().toString();
                    require(origin.endsWith(".jar"), "production classes loaded from release JAR: " + origin);
                }
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("animatedinventory-port-" + System.currentTimeMillis(),
                    new LevelSettings("Animated Inventory port validation", GameType.SURVIVAL,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42, false, false),
                    registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    new TitleScreen());
                stage = 1; age = 0; return;
            }
            if (mc.level == null || mc.player == null || mc.gui.overlay() != null) return;
            if (stage == 7) { if (GameplayValidation.tick()) stage = 8; return; }
            if (stage == 8) { if (ConfigValidation.tick()) { pass("complete"); stage = 99; mc.stop(); } return; }
            if (++age < 10) return;
            age = 0;
            if (Boolean.getBoolean("animatedinventory.captureValidation") && stage >= 2 && stage <= 5) net.minecraft.client.Screenshot.grab(mc, false);
            var runtime = ClientRuntime.INSTANCE;
            if (stage == 1) {
                ClientConfig.MOVE_MS.set(1000); ClientConfig.PICKUP_MS.set(1000); ClientConfig.PLACE_MS.set(1000);
                ClientConfig.OPEN_MS.set(1000); ClientConfig.CLOSE_MS.set(1000);
                mc.player.getInventory().setItem(9, new ItemStack(Items.APPLE, 12));
                mc.player.getInventory().setItem(10, new ItemStack(Items.APPLE, 60));
                screen = new InventoryScreen(mc.player); mc.gui.setScreen(screen);
                source = screen.getMenu().slots.stream().filter(s -> s.container == mc.player.getInventory() && s.getContainerSlot() == 9).findFirst().orElseThrow();
                target = screen.getMenu().slots.stream().filter(s -> s.container == mc.player.getInventory() && s.getContainerSlot() == 10).findFirst().orElseThrow();
                pass("integrated server and inventory screen opened"); stage = 2;
            } else if (stage == 2) {
                require(runtime.canAnimateSlot(screen, source), "native slot ownership observed");
                // Rebase after the integrated server's initial synchronization, then use the real screen click path.
                source.set(new ItemStack(Items.APPLE, 12)); target.set(new ItemStack(Items.APPLE, 60)); screen.getMenu().setCarried(ItemStack.EMPTY);
                runtime.invalidate(screen, false); click(source);
                require(screen.getMenu().getCarried().getCount() == 12, "pickup state remains immediate");
                require(!runtime.animations.active().isEmpty(), "pickup animation created by actual slot click");
                stage = 3;
            } else if (stage == 3) {
                require(runtime.enabled(), "pickup frame rendered without failure");
                // Isolated menu mutation avoids server reconciliation racing the next assertion.
                screen.getMenu().setCarried(new ItemStack(Items.APPLE, 12)); target.set(new ItemStack(Items.APPLE, 60));
                runtime.invalidate(screen, false);
                runtime.beforeInteraction(screen, target, ContainerInput.PICKUP);
                screen.getMenu().clicked(target.index, 0, ContainerInput.PICKUP, mc.player);
                runtime.afterInteraction();
                require(target.getItem().getCount() == 64 && screen.getMenu().getCarried().getCount() == 8, "partial merge preserves counts");
                require(!runtime.animations.active().isEmpty(), "merge animation created");
                stage = 4;
            } else if (stage == 4) {
                require(runtime.enabled(), "merge and item-opacity frames rendered without failure");
                mc.gui.setScreen(null); stage = 5;
            } else if (stage == 5) {
                require(runtime.enabled(), "screen close replay rendered without failure");
                mc.gui.setScreen(new InventoryScreen(mc.player)); stage = 6;
            } else if (stage == 6) {
                require(runtime.enabled(), "screen reopen rendered without failure");
                stage = 7;
            }
        } catch (Throwable error) {
            error.printStackTrace();
            try { Files.writeString(Path.of("validation.txt"), "FAIL " + error + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception ignored) {}
            stage = 99; mc.stop();
        }
    }
    private static void click(Slot slot) {
        var a = (ContainerScreenAccess)screen;
        var event = new MouseButtonEvent(a.animatedinventory$left() + slot.x + 8, a.animatedinventory$top() + slot.y + 8, new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static void require(boolean value, String message) throws Exception { if (!value) throw new AssertionError(message); pass(message); }
    private static void pass(String message) throws Exception { Files.writeString(Path.of("validation.txt"), "PASS " + message + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
}
