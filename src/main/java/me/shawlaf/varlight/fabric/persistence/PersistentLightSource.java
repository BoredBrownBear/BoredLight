package me.shawlaf.varlight.fabric.persistence;

import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.Objects;

import static me.shawlaf.varlight.fabric.util.IntPositionExtension.getBlockState;
import static me.shawlaf.varlight.fabric.util.IntPositionExtension.toBlockPos;

public class PersistentLightSource implements ICustomLightSource {

    private final IntPosition position;
    private Block type;
    boolean migrated = false;
    private transient ServerWorld world;
    private transient VarLightMod mod;
    private int emittingLight;

    PersistentLightSource(VarLightMod mod, ServerWorld world, IntPosition position, int emittingLight) {
        Objects.requireNonNull(mod);
        Objects.requireNonNull(world);
        Objects.requireNonNull(position);

        this.mod = mod;
        this.world = world;
        this.position = position;
        this.type = getBlockState(position, world).getBlock();
        this.emittingLight = (emittingLight & 0xF);
    }

    PersistentLightSource(VarLightMod mod, IntPosition position, Block type, boolean migrated, ServerWorld world, int emittingLight) {
        Objects.requireNonNull(mod);
        Objects.requireNonNull(position);
        Objects.requireNonNull(type);
        Objects.requireNonNull(world);

        this.mod = mod;
        this.position = position;
        this.type = type;
        this.migrated = migrated;
        this.world = world;
        this.emittingLight = emittingLight;
    }

    @Override
    public IntPosition getPosition() {
        return position;
    }

    @Override
    public String getType() {
        return Registry.BLOCK.getId(type).toString();
    }

    @Override
    public boolean isMigrated() {
        return migrated;
    }

    @Override
    public int getCustomLuminance() {
        return emittingLight;
    }

    public ServerWorld getWorld() {
        return world;
    }

    public boolean needsMigration() {
        return !migrated;
    }

    public boolean migrate() {
        if (needsMigration() && world.isChunkLoaded(position.getChunkX(), position.getChunkZ())) {
            BlockPos blockPos = toBlockPos(position);

            if (!world.isChunkLoaded(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
                return false;
            }

            boolean success = mod.getLightModifier().setLuminance(null, world, blockPos, emittingLight).isSuccess();

            migrated = success;

            return success;
        }

        return false;
    }

    public boolean isInvalid() {
        return false; // TODO
    }

    public void update(WorldLightSourceManager manager, BlockState oldState, BlockState newState) {
        mod.getScheduledTaskManager().enqueue(() -> {
            if (oldState.getBlock() == newState.getBlock() || newState.isFullCube(world, toBlockPos(position))) {
                Block oldType = type;
                type = newState.getBlock();

                if (oldType != type) {
                    manager.getRegionPersistor(position.toRegionCoords()).markDirty(position);
                }
            } else {
                manager.deleteLightSource(toBlockPos(position));
            }
        });
    }

    @Override
    public String toString() {
        return "PersistentLightSource{" +
                "position=" + position +
                ", type=" + getType() +
                ", migrated=" + migrated +
                ", world=" + world.getLevelProperties().getLevelName() +
                ", emittingLight=" + emittingLight +
                '}';
    }

    public int getEmittingLight() {
        return getCustomLuminance(); // TODO
    }
}