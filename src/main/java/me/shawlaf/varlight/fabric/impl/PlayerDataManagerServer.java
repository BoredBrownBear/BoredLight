package me.shawlaf.varlight.fabric.impl;

import me.shawlaf.varlight.fabric.IPlayerDataManager;
import me.shawlaf.varlight.fabric.persistence.nbt.VarLightPlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManagerServer implements IPlayerDataManager {

    private final Map<UUID, VarLightPlayerData> dataMap = new HashMap<>();

    @Override
    public VarLightPlayerData getData(ServerPlayerEntity player) {
        if (!dataMap.containsKey(player.getUuid())) {
            dataMap.put(player.getUuid(), new VarLightPlayerData(player));
        }

        return dataMap.get(player.getUuid());
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        dataMap.remove(player.getUuid());
    }

    @Override
    public void clear() {
        dataMap.clear();
    }

    @Override
    public void onServerShutdown() {
        clear();
    }
}
