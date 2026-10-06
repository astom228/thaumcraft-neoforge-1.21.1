package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Маяк» (возвращает вдохновение): бонусное вытягивание, штраф сдвигается. */
public class CardBeacon extends TheorycraftCard {
    @Override
    public boolean isAidOnly() {
        return true;
    }

    @Override
    public int getInspirationCost() {
        return -2;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.beacon.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.beacon.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.bonusDraws++;
        data.penaltyStart++;
        return true;
    }
}
