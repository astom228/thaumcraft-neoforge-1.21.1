package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.items.consumables.ItemBathSalts;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/**
 * Магическая купель (порт TileSpa): бак на 5000 мБ и слот солей. Раз в 40 тиков без сигнала тратит 1000 мБ (и соль при
 * смешивании) и ставит над собой источник — очищающей жидкости (смешивание с водой) или жидкости из бака; если над купелью
 * уже стоит такой источник, расширяет «бассейн» в квадрате 5×5 на уровень выше (рядом с уже налитым).
 */
public class TileSpa extends TileThaumcraftInventory {
    private boolean mix = true;
    private int counter = 0;
    public final FluidTank tank = new FluidTank(5000) {
        @Override
        protected void onContentsChanged() {
            TileSpa.this.setChanged();
            TileSpa.this.syncTile(false);
        }
    };

    public TileSpa(BlockPos pos, BlockState state) {
        super(ConfigBlocks.SPA_TILE.get(), pos, state, 1);
    }

    public void toggleMix() {
        this.mix = !this.mix;
        this.syncTile(false);
        this.setChanged();
    }

    public boolean getMix() {
        return this.mix;
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.mix = nbt.getBoolean("mix");
        this.tank.readFromNBT(registries, nbt);
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putBoolean("mix", this.mix);
        this.tank.writeToNBT(registries, nbt);
        return nbt;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemBathSalts;
    }

    /** Предметы — со всех сторон, кроме верха (getSlotsForFace оригинала); жидкость — с любой стороны. */
    public static IItemHandler itemCapability(TileSpa tile, Direction side) {
        return side == Direction.UP ? null : tile.getItemHandler();
    }

    public static IFluidHandler fluidCapability(TileSpa tile, Direction side) {
        return tile.tank;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileSpa tile) {
        if (tile.counter++ % 40 == 0 && !level.hasNeighborSignal(pos) && tile.hasIngredients()) {
            BlockState above = level.getBlockState(pos.above());
            Block tb = tile.mix ? BlocksTC.purifyingFluid.get() : tile.tank.getFluid().getFluid().defaultFluidState().createLegacyBlock().getBlock();
            if (above.getBlock() == tb && above.getFluidState().isSource()) {
                for (int xx = -2; xx <= 2; xx++) {
                    for (int zz = -2; zz <= 2; zz++) {
                        BlockPos p = pos.offset(xx, 1, zz);
                        if (tile.isValidLocation(p, true, tb)) {
                            tile.consumeIngredients();
                            level.setBlockAndUpdate(p, tb.defaultBlockState());
                            return;
                        }
                    }
                }
            } else if (tile.isValidLocation(pos.above(), false, tb)) {
                tile.consumeIngredients();
                level.setBlockAndUpdate(pos.above(), tb.defaultBlockState());
            }
        }
    }

    private boolean hasIngredients() {
        FluidStack fs = this.tank.getFluid();
        if (this.mix) {
            return fs.getFluid() == Fluids.WATER && fs.getAmount() >= 1000 && this.getStackInSlot(0).getItem() instanceof ItemBathSalts;
        }
        // canBePlacedInWorld 1.12: у жидкости есть блок
        return !fs.isEmpty() && !fs.getFluid().defaultFluidState().createLegacyBlock().isAir() && fs.getAmount() >= 1000;
    }

    private void consumeIngredients() {
        if (this.mix) {
            this.decrStackSize(0, 1);
        }
        this.tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
    }

    private boolean isValidLocation(BlockPos p, boolean mustBeAdjacent, Block target) {
        if (target == Blocks.WATER && this.level.dimensionType().ultraWarm()) {
            return false;
        }
        BlockState bs = this.level.getBlockState(p);
        if (this.level.getBlockState(p.below()).isFaceSturdy(this.level, p.below(), Direction.UP) && bs.canBeReplaced()
                && (bs.getBlock() != target || !bs.getFluidState().isSource())) {
            return !mustBeAdjacent || this.isTouchingSource(p, target);
        }
        return false;
    }

    /** BlockUtils.isBlockTouching: рядом (по 6 сторонам) — источник целевой жидкости. */
    private boolean isTouchingSource(BlockPos p, Block target) {
        for (Direction d : Direction.values()) {
            BlockState s = this.level.getBlockState(p.relative(d));
            if (s.getBlock() == target && s.getFluidState().isSource()) {
                return true;
            }
        }
        return false;
    }
}
