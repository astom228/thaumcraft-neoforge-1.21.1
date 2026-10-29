package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.common.blocks.devices.BlockInfernalFurnace;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/**
 * Адская печь (порт TileInfernalFurnace): 32 слота; предметы бросают в лаву (или вставляют сверху). Плавит по одному предмету
 * за 140 тиков (80 — пока есть «ускорение» от 20 Вис ауры; каждые мехи вокруг на расстоянии 2 — быстрее, минимум 10), результат
 * выбрасывает в окно (решётку) вместе с бонусами плавки (за каждые мехи + 1 — ещё попытка) и опытом; изредка — выброс.
 * Неплавкие предметы сгорают.
 */
public class TileInfernalFurnace extends TileThaumcraftInventory {
    public int furnaceCookTime = 0;
    public int furnaceMaxCookTime = 0;
    public int speedyTime = 0;

    public TileInfernalFurnace(BlockPos pos, BlockState state) {
        super(ConfigBlocks.INFERNAL_FURNACE_TILE.get(), pos, state, 32);
    }

    public Direction facing() {
        return this.getBlockState().getValue(BlockInfernalFurnace.FACING);
    }

    /** Вставка только сверху, извлечения нет (getSlotsForFace / canExtractItem оригинала). */
    public IItemHandler capability(Direction side) {
        if (side != null && side != Direction.UP) {
            return null;
        }
        IItemHandler inner = this.inventory;
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return inner.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inner.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return inner.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return inner.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return inner.isItemValid(slot, stack);
            }
        };
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.furnaceCookTime = nbt.getShort("CookTime");
        this.speedyTime = nbt.getShort("SpeedyTime");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putShort("CookTime", (short) this.furnaceCookTime);
        nbt.putShort("SpeedyTime", (short) this.speedyTime);
    }

    private Optional<RecipeHolder<SmeltingRecipe>> recipe(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), this.level);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileInfernalFurnace tile) {
        boolean cookedflag = false;
        if (tile.furnaceCookTime > 0) {
            tile.furnaceCookTime--;
            cookedflag = true;
        }
        if (tile.furnaceMaxCookTime <= 0) {
            tile.furnaceMaxCookTime = tile.calcCookTime();
        }
        if (tile.furnaceCookTime > tile.furnaceMaxCookTime) {
            tile.furnaceCookTime = tile.furnaceMaxCookTime;
        }
        if (tile.furnaceCookTime <= 0 && cookedflag) {
            for (int a = 0; a < tile.getSizeInventory(); a++) {
                ItemStack in = tile.getStackInSlot(a);
                if (in.isEmpty()) {
                    continue;
                }
                Optional<RecipeHolder<SmeltingRecipe>> r = tile.recipe(in);
                if (r.isPresent()) {
                    ItemStack out = r.get().value().assemble(new SingleRecipeInput(in), level.registryAccess());
                    if (!out.isEmpty()) {
                        if (tile.speedyTime > 0) {
                            tile.speedyTime--;
                        }
                        tile.ejectItem(out.copy(), in, r.get().value().getExperience());
                        level.blockEvent(pos, state.getBlock(), 3, 0);
                        if (level.random.nextInt(20) == 0) {
                            AuraHelper.polluteAura(level, pos.relative(tile.facing().getOpposite()), 1.0F, true);
                        }
                        tile.decrStackSize(a, 1);
                        break;
                    }
                }
                tile.setInventorySlotContents(a, ItemStack.EMPTY);
            }
        }
        if (tile.speedyTime <= 0) {
            tile.speedyTime = (int) AuraHelper.drainVis(level, pos, 20.0F, false);
        }
        if (tile.furnaceCookTime == 0 && !cookedflag) {
            for (int a = 0; a < tile.getSizeInventory(); a++) {
                if (tile.recipe(tile.getStackInSlot(a)).isPresent()) {
                    tile.furnaceMaxCookTime = tile.calcCookTime();
                    tile.furnaceCookTime = tile.furnaceMaxCookTime;
                    break;
                }
            }
        }
    }

    /** Мехи, обращённые к печи, на расстоянии 2 (кроме верха), без сигнала; не больше 4. */
    private int getBellows() {
        int bellows = 0;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue;
            }
            BlockPos p2 = this.worldPosition.relative(dir, 2);
            if (!this.level.isLoaded(p2)) {
                continue;
            }
            BlockEntity tile = this.level.getBlockEntity(p2);
            if (tile instanceof TileBellows b && b.facing() == dir.getOpposite() && !this.level.hasNeighborSignal(p2)) {
                bellows++;
            }
        }
        return Math.min(4, bellows);
    }

    private int calcCookTime() {
        int b = this.getBellows();
        if (b > 0) {
            b = (20 - (b - 1)) * b;
        }
        return Math.max(10, (this.speedyTime > 0 ? 80 : 140) - b);
    }

    /** Предмет, упавший в лаву: плавкий — внутрь (остаток возвращается), иначе сгорает. */
    public ItemStack addItemsToInventory(ItemStack items) {
        if (this.recipe(items).isPresent()) {
            return InventoryUtils.insertStackAt(this.level, this.worldPosition, Direction.UP, items, false);
        }
        this.destroyItem();
        return ItemStack.EMPTY;
    }

    private void destroyItem() {
        this.level.playSound(null, this.worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3F,
                2.6F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.8F);
        if (this.level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, this.worldPosition.getX() + this.level.random.nextFloat(), this.worldPosition.getY() + 1,
                    this.worldPosition.getZ() + this.level.random.nextFloat(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void ejectItem(ItemStack items, ItemStack furnaceItemStack, float xpf) {
        List<ItemStack> ejecti = new ArrayList<>();
        ejecti.add(items.copy());
        int bellows = this.getBellows() + 1;
        Direction face = this.facing().getOpposite();
        int facingX = face.getStepX();
        int facingZ = face.getStepZ();
        float lx = 0.5F + facingX * 1.2F;
        float lz = 0.5F + facingZ * 1.2F;
        for (int a = 0; a < bellows; a++) {
            for (ThaumcraftApi.SmeltBonus bonus : CommonInternals.smeltingBonus) {
                if (bonus.matches(furnaceItemStack) && this.level.random.nextFloat() <= bonus.chance()) {
                    ItemStack is = bonus.out().copy();
                    if (is.getCount() < 1) {
                        is.setCount(1);
                    }
                    ejecti.add(is);
                }
            }
        }
        for (ItemStack outItem : ejecti) {
            if (!outItem.isEmpty()) {
                InventoryUtils.ejectStackAt(this.level, this.worldPosition, face, outItem);
            }
        }
        int cnt = items.getCount();
        if (xpf == 0.0F) {
            cnt = 0;
        } else if (xpf < 1.0F) {
            int var4 = Mth.floor(cnt * xpf);
            if (var4 < Mth.ceil(cnt * xpf) && Math.random() < cnt * xpf - var4) {
                var4++;
            }
            cnt = var4;
        }
        while (cnt > 0) {
            int var4 = ExperienceOrb.getExperienceValue(cnt);
            cnt -= var4;
            ExperienceOrb xp = new ExperienceOrb(this.level, this.worldPosition.getX() + lx, this.worldPosition.getY() + 0.4F,
                    this.worldPosition.getZ() + lz, var4);
            double mx = facingX == 0 ? (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.025F : facingX * 0.13F;
            double mz = facingZ == 0 ? (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.025F : facingZ * 0.13F;
            xp.setDeltaMovement(mx, 0.0, mz);
            this.level.addFreshEntity(xp);
        }
    }

    /** Событие блока 3: брызги лавы из окна и бульканье (клиент). */
    @Override
    public boolean triggerEvent(int i, int j) {
        if (i != 3) {
            return super.triggerEvent(i, j);
        }
        if (this.level.isClientSide) {
            Direction face = this.facing().getOpposite();
            for (int a = 0; a < 5; a++) {
                IFXDispatcher.get().furnaceLavaFx(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), face.getStepX(),
                        face.getStepZ());
                this.level.playLocalSound(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.5F, this.worldPosition.getZ() + 0.5F,
                        SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.1F + this.level.random.nextFloat() * 0.1F, 0.9F + this.level.random.nextFloat() * 0.15F,
                        false);
            }
        }
        return true;
    }
}
