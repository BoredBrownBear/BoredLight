package me.shawlaf.varlight.fabric.impl;

import me.shawlaf.varlight.fabric.ILightStorageManager;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;

public class LightStorageManagerServer implements ILightStorageManager {

    private final VarLightMod mod;
    private final Map<String, WorldLightSourceManager> managers;

    public LightStorageManagerServer(VarLightMod mod) {
        this.mod = mod;
        this.managers = new HashMap<>();
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
}
