package com.cappleapple.animatedinventory.mixin.sophisticated;

import com.cappleapple.animatedinventory.client.compat.sophisticated.SophisticatedMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.AbstractList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import net.minecraft.world.item.Item;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
abstract class SophisticatedMenuMixin implements SophisticatedMenu {
    @Shadow @Final public List<Slot> upgradeSlots;
    @Shadow public abstract int getNumberOfStorageInventorySlots();
    @Shadow public abstract boolean isInfiniteSlot(int index);
    @Shadow public abstract boolean isInaccessibleSlot(int index);
    @Unique private long animatedinventory$permissionsRevision;
    @Unique private List<Object> animatedinventory$permissions = List.of();
    @Override public long animatedinventory$permissionsRevision() { return animatedinventory$permissionsRevision; }
    @Inject(method = "updateAdditionalSlotInfo", at = @At("TAIL"))
    private void animatedinventory$permissionsChanged(Set<Integer> inaccessible, Set<Integer> noOverlay,
            Map<Integer, Integer> limits, Set<Integer> infinite, Map<Integer, Item> filters, CallbackInfo ci) {
        // Core resends this packet after ordinary contents changes too. Identical permissions are not a new layout.
        List<Object> next = List.of(Set.copyOf(inaccessible), Set.copyOf(noOverlay), Map.copyOf(limits),
                Set.copyOf(infinite), Map.copyOf(filters));
        if (!next.equals(animatedinventory$permissions)) {
            animatedinventory$permissions = next;
            animatedinventory$permissionsRevision++;
        }
    }

    @Override public List<Slot> animatedinventory$allSlots() {
        List<Slot> inventory = ((AbstractContainerMenu)(Object)this).slots;
        List<Slot> upgrades = upgradeSlots;
        return new AbstractList<>() {
            @Override public Slot get(int index) {
                return index < inventory.size() ? inventory.get(index) : upgrades.get(index - inventory.size());
            }
            @Override public int size() { return inventory.size() + upgrades.size(); }
        };
    }
}
