package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.IntPosition;
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

import static me.shawlaf.varlight.fabric.util.IntPositionExtension.toIntPosition;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    protected ServerWorldMixin(LevelProperties levelProperties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> chunkManagerProvider, Profiler profiler, boolean isClient) {
        super(levelProperties, dimensionType, chunkManagerProvider, profiler, isClient);
    }

    @Inject(at = @At("INVOKE"), method = "onBlockChanged(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V")
    public void blockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        IntPosition position = toIntPosition(pos);

        WorldLightSourceManager manager = getManager();

        if (manager.getCustomLuminance(position, 0) > 0 && oldBlock != newBlock) {
            manager.deleteLightSource(pos);
            return;
        }

        checkAndUpdateCustomLightSource(manager, pos.up());
        checkAndUpdateCustomLightSource(manager, pos.down());
        checkAndUpdateCustomLightSource(manager, pos.north());
        checkAndUpdateCustomLightSource(manager, pos.east());
        checkAndUpdateCustomLightSource(manager, pos.south());
        checkAndUpdateCustomLightSource(manager, pos.west());
    }

    @Inject(at = @At("HEAD"), method = "save(Lnet/minecraft/util/ProgressListener;ZZ)V")
    public void onLevelSave(ProgressListener progressListener, boolean flush, boolean bl, CallbackInfo ci) {
        if (bl) {
            return;
        }

        if (progressListener != null) {
            progressListener.method_15412(new LiteralText("Saving Custom Light sources"));
        }

        getManager().save(null);
    }

    @Override
    public int getLuminance(BlockPos pos) {
        return getChunk(pos).getLuminance(pos);
    }

    private void checkAndUpdateCustomLightSource(WorldLightSourceManager manager, BlockPos blockPos) {
        int customLuminance = manager.getCustomLuminance(blockPos, 0);

        if (customLuminance == 0) {
            return;
        }

        VarLightMod.INSTANCE.updateLight(castThis(), blockPos);
    }

    private ServerWorld castThis() {
        return (ServerWorld) ((Object) this);
    }

    private WorldLightSourceManager getManager() {
        return VarLightMod.INSTANCE.getManager(castThis());
    }
}
