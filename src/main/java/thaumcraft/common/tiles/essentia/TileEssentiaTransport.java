package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.essentia.BlockEssentiaTransport;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Общая часть переливателей эссенции (в оригинале продублирована в TileEssentiaInput / TileEssentiaOutput):
 * подключение только с обратной стороны, раз в 5 тиков — обмен с трубой.
 */
public abstract class TileEssentiaTransport extends TileThaumcraft implements IEssentiaTransport {
    int count = 0;

    protected TileEssentiaTransport(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Direction getFacing() {
        return this.getBlockState().getValue(BlockEssentiaTransport.FACING);
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face == this.getFacing().getOpposite();
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public Aspect getSuctionType(Direction loc) {
        return null;
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
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return amount;
    }

    public void serverTick() {
        if (++this.count % 5 == 0) {
            this.transfer();
        }
    }

    protected abstract void transfer();
}
