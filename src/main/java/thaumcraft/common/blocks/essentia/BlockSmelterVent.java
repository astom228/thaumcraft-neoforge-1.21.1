package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Отводящий клапан плавильни (порт BlockSmelterVent): ставится как насос; часть потерь при переплавке (с шансом 1/3 на
 * каждую единицу) уходит через него паром, а не в Выброс ауры. Половина блока у грани плавильни.
 */
public class BlockSmelterVent extends BlockSmelterAux {
    public static final MapCodec<BlockSmelterVent> CODEC = simpleCodec(BlockSmelterVent::new);
    private static final VoxelShape SOUTH = Block.box(2.0, 2.0, 8.0, 14.0, 14.0, 16.0);
    private static final VoxelShape WEST = Block.box(0.0, 2.0, 2.0, 8.0, 14.0, 14.0);
    private static final VoxelShape EAST = Block.box(8.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    private static final VoxelShape NORTH = Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 8.0);

    public BlockSmelterVent(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }
}
