package thaumcraft.api.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.ResearchCategories;

/** «Эксперимент»: 15–30 в случайной категории и 1–10 «Основ». */
public class CardExperimentation extends TheorycraftCard {
    @Override
    public int getInspirationCost() {
        return 2;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.experimentation.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.experimentation.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        try {
            String[] s = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
            String cat = s[player.getRandom().nextInt(s.length)];
            data.addTotal(cat, randInt(player.getRandom(), 15, 30));
            data.addTotal("BASICS", randInt(player.getRandom(), 1, 10));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
