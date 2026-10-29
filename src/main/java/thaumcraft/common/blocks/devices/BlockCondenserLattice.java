package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.tiles.devices.TileCondenser;

import java.util.HashSet;
import java.util.Set;

/**
 * Решётка конденсатора (порт BlockCondenserLattice): соединяется с решётками и с конденсатором снизу. Решётка, не связанная
 * с конденсатором (или дальше √74 от него), выпадает предметом. Изменение решётки заставляет конденсатор пересчитать её.
 * Засорённую решётку чистит фильтр эссенции (в половине случаев — кристалл Выброса).
 */
public class BlockCondenserLattice extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    private final boolean dirty;

    public BlockCondenserLattice(boolean dirty, Properties props) {
        super(props);
        this.dirty = dirty;
        this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    private static BooleanProperty prop(Direction d) {
        return switch (d) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    private static boolean connects(BlockGetter world, BlockPos pos, Direction face) {
        Block b = world.getBlockState(pos.relative(face)).getBlock();
        return b instanceof BlockCondenserLattice || face == Direction.DOWN && b == BlocksTC.condenser.get();
    }

    /** getActualState оригинала: соединения по соседям. */
    private BlockState withConnections(BlockState state, BlockGetter world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            state = state.setValue(prop(d), connects(world, pos, d));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.withConnections(this.defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(prop(dir), connects(level, pos, dir));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.box(state.getValue(WEST) ? 0.0 : 0.3125, state.getValue(DOWN) ? 0.0 : 0.3125, state.getValue(NORTH) ? 0.0 : 0.3125,
                state.getValue(EAST) ? 1.0 : 0.6875, state.getValue(UP) ? 1.0 : 0.6875, state.getValue(SOUTH) ? 1.0 : 0.6875);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, world, pos, old, moving);
        if (!old.is(state.getBlock())) {
            triggerUpdate(world, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, world, pos, block, from, moving);
        if (block instanceof BlockCondenserLattice) {
            triggerUpdate(world, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, world, pos, newState, moving);
        if (!(newState.getBlock() instanceof BlockCondenserLattice)) {
            triggerUpdate(world, pos);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!this.dirty || !stack.is(ItemsTC.filter.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClientSide) {
            stack.shrink(1);
            Direction facing = hit.getDirection();
            if (world.random.nextBoolean()) {
                world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5F + facing.getStepX() / 3.0F, pos.getY() + 0.5F,
                        pos.getZ() + 0.5F + facing.getStepZ() / 3.0F, ThaumcraftApiHelper.makeCrystal(Aspect.FLUX)));
            }
            world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
                    ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 1.6F);
            world.setBlock(pos, this.withConnections(BlocksTC.condenserlattice.get().defaultBlockState(), world, pos), 3);
            triggerUpdate(world, pos);
        }
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    /**
     * Ищет по решёткам конденсатор (и сбрасывает ему подсчёт решётки). Не нашёлся или дальше √74 — решётка выпадает (сервер).
     * Обход — локальным множеством (в оригинале — поле блока, общее для обеих сторон).
     */
    public static void triggerUpdate(Level world, BlockPos pos) {
        BlockPos p = processUpdate(world, pos, new HashSet<>());
        if (!world.isClientSide && world.getBlockState(pos).getBlock() instanceof BlockCondenserLattice && (p == null || p.distSqr(pos) > 74.0)) {
            world.destroyBlock(pos, true);
        }
    }

    private static BlockPos processUpdate(Level world, BlockPos pos, Set<Long> history) {
        history.add(pos.asLong());
        for (Direction face : Direction.values()) {
            BlockPos p2 = pos.relative(face);
            if (!history.contains(p2.asLong()) && world.isLoaded(p2)) {
                Block b = world.getBlockState(p2).getBlock();
                if (b instanceof BlockCondenserLattice) {
                    BlockPos pp = processUpdate(world, p2, history);
                    if (pp != null) {
                        return pp;
                    }
                }
                if (face == Direction.DOWN && b == BlocksTC.condenser.get()) {
                    if (world.getBlockEntity(p2) instanceof TileCondenser te) {
                        te.latticeCount = -1.0F;
                    }
                    return p2;
                }
            }
        }
        return null;
    }
}
