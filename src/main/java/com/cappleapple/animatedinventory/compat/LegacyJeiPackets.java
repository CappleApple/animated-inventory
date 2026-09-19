package com.cappleapple.animatedinventory.compat;

import net.minecraft.network.FriendlyByteBuf;

/** JEI 1.20.1's shared channel prefixes each message with its PacketIdServer ordinal. */
public final class LegacyJeiPackets {
    private LegacyJeiPackets() { }

    public static boolean isRecipeTransfer(FriendlyByteBuf data) {
        if (!data.isReadable()) return false;
        // Absolute reads preserve the outgoing packet's reader index and payload.
        return switch (data.getUnsignedByte(data.readerIndex())) {
            case 0, 5, 6, 7 -> true;
            default -> false;
        };
    }
}
