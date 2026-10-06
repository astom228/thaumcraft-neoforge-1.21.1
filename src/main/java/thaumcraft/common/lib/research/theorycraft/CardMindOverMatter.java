package thaumcraft.common.lib.research.theorycraft;

import java.util.Random;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

/** «Разум над материей»: тратит механизм из списка → 10 + корень количества его аспектов артефакторики. */
public class CardMindOverMatter extends TheorycraftCard {
    ItemStack stack = ItemStack.EMPTY;

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
        ItemStack[] options = CardTinker.options();
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

    @Override
    public String getLocalizedName() {
        return tr("card.mindmatter.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.mindmatter.text", this.getVal());
    }

    private int getVal() {
        int q = 10;
        try {
            q = (int) (q + Math.sqrt(ThaumcraftCraftingManager.getObjectTags(this.stack).visSize()));
        } catch (Exception ignored) {
        }
        return q;
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {this.stack};
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] {true};
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), this.getVal());
        return true;
    }
}
