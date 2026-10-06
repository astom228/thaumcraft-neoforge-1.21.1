package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.tiles.crafting.TileResearchTable;

/** «Программирование»: тратит чернила и бумагу стола, 25 големостроения. */
public class CardScripting extends TheorycraftCard {
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
        return tr("card.scripting.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.scripting.text");
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        if (data.table instanceof TileResearchTable t && !t.getStackInSlot(0).isEmpty()
                && t.getStackInSlot(0).getDamageValue() < t.getStackInSlot(0).getMaxDamage() && !t.getStackInSlot(1).isEmpty()) {
            t.consumeInkFromTable();
            t.consumepaperFromTable();
            data.addTotal(this.getResearchCategory(), 25);
            return true;
        }
        return false;
    }
}
