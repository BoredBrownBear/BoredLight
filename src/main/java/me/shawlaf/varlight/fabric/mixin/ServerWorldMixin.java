package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ProgressListener;
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

    @Inject(at = @At("HEAD"), method = "save(Lnet/minecraft/util/ProgressListener;ZZ)V")
    public void onLevelSave(ProgressListener progressListener, boolean flush, boolean bl, CallbackInfo ci) {
        if (bl) {
            return;
        }

        if (progressListener != null) {
            progressListener.method_15412(new LiteralText("Saving Custom Light sources"));
        }

        VarLightMod.INSTANCE.getManager(castThis()).save(null);
    }

    private void checkAndUpdateCustomLightSource(BlockPos blockPos) {
        int customLuminance = VarLightMod.INSTANCE.getManager(castThis()).getCustomLuminance(blockPos, 0);

        if (customLuminance == 0) {
            return;
        }

        VarLightMod.INSTANCE.setLuminance(castThis(), blockPos, customLuminance);
    }

    private ServerWorld castThis() {
        return (ServerWorld) ((Object) this);
    }
}
