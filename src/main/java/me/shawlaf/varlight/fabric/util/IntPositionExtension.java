package me.shawlaf.varlight.fabric.util;

import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class IntPositionExtension {

    private IntPositionExtension() {
        throw new IllegalStateException();
    }

    public static BlockPos toBlockPos(IntPosition position) {
        return new BlockPos(position.x, position.y, position.z);
    }

    public static BlockState getBlockState(IntPosition position, World world) {
        return world.getBlockState(toBlockPos(position));
    }

    public static IntPosition toIntPosition(BlockPos blockPos) {
        return new IntPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

}
