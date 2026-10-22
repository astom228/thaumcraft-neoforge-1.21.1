package thaumcraft.common.tiles.crafting;

import java.awt.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Тигель (порт TileCrucible): вода (до 1000), нагрев от огня, лавы, нитора или магмы снизу; при кипении (heat > 150) брошенные
 * предметы растворяются в аспекты или по рецепту превращаются в другие. Аспекты сверх 500 и раз в 5 с по одному «выкипают»
 * (иногда — Выброс в ауру). Жидкость доступна трубам через IFluidHandler.
 */
public class TileCrucible extends TileThaumcraft implements IAspectContainer {
    public short heat;
    public AspectList aspects = new AspectList();
    public final int maxTags = 500;
    private int delay = 0;
    private long counter = -100L;
    int prevcolor = 0;
    int prevx = 0;
    int prevy = 0;
    /** В оригинале возможность жидкости отдавала сам бак — изменения через трубы не сохранялись и не уходили клиентам (DEVIATIONS.md). */
    public final FluidTank tank = new FluidTank(1000) {
        @Override
        protected void onContentsChanged() {
            TileCrucible.this.setChanged();
            TileCrucible.this.syncTile(false);
        }
    };

    public TileCrucible(BlockPos pos, BlockState state) {
        super(ConfigBlocks.CRUCIBLE_TILE.get(), pos, state);
        this.heat = 0;
    }

