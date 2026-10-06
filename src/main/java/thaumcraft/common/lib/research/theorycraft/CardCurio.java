package thaumcraft.common.lib.research.theorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.items.curios.ItemCurio;

/** «Изучить реликт»: тратит реликт из инвентаря, очки в его категории, «Основы» и случайную категорию, бонусные вытягивания. */
public class CardCurio extends TheorycraftCard {
    ItemStack curio = ItemStack.EMPTY;

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        if (!this.curio.isEmpty()) {
            nbt.put("stack", this.curio.save(registries));
        }
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.curio = ItemStack.parseOptional(registries, nbt.getCompound("stack"));
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getLocalizedName() {
        return tr("card.curio.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.curio.text");
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {this.curio};
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] {true};
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(this.getSeed());
        List<ItemStack> curios = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemCurio) {
                ItemStack c = stack.copy();
                c.setCount(1);
                curios.add(c);
            }
        }
        if (!curios.isEmpty()) {
            this.curio = curios.get(r.nextInt(curios.size()));
        }
        return !this.curio.isEmpty();
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal("BASICS", 5);
        String[] s = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
        data.addTotal(s[player.getRandom().nextInt(s.length)], 5);
        String type = this.getRequiredItems()[0].getItem() instanceof ItemCurio ic ? ic.getVariant() : "";
        switch (type) {
            case "arcane" -> data.addTotal("AUROMANCY", randInt(player.getRandom(), 25, 35));
            case "preserved" -> data.addTotal("ALCHEMY", randInt(player.getRandom(), 25, 35));
            case "ancient" -> data.addTotal("GOLEMANCY", randInt(player.getRandom(), 25, 35));
            case "eldritch" -> data.addTotal("ELDRITCH", randInt(player.getRandom(), 25, 35));
            case "knowledge" -> data.addTotal("INFUSION", randInt(player.getRandom(), 25, 35));
            case "twisted" -> data.addTotal("ARTIFICE", randInt(player.getRandom(), 25, 35));
            case "rites" -> {
                data.addTotal("ELDRITCH", randInt(player.getRandom(), 15, 20));
                data.addTotal("AUROMANCY", randInt(player.getRandom(), 10, 15));
            }
            default -> data.addTotal("BASICS", randInt(player.getRandom(), 25, 35));
        }
        if (player.getRandom().nextBoolean()) {
            data.bonusDraws++;
        }
        if (player.getRandom().nextBoolean()) {
            data.bonusDraws++;
        }
        return true;
    }
}
