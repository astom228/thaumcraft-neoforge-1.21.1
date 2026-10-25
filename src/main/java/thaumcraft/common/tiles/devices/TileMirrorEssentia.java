package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.blocks.devices.BlockMirror;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.events.EssentiaHandler;

/**
 * Зеркало эссенции (порт TileMirrorEssentia): источник эссенции для устройств — по единице берёт и кладёт её в банки
 * в 8 блоках перед парным зеркалом. Нестабильность свыше 64 — Выброс в ауру.
 */
public class TileMirrorEssentia extends TileMirrorLinked implements IAspectSource {
    public Direction linkedFacing = Direction.DOWN;

    public TileMirrorEssentia(BlockPos pos, BlockState state) {
        super(ConfigBlocks.MIRROR_ESSENTIA_TILE.get(), pos, state);
    }

    @Override
    protected int instabilityLimit() {
        return 64;
    }

    @Override
    protected boolean isPartner(BlockEntity te) {
        return te instanceof TileMirrorEssentia;
    }

    @Override
    protected void onLinkRestored(ServerLevel targetWorld) {
        this.linkedFacing = facingOf(targetWorld.getBlockState(this.linkPos));
    }

    @Override
    protected void onUnlinked() {
        this.linkedFacing = Direction.DOWN;
    }

    private static Direction facingOf(BlockState state) {
        return state.hasProperty(BlockMirror.FACING) ? state.getValue(BlockMirror.FACING) : Direction.DOWN;
    }

    /** Парное зеркало (если загружено), заодно уточняет его сторону. */
    private BlockEntity partner() {
        if (!this.linked) {
            return null;
        }
        BlockEntity te = this.targetTile(true);
        if (te != null && this.linkedFacing == Direction.DOWN) {
            this.linkedFacing = facingOf(te.getBlockState());
        }
        return te;
    }

    @Override
    public AspectList getAspects() {
        return null;
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        BlockEntity te = this.partner();
        return te == null || EssentiaHandler.canAcceptEssentia(te, tag, this.linkedFacing, 8, true);
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        if (this.isLinkValid(true) && amount <= 1) {
            BlockEntity te = this.partner();
            if (te != null) {
                boolean b = EssentiaHandler.addEssentia(te, tag, this.linkedFacing, 8, true, 5);
                if (b) {
                    this.addInstability(amount);
                }
                return b ? 0 : 1;
            }
        }
        return amount;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        if (this.isLinkValid(true) && amount <= 1) {
            BlockEntity te = this.partner();
            if (te != null) {
                boolean b = EssentiaHandler.drainEssentia(te, tag, this.linkedFacing, 8, true, 5);
                if (b) {
                    this.addInstability(amount);
                }
                return b;
            }
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        if (this.isLinkValid(true) && amount <= 1) {
            BlockEntity te = this.partner();
            return te != null && EssentiaHandler.findEssentia(te, tag, this.linkedFacing, 8, true);
        }
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
    public boolean isBlocked() {
        return false;
    }
}
