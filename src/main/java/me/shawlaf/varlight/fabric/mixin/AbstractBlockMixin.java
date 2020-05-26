package me.shawlaf.varlight.fabric.mixin;

import me.shawlaf.varlight.fabric.VarLightMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {

    @Inject(
            at = @At("RETURN"),
            method = "getDroppedStacks"
    )
    public void getDroppedStacks(BlockState state, LootContext.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> drops = cir.getReturnValue();

        if (drops.isEmpty()) {
            return;
        }

        ServerWorld world = builder.getWorld();
        BlockPos pos = builder.get(LootContextParameters.POSITION);

        int customLuminance = getMod().getLightStorageManager().getManager(world).getCustomLuminance(pos, 0);

        if (customLuminance == 0) {
            return;
        }

        ItemStack tool = builder.get(LootContextParameters.TOOL);

        if (tool.isEmpty()) {
            drops.add(new ItemStack(Items.GLOWSTONE_DUST, 1));
        } else {
            int silkTouchLevel = 0, fortuneLevel = 0;

            ListTag enchantments = tool.getEnchantments();

            String silkTouchId = String.valueOf(Registry.ENCHANTMENT.getId(Enchantments.SILK_TOUCH));
            String fortuneId = String.valueOf(Registry.ENCHANTMENT.getId(Enchantments.FORTUNE));

            for (int i = 0; i < enchantments.size(); i++) {
                CompoundTag enchantment = enchantments.getCompound(i);
                String id = enchantment.getString("id");

                if (silkTouchId.equals(id)) {
                    silkTouchLevel = enchantment.getShort("lvl");
                } else if (fortuneId.equals(id)) {
                    fortuneLevel = enchantment.getShort("lvl");
                }
            }

            if (silkTouchLevel > 0) {
                for (ItemStack drop : drops) { // Should in theory be only one, but /shrug
                    getMod().getGlowingBlockCreator().makeGlowing(drop, customLuminance);
                }
            } else if (fortuneLevel > 0) {
                double chance = 1d - (1.5) * Math.exp(-0.6 * fortuneLevel);
                int count = 1;

                for (int i = 0; i < customLuminance; i++) {
                    if (Math.random() <= chance) {
                        ++count;
                    }
                }

                drops.add(new ItemStack(Items.GLOWSTONE_DUST, count));
            }
        }
    }

    private static VarLightMod getMod() {
        return VarLightMod.INSTANCE;
    }

}
