package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.advancement.criterion.Criterions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
    @Shadow
    public abstract ItemPlacementContext getPlacementContext(ItemPlacementContext context);

    @Shadow
    protected abstract BlockState getPlacementState(ItemPlacementContext context);

    @Shadow
    protected abstract BlockState placeFromTag(BlockPos pos, World world, ItemStack stack, BlockState state);

    @Shadow
    protected abstract boolean postPlacement(BlockPos pos, World world, PlayerEntity player, ItemStack stack, BlockState state);

    @Shadow
    protected abstract SoundEvent getPlaceSound(BlockState state);

    @Shadow
    protected abstract boolean place(ItemPlacementContext context, BlockState state);

    public BlockItemMixin(Settings settings) {
        super(settings);
    }

    /**
     * @author
     */
    @Overwrite
    public ActionResult place(ItemPlacementContext context) {
        if (!context.canPlace()) {
            return ActionResult.FAIL;
        } else {
            ItemPlacementContext itemPlacementContext = this.getPlacementContext(context);
            if (itemPlacementContext == null) {
                return ActionResult.FAIL;
            } else {
                BlockState blockState = this.getPlacementState(itemPlacementContext);
                if (blockState == null) {
                    return ActionResult.FAIL;
                } else if (!this.place(itemPlacementContext, blockState)) {
                    return ActionResult.FAIL;
                } else {
                    BlockPos blockPos = itemPlacementContext.getBlockPos();
                    World world = itemPlacementContext.getWorld();
                    PlayerEntity playerEntity = itemPlacementContext.getPlayer();
                    ItemStack itemStack = itemPlacementContext.getStack();
                    BlockState blockState2 = world.getBlockState(blockPos);
                    Block block = blockState2.getBlock();
                    if (block == blockState.getBlock()) {
                        blockState2 = this.placeFromTag(blockPos, world, itemStack, blockState2);
                        this.postPlacement(blockPos, world, playerEntity, itemStack, blockState2);
                        block.onPlaced(world, blockPos, blockState2, playerEntity, itemStack);
                        if (playerEntity instanceof ServerPlayerEntity) {
                            Criterions.PLACED_BLOCK.trigger((ServerPlayerEntity) playerEntity, blockPos, itemStack);
                        }
                    }

                    BlockSoundGroup blockSoundGroup = blockState2.getSoundGroup();
                    world.playSound(playerEntity, blockPos, this.getPlaceSound(blockState2), SoundCategory.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);

                    // Begin VarLight
                    if (!world.isClient) {
                        ServerPlayerEntity spe = (ServerPlayerEntity) playerEntity;

                        VarLightMod.INSTANCE.onBlockPlaced(spe, itemStack, blockPos);
                    }
                    // End VarLight

                    itemStack.decrement(1);
                    return ActionResult.SUCCESS;
                }
            }
        }
    }
}
