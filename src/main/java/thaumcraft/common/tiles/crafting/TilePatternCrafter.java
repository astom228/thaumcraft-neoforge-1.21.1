package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.blocks.crafting.BlockPatternCrafter;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Шаблонный крафтер (порт TilePatternCrafter): раз в секунду (без сигнала) берёт из инвентаря сверху нужное число одинаковых
 * предметов, раскладывает их в сетку 3×3 по одному из 10 шаблонов и кладёт результат (и остатки рецепта) в инвентарь снизу.
 * Каждый крафт — 1 заряд; заряды берутся из ауры по 5 Вис. На клиенте — вращение шестерёнок и щелчки после крафта.
 */
public class TilePatternCrafter extends TileThaumcraft {
    public byte type = 0;
    public int count;
    float power = 0.0F;
    public float rot;
    public float rp;
    public int rotTicks = 0;
    private final NonNullList<ItemStack> matrix = NonNullList.withSize(9, ItemStack.EMPTY);
    private final List<ItemStack> remaining = new ArrayList<>();
    private ItemStack outStack = ItemStack.EMPTY;

    public TilePatternCrafter(BlockPos pos, BlockState state) {
        super(ConfigBlocks.PATTERN_CRAFTER_TILE.get(), pos, state);
        this.count = (int) (Math.random() * 20);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.type = nbt.getByte("type");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putByte("type", this.type);
        return nbt;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        this.power = nbt.getFloat("power");
        super.loadAdditional(nbt, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putFloat("power", this.power);
        super.saveAdditional(nbt, registries);
    }

    /** Сколько предметов нужно шаблону type. */
    private int amount() {
        return switch (this.type) {
            case 1 -> 1;
            case 2, 3 -> 2;
            case 4 -> 4;
            case 5, 6 -> 3;
            case 7, 8 -> 6;
            case 9 -> 8;
            default -> 9;
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TilePatternCrafter tile) {
        if (level.isClientSide) {
            if (tile.rotTicks > 0) {
                tile.rotTicks--;
                if (tile.rotTicks % Math.floor(Math.max(1.0F, tile.rp)) == 0.0) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.clack.get(), SoundSource.BLOCKS, 0.2F, 1.7F, false);
                }
                tile.rp++;
            } else {
                tile.rp *= 0.8F;
            }
            tile.rot = tile.rot + tile.rp;
            return;
        }
        if (tile.count++ % 20 != 0 || !state.getValue(BlockPatternCrafter.ENABLED)) {
            return;
        }
        if (tile.power <= 0.0F) {
            tile.power = tile.power + AuraHelper.drainVis(level, pos, 5.0F, false);
        }
        int amt = tile.amount();
        IItemHandler above = ThaumcraftApiHelper.getItemHandlerAt(level, pos.above(), Direction.DOWN);
        IItemHandler below = ThaumcraftApiHelper.getItemHandlerAt(level, pos.below(), Direction.UP);
        if (above == null || below == null) {
            return;
        }
        for (int a = 0; a < above.getSlots(); a++) {
            ItemStack testStack = above.getStackInSlot(a).copy();
            if (testStack.isEmpty()) {
                continue;
            }
            testStack.setCount(amt);
            if (InventoryUtils.removeStackFrom(above, testStack.copy(), true).getCount() == amt && tile.craft(level, testStack) && tile.power >= 1.0F
                    && ItemHandlerHelper.insertItem(below, tile.outStack.copy(), true).isEmpty()) {
                boolean b = true;
                for (ItemStack r : tile.remaining) {
                    if (!ItemHandlerHelper.insertItem(below, r.copy(), true).isEmpty()) {
                        b = false;
                        break;
                    }
                }
                if (b) {
                    ItemHandlerHelper.insertItem(below, tile.outStack.copy(), false);
                    for (ItemStack r : tile.remaining) {
                        ItemHandlerHelper.insertItem(below, r.copy(), false);
                    }
                    InventoryUtils.removeStackFrom(above, testStack, false);
                    level.blockEvent(pos, state.getBlock(), 1, 0);
                    tile.power--;
                    tile.setChanged();
                    break;
                }
            }
        }
    }

    private boolean craft(Level level, ItemStack inStack) {
        this.outStack = ItemStack.EMPTY;
        this.remaining.clear();
        for (int i = 0; i < 9; i++) {
            this.matrix.set(i, ItemStack.EMPTY);
        }
        ItemStack one = inStack.copyWithCount(1);
        switch (this.type) {
            case 0 -> {
                for (int a = 0; a < 9; a++) {
                    this.matrix.set(a, one.copy());
                }
            }
            case 1 -> this.matrix.set(0, one.copy());
            case 2 -> {
                for (int a = 0; a < 2; a++) {
                    this.matrix.set(a, one.copy());
                }
            }
            case 3 -> {
                for (int a = 0; a < 2; a++) {
                    this.matrix.set(a * 3, one.copy());
                }
            }
            case 4 -> {
                for (int a = 0; a < 2; a++) {
                    for (int b = 0; b < 2; b++) {
                        this.matrix.set(a + b * 3, one.copy());
                    }
                }
            }
            case 5 -> {
                for (int a = 0; a < 3; a++) {
                    this.matrix.set(a, one.copy());
                }
            }
            case 6 -> {
                for (int a = 0; a < 3; a++) {
                    this.matrix.set(a * 3, one.copy());
                }
            }
            case 7 -> {
                for (int a = 0; a < 6; a++) {
                    this.matrix.set(a, one.copy());
                }
            }
            case 8 -> {
                for (int a = 0; a < 2; a++) {
                    for (int b = 0; b < 3; b++) {
                        this.matrix.set(a + b * 3, one.copy());
                    }
                }
            }
            case 9 -> {
                for (int a = 0; a < 9; a++) {
                    if (a != 4) {
                        this.matrix.set(a, one.copy());
                    }
                }
            }
            default -> {
            }
        }
        CraftingInput input = CraftingInput.of(3, 3, this.matrix);
        Optional<RecipeHolder<CraftingRecipe>> ir = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (ir.isEmpty()) {
            return false;
        }
        this.outStack = ir.get().value().assemble(input, level.registryAccess());
        for (ItemStack r : ir.get().value().getRemainingItems(input)) {
            if (!r.isEmpty()) {
                this.remaining.add(r);
            }
        }
        return !this.outStack.isEmpty();
    }

    public void cycle() {
        this.type++;
        if (this.type > 9) {
            this.type = 0;
        }
        this.syncTile(false);
        this.setChanged();
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 1) {
            if (this.level != null && this.level.isClientSide) {
                this.rotTicks = 10;
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }

    /** getCuboidByFacing оригинала: переключатель шаблона на передней стороне (в координатах блока). */
    public static AABB knob(Direction facing) {
        return switch (facing) {
            case EAST -> new AABB(0.125, 0.125, 0.375, 0.25, 0.375, 0.625);
            case NORTH -> new AABB(0.375, 0.125, 0.75, 0.625, 0.375, 0.875);
            case SOUTH -> new AABB(0.375, 0.125, 0.125, 0.625, 0.375, 0.25);
            default -> new AABB(0.75, 0.125, 0.375, 0.875, 0.375, 0.625);
        };
    }
}
