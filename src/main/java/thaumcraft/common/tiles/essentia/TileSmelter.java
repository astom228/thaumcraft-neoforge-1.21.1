package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.essentia.BlockSmelter;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/**
 * Плавильня эссенции (порт TileSmelter): слот 0 — предмет с аспектами, слот 1 — топливо. Переплавка даёт до 256 эссенции
 * (с потерями по КПД плавильни — они уходят в Выброс или через отводящий клапан), раз в speed тиков по единице уходит
 * в алембики над плавильней и над вспомогательными насосами. Снизу — топливо, с боков — переплавляемое, сверху — ничего.
 */
public class TileSmelter extends TileThaumcraftInventory {
    private static final int[] SLOTS_BOTTOM = new int[] {1};
    private static final int[] SLOTS_TOP = new int[0];
    private static final int[] SLOTS_SIDES = new int[] {0};
    public AspectList aspects = new AspectList();
    public int vis;
    private final int maxVis = 256;
    public int smeltTime = 100;
    boolean speedBoost = false;
    public int furnaceBurnTime;
    public int currentItemBurnTime;
    public int furnaceCookTime;
    int count = 0;
    int bellows = -1;
    private final IItemHandler[] sided = new IItemHandler[6];

    /** Свойства меню (detectAndSendChanges / updateProgressBar ContainerSmelter). */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TileSmelter.this.furnaceCookTime;
                case 1 -> TileSmelter.this.furnaceBurnTime;
                case 2 -> TileSmelter.this.currentItemBurnTime;
                case 3 -> TileSmelter.this.vis;
                case 4 -> TileSmelter.this.smeltTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public TileSmelter(BlockPos pos, BlockState state) {
        super(ConfigBlocks.SMELTER_TILE.get(), pos, state, 2);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.furnaceBurnTime = nbt.getShort("BurnTime");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putShort("BurnTime", (short) this.furnaceBurnTime);
        return nbt;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.speedBoost = nbt.getBoolean("speedBoost");
        this.furnaceCookTime = nbt.getShort("CookTime");
        this.currentItemBurnTime = burnTime(this.getStackInSlot(1));
        this.aspects = new AspectList();
        this.aspects.readFromNBT(nbt);
        this.vis = this.aspects.visSize();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putBoolean("speedBoost", this.speedBoost);
        nbt.putShort("CookTime", (short) this.furnaceCookTime);
        this.aspects.writeToNBT(nbt);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileSmelter tile) {
        tile.update();
    }

    /** Серверный тик (update оригинала; клиентская часть — только счётчики, на клиенте не нужна). */
    void update() {
        boolean flag = this.furnaceBurnTime > 0;
        boolean flag1 = false;
        this.count++;
        if (this.furnaceBurnTime > 0) {
            this.furnaceBurnTime--;
        }
        if (this.bellows < 0) {
            this.checkNeighbours();
        }
        int speed = this.getSpeed();
        if (this.speedBoost) {
            speed = (int) (speed * 0.8);
        }
        if (this.count % speed == 0 && this.aspects.size() > 0) {
            for (Aspect aspect : this.aspects.getAspects()) {
                if (this.aspects.getAmount(aspect) > 0 && TileAlembic.processAlembics(this.level, this.worldPosition, aspect)) {
                    this.takeFromContainer(aspect, 1);
                    break;
                }
            }
            for (Direction face : Direction.Plane.HORIZONTAL) {
                BlockState aux = this.level.getBlockState(this.worldPosition.relative(face));
                if (aux.is(BlocksTC.smelterAux.get()) && aux.getValue(BlockStateProperties.HORIZONTAL_FACING) == face.getOpposite()) {
                    for (Aspect aspect : this.aspects.getAspects()) {
                        if (this.aspects.getAmount(aspect) > 0 && TileAlembic.processAlembics(this.level, this.worldPosition.relative(face), aspect)) {
                            this.takeFromContainer(aspect, 1);
                            break;
                        }
                    }
                }
            }
        }
        if (this.furnaceBurnTime == 0) {
            if (this.canSmelt()) {
                this.currentItemBurnTime = this.furnaceBurnTime = burnTime(this.getStackInSlot(1));
                if (this.furnaceBurnTime > 0) {
                    BlockSmelter.setFurnaceState(this.level, this.worldPosition, true);
                    flag1 = true;
                    this.speedBoost = false;
                    ItemStack fuel = this.getStackInSlot(1);
                    if (!fuel.isEmpty()) {
                        // TODO этап 13: алюментум (ItemsTC.alumentum) ускоряет плавильню — speedBoost = true
                        // В оригинале остаток топлива брался у уже пустого стака (ведро лавы пропадало) — DEVIATIONS 30
                        ItemStack remainder = fuel.getCraftingRemainingItem();
                        ItemStack copy = fuel.copy();
                        copy.shrink(1);
                        this.setInventorySlotContents(1, copy.isEmpty() ? remainder : copy);
                    }
                } else {
                    BlockSmelter.setFurnaceState(this.level, this.worldPosition, false);
                }
            } else {
                BlockSmelter.setFurnaceState(this.level, this.worldPosition, false);
            }
        }
        if (this.getBlockState().getValue(BlockSmelter.ENABLED) && this.canSmelt()) {
            this.furnaceCookTime++;
            if (this.furnaceCookTime >= this.smeltTime) {
                this.furnaceCookTime = 0;
                this.smeltItem();
                flag1 = true;
            }
        } else {
            this.furnaceCookTime = 0;
        }
        if (flag != this.furnaceBurnTime > 0) {
            flag1 = true;
        }
        if (flag1) {
            this.setChanged();
        }
    }

