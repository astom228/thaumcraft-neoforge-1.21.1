package thaumcraft.api.research.theorycraft;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;

/**
 * «Анализ»: тратит очко наблюдения категории, даёт 25–50 в ней и 5 в «Основах».
 * Как в оригинале, initialize проверяет наблюдение по ещё не выбранной категории (null), поэтому карточка не выпадает.
 */
public class CardAnalyze extends TheorycraftCard {
    String cat = null;

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("cat", this.cat == null ? "" : this.cat);
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
        ArrayList<String> cats = new ArrayList<>();
        for (ResearchCategory rc : ResearchCategories.researchCategories.values()) {
            if (!rc.key.equals("BASICS") && ThaumcraftCapabilities.getKnowledge(player)
                    .getKnowledge(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, ResearchCategories.researchCategories.get(this.cat)) > 0) {
                cats.add(rc.key);
            }
        }
        if (!cats.isEmpty()) {
            this.cat = cats.get(r.nextInt(cats.size()));
        }
        return this.cat != null;
    }

    @Override
    public int getInspirationCost() {
        return 2;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.analyze.name", ChatFormatting.DARK_BLUE + "" + ChatFormatting.BOLD + tr("tc.research_category." + this.cat) + ChatFormatting.RESET);
    }

    @Override
    public String getLocalizedText() {
        return tr("card.analyze.text", boldCategory(this.cat), boldCategory("BASICS"));
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        ResearchCategory rc = ResearchCategories.getResearchCategory(this.cat);
        int k = ThaumcraftCapabilities.getKnowledge(player).getKnowledge(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, rc);
        if (k >= 1) {
            data.addTotal("BASICS", 5);
            ThaumcraftCapabilities.getKnowledge(player).addKnowledge(IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, rc,
                    -IPlayerKnowledge.EnumKnowledgeType.OBSERVATION.getProgression());
            data.addTotal(this.cat, randInt(player.getRandom(), 25, 50));
            return true;
        }
        return false;
    }
}
