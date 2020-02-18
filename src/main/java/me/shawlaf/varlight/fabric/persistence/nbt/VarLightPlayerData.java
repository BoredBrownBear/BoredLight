package me.shawlaf.varlight.fabric.persistence.nbt;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VarLightPlayerData implements INbtSerializable {

    private static final Map<UUID, VarLightPlayerData> PLAYER_DATA = Collections.synchronizedMap(new HashMap<>());

    @Deprecated
    public static void clear() {
        PLAYER_DATA.clear();
    }

    private byte stepSize = 1;
    private final ServerPlayerEntity player;

    public VarLightPlayerData(ServerPlayerEntity player) {
        this.player = player;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public int getStepSize() {
        return stepSize;
    }

    public void setStepSize(int stepSize) {
        this.stepSize = (byte) stepSize;
    }

    @Override
    public Identifier getKey() {
        return new Identifier("varlight", "player_data");
    }

    @Override
    public void writeToTag(CompoundTag tag) {
        tag.put("stepSize", ByteTag.of(stepSize));
    }

    @Override
    public void readFromTag(CompoundTag tag) {
        this.stepSize = tag.getByte("stepSize");
    }
}
