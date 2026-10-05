package thaumcraft.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Предметы из тегов (в оригинале — имена OreDictionary; см. ThaumcraftApi.registerObjectTag). Кеш оригинала не нужен:
 * проверка тега дешёвая, а теги перезагружаются.
 */
public class ScanOreDictionary implements IScanThing {
    String research;
    TagKey<Item>[] entries;

    @SafeVarargs
    public ScanOreDictionary(String research, TagKey<Item>... entries) {
        this.research = research;
        this.entries = entries;
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        ItemStack stack = null;
        if (obj instanceof BlockPos pos) {
            BlockState state = player.level().getBlockState(pos);
            stack = state.getBlock().getCloneItemStack(player.level(), pos, state);
        } else if (obj instanceof ItemStack s) {
            stack = s;
        } else if (obj instanceof ItemEntity ie) {
            stack = ie.getItem();
        }
        if (stack != null && !stack.isEmpty()) {
            for (TagKey<Item> entry : this.entries) {
                if (stack.is(entry)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getResearchKey(Player player, Object object) {
        return this.research;
    }
}
