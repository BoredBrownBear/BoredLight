package me.shawlaf.varlight.fabric.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.command.VarLightSubCommand;
import me.shawlaf.varlight.fabric.util.OpPermissionLevel;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.BlockPredicateArgumentType.BlockPredicate;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.arguments.BlockPosArgumentType.blockPos;
import static net.minecraft.command.arguments.BlockPredicateArgumentType.blockPredicate;
import static net.minecraft.command.arguments.BlockPredicateArgumentType.getBlockPredicate;

public class VarLightCommandFill extends VarLightSubCommand {
    public VarLightCommandFill(VarLightMod mod) {
        super(mod, "fill");
    }

    @Override
    public LiteralArgumentBuilder<ServerCommandSource> build(LiteralArgumentBuilder<ServerCommandSource> root) {

        root.requires(scs -> scs.hasPermissionLevel(OpPermissionLevel.MANAGE_GAME));

        root.then(
                RequiredArgumentBuilder.<ServerCommandSource, PosArgument>argument("from", blockPos())
                        .then(
                                RequiredArgumentBuilder.<ServerCommandSource, PosArgument>argument("to", blockPos())
                                        .then(
                                                RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("light level", integer(1, 15))
                                                        .executes(this::executeNoFilter)

                                                        .then(
                                                                LiteralArgumentBuilder.<ServerCommandSource>literal("include")
                                                                        .then(
                                                                                RequiredArgumentBuilder.<ServerCommandSource, BlockPredicate>argument("include filter", blockPredicate())
                                                                                        .executes(
                                                                                                c -> execute(c, getBlockPredicate(c, "include filter"))
                                                                                        )
                                                                        )
                                                        ).then(
                                                        LiteralArgumentBuilder.<ServerCommandSource>literal("exclude")
                                                                .then(
                                                                        RequiredArgumentBuilder.<ServerCommandSource, BlockPredicate>argument("exclude filter", blockPredicate())
                                                                                .executes(
                                                                                        c -> execute(c, getBlockPredicate(c, "exclude filter").negate())
                                                                                )
                                                                )
                                                )

                                        )
                        )
        );

        return root;
    }

    private int executeNoFilter(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return execute(context, cbp -> true);
    }

    private int execute(CommandContext<ServerCommandSource> context, Predicate<CachedBlockPosition> predicate) throws CommandSyntaxException {
        BlockPos from = BlockPosArgumentType.getLoadedBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getLoadedBlockPos(context, "to");

        PlayerEntity player = null;

        if (context.getSource().getEntity() != null) {
            if (context.getSource().getEntity() instanceof PlayerEntity) {
                player = context.getSource().getPlayer();
            }
        }

        int lightLevel = context.getArgument("light level", int.class);

        BlockBox blockBox = new BlockBox(from, to);

        Iterator<BlockPos> iterator = BlockPos.iterate(from, to).iterator();

        CachedBlockPosition cbp;

        int succeeded = 0, skipped = 0, failed = 0;

        Set<ChunkPos> toUpdate = new HashSet<>();

        while (iterator.hasNext()) {
            cbp = new CachedBlockPosition(context.getSource().getWorld(), iterator.next(), false);

            if (predicate.test(cbp)) {
                if (mod.getLightModifier().setLuminance(player, context.getSource().getWorld(), cbp.getBlockPos(), lightLevel, false).isSuccess()) {
                    ++succeeded;
                } else {
                    ++failed;
                }
            } else {
                ++skipped;
            }

            toUpdate.addAll(mod.getLightModifier().collectNeighbouringChunks(cbp.getBlockPos()));
        }

        for (ChunkPos chunkPos : toUpdate) {
            mod.getLightModifier().updateLight(context.getSource().getWorld(), chunkPos);
        }

        context.getSource().sendFeedback(
                new LiteralText(
                        String.format("Successfully updated %d Light sources in Region [%s] to [%s]. (Total blocks: %d, Skipped Blocks: %d, Failed Blocks: %d)",
                                succeeded,
                                from.toShortString(),
                                to.toShortString(),
                                blockBox.getBlockCountX() * blockBox.getBlockCountY() * blockBox.getBlockCountZ(),
                                skipped,
                                failed
                        )
                ),
                true
        );

        return succeeded;
    }
}
