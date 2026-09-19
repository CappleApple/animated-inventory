package com.cappleapple.animatedinventory.client;

import com.cappleapple.animatedinventory.client.compat.RecipeViewerCompatibility;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ClientBootstrap {
    public static void initialize() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ClientConfigScreen(parent)));
        BundledCompatibility.initialize();
        InventoryParticlesCompatibility.initialize();
        ClientRuntime.INSTANCE.initialize();
        LogUtils.getLogger().info("Animated Inventory initialized (client cosmetic). Recipe viewers: {}", RecipeViewerCompatibility.detected());
    }
    private ClientBootstrap() { }
}
