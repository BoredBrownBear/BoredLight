package me.shawlaf.varlight.fabric.persistence.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

public interface INbtSerializable {

    Identifier getKey();

    void writeToTag(CompoundTag tag);

    void readFromTag(CompoundTag tag);

}
