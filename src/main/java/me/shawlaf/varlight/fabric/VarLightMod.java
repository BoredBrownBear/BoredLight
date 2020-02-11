package me.shawlaf.varlight.fabric;

import me.shawlaf.varlight.fabric.command.VarLightCommand;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class VarLightMod implements ModInitializer {

    public static VarLightMod INSTANCE;

    private final VarLightCommand command = new VarLightCommand(this);
    private final Logger logger = LogManager.getLogger("VarLight");

    private final Map<String, WorldLightSourceManager> managers = new HashMap<>();

    {
        INSTANCE = this;
    }

    @Override
    public void onInitialize() {
        this.command.register();

        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
            if (!(world instanceof ServerWorld) || !(playerEntity instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }

            return useBlock(((ServerPlayerEntity) playerEntity), (ServerWorld) world, hand, blockHitResult);
        });

        AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> {
            if (!(world instanceof ServerWorld) || !(playerEntity instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }

            return attackBlock(((ServerPlayerEntity) playerEntity), (ServerWorld) world, hand, blockPos);
        });
    }

    public Logger getLogger() {
        return logger;
    }

    public LightUpdateResult setLuminance(PlayerEntity modifier, ServerWorld world, BlockPos blockPos, int lightLevel) {
        if (modifier != null && !world.canPlayerModifyAt(modifier, blockPos)) {
            return LightUpdateResult.CANNOT_MODIFY;
        }

        if (isIllegalBlock(world, blockPos)) {
            return LightUpdateResult.ILLEGAL_BLOCK;
        }

        if (lightLevel < 0) {
            return LightUpdateResult.ZERO_REACHED;
        }

        if (lightLevel > 15) {
            return LightUpdateResult.FIFTEEN_REACHED;
        }

        WorldLightSourceManager manager = getManager(world);

        manager.deleteLightSource(blockPos);

        ((ChunkBlockLightProvider) world.getLightingProvider().get(LightType.BLOCK)).checkBlock(blockPos);

        if (lightLevel > 0) {
            manager.createPersistentLightSource(blockPos, lightLevel);
            updateLight(world, blockPos);
        }

        return LightUpdateResult.SUCCESS;
    }

    private void setLightRaw(ServerWorld world, BlockPos blockPos, int lightLevel) {
        ((ChunkBlockLightProvider) world.getLightingProvider().get(LightType.BLOCK)).addLightSource(blockPos, lightLevel);
    }

    public CompletableFuture<Void> updateLight(ServerWorld world, BlockPos blockPos) {
        LightingProvider provider = world.getLightingProvider();

        return ((ServerLightingProvider) provider).light(world.getChunk(blockPos), false).thenRun(() -> {
            for (ChunkPos pos : collectLightUpdateChunks(blockPos)) {
                LightUpdateS2CPacket packet = new LightUpdateS2CPacket(pos, provider);

                world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(pos, false).forEach(spe -> {
                    spe.networkHandler.sendPacket(packet);
                });
            }
        });
    }

    public WorldLightSourceManager getManager(ServerWorld world) {
        final String key = getKey(world);

        if (!managers.containsKey(key)) {
            managers.put(key, new WorldLightSourceManager(this, world));
        }

        return managers.get(key);
    }

    public File getVarLightSaveDirectory(ServerWorld world) {
        File regionRoot = world.getDimension().getType().getSaveDirectory(world.getSaveHandler().getWorldDir());
        File saveDir = new File(regionRoot, "varlight");

        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new RuntimeException("Could not create VarLight directory \"" + saveDir.getAbsolutePath() + "\"");
            }
        }

        return saveDir;
    }

    public boolean isIllegalBlock(World world, BlockPos blockPos) {
        final BlockState blockState = world.getBlockState(blockPos);

        if (!blockState.isFullCube(world, blockPos)) {
            return true;
        }

        if (blockState.getLuminance() > 0) {
            return true;
        }

        return !blockState.isOpaque();
    }

    private List<ChunkPos> collectLightUpdateChunks(BlockPos center) {
        List<ChunkPos> list = new ArrayList<>();

        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        for (int cz = centerChunkZ - 1; cz <= centerChunkZ + 1; cz++) {
            for (int cx = centerChunkX - 1; cx <= centerChunkX + 1; cx++) {
                list.add(new ChunkPos(cx, cz));
            }
        }

        return list;
    }

    private String getKey(ServerWorld world) {
        return world.getLevelProperties().getLevelName() + "/" + world.getDimension().getType().toString();
    }

    private ActionResult useBlock(ServerPlayerEntity player, ServerWorld world, Hand hand, BlockHitResult hitResult) {
        return modLight(player, world, hand, hitResult.getBlockPos(), 1);
    }

    private ActionResult attackBlock(ServerPlayerEntity player, ServerWorld world, Hand hand, BlockPos blockPos) {
        return modLight(player, world, hand, blockPos, -1);
    }

    private ActionResult modLight(ServerPlayerEntity player, ServerWorld world, Hand hand, BlockPos blockPos, int mod) {
        ItemStack stack;

        switch (hand) {
            case MAIN_HAND: {
                stack = player.inventory.getMainHandStack();
                break;
            }

            case OFF_HAND: {
                stack = player.inventory.offHand.get(0);
                break;
            }

            default: {
                throw new IllegalStateException("More than 2 Hands???");
            }
        }

        if (stack == ItemStack.EMPTY || stack.getItem() != Items.GLOWSTONE_DUST) {
            return ActionResult.PASS;
        }

        int fromLight = world.getLuminance(blockPos); // Will return custom Luminance thanks to Mixins
        int toLight = fromLight + mod;

        LightUpdateResult result = setLuminance(player, world, blockPos, toLight);

        if (result.isSuccess()) {
            player.addChatMessage(new LiteralText("Updated Light level"), true);

            if (player.interactionManager.isSurvivalLike() && mod > 0) {
                stack.decrement(1);
            }
        } else {
            player.addChatMessage(new LiteralText(result.name()), true);
        }

        return ActionResult.SUCCESS;
    }
}
