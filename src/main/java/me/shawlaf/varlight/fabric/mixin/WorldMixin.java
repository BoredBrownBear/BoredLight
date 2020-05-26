package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess, AutoCloseable {

    @Shadow
    @Final
    public boolean isClient;

    @Inject(
            at = @At("HEAD"),
            method = "close"
    )
    public void onClose(CallbackInfo ci) {
        if (isClient) {
            return;
        }

        VarLightMod.INSTANCE.onWorldUnload((ServerWorld) (Object) this);
    }

}
