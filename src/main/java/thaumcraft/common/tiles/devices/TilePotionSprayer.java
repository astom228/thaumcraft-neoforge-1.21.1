package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.config.ConfigAspects;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/**
 * Распылитель зелий (порт TilePotionSprayer): по аспектам зелья в слоте (не больше 10 каждого) втягивает эссенцию со всех сторон,
 * кроме выхода (тягой 128, по одной раз в 5 тиков), и копит до 8 зарядов. По фронту сигнала редстоуна тратит заряд — эффекты
 * зелья получают все существа в кубе 3×3×3 в двух блоках перед выходом; брызги — событием блока.
 */
public class TilePotionSprayer extends TileThaumcraftInventory implements IAspectContainer, IEssentiaTransport {
    public AspectList recipe = new AspectList();
    public AspectList recipeProgress = new AspectList();
    public int charges = 0;
    public int color = 0;
    int counter = 0;
    boolean activated = false;
    int venting = 0;
    Aspect currentSuction = null;

    public TilePotionSprayer(BlockPos pos, BlockState state) {
        super(ConfigBlocks.POTION_SPRAYER_TILE.get(), pos, state, 1);
    }

    private Direction facing() {
        return this.getBlockState().getValue(BlockTCDeviceFacing.FACING);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TilePotionSprayer tile) {
        tile.counter++;
        Direction facing = tile.facing();
        if (level.isClientSide) {
            if (tile.venting > 0) {
                tile.venting--;
                for (int a = 0; a < tile.venting / 2; a++) {
                    float fx = 0.1F - level.random.nextFloat() * 0.2F;
                    float fz = 0.1F - level.random.nextFloat() * 0.2F;
                    float fy = 0.1F - level.random.nextFloat() * 0.2F;
                    float fx2 = (float) (level.random.nextGaussian() * 0.06);
                    float fz2 = (float) (level.random.nextGaussian() * 0.06);
                    float fy2 = (float) (level.random.nextGaussian() * 0.06);
                    IFXDispatcher.get().drawVentParticles2(pos.getX() + 0.5F + fx + facing.getStepX() / 2.0F, pos.getY() + 0.5F + fy + facing.getStepY() / 2.0F,
                            pos.getZ() + 0.5F + fz + facing.getStepZ() / 2.0F, fx2 + facing.getStepX() * 0.25, fy2 + facing.getStepY() * 0.25,
                            fz2 + facing.getStepZ() * 0.25, tile.color, 4.0F);
                }
            }
            return;
        }
        if (tile.counter % 5 == 0) {
            tile.currentSuction = null;
            if (tile.getStackInSlot(0).isEmpty() || tile.charges >= 8) {
                return;
            }
            boolean done = true;
            for (Aspect aspect : tile.recipe.getAspectsSortedByName()) {
                if (tile.recipeProgress.getAmount(aspect) < tile.recipe.getAmount(aspect)) {
                    tile.currentSuction = aspect;
                    done = false;
                    break;
                }
            }
            if (done) {
                tile.recipeProgress = new AspectList();
                tile.charges++;
                tile.syncTile(false);
                tile.setChanged();
            } else if (tile.currentSuction != null) {
                tile.fill();
            }
        }
        if (!state.getValue(BlockTCDeviceFacing.ENABLED)) {
            if (!tile.activated && tile.charges > 0) {
                tile.charges--;
                tile.spray(level, pos, facing);
                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25F,
                        2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
                level.blockEvent(pos, state.getBlock(), 0, 0);
                tile.syncTile(false);
                tile.setChanged();
            }
            tile.activated = true;
        } else if (tile.activated) {
            tile.activated = false;
        }
    }

