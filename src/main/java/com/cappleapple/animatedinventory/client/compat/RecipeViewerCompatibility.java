package com.cappleapple.animatedinventory.client.compat;

import net.minecraftforge.fml.ModList;

/** No viewer classes are linked. All screen init/return flows use the same fresh-owner lifecycle. */
public final class RecipeViewerCompatibility {
    public static String detected() {
        return (ModList.get().isLoaded("jei") ? "JEI " : "") + (ModList.get().isLoaded("emi") ? "EMI " : "")
                + (ModList.get().isLoaded("roughlyenoughitems") ? "REI " : "");
    }
    private RecipeViewerCompatibility() { }
}
