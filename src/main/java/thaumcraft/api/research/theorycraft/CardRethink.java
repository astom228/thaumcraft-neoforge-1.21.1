package thaumcraft.api.research.theorycraft;

import net.minecraft.world.entity.player.Player;

/** «Переосмысление» (возвращает вдохновение): снимает до 10 очков по категориям, бонусное вытягивание, 1–10 «Основ». */
public class CardRethink extends TheorycraftCard {
    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        int a = 0;
        for (String category : data.categoryTotals.keySet()) {
            a += data.getTotal(category);
        }
        return a >= 10;
    }

    @Override
    public int getInspirationCost() {
        return -1;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.rethink.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.rethink.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        if (!this.initialize(player, data)) {
            return false;
        }
        int a = 0;
        for (String category : data.categoryTotals.keySet()) {
            a += data.getTotal(category);
        }
        a = Math.min(a, 10);
        int tries = 0;
        while (a > 0 && tries < 1000) {
            tries++;
            for (String category : data.categoryTotals.keySet().toArray(new String[0])) {
                data.addTotal(category, -1);
                if (--a <= 0 || !data.hasTotal(category)) {
                    break;
                }
            }
        }
        data.bonusDraws++;
        data.addTotal("BASICS", randInt(player.getRandom(), 1, 10));
        return true;
    }
}
