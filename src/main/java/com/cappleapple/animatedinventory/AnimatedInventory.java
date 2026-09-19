package com.cappleapple.animatedinventory;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.compat.RecipeViewerCompatibility;
import com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
public final class AnimatedInventory implements ClientModInitializer {
    public static final String MOD_ID = "animatedinventory";
    @Override public void onInitializeClient() {
        ClientConfig.SPEC.load();
        InventoryParticlesCompatibility.initialize();
        ClientRuntime.INSTANCE.initialize();
        LogUtils.getLogger().info("Animated Inventory initialized (client cosmetic). Recipe viewers: {}", RecipeViewerCompatibility.detected());
    }
}
