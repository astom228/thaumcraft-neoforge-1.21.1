package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Связывание чар»: до 5 уровней опыта → по 5 ауромантии за уровень. */
public class CardSpellbinding extends TheorycraftCard {
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
        return tr("card.spellbinding.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.spellbinding.text");
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        return player.experienceLevel > 0;
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        if (player.experienceLevel <= 0) {
            return false;
        }
        int l = Math.min(5, player.experienceLevel);
        data.addTotal(this.getResearchCategory(), l * 5);
        player.giveExperienceLevels(-l);
        return true;
    }
}
