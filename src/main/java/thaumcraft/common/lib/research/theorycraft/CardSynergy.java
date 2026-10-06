package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Синергия»: 15 очков артефакторики/алхимии/наполнения → 30 големостроения, штраф сдвигается. */
public class CardSynergy extends TheorycraftCard {
    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "GOLEMANCY";
    }

    @Override
    public String getLocalizedName() {
        return tr("card.synergy.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.synergy.text");
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        int tot = data.getTotal("ARTIFICE") + data.getTotal("ALCHEMY") + data.getTotal("INFUSION");
        return tot >= 15;
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        int tot = data.getTotal("ARTIFICE") + data.getTotal("ALCHEMY") + data.getTotal("INFUSION");
        if (tot < 15) {
            return false;
        }
        tot = 15;
        String[] cats = new String[] {"ARTIFICE", "ALCHEMY", "INFUSION"};
        int tries = 0;
        while (tot > 0 && tries < 1000) {
            tries++;
            for (String category : cats) {
                if (data.getTotal(category) > 0) {
                    data.addTotal(category, -1);
                    if (--tot <= 0) {
                        break;
                    }
                }
            }
        }
        data.addTotal("GOLEMANCY", 30);
        data.penaltyStart++;
        return true;
    }
}
