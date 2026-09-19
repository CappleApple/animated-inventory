package com.cappleapple.animatedinventory.mixin;

import com.cappleapple.animatedinventory.client.ClientRuntime;
import com.cappleapple.animatedinventory.compat.LegacyJeiPackets;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

/** Observe existing requests; their contents, ordering and send behavior are unchanged. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @org.spongepowered.asm.mixin.Unique private static final Set<String> ANIMATEDINVENTORY_RECIPE_REQUESTS = Set.of(
            "bundlednotsiloed:recipe_transfer", "emi:fill_recipe",
            "jei:recipe_transfer", "jei:recipe_transfer_counted",
            "jei:recipe_transfer_with_result", "jei:recipe_transfer_counted_with_result");
    @Inject(method = "send", at = @At("HEAD"))
    private void animatedinventory$observeRequest(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundPlaceRecipePacket) ClientRuntime.INSTANCE.remoteTransfer(true);
        else if (packet instanceof ServerboundCustomPayloadPacket custom) {
            String id = custom.getIdentifier().toString();
            if (ANIMATEDINVENTORY_RECIPE_REQUESTS.contains(id)
                    || (id.equals("jei:channel") && LegacyJeiPackets.isRecipeTransfer(custom.getData()))) ClientRuntime.INSTANCE.remoteTransfer(true);
            else if (id.equals("bundlednotsiloed:inventory_view_preferences")
                    || id.equals("bundlednotsiloed:bulk_transfer")) ClientRuntime.INSTANCE.remoteTransfer(false);
        }
    }
}