    private void spray(Level level, BlockPos pos, Direction facing) {
        PotionContents contents = this.getStackInSlot(0).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        int area = 1;
        BlockPos p = pos.relative(facing, 2);
        AABB box = new AABB(p.getX() - area, p.getY() - area, p.getZ() - area, p.getX() + 1 + area, p.getY() + 1 + area, p.getZ() + 1 + area);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e.isAlive() && e.isAffectedByPotions()) {
                for (MobEffectInstance eff : contents.getAllEffects()) {
                    Holder<MobEffect> potion = eff.getEffect();
                    if (potion.value().isInstantenous()) {
                        potion.value().applyInstantenousEffect(null, null, e, eff.getAmplifier(), 1.0);
                    } else {
                        e.addEffect(new MobEffectInstance(potion, eff.getDuration(), eff.getAmplifier()));
                    }
                }
            }
        }
    }

    /** Событие блока 0: струи пара из выхода (клиент). */
    @Override
    public boolean triggerEvent(int i, int j) {
        if (i >= 0) {
            if (this.level != null && this.level.isClientSide) {
                this.venting = 15;
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.recipe = new AspectList();
        this.recipe.readFromNBT(nbt, "recipe");
        this.recipeProgress = new AspectList();
        this.recipeProgress.readFromNBT(nbt, "progress");
        this.charges = nbt.getInt("charges");
        this.color = nbt.getInt("color");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.recipe.writeToNBT(nbt, "recipe");
        this.recipeProgress.writeToNBT(nbt, "progress");
        nbt.putInt("charges", this.charges);
        nbt.putInt("color", this.color);
        return nbt;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return !stack.isEmpty() && isValidPotion(stack);
    }

    /** SlotPotion.isValidPotion: зелье (обычное, взрывное, туманное) с настоящим эффектом — не вода и не «пустые» зелья. */
    public static boolean isValidPotion(ItemStack stack) {
        if (stack.is(Items.POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.SPLASH_POTION)) {
            PotionContents c = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            return c.potion().isPresent() && !c.is(Potions.WATER) && !c.is(Potions.AWKWARD) && !c.is(Potions.MUNDANE) && !c.is(Potions.THICK);
        }
        return false;
    }

    /** setInventorySlotContents/decrStackSize оригинала: новое зелье — новый рецепт, заряды сбрасываются. */
    @Override
    protected void onSlotContentsChanged(int slot) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        ItemStack stack = this.getStackInSlot(0);
        this.color = 3355443;
        if (!stack.isEmpty()) {
            this.recipe = ConfigAspects.getPotionAspects(stack);
            this.color = getPotionColor(stack);
        } else {
            this.recipe = new AspectList();
        }
        this.charges = 0;
        this.recipe = AspectHelper.cullTags(this.recipe, 10);
        this.recipeProgress = new AspectList();
        this.syncTile(false);
        this.setChanged();
    }

    public static int getPotionColor(ItemStack stack) {
        PotionContents c = stack.get(DataComponents.POTION_CONTENTS);
        return c != null && c.potion().isPresent() ? c.getColor() & 0xFFFFFF : 3355443;
    }

    void fill() {
        Direction facing = this.facing();
        for (int y = 0; y <= 1; y++) {
            for (Direction dir : Direction.values()) {
                if (dir != facing) {
                    BlockEntity te = ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition.above(y), dir);
                    if (te instanceof IEssentiaTransport ic) {
                        if (ic.getEssentiaAmount(dir.getOpposite()) > 0 && ic.getSuctionAmount(dir.getOpposite()) < this.getSuctionAmount(null)
                                && this.getSuctionAmount(null) >= ic.getMinimumSuction()) {
                            int ess = ic.takeEssentia(this.currentSuction, 1, dir.getOpposite());
                            if (ess > 0) {
                                this.addToContainer(this.currentSuction, ess);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int addToContainer(Aspect tt, int am) {
        int ce = this.recipe.getAmount(tt) - this.recipeProgress.getAmount(tt);
        if (ce <= 0) {
            return am;
        }
        int add = Math.min(ce, am);
        this.recipeProgress.add(tt, add);
        this.syncTile(false);
        this.setChanged();
        return am - add;
    }

    @Override
    public boolean takeFromContainer(Aspect tt, int am) {
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tt, int am) {
        return this.recipeProgress.getAmount(tt) >= am;
    }

    @Override
    public int containerContains(Aspect tt) {
        return this.recipeProgress.getAmount(tt);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face != this.facing();
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != this.facing();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        this.currentSuction = aspect;
    }

    @Override
    public Aspect getSuctionType(Direction loc) {
        return this.currentSuction;
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        return this.currentSuction != null ? 128 : 0;
    }

    @Override
    public Aspect getEssentiaType(Direction loc) {
        return null;
    }

    @Override
    public int getEssentiaAmount(Direction loc) {
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return this.canOutputTo(face) && this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return this.canInputFrom(face) ? amount - this.addToContainer(aspect, amount) : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public AspectList getAspects() {
        return this.recipeProgress;
    }

    @Override
    public void setAspects(AspectList aspects) {
        this.recipeProgress = aspects;
    }
}
