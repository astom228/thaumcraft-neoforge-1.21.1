package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Магическая лампа (порт TileLampArcane): раз в 5 тиков, если нет сигнала, выбирает случайную клетку в радиусе ~15 (не выше
 * поверхности + 4, не ниже y 5); если там воздух, свет блоков меньше 11 и есть прямая видимость — ставит «огонёк» (effect_glimmer).
 * При выключении и сломе лампы огоньки в кубе 31×31×31 убираются. Только загруженные клетки.
 */
public class TileLampArcane extends TileThaumcraft {
    public TileLampArcane(BlockPos pos, BlockState state) {
        super(ConfigBlocks.LAMP_ARCANE_TILE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileLampArcane tile) {
        if (level.getGameTime() % 5L != 0L || tile.gettingPower()) {
            return;
        }
        int x = level.random.nextInt(16) - level.random.nextInt(16);
        int y = level.random.nextInt(16) - level.random.nextInt(16);
        int z = level.random.nextInt(16) - level.random.nextInt(16);
        BlockPos bp = pos.offset(x, y, z);
        if (!level.isLoaded(bp)) {
            return;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, bp);
        if (bp.getY() > top.getY() + 4) {
            bp = top.above(4);
        }
        if (bp.getY() < 5) {
            bp = new BlockPos(bp.getX(), 5, bp.getZ());
        }
        if (level.isEmptyBlock(bp) && !level.getBlockState(bp).is(BlocksTC.effectGlimmer.get()) && level.getBrightness(LightLayer.BLOCK, bp) < 11
                && hasLOS(level, pos, bp)) {
            level.setBlock(bp, BlocksTC.effectGlimmer.get().defaultBlockState(), 3);
        }
    }

    /** BlockUtils.hasLOS: луч от центра к центру не задевает блоков (или упирается в саму цель). */
    static boolean hasLOS(Level level, BlockPos source, BlockPos target) {
        Vec3 a = Vec3.atCenterOf(source);
        Vec3 b = Vec3.atCenterOf(target);
        BlockHitResult hit = level.clip(new ClipContext(a.add(b.subtract(a).normalize().scale(0.6)), b, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target);
    }

    public void removeLights() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        for (int x = -15; x <= 15; x++) {
            for (int y = -15; y <= 15; y++) {
                for (int z = -15; z <= 15; z++) {
                    BlockPos bp = this.worldPosition.offset(x, y, z);
                    if (this.level.isLoaded(bp) && this.level.getBlockState(bp).is(BlocksTC.effectGlimmer.get())) {
                        this.level.removeBlock(bp, false);
                    }
                }
            }
        }
    }
}
