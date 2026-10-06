package thaumcraft.api.research.theorycraft;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** «Записи» (только из помощников): очки слабейшей категории переходят (наполовину и больше) в сильнейшую. */
public class CardNotation extends TheorycraftCard {
    private String cat1;
    private String cat2;

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("cat1", this.cat1 == null ? "" : this.cat1);
        nbt.putString("cat2", this.cat2 == null ? "" : this.cat2);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.cat1 = nbt.getString("cat1");
        this.cat2 = nbt.getString("cat2");
    }

    @Override
    public boolean isAidOnly() {
        return true;
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.notation.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.notation.text", boldCategory(this.cat1), boldCategory(this.cat2));
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        if (data.categoryTotals.size() < 2) {
            return false;
        }
        int lVal = Integer.MAX_VALUE;
        String lKey = "";
        int hVal = 0;
        String hKey = "";
        for (String category : data.categoryTotals.keySet()) {
            int q = data.getTotal(category);
            if (q < lVal) {
                lVal = q;
                lKey = category;
            }
            if (q > hVal) {
                hVal = q;
                hKey = category;
            }
        }
        if (!hKey.equals(lKey) && lVal > 0) {
            this.cat1 = lKey;
            this.cat2 = hKey;
            return true;
        }
        return false;
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        if (this.cat1 != null && this.cat2 != null) {
            int lVal = data.getTotal(this.cat1);
            data.addTotal(this.cat1, -lVal);
            data.addTotal(this.cat2, lVal / 2 + randInt(player.getRandom(), 0, lVal / 2));
            return true;
        }
        return false;
    }
}
