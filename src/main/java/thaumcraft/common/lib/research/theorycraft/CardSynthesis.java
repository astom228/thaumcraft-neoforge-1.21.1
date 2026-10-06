package thaumcraft.common.lib.research.theorycraft;

import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** «Синтез»: тратит кристаллы двух компонентов составного аспекта, даёт кристалл самого аспекта и 40 алхимии. */
public class CardSynthesis extends TheorycraftCard {
    Aspect aspect1;
    Aspect aspect2;
    Aspect aspect3;

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("aspect1", this.aspect1 == null ? "" : this.aspect1.getTag());
        nbt.putString("aspect2", this.aspect2 == null ? "" : this.aspect2.getTag());
        nbt.putString("aspect3", this.aspect3 == null ? "" : this.aspect3.getTag());
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.aspect1 = Aspect.getAspect(nbt.getString("aspect1"));
        this.aspect2 = Aspect.getAspect(nbt.getString("aspect2"));
        this.aspect3 = Aspect.getAspect(nbt.getString("aspect3"));
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(this.getSeed());
        int num = randInt(r, 0, Aspect.getCompoundAspects().size() - 1);
        this.aspect3 = Aspect.getCompoundAspects().get(num);
        this.aspect1 = this.aspect3.getComponents()[0];
        this.aspect2 = this.aspect3.getComponents()[1];
        return true;
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "ALCHEMY";
    }

    @Override
    public String getLocalizedName() {
        return tr("card.synthesis.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.synthesis.text", ChatFormatting.BOLD + this.aspect1.getName() + ChatFormatting.RESET,
                ChatFormatting.BOLD + this.aspect2.getName() + ChatFormatting.RESET);
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {ThaumcraftApiHelper.makeCrystal(this.aspect1), ThaumcraftApiHelper.makeCrystal(this.aspect2)};
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] {true, true};
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        ItemStack res = ThaumcraftApiHelper.makeCrystal(this.aspect3);
        data.addTotal(this.getResearchCategory(), 40);
        if (player.getRandom().nextFloat() < 0.33) {
            data.addInspiration(1);
        }
        if (!player.getInventory().add(res)) {
            player.drop(res, true);
        }
        return true;
    }
}
