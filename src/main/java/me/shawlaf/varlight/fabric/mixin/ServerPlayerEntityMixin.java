package me.shawlaf.varlight.fabric.mixin;

import com.mojang.authlib.GameProfile;
import me.shawlaf.varlight.fabric.VarLightMod;
import me.shawlaf.varlight.fabric.persistence.nbt.VarLightPlayerData;
import net.minecraft.container.ContainerListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ContainerListener {

    public ServerPlayerEntityMixin(World world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(
            at = @At("HEAD"),
            method = "writeCustomDataToTag"
    )
    public void onWriteCustomDataToTag(CompoundTag tag, CallbackInfo ci) {
        CompoundTag subTag = new CompoundTag();

        VarLightPlayerData data = VarLightMod.INSTANCE.getPlayerDataManager().getData(castThis());
        data.writeToTag(subTag);

        tag.put(data.getKey().toString(), subTag);
    }

    @Inject(
            at = @At("HEAD"),
            method = "readCustomDataFromTag"
    )
    public void onReadCustomDataFromTag(CompoundTag tag, CallbackInfo ci) {
        VarLightPlayerData data = VarLightMod.INSTANCE.getPlayerDataManager().getData(castThis());

        if (tag.contains(data.getKey().toString(), 10)) {
            data.readFromTag(tag.getCompound(data.getKey().toString()));
        }
    }

    private ServerPlayerEntity castThis() {
        return ((ServerPlayerEntity) (Object) this);
    }
}
