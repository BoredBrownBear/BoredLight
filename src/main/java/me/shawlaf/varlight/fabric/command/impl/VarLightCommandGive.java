package me.shawlaf.varlight.fabric.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.command.VarLightSubCommand;
import me.shawlaf.varlight.fabric.util.OpPermissionLevel;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.ItemStackArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.Registry;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.arguments.EntityArgumentType.players;
import static net.minecraft.command.arguments.ItemStackArgumentType.itemStack;

public class VarLightCommandGive extends VarLightSubCommand {


    public VarLightCommandGive(VarLightMod mod) {
        super(mod, "give");
    }

    @Override
    public LiteralArgumentBuilder<ServerCommandSource> build(LiteralArgumentBuilder<ServerCommandSource> root) {

        root.requires((scs) -> scs.hasPermissionLevel(OpPermissionLevel.MANAGE_GAME));

        root.then(
                RequiredArgumentBuilder.<ServerCommandSource, EntitySelector>argument("targets", players())
                        .then(
                                RequiredArgumentBuilder.<ServerCommandSource, ItemStackArgument>argument("item", itemStack())
                                        .then(
                                                RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("lightlevel", integer(1, 15))
                                                        .executes(c -> execute(c, 1))
                                                        .then(
                                                                RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("count", integer(1))
                                                                        .executes(c -> execute(c, c.getArgument("count", int.class)))
                                                        )

                                        )
                        )
        );

        return root;
    }

    private int execute(CommandContext<ServerCommandSource> context, final int amount) throws CommandSyntaxException {
        ItemStackArgument stack = context.getArgument("item", ItemStackArgument.class);

        if (!(stack.getItem() instanceof BlockItem)) {
            context.getSource().sendError(new LiteralText(Registry.ITEM.getId(stack.getItem()).toString() + " is not a block!"));
            return 1;
        }

        int lightLevel = context.getArgument("lightlevel", int.class);

        List<? extends Entity> targets = context.getArgument("targets", EntitySelector.class).getEntities(context.getSource());

        for (Entity entity : targets) {
            ServerPlayerEntity spe = (ServerPlayerEntity) entity;

            int given = 0;

            while (given < amount) {
                int toGive = Math.min((amount - given), stack.getItem().getMaxCount());
                given += toGive;

                ItemStack itemStack = mod.makeGlowing(stack.createStack(toGive, false), lightLevel);

                boolean bl = spe.inventory.insertStack(itemStack);

                ItemEntity itemEntity;

                if (bl && itemStack.isEmpty()) {
                    itemStack.setCount(1);

                    itemEntity = spe.dropItem(itemStack, false);

                    if (itemEntity != null) {
                        itemEntity.setDespawnImmediately();
                    }

                    spe.world.playSound(null, spe.getX(), spe.getY(), spe.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((spe.getRandom().nextFloat() - spe.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    spe.playerContainer.sendContentUpdates();
                } else {
                    itemEntity = spe.dropItem(itemStack, false);

                    if (itemEntity != null) {
                        itemEntity.resetPickupDelay();
                        itemEntity.setOwner(spe.getUuid());
                    }
                }
            }
        }

        return targets.size();
    }
}
