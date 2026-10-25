package thaumcraft.common.tiles.crafting;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

/**
 * Тауматориум (порт TileThaumatorium): автоматический тигель. Над тиглем с нагревом (лава, огонь, нитор, магма под ним) по
 * выбранным в окне рецептам тигля и катализатору в слоте всасывает из труб нужную эссенцию и выдаёт результат на лицевую
 * сторону. Мнемонические матрицы по бокам — по 2 рецепта сверх одного. Сигнал красного камня останавливает его.
 */
public class TileThaumatorium extends TileThaumcraftInventory implements IAspectContainer, IEssentiaTransport {
    public AspectList essentia = new AspectList();
    public ArrayList<Integer> recipeHash = new ArrayList<>();
    public ArrayList<AspectList> recipeEssentia = new ArrayList<>();
    public ArrayList<String> recipePlayer = new ArrayList<>();
    public int currentCraft = -1;
    public int maxRecipes = 1;
    public Aspect currentSuction = null;
    int venting = 0;
    int counter = 0;
    boolean heated = false;
    CrucibleRecipe currentRecipe = null;

    public TileThaumatorium(BlockPos pos, BlockState state) {
        super(ConfigBlocks.THAUMATORIUM_TILE.get(), pos, state, 1);
    }

    public Direction getFacing() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        super.readSyncNBT(nbt, registries);
        this.essentia = new AspectList();
        this.essentia.readFromNBT(nbt);
        this.maxRecipes = nbt.getByte("maxrec");
        this.recipeEssentia = new ArrayList<>();
        this.recipeHash = new ArrayList<>();
        this.recipePlayer = new ArrayList<>();
        for (int hash : nbt.getIntArray("recipes")) {
            CrucibleRecipe recipe = ThaumcraftApi.getCrucibleRecipeFromHash(hash);
            if (recipe != null) {
                this.recipeEssentia.add(recipe.getAspects().copy());
                this.recipePlayer.add("");
                this.recipeHash.add(hash);
            }
        }
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        super.writeSyncNBT(nbt, registries);
        nbt.putByte("maxrec", (byte) this.maxRecipes);
        this.essentia.writeToNBT(nbt);
        int[] hashes = new int[this.recipeHash.size()];
        for (int a = 0; a < this.recipeHash.size(); a++) {
            hashes[a] = this.recipeHash.get(a);
        }
        nbt.putIntArray("recipes", hashes);
        return nbt;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        ListTag list = nbt.getList("OutputPlayer", Tag.TAG_STRING);
        for (int a = 0; a < list.size(); a++) {
            if (this.recipePlayer.size() > a) {
                this.recipePlayer.set(a, list.getString(a));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        ListTag list = new ListTag();
        for (String p : this.recipePlayer) {
            if (p != null) {
                list.add(StringTag.valueOf(p));
            }
        }
        nbt.put("OutputPlayer", list);
    }

    boolean checkHeat() {
        BlockState block = this.level.getBlockState(this.worldPosition.below(2));
        return block.getFluidState().is(FluidTags.LAVA) || block.is(BlockTags.FIRE)
                || BlocksTC.nitor.values().stream().anyMatch(n -> block.is(n.get())) || block.is(Blocks.MAGMA_BLOCK);
    }

    public ItemStack getCurrentOutputRecipe() {
        ItemStack out = ItemStack.EMPTY;
        if (this.currentCraft >= 0 && this.recipeHash != null && !this.recipeHash.isEmpty()) {
            CrucibleRecipe recipe = ThaumcraftApi.getCrucibleRecipeFromHash(this.recipeHash.get(this.currentCraft));
            if (recipe != null) {
                out = recipe.getRecipeOutput().copy();
            }
        }
        return out;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileThaumatorium tile) {
        tile.update();
    }

    void update() {
        if (!this.level.isClientSide) {
            if (this.counter == 0 || this.counter % 40 == 0) {
                this.heated = this.checkHeat();
                this.getUpgrades();
            }
            this.counter++;
            if (this.heated && !this.gettingPower() && this.counter % 5 == 0 && this.recipeHash != null && !this.recipeHash.isEmpty()) {
                if (this.getStackInSlot(0).isEmpty()) {
                    this.currentSuction = null;
                    return;
                }
                if (this.currentCraft < 0 || this.currentCraft >= this.recipeHash.size() || this.currentRecipe == null
                        || !this.currentRecipe.catalystMatches(this.getStackInSlot(0))) {
                    for (int a = 0; a < this.recipeHash.size(); a++) {
                        CrucibleRecipe recipe = ThaumcraftApi.getCrucibleRecipeFromHash(this.recipeHash.get(a));
                        if (recipe != null && recipe.catalystMatches(this.getStackInSlot(0))) {
                            this.currentCraft = a;
                            this.currentRecipe = recipe;
                            break;
                        }
                    }
                }
                if (this.currentCraft < 0 || this.currentCraft >= this.recipeHash.size()) {
                    return;
                }
                boolean done = true;
                this.currentSuction = null;
                for (Aspect aspect : this.recipeEssentia.get(this.currentCraft).getAspectsSortedByName()) {
                    if (this.essentia.getAmount(aspect) < this.recipeEssentia.get(this.currentCraft).getAmount(aspect)) {
                        this.currentSuction = aspect;
                        done = false;
                        break;
                    }
                }
                if (done) {
                    this.completeRecipe();
                } else if (this.currentSuction != null) {
                    this.fill();
                }
            }
        } else if (this.venting > 0) {
            this.venting--;
            var r = this.level.random;
            float fx = 0.1F - r.nextFloat() * 0.2F;
            float fz = 0.1F - r.nextFloat() * 0.2F;
            float fy = 0.1F - r.nextFloat() * 0.2F;
            float fx2 = 0.1F - r.nextFloat() * 0.2F;
            float fz2 = 0.1F - r.nextFloat() * 0.2F;
            float fy2 = 0.1F - r.nextFloat() * 0.2F;
            Direction facing = this.getFacing();
            IFXDispatcher.get().drawVentParticles(this.worldPosition.getX() + 0.5F + fx + facing.getStepX() / 2.0F, this.worldPosition.getY() + 0.5F + fy,
                    this.worldPosition.getZ() + 0.5F + fz + facing.getStepZ() / 2.0F, facing.getStepX() / 4.0F + fx2, fy2, facing.getStepZ() / 4.0F + fz2,
                    16777215);
        }
    }

    private void completeRecipe() {
        if (this.currentRecipe != null && this.currentCraft < this.recipeHash.size() && this.currentRecipe.matches(this.essentia, this.getStackInSlot(0))) {
            ItemStack catalyst = this.decrStackSize(0, 1);
            this.essentia = new AspectList();
            ItemStack dropped = this.getCurrentOutputRecipe();
            Player p = this.level.getServer() == null ? null : this.level.getServer().getPlayerList().getPlayerByName(this.recipePlayer.get(this.currentCraft));
            if (p != null) {
                EventHooks.firePlayerCraftingEvent(p, dropped, new SimpleContainer(catalyst));
            }
            InventoryUtils.ejectStackAt(this.level, this.worldPosition, this.getFacing(), dropped);
            this.level.playSound(null, this.worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25F,
                    2.6F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.8F);
            this.currentCraft = -1;
            this.syncTile(false);
            this.setChanged();
        }
    }

    void fill() {
        Direction facing = this.getFacing();
        for (int y = 0; y <= 1; y++) {
            for (Direction dir : Direction.values()) {
                if (dir != facing && dir != Direction.DOWN && (y != 0 || dir != Direction.UP)) {
                    BlockEntity te = ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition.above(y), dir);
                    if (te != null) {
                        IEssentiaTransport ic = (IEssentiaTransport) te;
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

    /** В оригинале currentRecipe разыменовывался до проверки на null (эссенция из трубы без рецепта — сбой) — DEVIATIONS 33. */
    @Override
    public int addToContainer(Aspect tt, int am) {
        if (this.currentRecipe == null) {
            return am;
        }
        int ce = this.currentRecipe.getAspects().getAmount(tt) - this.essentia.getAmount(tt);
        if (ce > 0) {
            int add = Math.min(ce, am);
            this.essentia.add(tt, add);
            this.syncTile(false);
            this.setChanged();
            return am - add;
        }
        return am;
    }

    @Override
    public boolean takeFromContainer(Aspect tt, int am) {
        if (this.essentia.getAmount(tt) >= am) {
            this.essentia.remove(tt, am);
            this.syncTile(false);
            this.setChanged();
            return true;
        }
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
        return this.essentia.getAmount(tt) >= am;
    }

    @Override
    public int containerContains(Aspect tt) {
        return this.essentia.getAmount(tt);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i >= 0) {
            if (this.level != null && this.level.isClientSide) {
                this.venting = 7;
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face != this.getFacing();
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != this.getFacing();
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
        return this.essentia;
    }

    @Override
    public void setAspects(AspectList aspects) {
        this.essentia = aspects;
    }

    /** Сигнал у самого блока, под ним или над ним. */
    @Override
    public boolean gettingPower() {
        return this.level.hasNeighborSignal(this.worldPosition) || this.level.hasNeighborSignal(this.worldPosition.below())
                || this.level.hasNeighborSignal(this.worldPosition.above());
    }

    /** Мнемонические матрицы, смотрящие на тауматориум (или его верх), — по +2 рецепта. */
    public void getUpgrades() {
        Direction facing = this.getFacing();
        int mr = 1;
        for (int yy = 0; yy <= 1; yy++) {
            for (Direction dir : Direction.values()) {
                if (dir != Direction.DOWN && dir != facing) {
                    BlockPos bp = new BlockPos(this.worldPosition.getX() + dir.getStepX(), this.worldPosition.getY() + yy + dir.getStepY(),
                            this.worldPosition.getZ() + dir.getStepZ());
                    BlockState bs = this.level.getBlockState(bp);
                    if (bs.is(BlocksTC.brainBox.get()) && bs.getValue(BlockStateProperties.FACING) == dir.getOpposite()) {
                        mr += 2;
                    }
                }
            }
        }
        if (mr != this.maxRecipes) {
            this.maxRecipes = mr;
            while (this.recipeHash.size() > this.maxRecipes) {
                this.recipeHash.remove(this.recipeHash.size() - 1);
                this.recipeEssentia.remove(this.recipeEssentia.size() - 1);
                this.recipePlayer.remove(this.recipePlayer.size() - 1);
            }
            this.syncTile(false);
            this.setChanged();
        }
    }
}
