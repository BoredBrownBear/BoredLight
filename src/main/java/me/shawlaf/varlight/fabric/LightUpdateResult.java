package me.shawlaf.varlight.fabric;

import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public abstract class LightUpdateResult {

    public static final LightUpdateResult CANNOT_MODIFY = new LightUpdateResult() {

        private final Text message = new LiteralText("You cannot modify blocks in that area.");

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Text getMessage() {
            return message;
        }
    };

    public static final LightUpdateResult ILLEGAL_BLOCK = new LightUpdateResult() {

        private final Text message = new LiteralText("That Block cannot be turned into a Light Source.");

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Text getMessage() {
            return message;
        }
    };

    public static final LightUpdateResult ZERO_REACHED = new LightUpdateResult() {

        private final Text message = new LiteralText("Cannot set Light Level below Zero.");

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Text getMessage() {
            return message;
        }

    };

    public static final LightUpdateResult FIFTEEN_REACHED = new LightUpdateResult() {

        private final Text message = new LiteralText("Cannot set Light Level above Fifteen.");

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Text getMessage() {
            return message;
        }
    };


    public static LightUpdateResult success(int lightLevel) {
        return new LightUpdateResult() {
            @Override
            public boolean isSuccess() {
                return true;
            }

            @Override
            public Text getMessage() {
                return new LiteralText("Updated Light Level to " + lightLevel);
            }
        };
    }

    public abstract boolean isSuccess();

    public final void sendActionBarMessage(ServerPlayerEntity spe) {
        spe.sendMessage(getMessage(), true);
    }

    public abstract Text getMessage();

}
