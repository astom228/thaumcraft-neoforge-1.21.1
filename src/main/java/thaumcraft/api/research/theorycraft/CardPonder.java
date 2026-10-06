package thaumcraft.api.research.theorycraft;

import net.minecraft.world.entity.player.Player;

/** «Размышление»: 25 очков поровну по незаблокированным категориям, +5 «Основ», бонусное вытягивание. */
public class CardPonder extends TheorycraftCard {
    @Override
    public int getInspirationCost() {
        return 2;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.ponder.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.ponder.text");
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        return data.categoriesBlocked.size() < data.categoryTotals.size();
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        int a = 25;
        int tries = 0;
        while (a > 0 && tries < 1000) {
            tries++;
            for (String category : data.categoryTotals.keySet().toArray(new String[0])) {
                if (data.categoriesBlocked.contains(category)) {
                    if (data.categoryTotals.size() <= 1) {
                        return false;
                    }
                } else {
                    data.addTotal(category, 1);
                    if (--a <= 0) {
                        break;
                    }
                }
            }
        }
        data.addTotal("BASICS", 5);
        data.bonusDraws++;
        return a != 20;
    }
}
