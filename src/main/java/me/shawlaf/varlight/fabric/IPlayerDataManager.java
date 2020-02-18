package me.shawlaf.varlight.fabric;

import me.shawlaf.varlight.fabric.persistence.nbt.VarLightPlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public interface IPlayerDataManager extends IModComponent {

    VarLightPlayerData getData(ServerPlayerEntity player);

    void remove(ServerPlayerEntity player);

    void clear();
}
