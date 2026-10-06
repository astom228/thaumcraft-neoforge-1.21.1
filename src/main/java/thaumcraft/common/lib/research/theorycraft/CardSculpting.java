package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Лепка»: тратит комок глины, 20 големостроения, бонусное вытягивание. */
public class CardSculpting extends TheorycraftCard {
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
        return tr("card.sculpting.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.sculpting.text");
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {new ItemStack(Items.CLAY_BALL)};
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] {true};
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), 20);
        data.bonusDraws++;
        return true;
    }
}
