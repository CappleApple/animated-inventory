package com.cappleapple.animatedinventory.mixin;

import com.cappleapple.animatedinventory.client.ClientRuntime;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Initial server contents are a baseline, not an item acquisition. Client-only mixin. */
@Mixin(AbstractContainerMenu.class)
abstract class InitialContentsMixin {
    @Unique private boolean animatedinventory$receivedContents;

    @Inject(method = "initializeContents", at = @At("TAIL"))
    private void animatedinventory$initialContents(CallbackInfo ci) {
        if (animatedinventory$receivedContents) return;
        animatedinventory$receivedContents = true;
        ClientRuntime.INSTANCE.initialContents((AbstractContainerMenu)(Object)this);
    }
}
