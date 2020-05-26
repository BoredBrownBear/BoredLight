package me.shawlaf.varlight.fabric.impl;

import me.shawlaf.varlight.fabric.ILightStorageManager;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.fabric.persistence.migrate.LightDatabaseMigratorFabric;
import me.shawlaf.varlight.fabric.persistence.migrate.data.JsonToNLSMigration;
import me.shawlaf.varlight.fabric.persistence.migrate.data.VLDBToNLSMigration;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;

public class LightStorageManagerServer implements ILightStorageManager {

    private final VarLightMod mod;
    private final Map<String, WorldLightSourceManager> managers;
    private final LightDatabaseMigratorFabric databaseMigrator;

    public LightStorageManagerServer(VarLightMod mod) {
        this.mod = mod;
        this.managers = new HashMap<>();

        this.databaseMigrator = new LightDatabaseMigratorFabric(mod);

        databaseMigrator.addDataMigrations(new JsonToNLSMigration(), new VLDBToNLSMigration());
    }

    @Override
    public WorldLightSourceManager getManager(ServerWorld world) {
        String key = getWorldIdentifier(world);

        if (managers.containsKey(key)) {
            return managers.get(key);
        }

        WorldLightSourceManager manager = new WorldLightSourceManager(mod, world);
        managers.put(key, manager);

        return manager;
    }

    @Override
    public String getWorldIdentifier(ServerWorld world) {
        return world.getLevelProperties().getLevelName() + "/" + world.getDimension().getType().toString();
    }

    @Override
    public void onWorldUnload(ServerWorld world) {
        managers.remove(getWorldIdentifier(world));
    }

    @Override
    public LightDatabaseMigratorFabric getDatabaseMigrator() {
        return databaseMigrator;
    }
}
