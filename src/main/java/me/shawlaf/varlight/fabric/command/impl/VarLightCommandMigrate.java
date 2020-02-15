package me.shawlaf.varlight.fabric.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.command.VarLightSubCommand;
import me.shawlaf.varlight.fabric.persistence.PersistentLightSource;
import me.shawlaf.varlight.fabric.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.fabric.util.OpPermissionLevel;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class VarLightCommandMigrate extends VarLightSubCommand {
    public VarLightCommandMigrate(VarLightMod mod) {
        super(mod, "migrate");
    }

    @Override
    public LiteralArgumentBuilder<ServerCommandSource> build(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.requires(scs -> scs.hasPermissionLevel(OpPermissionLevel.MANAGE_GAME));

        root.executes(c -> {
            ServerCommandSource scs = c.getSource();

            int amountWorlds = 0, total = 0, totalMigrated = 0, totalFailed = 0, totalSkipped = 0;

            scs.sendFeedback(new LiteralText("Starting Migration of Light Sources..."), true);

            for (ServerWorld world : scs.getMinecraftServer().getWorlds()) {

                ++amountWorlds;

                int migrated = 0, failed = 0, skipped = 0;

                WorldLightSourceManager manager = mod.getManager(world);

                manager.save(null);

                List<PersistentLightSource> lightSources = manager.getAllLightSources();

                total += lightSources.size();

                Queue<ChunkPos> toUnload = new LinkedList<>();

                for (PersistentLightSource pls : lightSources) {
                    if (!pls.needsMigration()) {
                        ++skipped;
                        continue;
                    }

                    IntPosition pos = pls.getPosition();

                    int cx = pos.getChunkX();
                    int cz = pos.getChunkZ();

                    if (!world.isChunkLoaded(cx, cz)) {
                        Chunk chunk = world.getChunk(cx, cz); // Load the chunk

                        world.getChunkManager().addTicket(ChunkTicketType.UNKNOWN, chunk.getPos(), 1, chunk.getPos());
                        toUnload.add(chunk.getPos());
                    }

                    if (pls.migrate()) {
                        ++migrated;
                    } else {
                        ++failed;
                    }
                }

                scs.sendFeedback(new LiteralText(String.format("Migrated %d / %d Light source in World %s (Failed: %d; Skipped: %d)", migrated, lightSources.size(), mod.getKey(world), failed, skipped)).formatted(Formatting.ITALIC, Formatting.GRAY), true);

                totalMigrated += migrated;
                totalFailed += failed;
                totalSkipped += skipped;

                while (!toUnload.isEmpty()) {
                    ChunkPos obj = toUnload.remove();

                    world.getChunkManager().removeTicket(ChunkTicketType.UNKNOWN, obj, 1, obj);
                }
            }

            scs.sendFeedback(new LiteralText(String.format("Migrated %d / %d Light Sources in %d Worlds (Failed: %d; Skipped: %d)", totalMigrated, total, amountWorlds, totalFailed, totalSkipped)), true);

            if (totalMigrated == 0) {
                if (total == totalSkipped) {
                    scs.sendFeedback(new LiteralText("All Light Sources have already been migrated!").formatted(Formatting.GREEN), true);
                }
            } else if (totalFailed != 0) {
                scs.sendFeedback(new LiteralText("Not all Light Sources have been migrated!").formatted(Formatting.RED), true);
            }

            return totalMigrated;
        });

        return root;
    }
}
