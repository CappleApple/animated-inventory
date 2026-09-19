package com.cappleapple.animatedinventory;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
public final class AnimatedInventory implements ClientModInitializer {
    public static final String MOD_ID = "animatedinventory";
    @Override public void onInitializeClient() {
        Platform.install(FabricLoader.getInstance()::isModLoaded);
        ClientConfig.SPEC.load();
        BundledCompatibility.initialize();
        InventoryParticlesCompatibility.initialize();
        ClientRuntime.INSTANCE.initialize();
    }
}
