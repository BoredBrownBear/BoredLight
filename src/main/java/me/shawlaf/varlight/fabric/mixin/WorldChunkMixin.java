package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements Chunk {

    @Final
    @Shadow
    private ChunkPos pos;

    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    /**
     * @author Florian
     */
    @Overwrite
    public Stream<BlockPos> getLightSourcesStream() {
        return StreamSupport.stream(BlockPos.iterate(this.pos.getStartX(), 0, this.pos.getStartZ(), this.pos.getEndX(), 255, this.pos.getEndZ()).spliterator(), false).filter((blockPos) -> {
            return getLuminance(blockPos) != 0; // TODO Implement custom light source check.
        });
    }

    @Override
    public int getLuminance(BlockPos pos) {
        int vanilla = getBlockState(pos).getLuminance();
        int custom = VarLightMod.INSTANCE.getCustomLuminance(pos);

        return Math.max(vanilla, custom);
    }
}
