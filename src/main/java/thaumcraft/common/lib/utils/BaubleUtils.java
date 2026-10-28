package thaumcraft.common.lib.utils;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.common.lib.TCTags;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Безделушки через Curios (замена BaublesApi / IBauble оригинала). Тип безделушки — по тегу слота Curios
 * (AMULET → necklace, RING → ring, BELT → belt, HEAD → head, CHARM → charm; DEVIATIONS 1).
 */
public final class BaubleUtils {
    private BaubleUtils() {
    }

    /** Все надетые безделушки (BaublesApi.getBaublesHandler); null — у сущности нет слотов Curios. */
    @Nullable
    public static IItemHandler getBaubles(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(h -> (IItemHandler) h.getEquippedCurios()).orElse(null);
    }

    /** Тип безделушки по-старому ("amulet", "ring", "belt", "head", "charm") или null — не безделушка. */
    @Nullable
    public static String getBaubleType(ItemStack stack) {
        if (stack.is(TCTags.Items.CURIOS_NECKLACE)) {
            return "amulet";
        }
        if (stack.is(TCTags.Items.CURIOS_RING)) {
            return "ring";
        }
        if (stack.is(TCTags.Items.CURIOS_BELT)) {
            return "belt";
        }
        if (stack.is(TCTags.Items.CURIOS_HEAD)) {
            return "head";
        }
        if (stack.is(TCTags.Items.CURIOS_CHARM)) {
            return "charm";
        }
        return null;
    }

    public static boolean isBauble(ItemStack stack) {
        return getBaubleType(stack) != null;
    }

    /** BaublesApi.isBaubleEquipped: номер слота с предметом или −1. */
    public static int isBaubleEquipped(LivingEntity entity, Item item) {
        IItemHandler baubles = getBaubles(entity);
        if (baubles != null) {
            for (int a = 0; a < baubles.getSlots(); a++) {
                if (baubles.getStackInSlot(a).is(item)) {
                    return a;
                }
            }
        }
        return -1;
    }
}
