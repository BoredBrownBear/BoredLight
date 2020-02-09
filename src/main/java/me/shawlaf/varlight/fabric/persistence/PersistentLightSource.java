package me.shawlaf.varlight.fabric.persistence;

import me.shawlaf.varlight.fabric.util.IntPositionExtension;
import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.Registry;

import java.util.Objects;

import static me.shawlaf.varlight.fabric.util.IntPositionExtension.getBlockState;

public class PersistentLightSource implements ICustomLightSource {

    private final IntPosition position;
    private final Block type;
    boolean migrated = false;
    private transient ServerWorld world;
    private int emittingLight;

    PersistentLightSource(ServerWorld world, IntPosition position, int emittingLight) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(position);

        this.world = world;
        this.position = position;
        this.type = getBlockState(position, world).getBlock();
        this.emittingLight = (emittingLight & 0xF);
    }

    PersistentLightSource(IntPosition position, Block type, boolean migrated, ServerWorld world, int emittingLight) {
        Objects.requireNonNull(position);
        Objects.requireNonNull(type);
        Objects.requireNonNull(world);

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

    public void migrate() {
        throw new RuntimeException("Not yet Implemented"); // TODO
    }

    public void update() {
        if (needsMigration() && world.isChunkLoaded(position.getChunkX(), position.getChunkZ())) {
            migrate();
        }
    }

    public boolean isInvalid() {
        throw new RuntimeException("Not yet Implemented"); // TODO
    }

    public String toCompactString(boolean colored) {
        throw new RuntimeException("Not yet Implemented"); // TODO
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
}
