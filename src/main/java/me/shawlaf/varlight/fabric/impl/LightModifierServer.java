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
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;

import java.lang.reflect.Field;

public class LightModifierServer implements ILightModifier {

    private final VarLightMod mod;
    private final Field processorField;

    public LightModifierServer(VarLightMod mod) {
        Field processorField1;
        this.mod = mod;

        try {
            //noinspection JavaReflectionMemberAccess : Intermediary name
            processorField1 = ServerLightingProvider.class.getDeclaredField("field_17255");
        } catch (NoSuchFieldException ignored) {
            try {
                processorField1 = ServerLightingProvider.class.getDeclaredField("processor");
            } catch (NoSuchFieldException e1) {
                throw new RuntimeException("Failed to find both intermediary and named versions of net.minecraft.server.world.ServerLightingProvider.processor", e1);
            }
        }

        this.processorField = processorField1;
        this.processorField.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
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

        manager.deleteLightSource(blockPos);

        if (doLightUpdate) {

            ServerLightingProvider serverLightingProvider = (ServerLightingProvider) world.getLightingProvider();
            ChunkBlockLightProvider lightProvider = ((ChunkBlockLightProvider) serverLightingProvider.get(LightType.BLOCK));

            try {
                ((TaskExecutor<Runnable>) processorField.get(serverLightingProvider)).send(() -> lightProvider.checkBlock(blockPos));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        if (lightLevel > 0) {
            manager.setCustomLuminance(blockPos, lightLevel);
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
