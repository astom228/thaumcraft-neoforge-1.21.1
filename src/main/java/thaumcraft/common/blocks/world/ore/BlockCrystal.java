package thaumcraft.common.blocks.world.ore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Вис-кристалл аспекта (порт BlockCrystal). Растёт и распространяется, регулируя ауру: забирает Вис (флюкс у кристалла
 * порчи) при избытке, отдаёт обратно при недостатке. Дроп — (рост + 1) вис-кристаллов, шёлковое касание не действует.
 * В 1.12 стороны крепления к камню вычислялись в «расширенном состоянии» при отрисовке; в 1.21 у блока без BlockEntity
 * такого механизма нет, поэтому они хранятся свойствами блока (UP…WEST) и обновляются при изменении соседей.
 * Material.ROCK 1.12 → тег minecraft:mineable/pickaxe (камень, руды, кирпичи…) + твёрдая грань.
 */
public class BlockCrystal extends Block {
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 3);
    public static final IntegerProperty GENERATION = IntegerProperty.create("gen", 1, 4);
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public final Aspect aspect;

    public BlockCrystal(Aspect aspect, Properties properties) {
        super(properties);
        this.aspect = aspect;
        this.registerDefaultState(this.stateDefinition.any().setValue(SIZE, 0).setValue(GENERATION, 1)
                .setValue(UP, false).setValue(DOWN, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE, GENERATION, UP, DOWN, NORTH, EAST, SOUTH, WEST);
    }

    public int getGrowth(BlockState state) {
        return state.getValue(SIZE);
    }

    public int getGeneration(BlockState state) {
        return state.getValue(GENERATION);
    }

    // --- крепление к камню ---------------------------------------------------------------------------------------------

    private static boolean isRock(BlockState bs) {
        return bs.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    /** drawAt оригинала: соседний блок — камень, и его грань в сторону кристалла твёрдая. */
    private static boolean drawAt(BlockGetter world, BlockPos pos, Direction side) {
        BlockPos np = pos.relative(side);
        BlockState fbs = world.getBlockState(np);
        return isRock(fbs) && fbs.isFaceSturdy(world, np, side.getOpposite());
    }

    /** BlockUtils.isBlockTouching(world, pos, Material.ROCK, true). */
    public static boolean isTouchingRock(BlockGetter world, BlockPos pos) {
        for (Direction face : Direction.values()) {
            if (drawAt(world, pos, face)) {
                return true;
            }
        }
        return false;
    }

    public BlockState withConnections(BlockState state, BlockGetter world, BlockPos pos) {
        return state.setValue(UP, drawAt(world, pos, Direction.UP)).setValue(DOWN, drawAt(world, pos, Direction.DOWN))
                .setValue(NORTH, drawAt(world, pos, Direction.NORTH)).setValue(EAST, drawAt(world, pos, Direction.EAST))
                .setValue(SOUTH, drawAt(world, pos, Direction.SOUTH)).setValue(WEST, drawAt(world, pos, Direction.WEST));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.withConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isTouchingRock(level, pos);
    }

    /** neighborChanged оригинала: без опоры — выпадает; иначе обновляются стороны крепления. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!isTouchingRock(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return this.withConnections(state, level, pos);
    }

    // --- форма ---------------------------------------------------------------------------------------------------------

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext context) {
        int c = (s.getValue(UP) ? 1 : 0) + (s.getValue(DOWN) ? 1 : 0) + (s.getValue(EAST) ? 1 : 0) + (s.getValue(WEST) ? 1 : 0)
                + (s.getValue(SOUTH) ? 1 : 0) + (s.getValue(NORTH) ? 1 : 0);
        if (c > 1) {
            return Shapes.block();
        }
        if (s.getValue(UP)) {
            return Block.box(0, 8, 0, 16, 16, 16);
        }
        if (s.getValue(DOWN)) {
            return Block.box(0, 0, 0, 16, 8, 16);
        }
        if (s.getValue(EAST)) {
            return Block.box(8, 0, 0, 16, 16, 16);
        }
        if (s.getValue(WEST)) {
            return Block.box(0, 0, 0, 8, 16, 16);
        }
        if (s.getValue(SOUTH)) {
            return Block.box(0, 0, 8, 16, 16, 16);
        }
        if (s.getValue(NORTH)) {
            return Block.box(0, 0, 0, 16, 16, 8);
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // --- дроп ----------------------------------------------------------------------------------------------------------

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> ret = new ArrayList<>();
        int count = this.getGrowth(state) + 1;
        for (int i = 0; i < count; i++) {
            ret.add(ThaumcraftApiHelper.makeCrystal(this.aspect));
        }
        return ret;
    }

    // --- рост и регулирование ауры (updateTick оригинала) ----------------------------------------------------------------

    private static boolean isBlockTouching(Level world, BlockPos pos, Block block) {
        for (Direction face : Direction.values()) {
            if (world.getBlockState(pos.relative(face)).is(block)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(3 + this.getGeneration(state)) != 0) {
            return;
        }
        int threshold = 10;
        int growth = this.getGrowth(state);
        int generation = this.getGeneration(state);
        if (this.aspect != Aspect.FLUX) {
            if (AuraHelper.getVis(worldIn, pos) <= threshold) {
                if (growth > 0) {
                    worldIn.setBlockAndUpdate(pos, state.setValue(SIZE, growth - 1));
                    AuraHelper.addVis(worldIn, pos, threshold);
                } else if (isBlockTouching(worldIn, pos, this)) {
                    worldIn.removeBlock(pos, false);
                    AuraHandler.addVis(worldIn, pos, threshold);
                }
            } else if (AuraHelper.getVis(worldIn, pos) > AuraHandler.getAuraBase(worldIn, pos) + threshold) {
                if (growth < 3 && growth < 5 - generation + pos.asLong() % 3L) {
                    if (AuraHelper.drainVis(worldIn, pos, threshold, false) > 0.0F) {
                        worldIn.setBlockAndUpdate(pos, state.setValue(SIZE, growth + 1));
                    }
                } else if (generation < 4) {
                    BlockPos p2 = spreadCrystal(worldIn, pos);
                    if (p2 != null && AuraHelper.drainVis(worldIn, pos, threshold, false) > 0.0F) {
                        if (rand.nextInt(6) == 0) {
                            generation--;
                        }
                        worldIn.setBlockAndUpdate(p2, this.withConnections(this.defaultBlockState().setValue(GENERATION, generation + 1), worldIn, p2));
                    }
                }
            }
        } else if (AuraHelper.getFlux(worldIn, pos) <= threshold) {
            if (growth > 0) {
                worldIn.setBlockAndUpdate(pos, state.setValue(SIZE, growth - 1));
                AuraHelper.polluteAura(worldIn, pos, threshold, false);
            } else if (isBlockTouching(worldIn, pos, this)) {
                worldIn.removeBlock(pos, false);
                AuraHelper.polluteAura(worldIn, pos, threshold, false);
            }
        } else if (AuraHelper.getFlux(worldIn, pos) > AuraHandler.getAuraBase(worldIn, pos) + threshold) {
            if (growth < 3 && growth < 5 - generation + pos.asLong() % 3L) {
                if (AuraHelper.drainFlux(worldIn, pos, threshold, false) > 0.0F) {
                    worldIn.setBlockAndUpdate(pos, state.setValue(SIZE, growth + 1));
                }
            } else if (generation < 4) {
                BlockPos p2 = spreadCrystal(worldIn, pos);
                if (p2 != null && AuraHelper.drainFlux(worldIn, pos, threshold, false) > 0.0F) {
                    if (rand.nextInt(6) == 0) {
                        generation--;
                    }
                    worldIn.setBlockAndUpdate(p2, this.withConnections(this.defaultBlockState().setValue(GENERATION, generation + 1), worldIn, p2));
                }
            }
        }
    }

    public static BlockPos spreadCrystal(Level world, BlockPos pos) {
        int xx = pos.getX() + world.random.nextInt(3) - 1;
        int yy = pos.getY() + world.random.nextInt(3) - 1;
        int zz = pos.getZ() + world.random.nextInt(3) - 1;
        BlockPos t = new BlockPos(xx, yy, zz);
        if (t.equals(pos)) {
            return null;
        }
        BlockState bs = world.getBlockState(t);
        return bs.getFluidState().isEmpty() && (bs.isAir() || bs.canBeReplaced()) && world.random.nextInt(16) == 0 && isTouchingRock(world, t)
                ? t : null;
    }
}
