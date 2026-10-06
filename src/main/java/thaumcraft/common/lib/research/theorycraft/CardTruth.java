package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Истина» (в оригинале не зарегистрирована и не выпадает): 10–25 мистики, бонусное вытягивание. */
public class CardTruth extends TheorycraftCard {
    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "ELDRITCH";
    }

    @Override
    public String getLocalizedName() {
        return tr("card.truth.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.truth.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal("ELDRITCH", randInt(player.getRandom(), 10, 25));
        data.bonusDraws++;
        ThaumcraftApi.internalMethods.addWarpToPlayer(player, 3, IPlayerWarp.EnumWarpType.TEMPORARY);
        return true;
    }
}
