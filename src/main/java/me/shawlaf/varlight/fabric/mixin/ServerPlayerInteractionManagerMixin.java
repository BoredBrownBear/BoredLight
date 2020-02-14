package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {

    @Shadow
    public ServerWorld world;

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    private GameMode gameMode;

    @Shadow
    public abstract boolean isCreative();

    /**
     * @author Florian
     * <p>
     * This is just the vanilly tryBreakBlock Code with some VarLight checks and logic patched in
     */
    @Overwrite
    public boolean tryBreakBlock(BlockPos blockPos) {
        BlockState blockState = this.world.getBlockState(blockPos);
        if (!this.player.getMainHandStack().getItem().canMine(blockState, this.world, blockPos, this.player)) {
            return false;
        } else {
            BlockEntity blockEntity = this.world.getBlockEntity(blockPos);
            Block block = blockState.getBlock();

            if ((block instanceof CommandBlock || block instanceof StructureBlock || block instanceof JigsawBlock) && !this.player.isCreativeLevelTwoOp()) {
                this.world.updateListeners(blockPos, blockState, blockState, 3);
                return false;
            } else if (this.player.canMine(this.world, blockPos, this.gameMode)) {
                return false;
            } else {
                // Begin VarLight
                int customLuminance = VarLightMod.INSTANCE.getManager(this.world).getCustomLuminance(blockPos, 0);
                // End VarLight

                block.onBreak(this.world, blockPos, blockState, this.player);
                boolean bl = this.world.removeBlock(blockPos, false);
                if (bl) {
                    block.onBroken(this.world, blockPos, blockState);
                }

                if (this.isCreative()) {
                    return true;
                } else {
                    ItemStack itemStack = this.player.getMainHandStack();
                    ItemStack itemStack2 = itemStack.copy();
                    boolean bl2 = this.player.isUsingEffectiveTool(blockState);
                    itemStack.postMine(this.world, blockState, blockPos, this.player);
                    if (bl && bl2) {
                        // Begin VarLight
                        if (customLuminance > 0) {
                            // world.removeBlock also automatically removed the Lightsource, create it here again to allow BlockMixin to read the custom luminance
                            VarLightMod.INSTANCE.getManager(this.world).createPersistentLightSource(blockPos, customLuminance);
                        }
                        // End VarLight

                        block.afterBreak(this.world, this.player, blockPos, blockState, blockEntity, itemStack2);

                        // Begin VarLight
                        if (customLuminance > 0) {
                            VarLightMod.INSTANCE.getManager(this.world).deleteLightSource(blockPos);
                        }
                        // End VarLight
                    }

                    return true;
                }
            }
        }
    }

}
