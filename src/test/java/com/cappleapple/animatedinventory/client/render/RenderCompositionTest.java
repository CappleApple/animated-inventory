package com.cappleapple.animatedinventory.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RenderCompositionTest {
    @Test void elevatedNativeScopeRestoresAnOuterBatchAndCleansUpWhenExtractionFails() throws Exception {
        var field = NativeSlotRenderer.class.getDeclaredField("ELEVATED");
        field.setAccessible(true);
        var state = (ThreadLocal<?>)field.get(null);
        assertNull(state.get());
        assertThrows(IllegalStateException.class, () -> NativeSlotRenderer.withElevatedLayer(null, () -> {
            Object outer = state.get();
            assertNotNull(outer);
            NativeSlotRenderer.withElevatedLayer(null, () -> assertNotSame(outer, state.get()));
            assertSame(outer, state.get());
            throw new IllegalStateException("fixture extraction failure");
        }));
        assertNull(state.get());
    }
    @Test void premultipliedFadesAttenuateRgbAndAlpha() {
        assertEquals(0x80402010, TextureCompositor.color(0xff804020, .5f, true));
        assertEquals(0x80804020, TextureCompositor.color(0xff804020, .5f, false));
        assertEquals(0, TextureCompositor.color(0xff804020, 0, true));
    }

    @Test void transformAndFadeReachVerticesThroughDefaultInterfaceMethods() {
        List<float[]> positions = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        VertexConsumer sink = (VertexConsumer)Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{VertexConsumer.class}, (proxy, method, args) -> {
            if (method.getName().equals("addVertex")) positions.add(new float[]{(float)args[0], (float)args[1], (float)args[2]});
            if (method.getName().equals("setColor")) colors.add((int)args[0]);
            return proxy;
        });
        VertexConsumer transformed = TextureCompositor.vertices(sink, new Matrix3x2f().translate(10, 20).scale(2), .5f, true);
        transformed.addVertexWith2DPose(new Matrix3x2f().translate(3, 4), 1, 2).setColor(0xffffffff);
        assertArrayEquals(new float[]{18, 32, 0}, positions.getFirst());
        assertEquals(0x80808080, colors.getFirst());
    }

    @Test void transformedScissorsRemainClippedToTheOwningView() {
        var clip = TextureCompositor.transformedClip(new ScreenRectangle(10, 10, 20, 20),
                new Matrix3x2f().translate(5, 5).scale(2), new ScreenRectangle(30, 30, 10, 15));
        assertEquals(new ScreenRectangle(30, 30, 10, 15), clip);
        var hidden = TextureCompositor.transformedClip(new ScreenRectangle(0, 0, 1, 1), new Matrix3x2f(), new ScreenRectangle(30, 30, 10, 10));
        assertEquals(0, hidden.width());
        assertEquals(0, hidden.height());
    }

    @Test void finishingCaptureForwardsCursorWorkWithoutKeepingItInTheSnapshot() {
        var recorded = new TextureCompositor.CaptureState();
        recorded.nextStratum();
        recorded.up();
        var snapshot = recorded.snapshot();
        var destination = new TextureCompositor.CaptureState();
        recorded.forwardTo(destination);
        recorded.nextStratum();
        assertEquals(2, snapshot.commands().size());
        assertTrue(recorded.snapshot().commands().isEmpty());
        assertEquals(1, destination.snapshot().commands().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.commands().clear());
    }
}
