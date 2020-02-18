package me.shawlaf.varlight.fabric;

import me.shawlaf.varlight.fabric.command.VarLightCommand;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.fabric.persistence.nbt.VarLightData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Language;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VarLightMod implements ModInitializer {

    public static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    public static final String KEY_GLOWING = "varlight:glowing";
    public static final String COLOR_SYMBOL = "\u00a7";

    public static VarLightMod INSTANCE;

    private final VarLightCommand command = new VarLightCommand(this);
    private final Logger logger = LogManager.getLogger("VarLight");

    private final Map<String, WorldLightSourceManager> managers = new HashMap<>();

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

            return useBlock(((ServerPlayerEntity) playerEntity), (ServerWorld) world, hand, blockHitResult);
        });

        AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> {
            if (!(world instanceof ServerWorld) || !(playerEntity instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }

            if (((ServerPlayerEntity) playerEntity).interactionManager.getGameMode() == GameMode.SPECTATOR) {
                return ActionResult.PASS;
            }

            return attackBlock(((ServerPlayerEntity) playerEntity), (ServerWorld) world, hand, blockPos);
        });

    }

    public Logger getLogger() {
        return logger;
    }

    public LightUpdateResult setLuminance(PlayerEntity modifier, ServerWorld world, BlockPos blockPos, int lightLevel) {
        return setLuminance(modifier, world, blockPos, lightLevel, true);
    }

    public LightUpdateResult setLuminance(PlayerEntity modifier, ServerWorld world, BlockPos blockPos, int lightLevel, boolean update) {
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

        if (update) {
            manager.deleteLightSource(blockPos);

            ((ChunkBlockLightProvider) world.getLightingProvider().get(LightType.BLOCK)).checkBlock(blockPos);
        }

        if (lightLevel > 0) {
            manager.createPersistentLightSource(blockPos, lightLevel);
        }

        if (update) {
            updateLight(world, blockPos);
        }

        return LightUpdateResult.success(lightLevel);
    }

    private void setLightRaw(ServerWorld world, BlockPos blockPos, int lightLevel) {
        ((ChunkBlockLightProvider) world.getLightingProvider().get(LightType.BLOCK)).addLightSource(blockPos, lightLevel);
    }

    public void updateLight(ServerWorld world, BlockPos blockPos) {
        updateLight(world, new ChunkPos(blockPos));
    }

    public void updateLight(ServerWorld world, ChunkPos chunkPos) {
        LightingProvider provider = world.getLightingProvider();

        ((ServerLightingProvider) provider).light(world.getChunk(chunkPos.x, chunkPos.z), false).thenRun(() -> {
            TASKS.add(() -> {
                for (ChunkPos pos : collectLightUpdateChunks(chunkPos)) {
                    LightUpdateS2CPacket packet = new LightUpdateS2CPacket(pos, provider);

                    world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(pos, false).forEach(spe -> {
                        spe.networkHandler.sendPacket(packet);
                    });
                }
            });
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

    public void onBlockPlaced(ServerPlayerEntity player, ItemStack stack, BlockPos blockPos) {
        if (!stack.hasTag()) {
            return;
        }

        //noinspection ConstantConditions not-Null Check already performed by ItemStack#hasTag
        if (!stack.getTag().contains(KEY_GLOWING)) {
            return;
        }

        if (!(stack.getItem() instanceof BlockItem)) {
            return;
        }

        int ll = stack.getTag().getInt(KEY_GLOWING);

        setLuminance(player, (ServerWorld) player.world, blockPos, ll).sendActionBarMessage(player);
    }

    public ItemStack makeGlowing(ItemStack base, int lightLevel) {
        base.getOrCreateTag().put(VarLightMod.KEY_GLOWING, IntTag.of(lightLevel));

        CompoundTag displayTag = base.getOrCreateSubTag("display");

        displayTag.putString("Name", Text.Serializer.toJson(getDisplayText(base.getItem())));
        ListTag loreTag = new ListTag();

        List<Text> lore = getLore(lightLevel);

        for (Text text : lore) {
            loreTag.add(StringTag.of(Text.Serializer.toJson(text)));
        }

        displayTag.put("Lore", loreTag);

        return base;
    }

    public List<ChunkPos> collectLightUpdateChunks(BlockPos center) {
        return collectLightUpdateChunks(new ChunkPos(center));
    }

    public List<ChunkPos> collectLightUpdateChunks(ChunkPos center) {
        List<ChunkPos> list = new ArrayList<>();

        int centerChunkX = center.x;
        int centerChunkZ = center.z;

        for (int cz = centerChunkZ - 1; cz <= centerChunkZ + 1; cz++) {
            for (int cx = centerChunkX - 1; cx <= centerChunkX + 1; cx++) {
                list.add(new ChunkPos(cx, cz));
            }
        }

        return list;
    }

    public String getKey(ServerWorld world) {
        return world.getLevelProperties().getLevelName() + "/" + world.getDimension().getType().toString();
    }

    private ItemStack getStack(ServerPlayerEntity player, Hand hand) {
        switch (hand) {
            case MAIN_HAND: {
                return player.inventory.getMainHandStack();
            }

            case OFF_HAND: {
                return player.inventory.offHand.get(0);
            }

            default: {
                throw new IllegalStateException("More than 2 Hands???");
            }
        }
    }

    private ActionResult useBlock(ServerPlayerEntity player, ServerWorld world, Hand hand, BlockHitResult hitResult) {
        return modLight(player, getStack(player, hand), world, hitResult.getBlockPos(), 1);
    }

    private ActionResult attackBlock(ServerPlayerEntity player, ServerWorld world, Hand hand, BlockPos blockPos) {
        return modLight(player, getStack(player, hand), world, blockPos, -1);
    }

    private ActionResult modLight(ServerPlayerEntity player, ItemStack stack, ServerWorld world, BlockPos blockPos, int mod) {
        if (stack == ItemStack.EMPTY || stack.getItem() != Items.GLOWSTONE_DUST) {
            return ActionResult.PASS;
        }

        int fromLight = world.getLuminance(blockPos); // Will return custom Luminance thanks to Mixins

        if (mod > 0) {
            mod *= Math.min(Math.min(VarLightData.get(player).getStepSize(), stack.getCount()), 15 - fromLight);
        } else {
            mod *= Math.min(VarLightData.get(player).getStepSize(), fromLight);
        }

        int toLight = fromLight + mod;

        LightUpdateResult result = setLuminance(player, world, blockPos, toLight);

        if (result.isSuccess()) {
            if (player.interactionManager.isSurvivalLike() && mod > 0) {
                stack.decrement(Math.abs(mod));
            }
        }

        result.sendActionBarMessage(player);

        return ActionResult.SUCCESS;
    }

    private Text getDisplayText(Item item) {
        return new LiteralText(COLOR_SYMBOL + "r" + COLOR_SYMBOL + "6Glowing " + Language.getInstance().translate(item.getTranslationKey()));
    }

    private List<Text> getLore(int lightLevel) {
        return Collections.singletonList(new LiteralText(COLOR_SYMBOL + "rEmitting Light: " + lightLevel));
    }
}
