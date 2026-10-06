package thaumcraft.common.lib.research.theorycraft;

import java.util.Random;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

/** «Возня с механизмом»: показать механизм из списка (не тратится) → артефакторика по количеству его аспектов. */
public class CardTinker extends TheorycraftCard {
    ItemStack stack = ItemStack.EMPTY;

    /** Варианты предметов (общие с «Разум над материей»). */
    static ItemStack[] options() {
        return new ItemStack[] {new ItemStack(ItemsTC.visResonator.get()), new ItemStack(ItemsTC.thaumometer.get()), new ItemStack(Blocks.ANVIL),
                new ItemStack(Blocks.ACTIVATOR_RAIL), new ItemStack(Blocks.DISPENSER), new ItemStack(Blocks.DROPPER), new ItemStack(Blocks.ENCHANTING_TABLE),
                new ItemStack(Blocks.ENDER_CHEST), new ItemStack(Blocks.JUKEBOX), new ItemStack(Blocks.DAYLIGHT_DETECTOR), new ItemStack(Blocks.PISTON),
                new ItemStack(Blocks.HOPPER), new ItemStack(Blocks.STICKY_PISTON), new ItemStack(Items.MAP), new ItemStack(Items.COMPASS),
                new ItemStack(Items.TNT_MINECART), new ItemStack(Items.COMPARATOR), new ItemStack(Items.CLOCK)};
    }

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        if (!this.stack.isEmpty()) {
            nbt.put("stack", this.stack.save(registries));
        }
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.stack = ItemStack.parseOptional(registries, nbt.getCompound("stack"));
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(this.getSeed());
        ItemStack[] options = options();
        this.stack = options[r.nextInt(options.length)].copy();
        return true;
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "ARTIFICE";
    }

    private int getVal() {
        int q = 0;
        try {
            q = (int) (q + Math.sqrt(ThaumcraftCraftingManager.getObjectTags(this.stack).visSize()));
        } catch (Exception ignored) {
        }
        return q;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.tinker.name");
    }

    @Override
    public String getLocalizedText() {
        int a = this.getVal() * 2;
        int b = a + 10;
        return tr("card.tinker.text", a, b);
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {this.stack};
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        int q = this.getVal() * 2;
        data.addTotal(this.getResearchCategory(), randInt(player.getRandom(), q, q + 10));
        return true;
    }
}
