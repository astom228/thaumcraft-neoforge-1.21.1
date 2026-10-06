package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Сосредоточение»: 15 ауромантии, бонусное вытягивание. */
public class CardFocus extends TheorycraftCard {
    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "AUROMANCY";
    }

    @Override
    public String getLocalizedName() {
        return tr("card.focus.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.focus.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), 15);
        data.bonusDraws++;
        return true;
    }
}
