package com.cappleapple.animatedinventory.validation;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.config.AnimationConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.nio.file.*;

/** Hidden render smoke test. The synthetic menu does not exercise server gameplay. */
public final class ClientSmoke implements ClientModInitializer {
    private static long started;
    private static int stage, ticks;
    private static ContainerScreen screen;
    private static boolean opacityRendered;
    @Override public void onInitializeClient() { started = System.nanoTime(); }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (started == 0 || stage == 99) return;
        try {
            if (System.nanoTime() - started > 180_000_000_000L) throw new AssertionError("Client startup timeout");
            if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                var inventory = new Inventory(null);
                var menu = ChestMenu.threeRows(1, inventory);
                menu.getSlot(0).set(new ItemStack(Items.APPLE, 12));
                screen = new ContainerScreen(menu, inventory, Component.literal("Animation validation"));
                mc.setScreen(screen); stage = 1; ticks = 0;
                Class.forName("net.minecraft.world.inventory.ResultSlot");
                pass("client entrypoint and mixin targets loaded");
            } else if (stage == 1 && ++ticks >= 10) {
                if (!ClientRuntime.INSTANCE.canAnimateSlot(screen, screen.getMenu().getSlot(0))) throw new AssertionError("Native slot render ownership was not observed");
                var slot = screen.getMenu().getSlot(0);
                ClientRuntime.INSTANCE.beforeInteraction(screen, slot, ClickType.PICKUP);
                slot.set(ItemStack.EMPTY); screen.getMenu().getSlot(1).set(new ItemStack(Items.APPLE, 12));
                ClientRuntime.INSTANCE.afterInteraction();
                if (ClientRuntime.INSTANCE.animations.active().isEmpty()) throw new AssertionError("Expected inferred move animation");
                pass("native container render and inferred item transfer"); stage = 2; ticks = 0;
            } else if (stage == 2 && ++ticks >= 10) {
                if (!ClientRuntime.INSTANCE.enabled()) throw new AssertionError("Animation runtime failed while rendering");
                if (!opacityRendered) throw new AssertionError("Opacity compositor was not rendered");
                mc.setScreen(new AnimationConfigScreen(new TitleScreen())); stage = 3; ticks = 0;
            } else if (stage == 3 && ++ticks >= 5) {
                pass("configuration screen rendered"); pass("complete"); stage = 99; mc.stop();
            }
        } catch (Throwable error) {
            error.printStackTrace();
            try { Files.writeString(Path.of("validation.txt"), "FAIL " + error + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception ignored) { }
            stage = 99; mc.stop();
        }
    }
    public static void render(net.minecraft.client.gui.GuiGraphics graphics) {
        if (stage < 1 || stage > 2 || opacityRendered) return;
        try {
            org.lwjgl.opengl.GL11.glGetError();
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(20, 20, 200);
                com.cappleapple.animatedinventory.client.render.ItemOpacityLayer.draw(graphics, new ItemStack(Items.APPLE, 4), .5f, true);
            } finally { graphics.pose().popPose(); }
            if (org.lwjgl.opengl.GL11.glGetError() != org.lwjgl.opengl.GL11.GL_NO_ERROR) throw new AssertionError("Opacity compositor produced a GL error");
            com.cappleapple.animatedinventory.client.render.ItemOpacityLayer.close();
            opacityRendered = true; pass("detached item opacity and GPU texture compositor");
        } catch (Exception error) { throw new RuntimeException(error); }
    }
    private static void pass(String message) throws Exception { Files.writeString(Path.of("validation.txt"), "PASS " + message + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
}
