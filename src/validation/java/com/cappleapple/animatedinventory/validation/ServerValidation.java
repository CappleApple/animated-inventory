package com.cappleapple.animatedinventory.validation;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;

@EventBusSubscriber(modid = "animatedinventory", value = Dist.DEDICATED_SERVER)
public final class ServerValidation {
    private static int ticks;
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (!Boolean.getBoolean("animatedinventory.serverValidation") || ++ticks != 40) return;
        try {
            Files.writeString(Path.of("server-validation.txt"), "PASS dedicated server reached 40 ticks without loading client entrypoint or client mixins.\n");
        } catch (Exception exception) { throw new IllegalStateException(exception); }
        LogUtils.getLogger().info("AI_SERVER_VALIDATION PASSED");
        event.getServer().halt(false);
    }
}
