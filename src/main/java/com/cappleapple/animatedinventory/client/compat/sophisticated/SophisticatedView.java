package com.cappleapple.animatedinventory.client.compat.sophisticated;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The custom screen retains ownership of its slot models, abbreviated counts and ghost filters. */
public interface SophisticatedView {
    boolean animatedinventory$visible(Slot slot);
    boolean animatedinventory$allows(Slot slot);
    void animatedinventory$drawStack(GuiGraphics graphics, int x, int y, ItemStack stack, boolean preview, String count);
}
