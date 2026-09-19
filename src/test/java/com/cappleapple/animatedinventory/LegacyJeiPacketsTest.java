package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.compat.LegacyJeiPackets;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyJeiPacketsTest {
    @Test
    void recognizesOnlyRecipeTransferRequests() {
        for (int id = 0; id <= 255; id++) {
            FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
            try {
                data.writeByte(id);
                assertEquals(id == 0 || id == 5 || id == 6 || id == 7,
                        LegacyJeiPackets.isRecipeTransfer(data), "packet " + id);
            } finally { data.release(); }
        }
    }

    @Test
    void respectsReaderIndexWithoutConsumingOrChangingData() {
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        try {
            data.writeByte(99).writeByte(5).writeByte(42);
            data.readerIndex(1);
            assertTrue(LegacyJeiPackets.isRecipeTransfer(data));
            assertEquals(1, data.readerIndex());
            assertEquals(3, data.writerIndex());
            assertEquals(5, data.readUnsignedByte());
            assertEquals(42, data.readUnsignedByte());
        } finally { data.release(); }
    }

    @Test
    void emptyPacketsAreIgnored() {
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        try { assertFalse(LegacyJeiPackets.isRecipeTransfer(data)); }
        finally { data.release(); }
    }
}
