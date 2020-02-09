package me.shawlaf.varlight.fabric;

import me.shawlaf.varlight.fabric.command.VarLightCommand;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VarLightMod implements ModInitializer {

    public static VarLightMod INSTANCE;

    private final VarLightCommand command = new VarLightCommand(this);
    private final Logger logger = LogManager.getLogger("VarLight");

    private final Map<String, WorldLightSourceManager> managers = new HashMap<>();

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

    public void setLuminance(ServerWorld world, BlockPos blockPos, int lightLevel) {
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

    }

    public WorldLightSourceManager getManager(ServerWorld world) {
        final String key = getKey(world);

        if (!managers.containsKey(key)) {
            managers.put(key, new WorldLightSourceManager(this, world));
        }

        return managers.get(key);
    }

    public File getVarLightSaveDirectory(ServerWorld world) {
        File regionRoot = world.getDimension().getType().getSaveDirectory(world.getSaveHandler().getWorldDir());
        File saveDir = new File(regionRoot, "varlight");

        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new RuntimeException("Could not create VarLight directory \"" + saveDir.getAbsolutePath() + "\"");
            }
        }

        return saveDir;
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

    private String getKey(ServerWorld world) {
        return world.getLevelProperties().getLevelName() + "/" + world.getDimension().getType().toString();
    }
}
