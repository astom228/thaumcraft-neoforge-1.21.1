package thaumcraft.common.config;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

/** Регистр предметов. Сами предметы объявлены в {@link thaumcraft.api.items.ItemsTC}. */
public class ConfigItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Thaumcraft.MODID);

    /**
     * Книга «Странные сны» (startBook оригинала, «потрёпанная» копия). В 1.12 страницы переводились на сервере;
     * в порте страницы — переводимые компоненты (видны на языке клиента), название — на языке сервера, как в оригинале.
     */
    public static ItemStack startBook(String author) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = List.of(
                Filterable.passThrough(Component.translatable("book.start.1")),
                Filterable.passThrough(Component.translatable("book.start.2")),
                Filterable.passThrough(Component.translatable("book.start.3")));
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(Language.getInstance().getOrDefault("book.start.title")), author, 3, pages, true));
        return book;
    }

    /** Элементы фокусов (ConfigItems.initFoci оригинала): ключ, значок для манипулятора, цвет. Вызывается при запуске. */
    public static void initFoci() {
        reg(thaumcraft.api.casters.FocusMediumRoot::new, "root", 10066329);
        reg(thaumcraft.common.items.casters.foci.FocusMediumTouch::new, "touch", 11371909);
        reg(thaumcraft.common.items.casters.foci.FocusMediumBolt::new, "bolt", 11377029);
        reg(thaumcraft.common.items.casters.foci.FocusMediumProjectile::new, "projectile", 11382149);
        reg(thaumcraft.common.items.casters.foci.FocusMediumCloud::new, "cloud", 10071429);
        reg(thaumcraft.common.items.casters.foci.FocusMediumMine::new, "mine", 8760709);
        reg(thaumcraft.common.items.casters.foci.FocusMediumPlan::new, "plan", 8760728);
        reg(thaumcraft.common.items.casters.foci.FocusMediumSpellBat::new, "spellbat", 8760748);
        // TODO этап 12.4: эффекты (fire, frost, air, earth, flux, break, rift, exchange, curse, heal) и модификаторы (scatter, split)
    }

    private static void reg(java.util.function.Supplier<? extends thaumcraft.api.casters.IFocusElement> element, String icon, int color) {
        thaumcraft.api.casters.FocusEngine.registerElement(element,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/foci/" + icon + ".png"), color);
    }
}
