package me.shawlaf.varlight.fabric;

import me.shawlaf.varlight.fabric.command.VarLightCommand;
import me.shawlaf.varlight.fabric.impl.LightModifierServer;
import me.shawlaf.varlight.fabric.impl.LightStorageManagerServer;
import me.shawlaf.varlight.fabric.impl.PlayerDataManagerServer;
import me.shawlaf.varlight.fabric.impl.ScheduledTaskManagerServer;
import me.shawlaf.varlight.fabric.persistence.migrate.LightDatabaseMigratorFabric;
import me.shawlaf.varlight.fabric.util.PlayerUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VarLightMod implements ModInitializer, IModComponent {

    public static VarLightMod INSTANCE;

    private final VarLightCommand command = new VarLightCommand(this);
    private final Logger logger = LogManager.getLogger("VarLight");
    private final java.util.logging.Logger javaUtilLogger = java.util.logging.Logger.getLogger("VarLight");

    private ILightModifier lightModifier;
    private ILightStorageManager storageManager;
    private GlowingBlockCreator glowingBlockCreator;
    private IPlayerDataManager playerDataManager;
    private IScheduledTaskManager scheduledTaskManager;
    private LightDatabaseMigratorFabric databaseMigrator;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        this.command.register();

        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
            if (!(world instanceof ServerWorld) || !(playerEntity instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }

            if (((ServerPlayerEntity) playerEntity).interactionManager.getGameMode() == GameMode.SPECTATOR) {
                return ActionResult.PASS;
            }

            return modLight(
                    (ServerPlayerEntity) playerEntity,
                    PlayerUtil.getStackInHand(playerEntity, hand),
                    (ServerWorld) world,
                    blockHitResult.getBlockPos(),
                    1
            );
        });

        AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> {
            if (!(world instanceof ServerWorld) || !(playerEntity instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }

            if (((ServerPlayerEntity) playerEntity).interactionManager.getGameMode() == GameMode.SPECTATOR) {
                return ActionResult.PASS;
            }

            return modLight(
                    (ServerPlayerEntity) playerEntity,
                    PlayerUtil.getStackInHand(playerEntity, hand),
                    (ServerWorld) world,
                    blockPos,
                    -1
            );
        });

        ServerStopCallback.EVENT.register(s -> onServerShutdown());

        this.lightModifier = new LightModifierServer(this);
        this.storageManager = new LightStorageManagerServer(this);
        this.glowingBlockCreator = new GlowingBlockCreator();
        this.playerDataManager = new PlayerDataManagerServer();
        this.scheduledTaskManager = new ScheduledTaskManagerServer();

        onModInitialize();
    }

    @Override
    public void onModInitialize() {
        lightModifier.onModInitialize();
        storageManager.onModInitialize();
        playerDataManager.onModInitialize();
        scheduledTaskManager.onModInitialize();
    }

    @Override
    public void onWorldUnload(ServerWorld world) {
        lightModifier.onWorldUnload(world);
        storageManager.onWorldUnload(world);
        playerDataManager.onWorldUnload(world);
        scheduledTaskManager.onWorldUnload(world);
    }

    @Override
    public void onServerShutdown() {
        lightModifier.onServerShutdown();
        storageManager.onServerShutdown();
        playerDataManager.onServerShutdown();
        scheduledTaskManager.onServerShutdown();
    }

    public Logger getLogger() {
        return logger;
    }

    public java.util.logging.Logger getJavaUtilLogger() {
        return javaUtilLogger;
    }

    public ILightModifier getLightModifier() {
        return lightModifier;
    }

    public ILightStorageManager getLightStorageManager() {
        return storageManager;
    }

    public GlowingBlockCreator getGlowingBlockCreator() {
        return glowingBlockCreator;
    }

    public IPlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public IScheduledTaskManager getScheduledTaskManager() {
        return scheduledTaskManager;
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

    private ActionResult modLight(ServerPlayerEntity player, ItemStack stack, ServerWorld world, BlockPos blockPos, int mod) {
        if (stack == ItemStack.EMPTY || stack.getItem() != Items.GLOWSTONE_DUST) {
            return ActionResult.PASS;
        }

        int stepSize = playerDataManager.getData(player).getStepSize();

        int fromLight = world.getLuminance(blockPos); // Will return custom Luminance thanks to Mixins

        if (mod > 0) {
            mod *= Math.min(Math.min(stepSize, stack.getCount()), 15 - fromLight);
        } else {
            mod *= Math.min(stepSize, fromLight);
        }

        int toLight = fromLight + mod;

        LightUpdateResult result = lightModifier.setLuminance(player, world, blockPos, toLight);

        if (result.isSuccess()) {
            if (player.interactionManager.isSurvivalLike() && mod > 0) {
                stack.decrement(Math.abs(mod));
            } else if (player.interactionManager.isSurvivalLike() && mod < 0) {
                Block.dropStack(world, blockPos, new ItemStack(Items.GLOWSTONE_DUST, -mod));
            }
        }

        result.sendActionBarMessage(player);

        return ActionResult.SUCCESS;
    }

}
