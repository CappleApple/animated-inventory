package com.cappleapple.animatedinventory.validation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

@Mod.EventBusSubscriber(modid = "animatedinventory", value = Dist.DEDICATED_SERVER)
public final class ServerValidation {
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || !Boolean.getBoolean("animatedinventory.serverValidation") || ++ticks != 40) return;
        Files.writeString(Path.of("server-validation.txt"), "PASS dedicated server reached 40 ticks with client-side mod installed.\n");
        event.getServer().halt(false);
    }
}
