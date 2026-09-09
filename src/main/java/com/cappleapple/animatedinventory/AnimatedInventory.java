package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.compat.RecipeViewerCompatibility;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = AnimatedInventory.MOD_ID, dist = Dist.CLIENT)
public final class AnimatedInventory {
    public static final String MOD_ID = "animatedinventory";
    public AnimatedInventory(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new ConfigurationScreen(container, parent));
        BundledCompatibility.initialize();
        InventoryParticlesCompatibility.initialize();
        ClientRuntime.INSTANCE.initialize();
        LogUtils.getLogger().info("Animated Inventory initialized (client cosmetic). Recipe viewers: {}", RecipeViewerCompatibility.detected());
    }
}
