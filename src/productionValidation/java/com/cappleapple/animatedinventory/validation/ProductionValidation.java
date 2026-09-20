package com.cappleapple.animatedinventory.validation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("animatedinventory_validation")
public final class ProductionValidation {
    public ProductionValidation() {
        System.out.println("Animated Inventory production validation registered on " + FMLEnvironment.dist);
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try (var writer = new java.io.PrintWriter("validation-uncaught.txt")) {
                writer.println(thread.getName()); error.printStackTrace(writer);
            } catch (Exception ignored) { }
            error.printStackTrace();
        });
        if (FMLEnvironment.dist == Dist.CLIENT) MinecraftForge.EVENT_BUS.register(ClientValidation.class);
        else MinecraftForge.EVENT_BUS.register(ServerValidation.class);
    }
}
