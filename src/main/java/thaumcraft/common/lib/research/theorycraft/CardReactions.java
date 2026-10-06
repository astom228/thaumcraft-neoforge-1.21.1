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

/** «Реакции»: показать два вис-кристалла разных составных аспектов (не тратятся) → 25 алхимии, шанс вдохновения. */
public class CardReactions extends TheorycraftCard {
    Aspect aspect1;
    Aspect aspect2;

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("aspect1", this.aspect1 == null ? "" : this.aspect1.getTag());
        nbt.putString("aspect2", this.aspect2 == null ? "" : this.aspect2.getTag());
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.aspect1 = Aspect.getAspect(nbt.getString("aspect1"));
        this.aspect2 = Aspect.getAspect(nbt.getString("aspect2"));
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(this.getSeed());
        int num = randInt(r, 0, Aspect.getCompoundAspects().size() - 1);
        this.aspect1 = Aspect.getCompoundAspects().get(num);
        int num2 = num;
        while (num2 == num) {
            num2 = randInt(r, 0, Aspect.getCompoundAspects().size() - 1);
        }
        this.aspect2 = Aspect.getCompoundAspects().get(num2);
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
        return tr("card.reactions.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.reactions.text", ChatFormatting.BOLD + this.aspect1.getName() + ChatFormatting.RESET,
                ChatFormatting.BOLD + this.aspect2.getName() + ChatFormatting.RESET);
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {ThaumcraftApiHelper.makeCrystal(this.aspect1), ThaumcraftApiHelper.makeCrystal(this.aspect2)};
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), 25);
        if (player.getRandom().nextFloat() < 0.33) {
            data.addInspiration(1);
        }
        return true;
    }
}