    private boolean canSmelt() {
        if (this.getStackInSlot(0).isEmpty()) {
            return false;
        }
        AspectList al = ThaumcraftCraftingManager.getObjectTags(this.getStackInSlot(0));
        if (al != null && al.size() != 0) {
            int vs = al.visSize();
            if (vs > this.maxVis - this.vis) {
                return false;
            }
            this.smeltTime = (int) (vs * 2 * (1.0F - 0.125F * this.bellows));
            return true;
        }
        return false;
    }

    /** Мехи по бокам, кроме лицевой стороны. TODO этап 14: TileBellows.getBellows — пока мехов нет, их 0. */
    public void checkNeighbours() {
        this.bellows = 0;
    }

    private int getSmelterType() {
        BlockState s = this.getBlockState();
        return s.is(BlocksTC.smelterBasic.get()) ? 0 : (s.is(BlocksTC.smelterThaumium.get()) ? 1 : 2);
    }

    private float getEfficiency() {
        float efficiency = 0.8F;
        if (this.getSmelterType() == 1) {
            efficiency = 0.85F;
        }
        if (this.getSmelterType() == 2) {
            efficiency = 0.95F;
        }
        return efficiency;
    }

    private int getSpeed() {
        return 20 - (this.getSmelterType() == 1 ? 10 : 5);
    }

