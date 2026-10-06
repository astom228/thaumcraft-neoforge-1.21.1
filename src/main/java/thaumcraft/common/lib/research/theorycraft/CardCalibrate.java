package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Калибровка»: 15 артефакторики, бонусное вытягивание. */
public class CardCalibrate extends TheorycraftCard {
    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "ARTIFICE";
    }

    @Override
    public String getLocalizedName() {
        return tr("card.calibrate.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.calibrate.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), 15);
        data.bonusDraws++;
        return true;
    }
}
