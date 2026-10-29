package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraftInventory;

import java.util.List;

/**
 * Пьедестал подзарядки (порт TileRechargePedestal): раз в 10 тиков заряжает лежащий предмет Вис из ауры (до 5 за раз);
 * при успехе — искры Вис случайного первичного цвета (событие блока 5). Как контейнер аспектов показывает заряд (Potentia).
 */
public class TileRechargePedestal extends TileThaumcraftInventory implements IAspectContainer {
    int counter = 0;

    public TileRechargePedestal(BlockPos pos, BlockState state) {
        super(ConfigBlocks.RECHARGE_PEDESTAL_TILE.get(), pos, state, 1);
        this.syncedSlots = new int[] {0};
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileRechargePedestal tile) {
        if (tile.counter++ % 10 == 0 && RechargeHelper.rechargeItem(level, tile.getStackInSlot(0), pos, null, 5) > 0.0F) {
            tile.syncTile(false);
            tile.setChanged();
            List<Aspect> al = Aspect.getPrimalAspects();
            level.blockEvent(pos, state.getBlock(), 5, al.get(level.random.nextInt(al.size())).getColor());
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof IRechargable;
    }

    @Override
    public AspectList getAspects() {
        if (this.getStackInSlot(0).getItem() instanceof IRechargable) {
            float c = RechargeHelper.getCharge(this.getStackInSlot(0));
            return new AspectList().add(Aspect.ENERGY, Math.round(c));
        }
        return null;
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 5) {
            if (this.level != null && this.level.isClientSide) {
                var r = this.level.random;
                BlockPos up = this.worldPosition.above();
                IFXDispatcher.get().visSparkle(this.worldPosition.getX() + r.nextInt(3) - r.nextInt(3), up.getY() + r.nextInt(3),
                        this.worldPosition.getZ() + r.nextInt(3) - r.nextInt(3), this.worldPosition.getX(), up.getY(), this.worldPosition.getZ(), j);
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }
}
