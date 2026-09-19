package com.cappleapple.animatedinventory;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.common.Mod;

@Mod(AnimatedInventory.MOD_ID)
public final class AnimatedInventory {
    public static final String MOD_ID = "animatedinventory";
    public AnimatedInventory() {
        if (FMLEnvironment.dist == Dist.CLIENT) com.cappleapple.animatedinventory.client.ClientBootstrap.initialize();
    }
}
