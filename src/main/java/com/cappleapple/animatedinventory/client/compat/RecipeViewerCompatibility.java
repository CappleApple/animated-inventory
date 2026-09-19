package com.cappleapple.animatedinventory.client.compat;

import com.cappleapple.animatedinventory.client.Platform;

/** No viewer classes are linked. All screen init/return flows use the same fresh-owner lifecycle. */
public final class RecipeViewerCompatibility {
    public static String detected() {
        return (Platform.isLoaded("jei") ? "JEI " : "") + (Platform.isLoaded("emi") ? "EMI " : "")
                + (Platform.isLoaded("roughlyenoughitems") ? "REI " : "");
    }
    private RecipeViewerCompatibility() { }
}
