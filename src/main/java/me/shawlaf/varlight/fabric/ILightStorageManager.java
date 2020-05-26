package me.shawlaf.varlight.fabric;

import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.fabric.persistence.migrate.LightDatabaseMigratorFabric;
import me.shawlaf.varlight.fabric.util.ReflectionHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;

import java.io.File;
import java.lang.reflect.Field;

public interface ILightStorageManager extends IModComponent {

    WorldLightSourceManager getManager(ServerWorld world);

    String getWorldIdentifier(ServerWorld world);

    default File getVarLightSaveDirectory(ServerWorld world) {
        Field saveDirField = ReflectionHelper.getDeclaredField(ThreadedAnvilChunkStorage.class, "saveDir", "field_17707");
        saveDirField.setAccessible(true);

        File worldRoot;

        try {
            worldRoot = (File) saveDirField.get(world.getChunkManager().threadedAnvilChunkStorage);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get Save Directory for World " + ((ServerWorldProperties) world.getLevelProperties()).getLevelName(), e);
        }

        world.getServer().method_27050(WorldSavePath.ROOT);

        File saveDir = new File(worldRoot, "varlight");

        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new RuntimeException("Could not create VarLight directory \"" + saveDir.getAbsolutePath() + "\"");
            }
        }

        return saveDir;
    }

    LightDatabaseMigratorFabric getDatabaseMigrator();
}