    @Override
    public void readSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        this.heat = nbttagcompound.getShort("Heat");
        this.tank.readFromNBT(registries, nbttagcompound);
        this.aspects.readFromNBT(nbttagcompound);
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        nbttagcompound.putShort("Heat", this.heat);
        this.tank.writeToNBT(registries, nbttagcompound);
        this.aspects.writeToNBT(nbttagcompound);
        return nbttagcompound;
    }

    private boolean isHeatSource(BlockState block) {
        // Material.LAVA / Material.FIRE оригинала → лава (жидкость) и огонь (тег minecraft:fire); нитор; блок магмы
        return block.getFluidState().is(FluidTags.LAVA) || block.is(BlockTags.FIRE)
                || BlocksTC.nitor.values().stream().anyMatch(n -> block.is(n.get())) || block.is(Blocks.MAGMA_BLOCK);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileCrucible te) {
        te.update();
    }

    public void update() {
        this.counter++;
        int prevheat = this.heat;
        if (!this.level.isClientSide) {
            if (this.tank.getFluidAmount() > 0) {
                BlockState block = this.level.getBlockState(this.getBlockPos().below());
                if (!this.isHeatSource(block)) {
                    if (this.heat > 0) {
                        this.heat--;
                        if (this.heat == 149) {
                            this.setChanged();
                            this.syncTile(false);
                        }
                    }
                } else if (this.heat < 200) {
                    this.heat++;
                    if (prevheat < 151 && this.heat >= 151) {
                        this.setChanged();
                        this.syncTile(false);
                    }
                }
            } else if (this.heat > 0) {
                this.heat--;
            }
            if (this.aspects.visSize() > 500) {
                this.spillRandom();
            }
            if (this.counter >= 100L) {
                this.spillRandom();
                this.counter = 0L;
            }
        } else if (this.tank.getFluidAmount() > 0) {
            this.drawEffects();
        }
        if (this.level.isClientSide && prevheat < 151 && this.heat >= 151) {
            this.heat++;
        }
    }

    private void drawEffects() {
        IFXDispatcher fx = IFXDispatcher.get();
        BlockPos pos = this.worldPosition;
        if (this.heat > 150) {
            fx.crucibleFroth(pos.getX() + 0.2F + this.level.random.nextFloat() * 0.6F, pos.getY() + this.getFluidHeight(),
                    pos.getZ() + 0.2F + this.level.random.nextFloat() * 0.6F);
            if (this.aspects.visSize() > 500) {
                for (int a = 0; a < 2; a++) {
                    fx.crucibleFrothDown(pos.getX(), pos.getY() + 1, pos.getZ() + this.level.random.nextFloat());
                    fx.crucibleFrothDown(pos.getX() + 1, pos.getY() + 1, pos.getZ() + this.level.random.nextFloat());
                    fx.crucibleFrothDown(pos.getX() + this.level.random.nextFloat(), pos.getY() + 1, pos.getZ());
                    fx.crucibleFrothDown(pos.getX() + this.level.random.nextFloat(), pos.getY() + 1, pos.getZ() + 1);
                }
            }
        }
        if (this.level.random.nextInt(6) == 0 && this.aspects.size() > 0) {
            int color = this.aspects.getAspects()[this.level.random.nextInt(this.aspects.size())].getColor() + -16777216;
            int x = 5 + this.level.random.nextInt(22);
            int y = 5 + this.level.random.nextInt(22);
            this.delay = this.level.random.nextInt(10);
            this.prevcolor = color;
            this.prevx = x;
            this.prevy = y;
            Color c = new Color(color);
            fx.crucibleBubble(pos.getX() + x / 32.0F + 0.015625F, pos.getY() + 0.05F + this.getFluidHeight(), pos.getZ() + y / 32.0F + 0.015625F,
                    c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
        }
    }

    public void ejectItem(ItemStack items) {
        boolean first = true;
        do {
            ItemStack spitout = items.copy();
            if (spitout.getCount() > spitout.getMaxStackSize()) {
                spitout.setCount(spitout.getMaxStackSize());
            }
            items.shrink(spitout.getCount());
            EntitySpecialItem entityitem = new EntitySpecialItem(this.level, this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.71F,
                    this.worldPosition.getZ() + 0.5F, spitout);
            entityitem.setDeltaMovement(first ? 0.0 : (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.01F, 0.075F,
                    first ? 0.0 : (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.01F);
            this.level.addFreshEntity(entityitem);
            first = false;
        } while (items.getCount() > 0);
    }

    /**
     * Растворить/переработать предметы стопки (по одному). Возвращает остаток или null, если ничего не осталось.
     * @param player бросивший игрок (в оригинале — по имени из данных предмета); без игрока рецепты не срабатывают
     */
    public ItemStack attemptSmelt(ItemStack item, Player player) {
        boolean bubble = false;
        boolean craftDone = false;
        int stacksize = item.getCount();
        for (int a = 0; a < stacksize; a++) {
            CrucibleRecipe rc = ThaumcraftCraftingManager.findMatchingCrucibleRecipe(player, this.aspects, item);
            if (rc != null && this.tank.getFluidAmount() > 0) {
                ItemStack out = rc.getRecipeOutput().copy();
                if (player != null) {
                    EventHooks.firePlayerCraftingEvent(player, out, new SimpleContainer(item));
                }
                this.aspects = rc.removeMatching(this.aspects);
                this.tank.drain(50, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                this.ejectItem(out);
                craftDone = true;
                stacksize--;
                this.counter = -250L;
            } else {
                AspectList ot = ThaumcraftCraftingManager.getObjectTags(item);
                if (ot != null && ot.size() != 0) {
                    for (Aspect tag : ot.getAspects()) {
                        this.aspects.add(tag, ot.getAmount(tag));
                    }
                    bubble = true;
                    stacksize--;
                    this.counter = -150L;
                }
            }
        }
        if (bubble) {
            this.level.playSound(null, this.worldPosition, SoundsTC.bubble.get(), SoundSource.BLOCKS, 0.2F, 1.0F + this.level.random.nextFloat() * 0.4F);
            this.syncTile(false);
            this.level.blockEvent(this.worldPosition, BlocksTC.crucible.get(), 2, 1);
        }
        if (craftDone) {
            this.syncTile(false);
            this.level.blockEvent(this.worldPosition, BlocksTC.crucible.get(), 99, 0);
        }
        this.setChanged();
        if (stacksize <= 0) {
            return null;
        }
        item.setCount(stacksize);
        return item;
    }

    /** Предмет упал в тигель: игрок — тот, кто его бросил. */
    public void attemptSmelt(ItemEntity entity) {
        ItemStack item = entity.getItem();
        Entity owner = entity.getOwner();
        ItemStack res = this.attemptSmelt(item, owner instanceof Player p ? p : null);
        if (res != null && res.getCount() > 0) {
            item.setCount(res.getCount());
            entity.setItem(item);
        } else {
            entity.discard();
        }
    }

    public float getFluidHeight() {
        float base = 0.3F + 0.5F * ((float) this.tank.getFluidAmount() / this.tank.getCapacity());
        float out = base + this.aspects.visSize() / 500.0F * (1.0F - base);
        if (out > 1.0F) {
            out = 1.001F;
        }
        if (out == 1.0F) {
            out = 0.9999F;
        }
        return out;
    }

    public void spillRandom() {
        if (this.aspects.size() > 0) {
            Aspect tag = this.aspects.getAspects()[this.level.random.nextInt(this.aspects.getAspects().length)];
            this.aspects.remove(tag, 1);
            if (this.level.random.nextBoolean()) {
                AuraHelper.polluteAura(this.level, this.getBlockPos(), 1.0F, true);
            }
        }
        this.setChanged();
        this.syncTile(false);
    }

    public void spillRemnants() {
        if (this.tank.getFluidAmount() > 0 || this.aspects.visSize() > 0) {
            this.tank.setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
            if (this.aspects.visSize() > 1) {
                AuraHelper.polluteAura(this.level, this.getBlockPos(), this.aspects.visSize() * 0.75F, true);
            }
            this.aspects = new AspectList();
            this.level.blockEvent(this.worldPosition, BlocksTC.crucible.get(), 2, 5);
            this.setChanged();
            this.syncTile(false);
        }
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 99) {
            if (this.level.isClientSide) {
                IFXDispatcher.get().drawBamf(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.25F, this.worldPosition.getZ() + 0.5, true, true,
                        Direction.UP);
                this.level.playLocalSound(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.5F, this.worldPosition.getZ() + 0.5F,
                        SoundsTC.spill.get(), SoundSource.BLOCKS, 0.2F, 1.0F, false);
            }
            return true;
        } else if (i == 1) {
            if (this.level.isClientSide) {
                BlockPos up = this.worldPosition.above();
                IFXDispatcher.get().drawBamf(up.getX() + 0.5, up.getY() + 0.5, up.getZ() + 0.5, true, true, Direction.UP);
            }
            return true;
        } else if (i == 2) {
            this.level.playLocalSound(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.5F, this.worldPosition.getZ() + 0.5F,
                    SoundsTC.spill.get(), SoundSource.BLOCKS, 0.2F, 1.0F, false);
            if (this.level.isClientSide) {
                for (int q = 0; q < 10; q++) {
                    IFXDispatcher.get().crucibleBoil(this.worldPosition, this.getFluidHeight(), this.aspects, j);
                }
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }

    @Override
    public AspectList getAspects() {
        return this.aspects;
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
}
