package thaumcraft.api.items;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.common.config.ConfigItems;

/** Предметы Thaumcraft (в оригинале — статические поля, заполняемые ConfigItems). Имена полей — как в оригинале. */
public class ItemsTC {
    // TODO этап 13: заменить на ItemGoggles (броня + очки откровения). Пока нужен как иконка вкладки.
    public static final DeferredItem<Item> goggles = ConfigItems.ITEMS.registerSimpleItem("goggles", new Item.Properties().stacksTo(1));

    /** Загружает класс, чтобы все предметы попали в регистр до события регистрации. */
    public static void init() {
    }
}
