package com.cappleapple.animatedinventory.client.compat.bundlednotsiloed;

import com.cappleapple.animatedinventory.api.animation.Bounds;

/** Projects a logical row onto the corresponding edge without inventing an on-page inventory slot. */
public final class StowedSlotGeometry {
    public static Bounds destination(int logicalSlot, int firstVisible, Bounds firstCell, double columnStep, double rowStep) {
        long offset = (long)logicalSlot - firstVisible;
        int column = (int)Math.floorMod(offset, 9L);
        double y = offset < 0 ? firstCell.y() : firstCell.y() + 2 * rowStep;
        return Bounds.item(firstCell.x() + column * columnStep, y);
    }
    public static Bounds source(int logicalSlot, int firstVisible, Bounds firstCell, double columnStep, double rowStep) {
        int column = (int)Math.floorMod((long)logicalSlot - firstVisible, 9L);
        return Bounds.item(firstCell.x() + column * columnStep, firstCell.y() + 2 * rowStep + 16);
    }
    private StowedSlotGeometry() { }
}
