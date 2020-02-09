package me.shawlaf.varlight.fabric.persistence;

import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.persistence.RegionPersistor;
import me.shawlaf.varlight.util.RegionCoords;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
}
