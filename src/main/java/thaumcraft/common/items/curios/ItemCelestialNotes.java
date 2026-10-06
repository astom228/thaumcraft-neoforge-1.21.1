package thaumcraft.common.items.curios;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Небесные заметки (порт ItemCelestialNotes; варианты metadata → отдельные предметы): sun, stars_1..4, moon_1..8.
 * Название у всех одно, вариант — в подсказке.
 */
public class ItemCelestialNotes extends Item {
    public static final String[] VARIANTS = {"sun", "stars_1", "stars_2", "stars_3", "stars_4", "moon_1", "moon_2", "moon_3", "moon_4",
            "moon_5", "moon_6", "moon_7", "moon_8"};
    private final String variant;

    public ItemCelestialNotes(String variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public String getVariant() {
        return this.variant;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.celestial_notes." + this.variant + ".text").withStyle(ChatFormatting.AQUA));
    }
}
