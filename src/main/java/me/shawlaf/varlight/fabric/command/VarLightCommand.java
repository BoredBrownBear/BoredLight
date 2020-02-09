package me.shawlaf.varlight.fabric.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.command.impl.VarLightCommandUpdate;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.InvocationTargetException;

public class VarLightCommand {

    private static final Class[] COMMANDS = {
            VarLightCommandUpdate.class
    };

    private final VarLightMod mod;

    public VarLightCommand(VarLightMod mod) {
        this.mod = mod;
    }

    public void register() {
        CommandRegistry.INSTANCE.register(false, dispatcher -> {
            LiteralArgumentBuilder<ServerCommandSource> root = LiteralArgumentBuilder.literal("varlight");

            for (Class commandClass : COMMANDS) {
                try {
                    VarLightSubCommand subCommand = (VarLightSubCommand) commandClass.getConstructor(VarLightMod.class).newInstance(mod);
                    root.then(subCommand.build(LiteralArgumentBuilder.literal(subCommand.getName())));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    mod.getLogger().error("Failed to register command " + commandClass.getSimpleName(), e);
                }
            }

            dispatcher.register(root);
        });
    }

}
