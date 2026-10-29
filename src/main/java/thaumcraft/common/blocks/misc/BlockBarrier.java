package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.blocks.BlocksTC;

/**
 * Барьер (порт BlockBarrier): невидимый, без рамки; для мобов (не игроков) — сплошной куб, если барьерный камень под ним
 * (на 1 или 2 блока ниже) без сигнала редстоуна. Заменяем, не роняет предметов; исчезает без опоры (камня или барьера ниже).
 */
public class BlockBarrier extends Block {
    public BlockBarrier(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof LivingEntity && !(ecc.getEntity() instanceof Player)
                && level instanceof LevelReader reader) {
            int a = 1;
            if (!level.getBlockState(pos.below(a)).is(BlocksTC.pavingStoneBarrier.get())) {
                a++;
            }
            if (!(reader instanceof net.minecraft.world.level.Level lvl) || lvl.getBestNeighborSignal(pos.below(a)) == 0) {
                return Shapes.block();
            }
        }
        return Shapes.empty();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(BlocksTC.pavingStoneBarrier.get()) && !below.is(this)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
