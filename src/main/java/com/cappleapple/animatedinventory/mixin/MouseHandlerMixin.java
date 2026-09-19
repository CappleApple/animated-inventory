package com.cappleapple.animatedinventory.mixin;
import com.cappleapple.animatedinventory.client.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observe the normal screen click dispatch, including screens supplied by custom providers. */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V", ordinal = 0))
    private void animatedinventory$click(CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (client.screen == null) return;
        double x = client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / Math.max(1, client.getWindow().getScreenWidth());
        double y = client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / Math.max(1, client.getWindow().getScreenHeight());
        ClientRuntime.INSTANCE.clickPre(client.screen, x, y);
    }
}
