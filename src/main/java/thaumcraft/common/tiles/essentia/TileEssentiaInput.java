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

/** Наполняющий переливатель эссенции (порт TileEssentiaInput): всасывает из трубы (128) и раскладывает по банкам в 16 блоках перед собой. */
public class TileEssentiaInput extends TileEssentiaTransport {

    public TileEssentiaInput(BlockPos pos, BlockState state) {
        super(ConfigBlocks.ESSENTIA_INPUT_TILE.get(), pos, state);
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face == this.getFacing().getOpposite();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        return 128;
    }

    /** fillJar оригинала. */
    @Override
    protected void transfer() {
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition, this.getFacing().getOpposite());
        if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport) te;
            if (!ic.canOutputTo(this.getFacing())) {
                return;
            }
            if (ic.getEssentiaAmount(this.getFacing()) > 0 && ic.getSuctionAmount(this.getFacing()) < this.getSuctionAmount(this.getFacing().getOpposite())
                    && this.getSuctionAmount(this.getFacing().getOpposite()) >= ic.getMinimumSuction()) {
                Aspect ta = ic.getEssentiaType(this.getFacing());
                if (EssentiaHandler.addEssentia(this, ta, this.getFacing(), 16, false, 5)) {
                    ic.takeEssentia(ta, 1, this.getFacing());
                }
            }
        }
    }
}
