package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.IScheduledTaskManager;
import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin extends ChunkManager {

    @Inject(
            at = @At("HEAD"),
            method = "tick()Z"
    )
    public void beforeTick(CallbackInfoReturnable<Boolean> ci) {
        IScheduledTaskManager scheduler = VarLightMod.INSTANCE.getScheduledTaskManager();

        while (!scheduler.isEmpty()) {
            scheduler.runNext();
        }
    }

}
