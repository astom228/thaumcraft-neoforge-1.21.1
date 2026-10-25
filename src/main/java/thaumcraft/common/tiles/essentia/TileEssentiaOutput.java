package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.events.EssentiaHandler;

/** Опустошающий переливатель эссенции (порт TileEssentiaOutput): отдаёт в трубу аспект, который она всасывает, из банок в 16 блоках перед собой. */
public class TileEssentiaOutput extends TileEssentiaTransport {

    public TileEssentiaOutput(BlockPos pos, BlockState state) {
        super(ConfigBlocks.ESSENTIA_OUTPUT_TILE.get(), pos, state);
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return false;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face == this.getFacing().getOpposite();
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        return 0;
    }

    /** fillBuffer оригинала. */
    @Override
    protected void transfer() {
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition, this.getFacing().getOpposite());
        if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport) te;
            if (!ic.canInputFrom(this.getFacing())) {
                return;
            }
            if (ic.getSuctionAmount(this.getFacing()) > 0 && ic.getSuctionType(this.getFacing()) != null) {
                Aspect ta = ic.getSuctionType(this.getFacing());
                if (EssentiaHandler.drainEssentiaWithConfirmation(this, ta, this.getFacing(), 16, false, 5) && ic.addEssentia(ta, 1, this.getFacing()) > 0) {
                    EssentiaHandler.confirmDrain();
                }
            }
        }
    }
}
