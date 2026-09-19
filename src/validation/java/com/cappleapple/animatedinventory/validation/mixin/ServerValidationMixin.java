package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.file.*;
@Mixin(MinecraftServer.class)
abstract class ServerValidationMixin {
    @Unique private boolean animatedinventory$tested;
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void animatedinventory$server(CallbackInfo ci) throws Exception {
        if (animatedinventory$tested) return;
        animatedinventory$tested = true;
        boolean skipped = !FabricLoader.getInstance().isModLoaded("animatedinventory");
        Files.writeString(Path.of("validation.txt"), skipped ? "PASS complete: dedicated server started and client mod was skipped\n" : "FAIL client mod should be skipped on server\n");
        ((MinecraftServer)(Object)this).halt(false);
    }
}
