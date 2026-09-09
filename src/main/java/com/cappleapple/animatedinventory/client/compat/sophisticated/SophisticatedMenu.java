package com.cappleapple.animatedinventory.client.compat.sophisticated;

import net.minecraft.world.inventory.Slot;
import java.util.List;

/** Read-only access implemented only when Sophisticated Core is installed. */
public interface SophisticatedMenu {
    List<Slot> animatedinventory$allSlots();
    int getNumberOfStorageInventorySlots();
    boolean isInfiniteSlot(int index);
    boolean isInaccessibleSlot(int index);
    long animatedinventory$permissionsRevision();
}
