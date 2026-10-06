package thaumcraft.api.research.theorycraft;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** «Изучение» (только из помощников): 15–25 в случайной доступной категории. */
public class CardStudy extends TheorycraftCard {
    String cat = "BASICS";

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("cat", this.cat);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.cat = nbt.getString("cat");
    }

    @Override
    public String getResearchCategory() {
        return this.cat;
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(this.getSeed());
        ArrayList<String> list = data.getAvailableCategories(player);
        this.cat = list.get(r.nextInt(list.size()));
        return this.cat != null;
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
        return tr("card.study.name", ChatFormatting.DARK_BLUE + "" + ChatFormatting.BOLD + tr("tc.research_category." + this.cat) + ChatFormatting.RESET);
    }

    @Override
    public String getLocalizedText() {
        return tr("card.study.text", boldCategory(this.cat));
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.cat, randInt(player.getRandom(), 15, 25));
        return true;
    }
}
