package me.shawlaf.varlight.fabric;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

import java.util.Collections;
import java.util.List;

public class GlowingBlockCreator {

    public static final String KEY_GLOWING = "varlight:glowing";
    public static final String COLOR_SYMBOL = "\u00a7";

    public ItemStack makeGlowing(ItemStack base, int lightLevel) {
        base.getOrCreateTag().put(KEY_GLOWING, IntTag.of(lightLevel));

        CompoundTag displayTag = base.getOrCreateSubTag("display");

        displayTag.putString("Name", Text.Serializer.toJson(
                new LiteralText(COLOR_SYMBOL + "r" + COLOR_SYMBOL + "6Glowing " +
                        Language.getInstance().get(base.getItem().getTranslationKey()))
        ));

        ListTag loreTag = new ListTag();

        List<Text> lore = Collections.singletonList(new LiteralText(COLOR_SYMBOL + "r" + COLOR_SYMBOL + "fEmitting Light: " + lightLevel));

        for (Text text : lore) {
            loreTag.add(StringTag.of(Text.Serializer.toJson(text)));
        }

        displayTag.put("Lore", loreTag);

        return base;
    }

    public int getGlowingValue(ItemStack stack) {
        if (!stack.hasTag()) {
            return 0;
        }

        CompoundTag tag = stack.getTag();

        if (!tag.contains(KEY_GLOWING, 3)) {
            return 0;
        }

        return tag.getInt(KEY_GLOWING);
    }

}
