package me.shawlaf.varlight.fabric.impl;

import me.shawlaf.varlight.fabric.ILightModifier;
import me.shawlaf.varlight.fabric.LightUpdateResult;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;

public class LightModifierServer implements ILightModifier {

    private final VarLightMod mod;

    public LightModifierServer(VarLightMod mod) {
        this.mod = mod;
    }

    @Override
    public LightUpdateResult setLuminance(PlayerEntity modifier, ServerWorld world, BlockPos blockPos, int lightLevel, boolean doLightUpdate) {
        if (modifier != null && !world.canPlayerModifyAt(modifier, blockPos)) {
            return LightUpdateResult.CANNOT_MODIFY;
        }

        if (mod.isIllegalBlock(world, blockPos)) {
            return LightUpdateResult.ILLEGAL_BLOCK;
        }

        if (lightLevel < 0) {
            return LightUpdateResult.ZERO_REACHED;
        }

        if (lightLevel > 15) {
            return LightUpdateResult.FIFTEEN_REACHED;
        }

        WorldLightSourceManager manager = mod.getLightStorageManager().getManager(world);

        if (doLightUpdate) {
            manager.deleteLightSource(blockPos);

            ((ChunkBlockLightProvider) world.getLightingProvider().get(LightType.BLOCK)).checkBlock(blockPos);
        }

        if (lightLevel > 0) {
            manager.createPersistentLightSource(blockPos, lightLevel);
        }

        if (doLightUpdate) {
            updateLight(world, blockPos);
        }

        return LightUpdateResult.success(lightLevel);
    }

    @Override
    public void updateLight(ServerWorld world, ChunkPos chunkPos) {
        LightingProvider provider = world.getLightingProvider();

        ((ServerLightingProvider) provider).light(world.getChunk(chunkPos.x, chunkPos.z), false).thenRun(() -> {
            mod.getScheduledTaskManager().enqueue(() -> {
                for (ChunkPos pos : collectNeighbouringChunks(chunkPos)) {
                    LightUpdateS2CPacket packet = new LightUpdateS2CPacket(pos, provider);

                    world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(pos, false).forEach(spe -> {
                        spe.networkHandler.sendPacket(packet);
                    });
                }
            });
        });
    }
}
