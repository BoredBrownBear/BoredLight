package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

import static me.shawlaf.varlight.fabric.util.IntPositionExtension.toBlockPos;
import static me.shawlaf.varlight.fabric.util.IntPositionExtension.toIntPosition;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    protected ServerWorldMixin(MutableWorldProperties mutableWorldProperties, RegistryKey<World> registryKey, RegistryKey<DimensionType> registryKey2, DimensionType dimensionType, Supplier<Profiler> profiler, boolean bl, boolean bl2, long l) {
        super(mutableWorldProperties, registryKey, registryKey2, dimensionType, profiler, bl, bl2, l);
    }

    @Inject(at = @At("INVOKE"), method = "onBlockChanged(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V")
    public void blockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        IntPosition position = toIntPosition(pos);

        WorldLightSourceManager manager = getManager();

        int lum = manager.getCustomLuminance(pos, 0);

        if (lum != 0) {
            manager.getMod().getScheduledTaskManager().enqueue(() -> {
                if (oldBlock.getBlock() != newBlock.getBlock() && !newBlock.isFullCube(castThis(), toBlockPos(position))) {
                    manager.deleteLightSource(toBlockPos(position));
                }
            });

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

    @Inject(
            at = @At("TAIL"),
            method = "removePlayer"
    )
    public void afterRemovePlayer(ServerPlayerEntity player, CallbackInfo ci) {
        VarLightMod.INSTANCE.getPlayerDataManager().remove(player);
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

        VarLightMod.INSTANCE.getLightModifier().updateLight(castThis(), blockPos);
    }

    private ServerWorld castThis() {
        return (ServerWorld) ((Object) this);
    }

    private WorldLightSourceManager getManager() {
        return VarLightMod.INSTANCE.getLightStorageManager().getManager(castThis());
    }
}
