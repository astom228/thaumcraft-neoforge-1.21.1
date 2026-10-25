package thaumcraft.common.items.consumables;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemGenericEssentiaContainer;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Фиал (порт ItemPhial; metadata 0/1 → предметы phial / essentia_phial): пустой набирает 10 эссенции из банки или алембика,
 * полный выливает её в банку (этапы 10.2, 10.4). Полный фиал без аспектов превращается в пустой.
 */
public class ItemPhial extends ItemGenericEssentiaContainer {
    private final boolean filled;

    public ItemPhial(boolean filled, Properties properties) {
        super(10, properties);
        this.filled = filled;
    }

    public boolean isFilled() {
        return this.filled;
    }

    public static ItemStack makePhial(Aspect aspect, int amt) {
        ItemStack i = new ItemStack(ItemsTC.phialFilled.get());
        ((ItemPhial) i.getItem()).setAspects(i, new AspectList().add(aspect, amt));
        return i;
    }

    public static ItemStack makeFilledPhial(Aspect aspect) {
        return makePhial(aspect, 10);
    }

    /** getSubItems оригинала: пустой фиал и полный каждого аспекта — во вкладке; здесь — полные (пустой добавляется сам). */
    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        if (this.filled) {
            super.addCreativeVariants(output);
        } else {
            output.accept(this);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide && this.filled && !stack.has(TCDataComponents.ASPECTS.get()) && entity instanceof Player p
                && p.getInventory().getItem(slot) == stack) {
            p.getInventory().setItem(slot, new ItemStack(ItemsTC.phial.get(), stack.getCount()));
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
    }

    // TODO этапы 10.2, 10.4: onItemUseFirst — набрать из банки/алембика, вылить в банку
}