    public void smeltItem() {
        if (this.canSmelt()) {
            int flux = 0;
            AspectList al = ThaumcraftCraftingManager.getObjectTags(this.getStackInSlot(0));
            for (Aspect a : al.getAspects()) {
                if (this.getEfficiency() < 1.0F) {
                    int qq = al.getAmount(a);
                    for (int q = 0; q < qq; q++) {
                        if (this.level.random.nextFloat() > (a == Aspect.FLUX ? this.getEfficiency() * 0.66F : this.getEfficiency())) {
                            al.reduce(a, 1);
                            flux++;
                        }
                    }
                }
                this.aspects.add(a, al.getAmount(a));
            }
            if (flux > 0) {
                int pp = 0;
                outer:
                for (int c = 0; c < flux; c++) {
                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        BlockState vent = this.level.getBlockState(this.worldPosition.relative(face));
                        if (vent.is(BlocksTC.smelterVent.get()) && vent.getValue(BlockStateProperties.HORIZONTAL_FACING) == face.getOpposite()
                                && this.level.random.nextFloat() < 0.333) {
                            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, face.getOpposite().ordinal());
                            continue outer;
                        }
                    }
                    pp++;
                }
                AuraHelper.polluteAura(this.level, this.worldPosition, pp, true);
            }
            this.vis = this.aspects.visSize();
            this.decrStackSize(0, 1);
        }
    }

    public static int burnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING);
    }

    public static boolean isItemFuel(ItemStack stack) {
        return burnTime(stack) > 0;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            AspectList al = ThaumcraftCraftingManager.getObjectTags(stack);
            if (al != null && al.size() > 0) {
                return true;
            }
        }
        return slot == 1 && isItemFuel(stack);
    }

    /** Возможность IItemHandler со стороны (getSlotsForFace / canInsertItem / canExtractItem оригинала); null — весь инвентарь. */
    public IItemHandler getItemHandler(Direction side) {
        if (side == null) {
            return this.inventory;
        }
        if (this.sided[side.ordinal()] == null) {
            int[] slots = side == Direction.DOWN ? SLOTS_BOTTOM : (side == Direction.UP ? SLOTS_TOP : SLOTS_SIDES);
            this.sided[side.ordinal()] = new SidedHandler(slots, side);
        }
        return this.sided[side.ordinal()];
    }

    private final class SidedHandler implements IItemHandler {
        private final int[] slots;
        private final Direction side;

        SidedHandler(int[] slots, Direction side) {
            this.slots = slots;
            this.side = side;
        }

        @Override
        public int getSlots() {
            return this.slots.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return TileSmelter.this.inventory.getStackInSlot(this.slots[slot]);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (this.side == Direction.UP) {
                return stack;
            }
            return TileSmelter.this.inventory.insertItem(this.slots[slot], stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int s = this.slots[slot];
            if (this.side == Direction.UP && s == 1 && !TileSmelter.this.inventory.getStackInSlot(s).is(Items.BUCKET)) {
                return ItemStack.EMPTY;
            }
            return TileSmelter.this.inventory.extractItem(s, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return TileSmelter.this.inventory.getSlotLimit(this.slots[slot]);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return this.side != Direction.UP && TileSmelter.this.isItemValidForSlot(this.slots[slot], stack);
        }
    }

    public boolean takeFromContainer(Aspect tag, int amount) {
        if (this.aspects != null && this.aspects.getAmount(tag) >= amount) {
            this.aspects.remove(tag, amount);
            this.vis = this.aspects.visSize();
            this.setChanged();
            return true;
        }
        return false;
    }

    /** Блочное событие 1 — выброс через отводящий клапан (receiveClientEvent): шипение и струйки пара. */
    @Override
    public boolean triggerEvent(int i, int j) {
        if (i != 1) {
            return super.triggerEvent(i, j);
        }
        if (this.level != null && this.level.isClientSide) {
            Direction d = Direction.values()[j];
            BlockPos p = this.worldPosition;
            var rand = this.level.random;
            this.level.playLocalSound(p.getX() + 0.5 + d.getOpposite().getStepX(), p.getY() + 0.5, p.getZ() + 0.5 + d.getOpposite().getStepZ(),
                    SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 2.6F + (rand.nextFloat() - rand.nextFloat()) * 0.8F, true);
            for (int a = 0; a < 4; a++) {
                float fx = 0.1F - rand.nextFloat() * 0.2F;
                float fz = 0.1F - rand.nextFloat() * 0.2F;
                float fy = 0.1F - rand.nextFloat() * 0.2F;
                float fx2 = 0.1F - rand.nextFloat() * 0.2F;
                float fz2 = 0.1F - rand.nextFloat() * 0.2F;
                float fy2 = 0.1F - rand.nextFloat() * 0.2F;
                int color = 11184810;
                IFXDispatcher.get().drawVentParticles(p.getX() + 0.5F + fx + d.getOpposite().getStepX(), p.getY() + 0.5F + fy,
                        p.getZ() + 0.5F + fz + d.getOpposite().getStepZ(), d.getOpposite().getStepX() / 4.0F + fx2,
                        d.getOpposite().getStepY() / 4.0F + fy2, d.getOpposite().getStepZ() / 4.0F + fz2, color);
            }
        }
        return true;
    }
}
