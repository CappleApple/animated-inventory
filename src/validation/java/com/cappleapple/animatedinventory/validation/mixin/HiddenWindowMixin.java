package com.cappleapple.animatedinventory.validation.mixin;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Window.class)
abstract class HiddenWindowMixin {
    // 26.3 creates an SDL window; 26.2 is hidden at its GLFW show call instead.
    @ModifyArg(method = "createWindow", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J"), index = 3, require = 0)
    private long animatedinventory$hidden(long flags) {
        try { return flags | ((Number)Class.forName("org.lwjgl.sdl.SDLVideo").getField("SDL_WINDOW_HIDDEN").get(null)).longValue(); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot hide validation window", error); }
    }
}
