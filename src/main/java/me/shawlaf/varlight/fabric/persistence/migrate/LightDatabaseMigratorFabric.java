package me.shawlaf.varlight.fabric.persistence.migrate;

import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.persistence.migrate.LightDatabaseMigrator;
import net.minecraft.server.world.ServerWorld;

import java.io.File;

public class LightDatabaseMigratorFabric extends LightDatabaseMigrator<ServerWorld> {

    private final VarLightMod mod;

    public LightDatabaseMigratorFabric(VarLightMod mod) {
        super(mod.getJavaUtilLogger());

        this.mod = mod;
    }

    @Override
    protected File getVarLightSaveDirectory(ServerWorld world) {
        return mod.getLightStorageManager().getVarLightSaveDirectory(world);
    }

    @Override
    protected String getName(ServerWorld world) {
        return mod.getLightStorageManager().getWorldIdentifier(world);
    }
}
