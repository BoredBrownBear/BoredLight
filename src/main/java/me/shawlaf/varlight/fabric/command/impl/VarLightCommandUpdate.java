package me.shawlaf.varlight.fabric.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.command.VarLightSubCommand;
import me.shawlaf.varlight.fabric.persistence.PersistentLightSource;
import me.shawlaf.varlight.fabric.util.OpPermissionLevel;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.arguments.BlockPosArgumentType.blockPos;

public class VarLightCommandUpdate extends VarLightSubCommand {
    public VarLightCommandUpdate(VarLightMod mod) {
        super(mod, "update");
    }

    @Override
    public LiteralArgumentBuilder<ServerCommandSource> build(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.requires(
                source -> source.hasPermissionLevel(OpPermissionLevel.MANAGE_GAME)
        );

        root.then(
                RequiredArgumentBuilder.<ServerCommandSource, PosArgument>argument("pos", blockPos()).then(
                        RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("light level", integer(0, 15))
                                .executes(this::execute)
                )
        );

        return root;
    }

    private int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
        int lightLevel = context.getArgument("light level", int.class);

        PersistentLightSource pls = mod.getManager(context.getSource().getWorld()).createPersistentLightSource(pos, lightLevel);

        if (pls != null) {
            mod.setLuminance(context.getSource().getWorld(), pos, pls.getEmittingLight());
            context.getSource().sendFeedback(new LiteralText("Updated Light level at Position [" + pos.toShortString() + "] to " + lightLevel), true);
        }

        return 1;
    }
}
