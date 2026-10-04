package thaumcraft.api.items;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.lib.TCDataComponents;

/** Предмет, хранящий аспекты (порт ItemGenericEssentiaContainer; NBT → компонент thaumcraft:aspects). */
public class ItemGenericEssentiaContainer extends Item implements IEssentiaContainerItem {
    protected int base;

    public ItemGenericEssentiaContainer(int base, Properties properties) {
        super(properties);
        this.base = base;
    }

    /** getSubItems оригинала: по одному стеку на каждый аспект (количество — base). */
    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Aspect tag : Aspect.aspects.values()) {
            ItemStack i = new ItemStack(this);
            this.setAspects(i, new AspectList().add(tag, this.base));
            output.accept(i);
        }
    }

    @Override
    public AspectList getAspects(ItemStack itemstack) {
        AspectList aspects = itemstack.get(TCDataComponents.ASPECTS.get());
        return aspects != null && aspects.size() > 0 ? aspects.copy() : null;
    }

    @Override
    public void setAspects(ItemStack itemstack, AspectList aspects) {
        itemstack.set(TCDataComponents.ASPECTS.get(), aspects.copy());
    }

    @Override
    public boolean ignoreContainedAspects() {
        return false;
    }

    /** onUpdate: у стека без аспектов (например, из рецепта) появляется случайный аспект. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && !stack.has(TCDataComponents.ASPECTS.get())) {
            Aspect[] displayAspects = Aspect.aspects.values().toArray(new Aspect[0]);
            this.setAspects(stack, new AspectList().add(displayAspects[level.random.nextInt(displayAspects.length)], this.base));
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide && !stack.has(TCDataComponents.ASPECTS.get())) {
            Aspect[] displayAspects = Aspect.aspects.values().toArray(new Aspect[0]);
            this.setAspects(stack, new AspectList().add(displayAspects[level.random.nextInt(displayAspects.length)], this.base));
        }
    }
}
