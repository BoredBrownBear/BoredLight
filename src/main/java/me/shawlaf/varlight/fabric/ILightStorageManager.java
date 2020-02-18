package me.shawlaf.varlight.fabric;

import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import net.minecraft.server.world.ServerWorld;

import java.io.File;

public interface ILightStorageManager extends IModComponent {

    WorldLightSourceManager getManager(ServerWorld world);

    String getWorldIdentifier(ServerWorld world);

    default File getVarLightSaveDirectory(ServerWorld world) {
        File regionRoot = world.getDimension().getType().getSaveDirectory(world.getSaveHandler().getWorldDir());
        File saveDir = new File(regionRoot, "varlight");

        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new RuntimeException("Could not create VarLight directory \"" + saveDir.getAbsolutePath() + "\"");
            }
        }

        return saveDir;
    }
}
