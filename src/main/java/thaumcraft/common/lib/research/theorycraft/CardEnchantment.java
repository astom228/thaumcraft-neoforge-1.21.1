package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Зачарование» (стол зачарований): 5 уровней опыта → 15–20 наполнения и ауромантии. */
public class CardEnchantment extends TheorycraftCard {
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
        return tr("card.enchantment.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.enchantment.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        if (player.experienceLevel >= 5) {
            player.giveExperienceLevels(-5);
            data.addTotal("INFUSION", randInt(player.getRandom(), 15, 20));
            data.addTotal("AUROMANCY", randInt(player.getRandom(), 15, 20));
            return true;
        }
        return false;
    }
}
