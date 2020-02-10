package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
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

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Shadow
    public abstract World getWorld();

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
        World world = getWorld();
        int vanilla = getBlockState(pos).getLuminance();

        if (world instanceof ServerWorld) {
            int custom = VarLightMod.INSTANCE.getManager((ServerWorld) world).getCustomLuminance(pos, 0);

            return Math.max(vanilla, custom);
        } else {
            return vanilla;
        }
    }
}
