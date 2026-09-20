package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.animation.HotbarAnimation;
import com.cappleapple.animatedinventory.client.config.AnimationConfigScreen;
import com.cappleapple.animatedinventory.mixin.ContainerScreenAccess;
import com.cappleapple.animatedinventory.validation.mixin.InputAccess;
import com.cappleapple.animatedinventory.validation.mixin.SlotClickAccess;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Real client prediction and integrated-server reconciliation in a disposable world. */
public final class ClientSmoke implements ClientModInitializer {
    private static long started;
    private static int stage, age, scenario, phase;
    private static InventoryScreen screen;
    private static CompletableFuture<?> seed;
    private static String capture;
    public static boolean shiftInput;
    private static boolean previewRendered, highlightRendered, hotbarRendered;
    private static int highlights;
    private static final List<String> results = new ArrayList<>();
    private static final String[] NAMES = {"pickup", "placement", "split", "single-placement", "partial-merge", "swap", "quick-move", "hotbar-swap", "crafting", "disabled", "reduced-motion", "drag-preview"};
    @Override public void onInitializeClient() { started = System.nanoTime(); }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (started == 0 || stage == 99) return;
        try {
            mc.mouseHandler.releaseMouse();
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            require(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(), GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE, "native window stays hidden");
            if (System.nanoTime() - started > 420_000_000_000L) throw new AssertionError("Client validation timeout at " + stage + "/" + scenario + "/" + phase);
            if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("animatedinventory-validation-" + System.currentTimeMillis(),
                    new LevelSettings("Animated Inventory validation", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                stage = 1; age = 0; return;
            }
            if (mc.player == null || mc.level == null || mc.getOverlay() != null || ++age < 6) return;
            age = 0;
            var runtime = ClientRuntime.INSTANCE;
            if (stage == 1) {
                ClientConfig.MOVE_MS.set(1000); ClientConfig.PICKUP_MS.set(1000); ClientConfig.PLACE_MS.set(1000); ClientConfig.QUICK_MS.set(1000); ClientConfig.CRAFT_MS.set(1000);
                ClientConfig.OPEN_MS.set(1000); ClientConfig.CLOSE_MS.set(1000);
                screen = new InventoryScreen(mc.player); mc.setScreen(screen);
                pass("integrated world and real inventory screen opened"); stage = 2;
            } else if (stage == 2) {
                if (phase == 0) {
                    final int selected = scenario;
                    seed = mc.getSingleplayerServer().submit(() -> {
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        var menu = player.inventoryMenu;
                        player.getInventory().clearContent();
                        for (int i = 1; i <= 4; i++) menu.getSlot(i).set(ItemStack.EMPTY);
                        menu.setCarried(ItemStack.EMPTY);
                        if (selected == 0 || selected == 9 || selected == 10) menu.getSlot(9).set(new ItemStack(Items.APPLE, 12));
                        if (selected == 1 || selected == 11) menu.setCarried(new ItemStack(Items.APPLE, 12));
                        if (selected == 2) menu.getSlot(9).set(new ItemStack(Items.APPLE, 13));
                        if (selected == 3) { menu.getSlot(9).set(new ItemStack(Items.APPLE, 5)); menu.setCarried(new ItemStack(Items.APPLE, 12)); }
                        if (selected == 4) { menu.getSlot(9).set(new ItemStack(Items.APPLE, 60)); menu.setCarried(new ItemStack(Items.APPLE, 12)); }
                        if (selected == 5) { menu.getSlot(9).set(new ItemStack(Items.DIAMOND, 4)); menu.setCarried(new ItemStack(Items.APPLE, 12)); }
                        if (selected == 6) menu.getSlot(9).set(new ItemStack(Items.IRON_INGOT, 32));
                        if (selected == 7) { menu.getSlot(9).set(new ItemStack(Items.APPLE, 12)); menu.getSlot(36).set(new ItemStack(Items.DIAMOND, 4)); }
                        if (selected == 8) menu.getSlot(1).set(new ItemStack(Items.OAK_LOG, 1));
                        menu.broadcastChanges();
                    });
                    phase = 1; return;
                }
                if (phase == 1) { if (!seed.isDone()) return; seed.join(); phase = 2; return; }
                if (phase == 2) {
                    require(runtime.canAnimateSlot(screen, screen.getMenu().getSlot(9)), "native slot rendering owns inventory items");
                    runtime.invalidate(screen, false);
                    ClientConfig.ENABLED.set(scenario != 9); ClientConfig.REDUCE_MOTION.set(scenario == 10);
                    if (scenario == 6) { shiftInput = true; try { click(9, 0); } finally { shiftInput = false; } }
                    else if (scenario == 7) { pointer(9); screen.keyPressed(GLFW.GLFW_KEY_1, 0, 0); }
                    else if (scenario == 11) { press(9, 0); drag(9); drag(10); }
                    else click(scenario == 8 ? 0 : 9, scenario == 2 || scenario == 3 ? 1 : 0);
                    if (scenario == 11) {
                        require(screen.getMenu().getCarried().getCount() == 12 && screen.getMenu().getSlot(9).getItem().isEmpty(), "drag preview preserves logical inventory until release");
                        capture = "12-drag-preview"; phase = 3; return;
                    }
                    checkCounts("immediate");
                    if (scenario == 9) require(runtime.animations.active().isEmpty(), "disabled mode creates no animation");
                    else {
                        require(!runtime.animations.active().isEmpty(), NAMES[scenario] + " creates a visual transition");
                        if (scenario == 8) require(runtime.animations.active().stream().anyMatch(a -> a.transition.type() == com.cappleapple.animatedinventory.api.animation.TransitionType.CRAFT), "crafting consumes ingredients through the result-slot hook");
                        if (scenario == 10) require(runtime.animations.active().stream().allMatch(a -> a.duration <= 60_000_000L && a.source.equals(a.destination)), "reduced motion uses stationary fades at most 60 ms");
                    }
                    capture = String.format("%02d-%s", scenario + 1, NAMES[scenario]); phase = 4;
                } else if (phase == 3) {
                    require(previewRendered, "native drag preview rendered six items while logical destination was empty");
                    release(10, 0); checkCounts("immediate"); phase = 4;
                } else if (phase == 4) {
                    checkCounts("server synchronized");
                    seed = mc.getSingleplayerServer().submit(() -> {
                        var serverPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        try { checkCounts("authoritative integrated server", serverPlayer.inventoryMenu); }
                        catch (Exception error) { throw new CompletionException(error); }
                    });
                    phase = 5;
                } else if (phase == 5) {
                    if (!seed.isDone()) return;
                    seed.join();
                    ClientConfig.ENABLED.set(true); ClientConfig.REDUCE_MOTION.set(false);
                    if (++scenario == NAMES.length) { stage = 3; phase = 0; } else phase = 0;
                }
            } else if (stage == 3) {
                require(runtime.enabled(), "all inventory frames rendered without runtime failure");
                capture = "13-hover-highlight"; pointer(9);
                long now = System.nanoTime(); HotbarAnimation.reset();
                HotbarAnimation.position(0, screen.width, now); HotbarAnimation.position(160, screen.width, now + 1);
                double midway = HotbarAnimation.position(160, screen.width, now + 30_000_000L);
                require(midway > 0 && midway < 160, "hotbar selector interpolates between logical positions");
                ClientConfig.REDUCE_MOTION.set(true);
                require(HotbarAnimation.position(160, screen.width, now + 31_000_000L) == 160, "reduced motion snaps hotbar selection"); ClientConfig.REDUCE_MOTION.set(false);
                mc.player.getInventory().selected = 4; stage = 4;
            } else if (stage == 4) {
                require(runtime.emphasis.scale("slot:9", true, System.nanoTime()) > 1, "hover emphasis scales the highlighted item");
                require(highlightRendered, "hover highlight renders exactly once per frame");
                require(hotbarRendered, "native hotbar selection sprite receives animated translation");
                mc.setScreen(null); capture = "16-hotbar-close"; require(texture("exit") != null, "screen close retains rendered GPU image"); stage = 5;
            } else if (stage == 5) {
                require(runtime.enabled(), "screen close image rendered without runtime failure");
                screen = new InventoryScreen(mc.player); mc.setScreen(screen); capture = "17-inventory-reopen"; stage = 6;
            } else if (stage == 6) {
                require(texture("live") != null, "screen reopen captures the live inventory");
                ((InputAccess)mc.keyboardHandler).animatedinventory$key(mc.getWindow().getWindow(), GLFW.GLFW_KEY_F8, 0, GLFW.GLFW_PRESS, 0);
                require(mc.screen instanceof AnimationConfigScreen, "F8 opens the configuration editor"); stage = 7;
            } else if (stage == 7) {
                if (ConfigValidation.tick()) stage = 8;
            } else if (stage == 8) {
                pass("complete"); stage = 99; mc.stop();
            }
        } catch (Throwable error) {
            error.printStackTrace();
            try { Files.writeString(Path.of("validation.txt"), "FAIL " + error + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception ignored) { }
            stage = 99; mc.stop();
        }
    }
    private static Object texture(String name) throws Exception { var field = ClientRuntime.INSTANCE.screens.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(ClientRuntime.INSTANCE.screens); }
    private static void checkCounts(String timing) throws Exception { checkCounts(timing, screen.getMenu()); }
    private static void checkCounts(String timing, AbstractContainerMenu menu) throws Exception {
        var slot = menu.getSlot(9).getItem(); var carried = menu.getCarried();
        boolean correct = switch (scenario) {
            case 0, 9, 10 -> slot.isEmpty() && carried.is(Items.APPLE) && carried.getCount() == 12;
            case 1 -> slot.is(Items.APPLE) && slot.getCount() == 12 && carried.isEmpty();
            case 2 -> slot.getCount() == 6 && carried.getCount() == 7;
            case 3 -> slot.getCount() == 6 && carried.getCount() == 11;
            case 4 -> slot.getCount() == 64 && carried.getCount() == 8;
            case 5 -> slot.is(Items.APPLE) && slot.getCount() == 12 && carried.is(Items.DIAMOND) && carried.getCount() == 4;
            case 6 -> slot.isEmpty() && java.util.stream.IntStream.range(36, 45).map(i -> menu.getSlot(i).getItem().is(Items.IRON_INGOT) ? menu.getSlot(i).getItem().getCount() : 0).sum() == 32;
            case 7 -> slot.is(Items.DIAMOND) && slot.getCount() == 4 && menu.getSlot(36).getItem().is(Items.APPLE) && menu.getSlot(36).getItem().getCount() == 12;
            case 8 -> menu.getSlot(1).getItem().isEmpty() && carried.is(Items.OAK_PLANKS) && carried.getCount() == 4;
            case 11 -> slot.getCount() == 6 && menu.getSlot(10).getItem().getCount() == 6 && carried.isEmpty();
            default -> false;
        };
        require(correct, NAMES[scenario] + " " + timing + " quantities: slot=" + slot + ", cursor=" + carried);
    }
    private static void pointer(int index) {
        var mc = Minecraft.getInstance(); var slot = screen.getMenu().getSlot(index); var access = (ContainerScreenAccess)screen;
        double x = access.animatedinventory$left() + slot.x + 8, y = access.animatedinventory$top() + slot.y + 8;
        ((com.cappleapple.animatedinventory.validation.mixin.MousePositionAccess)mc.mouseHandler).animatedinventory$x(x * mc.getWindow().getScreenWidth() / screen.width);
        ((com.cappleapple.animatedinventory.validation.mixin.MousePositionAccess)mc.mouseHandler).animatedinventory$y(y * mc.getWindow().getScreenHeight() / screen.height);
    }
    private static double x(int i) { return ((ContainerScreenAccess)screen).animatedinventory$left() + screen.getMenu().getSlot(i).x + 8; }
    private static double y(int i) { return ((ContainerScreenAccess)screen).animatedinventory$top() + screen.getMenu().getSlot(i).y + 8; }
    private static void press(int i, int b) { pointer(i); screen.mouseClicked(x(i), y(i), b); }
    private static void release(int i, int b) { screen.mouseReleased(x(i), y(i), b); }
    private static void drag(int i) { pointer(i); screen.mouseDragged(x(i), y(i), 0, 18, 0); }
    private static void click(int i, int b) { press(i, b); release(i, b); }
    static void capture(String name) { capture = name; }
    public static void hotbar(float translation) { if (stage == 4 && Math.abs(translation) > .01) hotbarRendered = true; }
    public static void decoration(ItemStack stack) { if (scenario == 11 && phase == 3 && stack.is(Items.APPLE) && stack.getCount() == 6) previewRendered = true; }
    public static void highlight() { highlights++; }
    public static void renderHud(GuiGraphics graphics) { if (Minecraft.getInstance().screen == null) render(graphics); }
    public static void render(GuiGraphics graphics) {
        if (stage == 4 && highlights == 1) highlightRendered = true;
        highlights = 0;
        if (capture == null) return;
        graphics.flush();
        var mc = Minecraft.getInstance();
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, capture + ".png", mc.getMainRenderTarget(), message -> {});
        capture = null;
    }
    static void require(boolean condition, String message) throws Exception { if (!condition) throw new AssertionError(message); pass(message); }
    static synchronized void pass(String message) throws Exception { String line = "PASS " + message; if (results.contains(line)) return; results.add(line); Files.writeString(Path.of("validation.txt"), line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
}
