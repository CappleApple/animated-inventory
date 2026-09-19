package com.cappleapple.animatedinventory.mixin;
import com.cappleapple.animatedinventory.client.ClientRuntime;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
abstract class ScreenLifecycleMixin {
    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void animatedinventory$initialized(int width, int height, CallbackInfo ci) {
        ClientRuntime.INSTANCE.init((Screen)(Object)this);
    }
    @WrapMethod(method = "extractRenderStateWithTooltipAndSubtitles")
    private void animatedinventory$screen(GuiGraphicsExtractor graphics, int x, int y, float delta, Operation<Void> original) {
        var runtime = ClientRuntime.INSTANCE;
        var screen = (Screen)(Object)this;
        runtime.renderPre(screen, x, y);
        GuiGraphicsExtractor captured = screen instanceof AbstractContainerScreen<?> container && runtime.enabled()
            ? runtime.screens.begin(container, graphics) : graphics;
        try { original.call(captured, x, y, delta); }
        finally { runtime.screens.complete(captured, graphics); }
    }
}
