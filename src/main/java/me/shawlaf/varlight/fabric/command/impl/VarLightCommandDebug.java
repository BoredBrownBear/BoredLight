package me.shawlaf.varlight.fabric.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.command.VarLightCommand;
import me.shawlaf.varlight.fabric.command.VarLightSubCommand;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.fabric.util.OpPermissionLevel;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoords;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.function.ToIntFunction;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public class VarLightCommandDebug extends VarLightSubCommand {

    private static final int PAGE_SIZE = 10;

    private static final RequiredArgumentBuilder<ServerCommandSource, Integer> ARG_REGION_X = argument("regionX", integer());
    private static final RequiredArgumentBuilder<ServerCommandSource, Integer> ARG_REGION_Z = argument("regionZ", integer());

    private static final RequiredArgumentBuilder<ServerCommandSource, Integer> ARG_CHUNK_X = argument("chunkX", integer());
    private static final RequiredArgumentBuilder<ServerCommandSource, Integer> ARG_CHUNK_Z = argument("chunkZ", integer());

    public VarLightCommandDebug(VarLightMod mod) {
        super(mod, "debug");
    }

    private void suggestCoordinate(RequiredArgumentBuilder<ServerCommandSource, Integer> coordinateArgument, ToIntFunction<Entity> coordinateSupplier) {
        coordinateArgument.suggests(((context, builder) -> {
            if (context.getSource().getEntity() == null) {
                return builder.buildFuture();
            }

            builder.suggest(coordinateSupplier.applyAsInt(context.getSource().getEntity()));

            return builder.buildFuture();
        }));
    }

    @Override
    public LiteralArgumentBuilder<ServerCommandSource> build(LiteralArgumentBuilder<ServerCommandSource> root) {
        suggestCoordinate(ARG_REGION_X, e -> e.chunkX >> 5);
        suggestCoordinate(ARG_REGION_Z, e -> e.chunkZ >> 5);
        suggestCoordinate(ARG_CHUNK_X, e -> e.chunkX);
        suggestCoordinate(ARG_CHUNK_Z, e -> e.chunkZ);

        root.requires(scs -> scs.hasPermissionLevel(OpPermissionLevel.MANAGE_GAME));

//        root.then(LiteralArgumentBuilder.<ServerCommandSource>literal("stick").executes(this::executeStick));

        root.then(
                LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                        .then(
                                LiteralArgumentBuilder.<ServerCommandSource>literal("region")
                                        .executes(this::listRegionImplicit)
                                        .then(
                                                ARG_REGION_X
                                                        .then(
                                                                ARG_REGION_Z.executes(c -> listRegionExplicit(c, 1))
                                                                        .then(
                                                                                RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("rpage", integer(1))
                                                                                        .executes(c -> listRegionExplicit(c, c.getArgument("rpage", int.class)))
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                LiteralArgumentBuilder.<ServerCommandSource>literal("chunk")
                                        .executes(this::listChunkImplicit)
                                        .then(
                                                ARG_CHUNK_X
                                                        .then(
                                                                ARG_CHUNK_Z.executes(c -> listChunkExplicit(c, 1))
                                                                        .then(
                                                                                RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("cpage", integer(1))
                                                                                        .executes(c -> listChunkExplicit(c, c.getArgument("cpage", int.class)))
                                                                        )
                                                        )
                                        )
                        )
        );


        return root;
    }

    private int executeStick(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        context.getSource().sendError(new LiteralText("not yet implemented!"));

        return -1;
    }

    private int listRegionImplicit(CommandContext<ServerCommandSource> context) {
        Entity entity = context.getSource().getEntity();

        if (entity == null) {
            context.getSource().sendError(new LiteralText("You must be a Player to use this command."));
            return -1;
        }

        int regionX = entity.chunkX >> 5;
        int regionZ = entity.chunkZ >> 5;

        return listRegion(context.getSource(), regionX, regionZ, 1);
    }

    private int listRegionExplicit(CommandContext<ServerCommandSource> context, int page) {
        int regionX = context.getArgument(ARG_REGION_X.getName(), int.class);
        int regionZ = context.getArgument(ARG_REGION_Z.getName(), int.class);

        return listRegion(context.getSource(), regionX, regionZ, page);
    }

    private int listRegion(ServerCommandSource source, int regionX, int regionZ, int page) {
        RegionCoords regionCoords = new RegionCoords(regionX, regionZ);
        List<IntPosition> lightSources;

        lightSources = mod.getLightStorageManager().getManager(source.getWorld()).getNLSFile(regionCoords).getAllLightSources();

        List<IntPosition> pageList = VarLightCommand.paginateEntries(lightSources, PAGE_SIZE, page);
        int pages = VarLightCommand.getAmountPages(lightSources, PAGE_SIZE);

        source.sendFeedback(new LiteralText(String.format("Amount of Light sources in Region [%d, %d]: %d (Showing Page %d / %d)", regionX, regionZ, lightSources.size(), Math.min(page, pages), pages)), false);

        return listLightSources(source, pageList);
    }

    private int listChunkImplicit(CommandContext<ServerCommandSource> context) {
        Entity entity = context.getSource().getEntity();

        if (entity == null) {
            context.getSource().sendError(new LiteralText("You must be a player to use this command!"));
            return -1;
        }

        int chunkX = entity.chunkX;
        int chunkZ = entity.chunkZ;

        return listChunk(context.getSource(), chunkX, chunkZ, 1);
    }

    private int listChunkExplicit(CommandContext<ServerCommandSource> context, int page) {
        int chunkX = context.getArgument(ARG_CHUNK_X.getName(), int.class);
        int chunkZ = context.getArgument(ARG_CHUNK_Z.getName(), int.class);

        return listChunk(context.getSource(), chunkX, chunkZ, page);
    }

    private int listChunk(ServerCommandSource source, int chunkX, int chunkZ, int page) {
        ChunkCoords chunkCoords = new ChunkCoords(chunkX, chunkZ);

        List<IntPosition> lightSources = mod.getLightStorageManager().getManager(source.getWorld()).getNLSFile(chunkCoords.toRegionCoords()).getAllLightSources(chunkCoords);

        List<IntPosition> pageList = VarLightCommand.paginateEntries(lightSources, PAGE_SIZE, page);
        int pages = VarLightCommand.getAmountPages(lightSources, PAGE_SIZE);

        source.sendFeedback(new LiteralText(String.format("Amount of Light sources in Chunk [%d, %d]: %d (Showing Page %d / %d)", chunkX, chunkZ, lightSources.size(), Math.min(page, pages), pages)), false);

        return listLightSources(source, pageList);
    }

    private int listLightSources(ServerCommandSource source, List<IntPosition> page) {
        WorldLightSourceManager manager = mod.getLightStorageManager().getManager(source.getWorld());

        for (IntPosition ls : page) {
            source.sendFeedback(generateText(source.getWorld(), ls, manager.getCustomLuminance(ls, 0)), false);
        }

        return page.size();
    }

    private Text generateText(ServerWorld world, IntPosition lightSource, int lightLevel) {
        Style locationStyle = Style.EMPTY;

        locationStyle.withFormatting(Formatting.GREEN); // Returns a new instance of Style

        locationStyle.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to Teleport.")));
        locationStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tp @s %d %d %d", lightSource.x, lightSource.y, lightSource.z)));

        return new LiteralText(lightSource.toShortString())
                .setStyle(locationStyle)
                .append(
                        new LiteralText(String.format(" (%s)", Registry.BLOCK.getId(world.getBlockState(new BlockPos(lightSource.x, lightSource.y, lightSource.z)).getBlock()).toString()))
                                .formatted(Formatting.GREEN)
                                .append(
                                        new LiteralText(" = " + lightLevel)
                                                .formatted(Formatting.GREEN)
                                )
                );
    }
}
