package me.shawlaf.varlight.fabric.persistence;

import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.util.IntPositionExtension;
import me.shawlaf.varlight.fabric.util.OpPermissionLevel;
import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.persistence.RegionPersistor;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoords;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.io.IOException;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.*;
import java.util.function.IntSupplier;

import static me.shawlaf.varlight.fabric.util.IntPositionExtension.toIntPosition;

public class WorldLightSourceManager {

    private final Map<RegionCoords, RegionPersistor<PersistentLightSource>> worldMap;
    private final ServerWorld world;
    private final VarLightMod mod;

    private long lastMigrateNotice;

    public WorldLightSourceManager(VarLightMod mod, ServerWorld world) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(mod);

        this.mod = mod;
        this.world = world;

        this.worldMap = new HashMap<>();

        synchronized (worldMap) {
            mod.getVarLightSaveDirectory(world);

            // TODO migrations
        }

        mod.getLogger().debug(String.format("Created a new Lightsource Persistor for world \"%s\"", world.getLevelProperties().getLevelName()));
    }

    public VarLightMod getMod() {
        return mod;
    }

    public ServerWorld getWorld() {
        return world;
    }

    public PersistentLightSource createPersistentLightSource(BlockPos blockPos, int emittingLight) {
        return createPersistentLightSource(toIntPosition(blockPos), emittingLight);
    }

    public PersistentLightSource createPersistentLightSource(IntPosition position, int emittingLight) {
        PersistentLightSource pls = new PersistentLightSource(world, position, emittingLight);
        pls.migrated = true; // pre 1.14.2 not supported -> New Lightsources always migrated

        try {
            getRegionPersistor(new RegionCoords(position)).put(pls);
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        }

        return pls;
    }

    public int getCustomLuminance(BlockPos pos, int def) {
        return getCustomLuminance(toIntPosition(pos), def);
    }

    public int getCustomLuminance(IntPosition position, int def) {
        return getCustomLuminance(position, () -> def);
    }

    public int getCustomLuminance(IntPosition position, IntSupplier def) {
        PersistentLightSource pls = getPersistentLightSource(position);

        if (pls == null) {
            return def.getAsInt();
        }

        return pls.getEmittingLight();
    }

    public PersistentLightSource getPersistentLightSource(IntPosition position) {
        RegionPersistor<PersistentLightSource> region = getRegionPersistor(new RegionCoords(position));

        PersistentLightSource pls;

        try {
            pls = region.getLightSource(position);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get Custom Light source: " + e.getMessage(), e);
        }

        if (pls == null) {
            return null;
        }

        if (!pls.isMigrated() && (System.currentTimeMillis() - lastMigrateNotice) > 30_000L) {
            Text text = new LiteralText(String.format("[VarLight] There are non-migrated Light sources present in world \"%s\", please run /varlight migrate!", world.getLevelProperties().getLevelName()));

            Style style = new Style();
            style.setColor(Formatting.RED);

            text.setStyle(style);

            for (ServerPlayerEntity playerEntity : world.getServer().getPlayerManager().getPlayerList()) {
                if (!playerEntity.allowsPermissionLevel(OpPermissionLevel.MANAGE_SERVER)) {
                    continue;
                }

                playerEntity.sendMessage(text);
            }

            lastMigrateNotice = System.currentTimeMillis();
        }

        return pls;
    }

    public RegionPersistor<PersistentLightSource> getRegionPersistor(RegionCoords regionCoords) {
        synchronized (worldMap) {
            if (!worldMap.containsKey(regionCoords)) {
                try {
                    worldMap.put(regionCoords, new RegionPersistor<PersistentLightSource>(mod.getVarLightSaveDirectory(world), regionCoords.x, regionCoords.z, true) {
                        @Override
                        protected PersistentLightSource[] createArray(int i) {
                            return new PersistentLightSource[i];
                        }

                        @Override
                        protected PersistentLightSource createInstance(IntPosition pos, int lightLevel, boolean migrated, String type) {
                            return new PersistentLightSource(pos, Registry.BLOCK.get(new Identifier(type)), migrated, world, lightLevel);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Could not create region persistor for region (" + regionCoords.x + ", " + regionCoords.z + "): " + e.getMessage(), e);
                }
            }

            return worldMap.get(regionCoords);
        }
    }

    public void save(ServerPlayerEntity source) {
        int modified = 0, deleted = 0;
        List<RegionCoords> toUnload = new ArrayList<>();

        synchronized (worldMap) {
            for (RegionPersistor<PersistentLightSource> region : worldMap.values()) {
                int loaded = 0;

                try {
                    region.flushAll();
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                List<ChunkCoords> affected = region.getAffectedChunks();

                if (affected.size() == 0) {
                    if (region.file.file.exists()) {
                        if (!region.file.delete()) {
                            throw new LightPersistFailedException("Could not delete file " + region.file.file.getAbsolutePath());
                        }

                        mod.getLogger().debug("Deleted File " + region.file.file.getAbsolutePath());

                        ++deleted;
                    }

                    toUnload.add(new RegionCoords(region.regionX, region.regionZ));
                    continue;
                }

                try {
                    if (region.save()) {
                        ++modified;
                    }
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                for (ChunkCoords chunkCoords : affected) {
                    if (world.isChunkLoaded(chunkCoords.x, chunkCoords.z)) {
                        ++loaded;
                    }
                }

                if (loaded == 0) {
                    toUnload.add(new RegionCoords(region.regionX, region.regionZ));
                }
            }

            for (RegionCoords regionCoords : toUnload) {
                worldMap.remove(regionCoords).unload();
            }
        }

        String msg = String.format("Light Sources persisted for World \"%s\", Files modified: %d, Files deleted: %d", world.getLevelProperties().getLevelName(), modified, deleted);

        if (source != null) {
            source.sendMessage(new LiteralText("[VarLight] " + msg));
        }

        mod.getLogger().debug(msg);
    }
}
