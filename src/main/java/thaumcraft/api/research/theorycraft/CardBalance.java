package thaumcraft.api.research.theorycraft;

import net.minecraft.world.entity.player.Player;

/** «Баланс»: уравнивает очки незаблокированных категорий, +5 «Основ», сдвигает начало штрафа. */
public class CardBalance extends TheorycraftCard {
    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.balance.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.balance.text");
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        int total = 0;
        int size = 0;
        for (String c : data.categoryTotals.keySet()) {
            if (!data.categoriesBlocked.contains(c)) {
                total += data.categoryTotals.get(c);
                size++;
            }
        }
        return data.categoriesBlocked.size() < data.categoryTotals.size() - 1 && total >= size;
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        int total = 0;
        int size = 0;
        for (String c : data.categoryTotals.keySet()) {
            if (!data.categoriesBlocked.contains(c)) {
                total += data.categoryTotals.get(c);
                size++;
            }
        }
        if (data.categoriesBlocked.size() < data.categoryTotals.size() - 1 && total >= size) {
            for (String category : data.categoryTotals.keySet()) {
                if (!data.categoriesBlocked.contains(category)) {
                    data.categoryTotals.put(category, total / size);
                }
            }
            data.addTotal("BASICS", 5);
            data.penaltyStart++;
            return true;
        }
        return false;
    }
}
