package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Измерение»: 15 наполнения, бонусное вытягивание. */
public class CardMeasure extends TheorycraftCard {
    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "INFUSION";
    }

    @Override
    public String getLocalizedName() {
        return tr("card.measure.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.measure.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), 15);
        data.bonusDraws++;
        return true;
    }
}
