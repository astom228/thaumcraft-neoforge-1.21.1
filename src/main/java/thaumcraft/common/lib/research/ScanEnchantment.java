package thaumcraft.common.lib.research;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ScanningManager;

/**
 * Предмет с зачарованием. Зачарования в 1.21 — реестр мира, поэтому сканеры создаются по ключам при загрузке реестра
 * (ConfigResearch.registerEnchantmentScans), а сравнение идёт по ключу, а не по объекту.
 * Ключ: "!" + ключ перевода зачарования (в оригинале — Enchantment.getName(), тоже ключ перевода).
 */
public class ScanEnchantment implements IScanThing {
    final ResourceKey<Enchantment> enchantment;

    public ScanEnchantment(ResourceKey<Enchantment> ench) {
        this.enchantment = ench;
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) {
            return false;
        }
        ItemStack is = ScanningManager.getItemFromParms(player, obj);
        if (!is.isEmpty()) {
            ItemEnchantments e = is.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Holder<Enchantment> ench : e.keySet()) {
                if (ench.is(this.enchantment)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getResearchKey(Player player, Object obj) {
        return "!enchantment." + this.enchantment.location().getNamespace() + "." + this.enchantment.location().getPath();
    }
}
