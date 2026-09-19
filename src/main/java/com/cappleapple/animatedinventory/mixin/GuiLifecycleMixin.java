package com.cappleapple.animatedinventory.mixin;
import com.cappleapple.animatedinventory.client.ClientRuntime;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
abstract class GuiLifecycleMixin {
    @Shadow public abstract Screen screen();
    @Inject(method = "tick", at = @At("TAIL"))
    private void animatedinventory$tick(CallbackInfo ci) { ClientRuntime.INSTANCE.tick(); }
    // Observe accepted screen changes after loader cancellation/replacement hooks.
    @Inject(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = 181))
    private void animatedinventory$opening(Screen next, CallbackInfo ci) {
        var runtime = ClientRuntime.INSTANCE;
        runtime.opening(screen(), next);
        if (screen() != null) runtime.closing(screen());
    }
}
