package me.shawlaf.varlight.fabric.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class PlayerUtil {

    private PlayerUtil() {
        throw new IllegalStateException();
    }

    public static ItemStack getStackInHand(PlayerEntity player, Hand hand) {
        switch (hand) {
            case MAIN_HAND: {
                return player.inventory.getMainHandStack();
            }

            case OFF_HAND: {
                return player.inventory.offHand.get(0);
            }

            default: {
                throw new IllegalStateException("More than 2 Hands???");
            }
        }
    }

}
