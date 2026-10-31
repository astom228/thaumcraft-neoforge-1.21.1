package thaumcraft.common.entities.construct.golem.seals;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Печать запаса (порт SealStock): раз в секунду проверяет контейнер; если образца из фильтра меньше заданного числа —
 * запрашивает снабжение недостающим (печати снабжения привозят). Сама задач не даёт.
 */
public class SealStock extends SealFiltered implements ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(50);
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"),
            new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"),
            new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod")};
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_stock");

    @Override
    public String getKey() {
        return "thaumcraft:stock";
    }

    @Override
    public int getFilterSize() {
        return 9;
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay++ % 20 == 0) {
            IItemHandler inv = ThaumcraftApiHelper.getItemHandlerAt(world, seal.getSealPos().pos, seal.getSealPos().face);
            if (inv != null) {
                for (int a = 0; a < 9; a++) {
                    if (this.getFilterSlot(a).isEmpty()) {
                        continue;
                    }
                    int amt = InventoryUtils.countStackIn(inv, this.getFilterSlot(a), invFilter(this.props));
                    if (amt < this.getFilterSlotSize(a)) {
                        ItemStack fs = this.getFilterSlot(a).copy();
                        fs.setCount(Math.min(fs.getMaxStackSize(), this.getFilterSlotSize(a) - amt));
                        GolemHelper.requestProvisioning(world, seal.getSealPos().pos, seal.getSealPos().face, fs);
                    }
                }
            }
        }
    }

    @Override
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        return false;
    }

    @Override
    public boolean canPlaceAt(Level world, BlockPos pos, Direction side) {
        return ThaumcraftApiHelper.getItemHandlerAt(world, pos, side) != null;
    }

    @Override
    public ResourceLocation getSealIcon() {
        return this.icon;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] {1, 3, 0, 4};
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return null;
    }

    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return new EnumGolemTrait[] {EnumGolemTrait.CLUMSY};
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
    }

    @Override
    public void onRemoval(Level world, BlockPos pos, Direction side) {
    }

    @Override
    public boolean hasStacksizeLimiters() {
        return true;
    }

    @Override
    public boolean isBlacklist() {
        return false;
    }

    @Override
    public SealToggle[] getToggles() {
        return this.props;
    }

    @Override
    public void setToggle(int indx, boolean value) {
        this.props[indx].setValue(value);
    }
}
