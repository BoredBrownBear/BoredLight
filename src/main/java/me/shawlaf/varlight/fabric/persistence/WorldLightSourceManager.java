package me.shawlaf.varlight.fabric.persistence;

import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.persistence.RegionPersistor;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoords;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.IntSupplier;

import static me.shawlaf.varlight.fabric.util.IntPositionExtension.toIntPosition;

public class WorldLightSourceManager {

    private final Map<RegionCoords, NLSFile> worldMap;
    private final ServerWorld world;
    private final VarLightMod mod;

    public WorldLightSourceManager(VarLightMod mod, ServerWorld world) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(mod);

        this.mod = mod;
        this.world = world;
        this.worldMap = new HashMap<>();

        synchronized (worldMap) {
            mod.getLightStorageManager().getVarLightSaveDirectory(world);

            mod.getLightStorageManager().getDatabaseMigrator().runMigrations(world);
        }

        mod.getLogger().debug(String.format("Created a new Lightsource Persistor for world \"%s\"", world.getLevelProperties().getLevelName()));
    }

    public VarLightMod getMod() {
        return mod;
    }

    public ServerWorld getWorld() {
        return world;
    }

    public void deleteLightSource(BlockPos blockPos) {
        setCustomLuminance(blockPos, 0);
    }

    public int getCustomLuminance(BlockPos pos, int def) {
        return getCustomLuminance(toIntPosition(pos), def);
    }

    public int getCustomLuminance(IntPosition position, int def) {
        return getCustomLuminance(position, () -> def);
    }

    public int getCustomLuminance(IntPosition position, IntSupplier def) {
        int custom = getNLSFile(position.toRegionCoords()).getCustomLuminance(position);

        if (custom == 0) {
            return def.getAsInt();
        }

        return custom;
    }

    public void setCustomLuminance(BlockPos blockPos, int luminance) {
        setCustomLuminance(toIntPosition(blockPos), luminance);
    }

    public void setCustomLuminance(IntPosition position, int luminance) {
        getNLSFile(position.toRegionCoords()).setCustomLuminance(position, luminance);
    }

    public NLSFile getNLSFile(RegionCoords regionCoords) {
        synchronized (worldMap) {
            if (!worldMap.containsKey(regionCoords)) {
                File file = new File(mod.getLightStorageManager().getVarLightSaveDirectory(world), String.format(NLSFile.FILE_NAME_FORMAT, regionCoords.x, regionCoords.z));
                NLSFile nlsFile;

                try {
                    if (file.exists()) {
                        nlsFile = NLSFile.existingFile(file);
                    } else {
                        nlsFile = NLSFile.newFile(file, regionCoords.x, regionCoords.z);
                    }
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                worldMap.put(regionCoords, nlsFile);
            }

            return worldMap.get(regionCoords);
        }
    }

    public void save(ServerPlayerEntity source) {
        int modified = 0, deleted = 0;
        List<RegionCoords> regionsToUnload = new ArrayList<>();

        synchronized (worldMap) {
            for (NLSFile nlsFile : worldMap.values()) {

                try {
                    if (nlsFile.save()) {
                        ++modified;
                    }
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                List<ChunkCoords> affected = nlsFile.getAffectedChunks();

                if (affected.size() == 0) {
                    if (nlsFile.file.exists()) {
                        if (!nlsFile.file.delete()) {
                            throw new LightPersistFailedException("Could not delete file " + nlsFile.file.getAbsolutePath());
                        } else {
                            mod.getLogger().debug(String.format("Deleted File %s", nlsFile.file.getName()));

                            ++deleted;
                        }
                    }

                    regionsToUnload.add(nlsFile.getRegionCoords());
                    continue;
                }

                boolean anyLoaded = false;

                for (ChunkCoords chunkCoords : affected) {
                    if (world.isChunkLoaded(chunkCoords.x, chunkCoords.z)) {
                        anyLoaded = true;
                        break;
                    }
                }

                if (!anyLoaded) {
                    regionsToUnload.add(nlsFile.getRegionCoords());
                }
            }

            for (RegionCoords regionCoords : regionsToUnload) {
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
