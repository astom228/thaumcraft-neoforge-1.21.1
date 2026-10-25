package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraft;

/** Верх тауматориума (порт TileThaumatoriumTop): эссенция и инвентарь — через нижнюю часть. */
public class TileThaumatoriumTop extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {

    public TileThaumatoriumTop(BlockPos pos, BlockState state) {
        super(ConfigBlocks.THAUMATORIUM_TOP_TILE.get(), pos, state);
    }

    /** Нижняя часть (в оригинале кэшировалась в update; здесь — без кэша, чтобы не держать снятый блок). */
    public TileThaumatorium thaumatorium() {
        return this.level != null && this.level.getBlockEntity(this.worldPosition.below()) instanceof TileThaumatorium t ? t : null;
    }

    public IItemHandler getItemHandler() {
        TileThaumatorium t = this.thaumatorium();
        return t == null ? null : t.getItemHandler();
    }

    @Override
    public int addToContainer(Aspect tt, int am) {
        TileThaumatorium t = this.thaumatorium();
        return t == null ? am : t.addToContainer(tt, am);
    }

    @Override
    public boolean takeFromContainer(Aspect tt, int am) {
        TileThaumatorium t = this.thaumatorium();
        return t != null && t.takeFromContainer(tt, am);
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
        TileThaumatorium t = this.thaumatorium();
        return t != null && t.doesContainerContainAmount(tt, am);
    }

    @Override
    public int containerContains(Aspect tt) {
        TileThaumatorium t = this.thaumatorium();
        return t == null ? 0 : t.containerContains(tt);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public boolean isConnectable(Direction face) {
        TileThaumatorium t = this.thaumatorium();
        return t != null && t.isConnectable(face);
    }

    @Override
    public boolean canInputFrom(Direction face) {
        TileThaumatorium t = this.thaumatorium();
        return t != null && t.canInputFrom(face);
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        TileThaumatorium t = this.thaumatorium();
        if (t != null) {
            t.setSuction(aspect, amount);
        }
    }

    @Override
    public Aspect getSuctionType(Direction loc) {
        TileThaumatorium t = this.thaumatorium();
        return t == null ? null : t.getSuctionType(loc);
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        TileThaumatorium t = this.thaumatorium();
        return t == null ? 0 : t.getSuctionAmount(loc);
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
        TileThaumatorium t = this.thaumatorium();
        return t == null ? 0 : t.takeEssentia(aspect, amount, face);
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        TileThaumatorium t = this.thaumatorium();
        return t == null ? 0 : t.addEssentia(aspect, amount, face);
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public AspectList getAspects() {
        TileThaumatorium t = this.thaumatorium();
        return t == null ? null : t.essentia;
    }

    @Override
    public void setAspects(AspectList aspects) {
        TileThaumatorium t = this.thaumatorium();
        if (t != null) {
            t.setAspects(aspects);
        }
    }
}
