package thaumcraft.common.blocks.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Свеча из сала (16 цветов). Прочность 0.1, звук ткани, свет 14 (0.9375), без коллизии, рамка 0.375..0.625 × 0.5.
 * Ставится только на твёрдую верхнюю грань и ломается, когда опора пропадает. Цвет — окраска модели (ColorHandler).
 * Стабилизирует наполнение (IInfusionStabiliser).
 */
public class BlockCandle extends Block implements thaumcraft.api.crafting.IInfusionStabiliser {
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 8.0, 10.0);
    public final DyeColor dye;

    public BlockCandle(DyeColor dye, Properties properties) {
        super(properties);
        this.dye = dye;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5F;
        double y = pos.getY() + 0.7F;
        double z = pos.getZ() + 0.5F;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    public boolean canStabaliseInfusion(Level world, BlockPos pos) {
        return true;
    }
}
