package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Мехи (порт TileBellows): пока включены, раз в 2 тика ускоряют печь перед собой на 1 тик плавки (до последнего тика); на клиенте
 * мешок надувается и сдувается со вздохом. Плавильни эссенции и адская печь считают мехи вокруг себя (getBellows).
 */
public class TileBellows extends TileThaumcraft {
    public float inflation = 1.0F;
    private boolean direction = false;
    private boolean firstrun = true;
    public int delay = 0;

    public TileBellows(BlockPos pos, BlockState state) {
        super(ConfigBlocks.BELLOWS_TILE.get(), pos, state);
    }

    private boolean enabled() {
        return this.getBlockState().getValue(BlockTCDeviceFacing.ENABLED);
    }

    public Direction facing() {
        return this.getBlockState().getValue(BlockTCDeviceFacing.FACING);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileBellows tile) {
        if (!tile.enabled()) {
            return;
        }
        if (level.isClientSide) {
            if (tile.firstrun) {
                tile.inflation = 0.35F + level.random.nextFloat() * 0.55F;
            }
            tile.firstrun = false;
            if (tile.inflation > 0.35F && !tile.direction) {
                tile.inflation -= 0.075F;
            }
            if (tile.inflation <= 0.35F && !tile.direction) {
                tile.direction = true;
            }
            if (tile.inflation < 1.0F && tile.direction) {
                tile.inflation += 0.025F;
            }
            if (tile.inflation >= 1.0F && tile.direction) {
                tile.direction = false;
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS, 0.01F,
                        0.5F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F, false);
            }
        } else if (++tile.delay >= 2) {
            tile.delay = 0;
            BlockPos front = pos.relative(tile.facing());
            if (level.isLoaded(front) && level.getBlockEntity(front) instanceof AbstractFurnaceBlockEntity tf) {
                int ct = tf.cookingProgress;
                if (ct > 0 && ct < tf.cookingTotalTime - 1) {
                    tf.cookingProgress = ct + 1;
                }
            }
        }
    }

    /** Число включённых мехов, обращённых к pos, со сторон directions. */
    public static int getBellows(Level world, BlockPos pos, Direction[] directions) {
        int bellows = 0;
        for (Direction dir : directions) {
            BlockPos p = pos.relative(dir);
            if (!world.isLoaded(p)) {
                continue;
            }
            BlockEntity tile = world.getBlockEntity(p);
            if (tile instanceof TileBellows b && b.facing() == dir.getOpposite() && b.enabled()) {
                bellows++;
            }
        }
        return bellows;
    }
}
