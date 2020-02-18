package me.shawlaf.varlight.fabric.persistence.nbt;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VarLightData implements INbtSerializable {

    private static final Map<UUID, VarLightData> PLAYER_DATA = Collections.synchronizedMap(new HashMap<>());

    public static VarLightData get(ServerPlayerEntity player) {

        VarLightData data = PLAYER_DATA.get(player.getUuid());

        if (data == null) {
            PLAYER_DATA.put(player.getUuid(), data = new VarLightData(player));
        }

        return data;
    }

    public static void remove(ServerPlayerEntity player) {
        PLAYER_DATA.remove(player.getUuid());
    }

    public static void clear() {
        PLAYER_DATA.clear();
    }

    private byte stepSize = 1;
    private final ServerPlayerEntity player;

    public VarLightData(ServerPlayerEntity player) {
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
