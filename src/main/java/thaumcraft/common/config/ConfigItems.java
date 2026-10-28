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

    /** Котёл с водой снимает краску с робы тауматурга (ItemRobeArmor.onItemUseFirst оригинала: −1 уровень воды). */
    public static void registerCauldron() {
        for (net.minecraft.world.item.Item robe : new net.minecraft.world.item.Item[] {thaumcraft.api.items.ItemsTC.clothChest.get(),
                thaumcraft.api.items.ItemsTC.clothLegs.get(), thaumcraft.api.items.ItemsTC.clothBoots.get()}) {
            net.minecraft.core.cauldron.CauldronInteraction.WATER.map().put(robe, net.minecraft.core.cauldron.CauldronInteraction.DYED_ITEM);
        }
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
        reg(thaumcraft.common.items.casters.foci.FocusEffectFire::new, "fire", 16734721);
        reg(thaumcraft.common.items.casters.foci.FocusEffectFrost::new, "frost", 14811135);
        reg(thaumcraft.common.items.casters.foci.FocusEffectAir::new, "air", 16777086);
        reg(thaumcraft.common.items.casters.foci.FocusEffectEarth::new, "earth", 5685248);
        reg(thaumcraft.common.items.casters.foci.FocusEffectFlux::new, "flux", 8388736);
        reg(thaumcraft.common.items.casters.foci.FocusEffectBreak::new, "break", 9063176);
        reg(thaumcraft.common.items.casters.foci.FocusEffectRift::new, "rift", 3084645);
        reg(thaumcraft.common.items.casters.foci.FocusEffectExchange::new, "exchange", 5735255);
        reg(thaumcraft.common.items.casters.foci.FocusEffectCurse::new, "curse", 6946821);
        reg(thaumcraft.common.items.casters.foci.FocusEffectHeal::new, "heal", 14548997);
        reg(thaumcraft.common.items.casters.foci.FocusModScatter::new, "scatter", 10066329);
        reg(thaumcraft.common.items.casters.foci.FocusModSplitTarget::new, "split_target", 10066329);
        reg(thaumcraft.common.items.casters.foci.FocusModSplitTrajectory::new, "split_trajectory", 10066329);
    }

    private static void reg(java.util.function.Supplier<? extends thaumcraft.api.casters.IFocusElement> element, String icon, int color) {
        thaumcraft.api.casters.FocusEngine.registerElement(element,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/foci/" + icon + ".png"), color);
    }
}
