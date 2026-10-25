package thaumcraft.common.lib.research.theorycraft;

import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

/**
 * «Наполнение»: тратит предмет из списка и фиал эссенции составного аспекта → наполнение по количеству аспектов предмета.
 * TODO: в начало списка вернуть алюментум (ItemsTC.alumentum), когда он появится.
 */
public class CardInfuse extends TheorycraftCard {
    Aspect aspect;
    ItemStack stack = ItemStack.EMPTY;

    static ItemStack[] options() {
        return new ItemStack[] {new ItemStack(BlocksTC.nitor.get(DyeColor.YELLOW).get()), new ItemStack(ItemsTC.amber.get()),
                new ItemStack(ItemsTC.brain.get()), new ItemStack(ItemsTC.fabric.get()), new ItemStack(ItemsTC.salisMundus.get()), new ItemStack(ItemsTC.ingotThaumium.get()),
                new ItemStack(ItemsTC.ingotBrass.get()), new ItemStack(ItemsTC.quicksilver.get()), new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD), new ItemStack(Items.BLAZE_ROD),
                new ItemStack(Items.LEATHER), new ItemStack(Blocks.WHITE_WOOL), new ItemStack(Items.BRICK), new ItemStack(Items.ARROW),
                new ItemStack(Items.EGG), new ItemStack(Items.FEATHER), new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.REDSTONE),
                new ItemStack(Items.GHAST_TEAR), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.BOW), new ItemStack(Items.GOLDEN_SWORD),
                new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.GOLDEN_PICKAXE), new ItemStack(Items.QUARTZ),
                new ItemStack(Items.APPLE)};
    }

    @Override
    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = super.serialize(registries);
        nbt.putString("aspect", this.aspect == null ? "" : this.aspect.getTag());
        if (!this.stack.isEmpty()) {
            nbt.put("stack", this.stack.save(registries));
        }
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        super.deserialize(nbt, registries);
        this.aspect = Aspect.getAspect(nbt.getString("aspect"));
        this.stack = ItemStack.parseOptional(registries, nbt.getCompound("stack"));
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(this.getSeed());
        int num = r.nextInt(Aspect.getCompoundAspects().size());
        this.aspect = Aspect.getCompoundAspects().get(num);
        ItemStack[] options = options();
        this.stack = options[r.nextInt(options.length)].copy();
        return this.aspect != null && !CardChannel.makeFilledPhial(this.aspect).isEmpty();
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
        return tr("card.infuse.name");
    }

    @Override
    public String getLocalizedText() {
        return tr("card.infuse.text", ChatFormatting.BOLD + this.aspect.getName() + ChatFormatting.RESET, this.stack.getHoverName().getString(),
                this.getVal());
    }

    private int getVal() {
        int q = 10;
        try {
            q = (int) (q + Math.sqrt(ThaumcraftCraftingManager.getObjectTags(this.stack).visSize()) * 1.5);
        } catch (Exception ignored) {
        }
        return q;
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] {this.stack, CardChannel.makeFilledPhial(this.aspect)};
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] {true, true};
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(this.getResearchCategory(), this.getVal());
        return true;
    }
}
