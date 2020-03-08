package me.shawlaf.varlight.fabric.persistence.nbt;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class VarLightPlayerData implements INbtSerializable {

    private final ServerPlayerEntity player;
    private byte stepSize = 1;

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
