package thaumcraft.common.lib.research.theorycraft;

import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/**
 * «Направление»: показать фиал эссенции составного аспекта (не тратится) → 25 наполнения.
 */
public class CardChannel extends TheorycraftCard {
    Aspect aspect;

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("aspect", this.aspect == null ? "" : this.aspect.getTag());
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.aspect = Aspect.getAspect(nbt.getString("aspect"));
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(this.getSeed());
        int num = r.nextInt(Aspect.getCompoundAspects().size());
        this.aspect = Aspect.getCompoundAspects().get(num);
        return !makeFilledPhial(this.aspect).isEmpty();
    }

    static ItemStack makeFilledPhial(Aspect aspect) {
        return thaumcraft.common.items.consumables.ItemPhial.makeFilledPhial(aspect);
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "INFUSION";
    }

    @Override
    public String getLocalizedName() {
        return tr("card.channel.name", ChatFormatting.DARK_BLUE + this.aspect.getName() + ChatFormatting.RESET + "" + ChatFormatting.BOLD);
    }

    @Override
    public String getLocalizedText() {
        return tr("card.channel.text", ChatFormatting.BOLD + this.aspect.getName() + ChatFormatting.RESET);
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {makeFilledPhial(this.aspect)};
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), 25);
        return true;
    }
}
