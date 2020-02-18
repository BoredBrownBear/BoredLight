package me.shawlaf.varlight.fabric;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface ILightModifier extends IModComponent {

    LightUpdateResult setLuminance(PlayerEntity modifier, ServerWorld world, BlockPos blockPos, int lightLevel, boolean doLightUpdate);

    void updateLight(ServerWorld world, ChunkPos chunkPos);

    default LightUpdateResult setLuminance(PlayerEntity modifier, ServerWorld world, BlockPos blockPos, int lightLevel) {
        return setLuminance(modifier, world, blockPos, lightLevel, true);
    }

    default void updateLight(ServerWorld world, BlockPos blockPos) {
        updateLight(world, new ChunkPos(blockPos));
    }

    default Collection<ChunkPos> collectNeighbouringChunks(ChunkPos chunkPos) {
        List<ChunkPos> list = new ArrayList<>();

        int centerChunkX = chunkPos.x;
        int centerChunkZ = chunkPos.z;

        for (int cz = centerChunkZ - 1; cz <= centerChunkZ + 1; cz++) {
            for (int cx = centerChunkX - 1; cx <= centerChunkX + 1; cx++) {
                list.add(new ChunkPos(cx, cz));
            }
        }

        return Collections.unmodifiableList(list);
    }

    default Collection<ChunkPos> collectNeighbouringChunks(BlockPos blockPos) {
        return collectNeighbouringChunks(new ChunkPos(blockPos));
    }


}
