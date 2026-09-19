package com.cappleapple.animatedinventory.client.compat;

import net.fabricmc.loader.api.FabricLoader;

/** No viewer classes are linked. All screen init/return flows use the same fresh-owner lifecycle. */
public final class RecipeViewerCompatibility {
    public static String detected() {
        return (FabricLoader.getInstance().isModLoaded("jei") ? "JEI " : "") + (FabricLoader.getInstance().isModLoaded("emi") ? "EMI " : "")
                + (FabricLoader.getInstance().isModLoaded("roughlyenoughitems") ? "REI " : "");
    }
    private RecipeViewerCompatibility() { }
}
