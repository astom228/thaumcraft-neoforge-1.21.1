package thaumcraft.api.research.theorycraft;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** «Отказ» (бесплатно): блокирует случайную категорию из набранных, +5 «Основ». */
public class CardReject extends TheorycraftCard {
    private String cat1;

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("cat", this.cat1 == null ? "" : this.cat1);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.cat1 = nbt.getString("cat");
    }

    @Override
    public int getInspirationCost() {
        return 0;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.reject.name", ChatFormatting.DARK_BLUE + "" + ChatFormatting.BOLD + tr("tc.research_category." + this.cat1)
                + ChatFormatting.RESET + "" + ChatFormatting.BOLD);
    }

    @Override
    public String getLocalizedText() {
        return tr("card.reject.text", boldCategory(this.cat1));
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        ArrayList<String> s = new ArrayList<>();
        for (String c : data.categoryTotals.keySet()) {
            if (!data.categoriesBlocked.contains(c)) {
                s.add(c);
            }
        }
        if (s.isEmpty()) {
            return false;
        }
        Random r = new Random(this.getSeed());
        this.cat1 = s.get(r.nextInt(s.size()));
        return this.cat1 != null;
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        if (this.cat1 == null) {
            return false;
        }
        data.addTotal("BASICS", 5);
        data.categoriesBlocked.add(this.cat1);
        return true;
    }
}
