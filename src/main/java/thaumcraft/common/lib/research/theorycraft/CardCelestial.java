package thaumcraft.common.lib.research.theorycraft;

import java.util.Random;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.items.curios.ItemCelestialNotes;

/**
 * «Изучение небес»: две разные небесные заметки (номера вариантов 0–12, как metadata оригинала) → 25–50 в ведущей категории;
 * звёзды — мистика и временное Искажение, солнце — штраф сдвигается, луна — бонусное вытягивание.
 */
public class CardCelestial extends TheorycraftCard {
    int md1;
    int md2;
    String cat = "BASICS";

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putInt("md1", this.md1);
        nbt.putInt("md2", this.md2);
        nbt.putString("cat", this.cat);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.md1 = nbt.getInt("md1");
        this.md2 = nbt.getInt("md2");
        this.cat = nbt.getString("cat");
    }

    @Override
    public String getResearchCategory() {
        return this.cat;
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        if (!data.categoryTotals.isEmpty() && ThaumcraftCapabilities.knowsResearch(player, "CELESTIALSCANNING")) {
            Random r = new Random(this.getSeed());
            this.md1 = randInt(r, 0, 12);
            this.md2 = this.md1;
            while (this.md1 == this.md2) {
                this.md2 = randInt(r, 0, 12);
            }
            int hVal = 0;
            String hKey = "";
            for (String category : data.categoryTotals.keySet()) {
                int q = data.getTotal(category);
                if (q > hVal) {
                    hVal = q;
                    hKey = category;
                }
            }
            this.cat = hKey;
            return this.cat != null;
        }
        return false;
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.celestial.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.celestial.text", boldCategory(this.cat));
    }

    private static ItemStack notes(int md) {
        return new ItemStack(ItemsTC.celestialNotes.get(ItemCelestialNotes.VARIANTS[Math.floorMod(md, ItemCelestialNotes.VARIANTS.length)]).get());
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {notes(this.md1), notes(this.md2)};
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] {true, true};
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), randInt(player.getRandom(), 25, 50));
        boolean sun = this.md1 == 0 || this.md2 == 0;
        boolean moon = this.md1 > 4 || this.md2 > 4;
        boolean stars = this.md1 > 0 && this.md1 < 5 || this.md2 > 0 && this.md2 < 5;
        if (stars) {
            int amt = randInt(player.getRandom(), 0, 5);
            data.addTotal("ELDRITCH", amt * 2);
            ThaumcraftApi.internalMethods.addWarpToPlayer(player, amt, IPlayerWarp.EnumWarpType.TEMPORARY);
        }
        if (sun) {
            data.penaltyStart++;
        }
        if (moon) {
            data.bonusDraws++;
        }
        return true;
    }
}
