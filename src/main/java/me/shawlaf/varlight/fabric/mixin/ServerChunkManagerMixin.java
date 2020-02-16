package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin extends ChunkManager {

    @Inject(
            at = @At("HEAD"),
            method = "tick(Ljava/util/function/BooleanSupplier;)V"
    )
    public void beforeTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        while (!VarLightMod.TASKS.isEmpty()) {
            VarLightMod.TASKS.remove().run();
        }
    }

}
