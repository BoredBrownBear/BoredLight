package me.shawlaf.varlight.fabric;

import me.shawlaf.varlight.fabric.command.VarLightCommand;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VarLightMod implements ModInitializer {

    public static VarLightMod INSTANCE;

    private final VarLightCommand command = new VarLightCommand(this);
    private final Logger logger = LogManager.getLogger("VarLight");

    private final Map<BlockPos, Integer> customLightSources = new HashMap<>();

    {
        INSTANCE = this;
    }

    @Override
    public void onInitialize() {
        this.command.register();
    }

    public Logger getLogger() {
        return logger;
    }

    public int getCustomLuminance(BlockPos blockPos) {
        return customLightSources.getOrDefault(blockPos, 0);
    }

    private List<ChunkPos> collectLightUpdateChunks(BlockPos center) {
        List<ChunkPos> list = new ArrayList<>();

        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        for (int cz = centerChunkZ - 1; cz <= centerChunkZ + 1; cz++) {
            for (int cx = centerChunkX - 1; cx <= centerChunkX + 1; cx++) {
                list.add(new ChunkPos(cx, cz));
            }
        }

        return list;
    }

    public boolean createCustomLightSource(ServerWorld serverWorld, BlockPos blockPos, int lightLevel) {
        if (lightLevel < 0 || lightLevel > 15) {
            throw new IllegalArgumentException("Light level must be 0 <= x <= 15");
        }

        customLightSources.put(blockPos, lightLevel); // This needs to be changed, temporary hack

        return setCustomLuminance(serverWorld, blockPos, lightLevel);
    }

    public boolean setCustomLuminance(ServerWorld world, BlockPos blockPos, int lightLevel) {
        LightingProvider provider = world.getLightingProvider();

        if (provider instanceof ServerLightingProvider) {
            ((ServerLightingProvider) provider).light(world.getChunk(blockPos), false).thenRun(() -> {
                for (ChunkPos pos : collectLightUpdateChunks(blockPos)) {
                    LightUpdateS2CPacket packet = new LightUpdateS2CPacket(pos, provider);

                    world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(pos, false).forEach(spe -> {
                        spe.networkHandler.sendPacket(packet);
                    });
                }
            });
        } else {
            provider.addLightSource(blockPos, lightLevel);
        }

        return true;
    }
}
