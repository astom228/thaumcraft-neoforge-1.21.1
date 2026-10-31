package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.entities.construct.golem.GolemProperties;
import thaumcraft.common.entities.construct.golem.ItemGolemPlacer;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/**
 * Пресс големов (порт TileGolemBuilder): по заказу из окна забирает у игрока детали и затем раз в 5 тиков всасывает по
 * единице эссенции Machina (стоимость — 2 за черту + число деталей); готовый голем — в слот выдачи. На клиенте —
 * движение пресса, пар и шипение.
 */
public class TileGolemBuilder extends TileThaumcraftInventory implements IEssentiaTransport {
    public long golem = -1L;
    public int cost = 0;
    public int maxCost = 0;
    boolean bufferedEssentia = false;
    int ticks = 0;
    public int press = 0;
    IGolemProperties props = null;
    ItemStack[] components = null;

    public TileGolemBuilder(BlockPos pos, BlockState state) {
        super(ConfigBlocks.GOLEM_BUILDER_TILE.get(), pos, state, 1);
        this.syncedSlots = new int[] {0};
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        super.readSyncNBT(nbt, registries);
        this.golem = nbt.getLong("golem");
        this.cost = nbt.getInt("cost");
        this.maxCost = nbt.getInt("mcost");
        if (this.golem >= 0L) {
            this.props = GolemProperties.fromLong(this.golem);
            this.components = this.props.generateComponents();
        } else {
            this.props = null;
            this.components = null;
        }
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putLong("golem", this.golem);
        nbt.putInt("cost", this.cost);
        nbt.putInt("mcost", this.maxCost);
        return super.writeSyncNBT(nbt, registries);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileGolemBuilder tile) {
        tile.update();
    }

    private boolean outputAccepts(ItemStack placer) {
        ItemStack out = this.getStackInSlot(0);
        return out.isEmpty() || out.getCount() < out.getMaxStackSize() && ItemStack.isSameItemSameComponents(out, placer);
    }

    void update() {
        boolean complete = false;
        Level world = this.level;
        BlockPos p = this.worldPosition;
        if (!world.isClientSide) {
            this.ticks++;
            if (this.ticks % 5 == 0 && this.cost > 0 && this.golem >= 0L) {
                if (this.bufferedEssentia || this.drawEssentia()) {
                    this.bufferedEssentia = false;
                    this.cost--;
                    this.setChanged();
                }
                if (this.cost <= 0) {
                    ItemStack placer = ItemGolemPlacer.createStack(this.golem, 0);
                    if (this.outputAccepts(placer)) {
                        if (!this.getStackInSlot(0).isEmpty()) {
                            ItemStack out = this.getStackInSlot(0).copy();
                            out.grow(1);
                            this.setInventorySlotContents(0, out);
                        } else {
                            this.setInventorySlotContents(0, placer);
                        }
                        complete = true;
                        world.playSound(null, p, SoundsTC.wand.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
        } else {
            IFXDispatcher fx = IFXDispatcher.get();
            if (this.press < 90 && this.cost > 0 && this.golem >= 0L) {
                this.press += 6;
                if (this.press >= 60) {
                    world.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.66F,
                            1.0F + world.random.nextFloat() * 0.1F, false);
                    for (int a = 0; a < 16; a++) {
                        fx.drawVentParticles(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, world.random.nextGaussian() * 0.1, 0.0,
                                world.random.nextGaussian() * 0.1, 11184810);
                    }
                }
            }
            if (this.press >= 90 && world.random.nextInt(8) == 0) {
                fx.drawVentParticles(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, world.random.nextGaussian() * 0.1, 0.0, world.random.nextGaussian() * 0.1,
                        11184810);
                world.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.1F,
                        1.0F + world.random.nextFloat() * 0.1F, false);
            }
            if (this.press > 0 && (this.cost <= 0 || this.golem == -1L)) {
                if (this.press >= 90) {
                    for (int a = 0; a < 10; a++) {
                        fx.drawVentParticles(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, world.random.nextGaussian() * 0.1, 0.0,
                                world.random.nextGaussian() * 0.1, 11184810);
                    }
                }
                this.press -= 3;
            }
        }
        if (complete) {
            this.cost = 0;
            this.golem = -1L;
            this.syncTile(false);
            this.setChanged();
        }
    }

    /**
     * Начать сборку (PacketStartGolemCraftToServer): слот выдачи должен принять голема, у игрока — все детали. Состав уже
     * проверен пакетом (исследования игрока, ранг 0).
     */
    public boolean startCraft(long id, Player p) {
        ItemStack placer = ItemGolemPlacer.createStack(id, 0);
        if (!this.outputAccepts(placer)) {
            return false;
        }
        IGolemProperties newProps = GolemProperties.fromLong(id);
        ItemStack[] comps = newProps.generateComponents();
        for (ItemStack stack : comps) {
            if (!InventoryUtils.isPlayerCarryingAmount(p, stack, true)) {
                return false;
            }
        }
        this.golem = id;
        this.props = newProps;
        this.components = comps;
        this.cost = this.props.getTraits().size() * 2;
        for (ItemStack stack : this.components) {
            this.cost += stack.getCount();
            InventoryUtils.consumePlayerItem(p, stack, true, true);
        }
        this.maxCost = this.cost;
        this.setChanged();
        this.syncTile(false);
        this.level.playSound(null, this.worldPosition, SoundsTC.wand.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
        return true;
    }

    /** Занят ли пресс сборкой (новый заказ не принимается, чтобы не потерять уже забранные детали). */
    public boolean isBusy() {
        return this.cost > 0 && this.golem >= 0L;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemGolemPlacer;
    }

    boolean drawEssentia() {
        for (Direction face : Direction.values()) {
            BlockEntity te = ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition, face);
            if (te != null) {
                IEssentiaTransport ic = (IEssentiaTransport) te;
                if (!ic.canOutputTo(face.getOpposite())) {
                    return false;
                }
                if (ic.getSuctionAmount(face.getOpposite()) < this.getSuctionAmount(face) && ic.takeEssentia(Aspect.MECHANISM, 1, face.getOpposite()) == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face.getAxis().isHorizontal() || face == Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return this.isConnectable(face);
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return Aspect.MECHANISM;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return this.cost > 0 && this.golem >= 0L ? 128 : 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (!this.bufferedEssentia && this.cost > 0 && this.golem >= 0L && aspect == Aspect.MECHANISM) {
            this.bufferedEssentia = true;
            return 1;
        }
        return 0;
    }
}
