package me.shawlaf.varlight.fabric.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.command.impl.*;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class VarLightCommand {

    private static final Class[] COMMANDS = {
            VarLightCommandDebug.class,
            VarLightCommandFill.class,
            VarLightCommandGive.class,
            VarLightCommandMigrate.class,
            VarLightCommandUpdate.class,
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

    public static int getAmountPages(List<?> all, int pageSize) {
        return (all.size() / pageSize) + (all.size() % pageSize > 0 ? 1 : 0);
    }

    public static <T> List<T> paginateEntries(List<T> all, int pageSize, int page) {
        List<T> toReturn = new ArrayList<>();

        int start = (page - 1) * (pageSize);

        for (int i = 0, index = start; i < pageSize; i++, ++index) {
            if (index >= all.size()) {
                break;
            }

            toReturn.add(all.get(index));
        }

        return toReturn;
    }

}
