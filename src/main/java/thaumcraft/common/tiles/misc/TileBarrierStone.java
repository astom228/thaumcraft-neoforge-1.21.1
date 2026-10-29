package thaumcraft.common.tiles.misc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigBlocks;

/**
 * Барьерный камень (порт TileBarrierStone, только сервер): раз в 5 тиков отбрасывает назад летящих мобов в столбе 1×3 над собой
 * (если нет сигнала), раз в 100 тиков ставит барьеры в пустые клетки над собой (1 и 2 блока).
 */
public class TileBarrierStone extends BlockEntity {
    private int count = 0;

    public TileBarrierStone(BlockPos pos, BlockState state) {
        super(ConfigBlocks.BARRIER_STONE_TILE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileBarrierStone tile) {
        if (tile.count == 0) {
            tile.count = level.random.nextInt(100);
        }
        if (tile.count % 5 == 0 && level.getBestNeighborSignal(pos) <= 0) {
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1).inflate(0.1));
            for (LivingEntity e : targets) {
                if (!e.onGround() && !(e instanceof Player)) {
                    e.push(-Mth.sin((e.getYRot() + 180.0F) * (float) Math.PI / 180.0F) * 0.2F, -0.1,
                            Mth.cos((e.getYRot() + 180.0F) * (float) Math.PI / 180.0F) * 0.2F);
                }
            }
        }
        if (++tile.count % 100 == 0) {
            for (int up = 1; up <= 2; up++) {
                BlockPos p = pos.above(up);
                if (!level.getBlockState(p).is(BlocksTC.barrier.get()) && level.isEmptyBlock(p)) {
                    level.setBlock(p, BlocksTC.barrier.get().defaultBlockState(), 3);
                }
            }
        }
    }
}
