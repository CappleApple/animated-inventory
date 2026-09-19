package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The render fixture has no world or player; skip only player lifecycle checks. */
@Mixin(AbstractContainerScreen.class)
abstract class SyntheticMenuMixin {
    @Inject(method = {"tick", "removed"}, at = @At("HEAD"), cancellable = true)
    private void animatedinventory$fixture(CallbackInfo ci) { if (Minecraft.getInstance().player == null) ci.cancel(); }
}
