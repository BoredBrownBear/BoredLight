package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.persistence.nbt.VarLightData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.snooper.SnooperListener;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantThreadExecutor<ServerTask> implements SnooperListener, CommandOutput, AutoCloseable, Runnable {
    public MinecraftServerMixin(String name) {
        super(name);
    }

    @Inject(
            at = @At("TAIL"),
            method = "shutdown"
    )
    public void onShutDown(CallbackInfo ci) {
        VarLightMod.TASKS.clear();
        VarLightData.clear();
    }
}
