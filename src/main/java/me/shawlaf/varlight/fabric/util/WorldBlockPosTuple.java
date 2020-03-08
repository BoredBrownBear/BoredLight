package me.shawlaf.varlight.fabric.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;

public class WorldBlockPosTuple {
    private final World world;
    private final BlockPos blockPos;

    private WorldBlockPosTuple(World world, BlockPos blockPos) {
        this.world = world;
        this.blockPos = blockPos;
    }

    public static WorldBlockPosTuple of(World world, BlockPos blockPos) {
        return new WorldBlockPosTuple(world, blockPos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldBlockPosTuple that = (WorldBlockPosTuple) o;
        return Objects.equals(world, that.world) &&
                Objects.equals(blockPos, that.blockPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, blockPos);
    }
}