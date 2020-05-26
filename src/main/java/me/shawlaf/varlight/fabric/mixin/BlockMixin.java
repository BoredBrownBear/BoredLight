package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.LightUpdateResult;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.util.WorldBlockPosTuple;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(Block.class)
public abstract class BlockMixin implements ItemConvertible {

    private static final Map<WorldBlockPosTuple, Integer> lightCache = new HashMap<>();

    @Inject(
            method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD")
    )
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }

        if (!(itemStack.getItem() instanceof BlockItem)) {
            return;
        }

        int ll = VarLightMod.INSTANCE.getGlowingBlockCreator().getGlowingValue(itemStack);

        if (ll > 0) {
            LightUpdateResult result = VarLightMod.INSTANCE.getLightModifier().setLuminance(
                    placer instanceof ServerPlayerEntity ? (ServerPlayerEntity) placer : null,
                    (ServerWorld) placer.world,
                    pos,
                    ll
            );

            if (placer instanceof ServerPlayerEntity) {
                result.sendActionBarMessage((ServerPlayerEntity) placer);
            }
        }
    }

    @Inject(
            method = "onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("HEAD")
    )
    public void beforeOnBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) world;

        int customLuminance = getMod().getLightStorageManager().getManager(serverWorld).getCustomLuminance(pos, 0);

        if (customLuminance > 0) {
            lightCache.put(WorldBlockPosTuple.of(serverWorld, pos), customLuminance);
        }
    }

    @Inject(
            method = "afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD")
    )
    public void beforeAfterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        WorldBlockPosTuple key = WorldBlockPosTuple.of(serverWorld, pos);

        if (!lightCache.containsKey(key)) {
            return;
        }

        // Create the custom light source to allow dropStacks(BlockState, World, BlockPos, BlockEntity, Entity, ItemStack) to read the Custom Light value
        getMod().getLightStorageManager().getManager(serverWorld).setCustomLuminance(pos, lightCache.get(key));
    }

    @Inject(
            method = "afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("TAIL")
    )
    public void afterAfterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        WorldBlockPosTuple key = WorldBlockPosTuple.of(world, pos);

        if (lightCache.remove(key) != null) {
            // Block was broken, delete the temporary light source
            getMod().getLightStorageManager().getManager(serverWorld).deleteLightSource(pos);
        }
    }

    private VarLightMod getMod() {
        return VarLightMod.INSTANCE;
    }

}
