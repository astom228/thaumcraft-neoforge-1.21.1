package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.common.blocks.devices.BlockMirror;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Магическое зеркало (порт TileMirror): брошенные в него предметы (и вставленные воронкой) переносятся в парное зеркало
 * и по одному выбрасываются из него. Каждый предмет добавляет нестабильность; свыше 128 — Выброс в ауру.
 */
public class TileMirror extends TileMirrorLinked {
    private ArrayList<ItemStack> outputStacks = new ArrayList<>();
    private final IItemHandler handler = new MirrorHandler();

    public TileMirror(BlockPos pos, BlockState state) {
        super(ConfigBlocks.MIRROR_TILE.get(), pos, state);
    }

    @Override
    protected int instabilityLimit() {
        return 128;
    }

    @Override
    protected boolean isPartner(BlockEntity te) {
        return te instanceof TileMirror;
    }

    public Direction getFacing() {
        return this.getBlockState().getValue(BlockMirror.FACING);
    }

    public IItemHandler getItemHandler() {
        return this.handler;
    }

    /** Предмет коснулся зеркала (onEntityCollision). */
    public boolean transport(ItemEntity ie) {
        ItemStack items = ie.getItem();
        if (this.linked && this.isLinkValid(true) && this.targetTile(true) instanceof TileMirror target) {
            if (!target.addStack(items)) return false;
            this.addInstability(items.getCount());
            ie.discard();
            this.setChanged();
            target.setChanged();
            // в оригинале событие шло в мир парного зеркала по координатам этого — DEVIATIONS 31
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, 0);
            return true;
        }
        return false;
    }

    /** Ручное зеркало: предметы сразу в очередь выдачи. */
    public boolean transportDirect(ItemStack items) {
        if (items != null && !items.isEmpty()) {
            return this.addStack(items);
        }
        return false;
    }

    public void eject() {
        if (!this.outputStacks.isEmpty() && this.count > 20) {
            int i = this.level.random.nextInt(this.outputStacks.size());
            ItemStack st = this.outputStacks.get(i);
            if (st != null && !st.isEmpty()) {
                ItemStack outItem = st.copy();
                outItem.setCount(1);
                if (this.spawnItem(outItem)) {
                    st.shrink(1);
                    this.addInstability(1);
                    this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, 0);
                    if (st.getCount() <= 0) {
                        this.outputStacks.remove(i);
                    }
                }
            } else {
                this.outputStacks.remove(i);
            }
            this.setChanged();
        }
    }

    public boolean spawnItem(ItemStack stack) {
        Direction face = this.getFacing();
        ItemEntity ie2 = new ItemEntity(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.25, this.worldPosition.getZ() + 0.5, stack);
        ie2.setDeltaMovement(face.getStepX() * 0.15F, face.getStepY() * 0.15F, face.getStepZ() * 0.15F);
        ie2.setPortalCooldown(20);
        return this.level.addFreshEntity(ie2);
    }

    private boolean canQueue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        long total = stack.getCount();
        for (var pending : this.outputStacks) total += pending.getCount();
        return total <= 4096;
    }

    public boolean addStack(ItemStack stack) {
        if (!this.canQueue(stack)) return false;
        var remaining = stack.copy();
        for (var pending : this.outputStacks) {
            if (ItemStack.isSameItemSameComponents(pending, remaining)) {
                int count = Math.min(remaining.getCount(), Math.max(0, pending.getMaxStackSize() - pending.getCount()));
                pending.grow(count); remaining.shrink(count);
                if (remaining.isEmpty()) break;
            }
        }
        while (!remaining.isEmpty()) this.outputStacks.add(remaining.split(Math.max(1, remaining.getMaxStackSize())));
        this.setChanged();
        return true;
    }

    /** Очередь — уже принятые вещи: при разрушении вернуть их как содержимое сундука. */
    public void dropPendingItems() {
        if (this.level == null || this.level.isClientSide) return;
        var pending = this.outputStacks;
        this.outputStacks = new ArrayList<>();
        this.setChanged();
        for (var stack : pending) net.minecraft.world.Containers.dropItemStack(this.level,
                this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, stack.copy());
    }

    @Override
    public void serverTick() {
        this.eject();
        super.serverTick();
    }

    /** Блочное событие 1 — перенос предмета: дымка у зеркала. */
    @Override
    public boolean triggerEvent(int i, int j) {
        if (i != 1) {
            return super.triggerEvent(i, j);
        }
        if (this.level != null && this.level.isClientSide) {
            Direction face = this.getFacing();
            var r = this.level.random;
            double xx = this.worldPosition.getX() + 0.33 + r.nextFloat() * 0.33F - face.getStepX() / 2.0;
            double yy = this.worldPosition.getY() + 0.33 + r.nextFloat() * 0.33F - face.getStepY() / 2.0;
            double zz = this.worldPosition.getZ() + 0.33 + r.nextFloat() * 0.33F - face.getStepZ() / 2.0;
            IFXDispatcher.get().drawWispyMotes(xx, yy, zz, face.getStepX() / 50.0 + r.nextGaussian() * 0.01, face.getStepY() / 50.0 + r.nextGaussian() * 0.01,
                    face.getStepZ() / 50.0 + r.nextGaussian() * 0.01, Mth.nextInt(r, 10, 30), r.nextFloat() / 3.0F, 0.0F, r.nextFloat() / 2.0F,
                    (float) (r.nextGaussian() * 0.01));
        }
        return true;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
        this.outputStacks = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!s.isEmpty()) {
                this.outputStacks.add(s);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        ListTag list = new ListTag();
        for (ItemStack s : this.outputStacks) {
            if (s != null && !s.isEmpty()) {
                list.add(s.save(registries));
            }
        }
        nbt.put("Items", list);
    }

    /**
     * Инвентарь зеркала для воронок и труб (IInventory оригинала): один слот, всегда пустой; вставленное сразу уходит
     * в парное зеркало, если оно есть (isItemValidForSlot / setInventorySlotContents).
     */
    private final class MirrorHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // без действующей связи к паре не обращаемся (иначе — загрузка её чанка при каждой попытке воронки)
            if (stack.isEmpty() || !TileMirror.this.linked || !TileMirror.this.isLinkValid(true) || !(TileMirror.this.targetTile(true) instanceof TileMirror target)) {
                return stack;
            }
            if (!target.canQueue(stack)) return stack;
            if (!simulate) {
                if (!target.addStack(stack)) return stack;
                TileMirror.this.addInstability(stack.getCount());
                TileMirror.this.level.blockEvent(TileMirror.this.worldPosition, TileMirror.this.getBlockState().getBlock(), 1, 0);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return TileMirror.this.linked && TileMirror.this.targetTile(true) instanceof TileMirror;
        }
    }
}
