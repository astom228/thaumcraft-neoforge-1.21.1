package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorLinked;

/**
 * Магическое зеркало и зеркало эссенции (порт BlockMirror): тонкая пластина на твёрдой грани, смотрит от неё; без опоры
 * отваливается. Рисуется только рендером BlockEntity. Связанное зеркало выпадает со связью (компонент предмета), а парное
 * при этом теряет связь.
 */
public class BlockMirror extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape UP = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final VoxelShape NORTH = Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SOUTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0);
    private static final VoxelShape WEST = Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape EAST = Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0);
    private static final VoxelShape DOWN = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
    private final BiFunction<BlockPos, BlockState, ? extends TileMirrorLinked> factory;
    private final Supplier<? extends BlockEntityType<? extends TileMirrorLinked>> type;
    private final MapCodec<BlockMirror> codec;

    public BlockMirror(BiFunction<BlockPos, BlockState, ? extends TileMirrorLinked> factory,
                       Supplier<? extends BlockEntityType<? extends TileMirrorLinked>> type, Properties properties) {
        super(properties);
        this.factory = factory;
        this.type = type;
        this.codec = simpleCodec(p -> new BlockMirror(factory, type, p));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** canPlaceBlockOnSide: опора — твёрдая грань соседа. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState st = this.defaultBlockState().setValue(FACING, ctx.getClickedFace());
        return st.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? st : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction d = state.getValue(FACING);
        BlockPos support = pos.relative(d.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, d);
    }

    /** neighborChanged оригинала: без опоры зеркало выпадает предметом. */
    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != this.type.get() ? null : (l, p, s, te) -> ((TileMirrorLinked) te).serverTick();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> DOWN;
        };
    }

    /** onBlockActivated оригинала всегда поглощал клик. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    /** Брошенный предмет, коснувшийся магического зеркала, уходит в парное (кроме только что выпавших из зеркала). */
    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!world.isClientSide && entity instanceof ItemEntity ie && ie.isAlive() && !ie.isOnPortalCooldown()
                && world.getBlockEntity(pos) instanceof TileMirror taf) {
            taf.transport(ie);
        }
        super.entityInside(state, world, pos, entity);
    }

    /** dropMirror: снятое связанное зеркало разрывает связь у парного. */
    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide && world.getBlockEntity(pos) instanceof TileMirrorLinked tm && tm.linked) {
            tm.invalidateLink();
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
