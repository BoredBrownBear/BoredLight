package me.shawlaf.varlight.fabric.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.server.command.ServerCommandSource;

public abstract class VarLightSubCommand {

    protected final VarLightMod mod;
    protected final String name;

    public VarLightSubCommand(VarLightMod mod, String name) {
        this.mod = mod;
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public abstract LiteralArgumentBuilder<ServerCommandSource> build(LiteralArgumentBuilder<ServerCommandSource> root);

}
