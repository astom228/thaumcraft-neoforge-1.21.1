package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Яйцо дракона» (в оригинале помощник не зарегистрирован): 10 раз по 2–5 в случайной категории. */
public class CardDragonEgg extends TheorycraftCard {
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
        return tr("card.dragonegg.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.dragonegg.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        String[] s = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
        for (int a = 0; a < 10; a++) {
            String cat = s[player.getRandom().nextInt(s.length)];
            data.addTotal(cat, randInt(player.getRandom(), 2, 5));
        }
        return true;
    }
}
