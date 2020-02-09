package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    protected ServerWorldMixin(LevelProperties levelProperties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> chunkManagerProvider, Profiler profiler, boolean isClient) {
        super(levelProperties, dimensionType, chunkManagerProvider, profiler, isClient);
    }

    @Inject(at = @At("INVOKE"), method = "onBlockChanged(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V")
    public void blockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        checkAndUpdateCustomLightSource(pos);
        checkAndUpdateCustomLightSource(pos.up());
        checkAndUpdateCustomLightSource(pos.down());
        checkAndUpdateCustomLightSource(pos.north());
        checkAndUpdateCustomLightSource(pos.east());
        checkAndUpdateCustomLightSource(pos.south());
        checkAndUpdateCustomLightSource(pos.west());
    }

    private void checkAndUpdateCustomLightSource(BlockPos blockPos) {
        int customLuminance = VarLightMod.INSTANCE.getCustomLuminance(blockPos);

        if (customLuminance == 0) {
            return;
        }

        VarLightMod.INSTANCE.setCustomLuminance((ServerWorld) ((Object) this), blockPos, customLuminance);
    }
}
