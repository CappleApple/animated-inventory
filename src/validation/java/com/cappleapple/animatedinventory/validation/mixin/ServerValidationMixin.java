package com.cappleapple.animatedinventory.validation.mixin;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.file.*;
@Mixin(MinecraftServer.class)
abstract class ServerValidationMixin {
    @Unique private int animatedinventory$ticks;
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void animatedinventory$server(CallbackInfo ci) {
        var server = (MinecraftServer)(Object)this;
        if (!server.isDedicatedServer() || ++animatedinventory$ticks != 40) return;
        try { Files.writeString(Path.of("server-validation.txt"), "PASS dedicated server reached 40 ticks with client mod present.\n"); }
        catch (Exception error) { throw new IllegalStateException(error); }
        server.halt(false);
    }
}
