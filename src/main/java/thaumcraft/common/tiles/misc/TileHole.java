package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.items.casters.foci.FocusEffectRift;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Дыра (порт TileHole, «Разлом»): на первом тике расширяется кольцом 3×3 в плоскости грани и продолжается вглубь, пока
 * count > 1; через countdownmax тиков возвращает прежний блок. На клиенте — искры по краям проёма.
 */
public class TileHole extends TileMemory {
    public short countdown = 0;
    public short countdownmax = 120;
    public byte count = 0;
    public Direction direction = null;

    public TileHole(BlockPos pos, BlockState state) {
        super(ConfigBlocks.HOLE_TILE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileHole tile) {
        tile.update();
    }

    void update() {
        if (this.level.isClientSide) {
            for (int a = 0; a < 2; a++) {
                this.surroundwithsparkles();
            }
            return;
        }
        if (this.countdown == 0 && this.count > 1 && this.direction != null) {
            BlockPos p = this.getBlockPos();
            switch (this.direction.getAxis()) {
                case Y -> {
                    for (int a = 0; a < 9; a++) {
                        if (a / 3 != 1 || a % 3 != 1) {
                            FocusEffectRift.createHole(this.level, p.offset(-1 + a / 3, 0, -1 + a % 3), null, (byte) 1, this.countdownmax);
                        }
                    }
                }
                case Z -> {
                    for (int a = 0; a < 9; a++) {
                        if (a / 3 != 1 || a % 3 != 1) {
                            FocusEffectRift.createHole(this.level, p.offset(-1 + a / 3, -1 + a % 3, 0), null, (byte) 1, this.countdownmax);
                        }
                    }
                }
                case X -> {
                    for (int a = 0; a < 9; a++) {
                        if (a / 3 != 1 || a % 3 != 1) {
                            FocusEffectRift.createHole(this.level, p.offset(0, -1 + a / 3, -1 + a % 3), null, (byte) 1, this.countdownmax);
                        }
                    }
                }
            }
            if (!FocusEffectRift.createHole(this.level, p.relative(this.direction.getOpposite()), this.direction, (byte) (this.count - 1), this.countdownmax)) {
                this.count = 0;
            }
        }
        this.countdown++;
        if (this.countdown % 20 == 0) {
            this.setChanged();
        }
        if (this.countdown >= this.countdownmax) {
            this.level.setBlock(this.getBlockPos(), this.oldblock, 3);
        }
    }

    private void surroundwithsparkles() {
        BlockPos pos = this.getBlockPos();
        for (Direction d1 : Direction.values()) {
            BlockState b1 = this.level.getBlockState(pos.relative(d1));
            if (b1.getBlock() != BlocksTC.hole.get() && !b1.isSolidRender(this.level, pos.relative(d1))) {
                for (Direction d2 : Direction.values()) {
                    if (d1.getAxis() != d2.getAxis() && (this.level.getBlockState(pos.relative(d2)).isSolidRender(this.level, pos.relative(d2))
                            || this.level.getBlockState(pos.relative(d1).relative(d2)).isSolidRender(this.level, pos.relative(d1).relative(d2)))) {
                        float sx = 0.5F * d1.getStepX();
                        float sy = 0.5F * d1.getStepY();
                        float sz = 0.5F * d1.getStepZ();
                        if (sx == 0.0F) {
                            sx = 0.5F * d2.getStepX();
                        }
                        if (sy == 0.0F) {
                            sy = 0.5F * d2.getStepY();
                        }
                        if (sz == 0.0F) {
                            sz = 0.5F * d2.getStepZ();
                        }
                        sx = sx == 0.0F ? this.level.random.nextFloat() : sx + 0.5F;
                        sy = sy == 0.0F ? this.level.random.nextFloat() : sy + 0.5F;
                        sz = sz == 0.0F ? this.level.random.nextFloat() : sz + 0.5F;
                        IFXDispatcher.get().sparkle(pos.getX() + sx, pos.getY() + sy, pos.getZ() + sz, 0.25F, 0.25F, 1.0F);
                    }
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.countdown = nbt.getShort("countdown");
        this.countdownmax = nbt.getShort("countdownmax");
        this.count = nbt.getByte("count");
        byte db = nbt.getByte("direction");
        this.direction = db >= 0 && db < 6 ? Direction.from3DDataValue(db) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putShort("countdown", this.countdown);
        nbt.putShort("countdownmax", this.countdownmax);
        nbt.putByte("count", this.count);
        nbt.putByte("direction", this.direction == null ? -1 : (byte) this.direction.get3DDataValue());
    }
}
