package me.shawlaf.varlight.fabric;

import net.minecraft.server.world.ServerWorld;

public interface IModComponent {

    default void onModInitialize() {

    }

    default void onWorldUnload(ServerWorld world) {

    }

    default void onServerShutdown() {

    }

}
