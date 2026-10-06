package thaumcraft.api.research.theorycraft;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** «Вдохновение»: в ведущую категорию — 10 + половина её очков. */
public class CardInspired extends TheorycraftCard {
    String cat = null;
    int amt;

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("cat", this.cat == null ? "" : this.cat);
        nbt.putInt("amt", this.amt);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.cat = nbt.getString("cat");
        this.amt = nbt.getInt("amt");
    }

    @Override
    public String getResearchCategory() {
        return this.cat;
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        if (data.categoryTotals.isEmpty()) {
            return false;
        }
        int hVal = 0;
        String hKey = "";
        for (String category : data.categoryTotals.keySet()) {
            int q = data.getTotal(category);
            if (q > hVal) {
                hVal = q;
                hKey = category;
            }
        }
        this.cat = hKey;
        this.amt = 10 + hVal / 2;
        return true;
    }

    @Override
    public int getInspirationCost() {
        return 2;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.inspired.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.inspired.text", this.amt, boldCategory(this.cat));
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.cat, this.amt);
        return true;
    }
}
