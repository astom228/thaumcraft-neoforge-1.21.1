package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigPotions;

/**
 * Волокна порчи (порт BlockTaintFibre): плёнка на твёрдых гранях соседей (свойства сторон) и иногда нарост (GROWTH1–4,
 * выбирается по координатам). Нарост 3 светится (12), 2 и 4 — слабее (6); при разрушении нарост 3 даёт кристалл Выброса
 * (шанс растёт с удачей) и Выброс в ауру. Волокна без опоры и без наростов отмирают; вдали от семени — тоже.
 */
public class BlockTaintFibre extends Block implements ITaintBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty GROWTH1 = BooleanProperty.create("growth1");
    public static final BooleanProperty GROWTH2 = BooleanProperty.create("growth2");
    public static final BooleanProperty GROWTH3 = BooleanProperty.create("growth3");
    public static final BooleanProperty GROWTH4 = BooleanProperty.create("growth4");
    private static final VoxelShape AABB_UP = Shapes.box(0.0, 0.95F, 0.0, 1.0, 1.0, 1.0);
    private static final VoxelShape AABB_DOWN = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.05F, 1.0);
    private static final VoxelShape AABB_EAST = Shapes.box(0.95F, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final VoxelShape AABB_WEST = Shapes.box(0.0, 0.0, 0.0, 0.05F, 1.0, 1.0);
    private static final VoxelShape AABB_SOUTH = Shapes.box(0.0, 0.0, 0.95F, 1.0, 1.0, 1.0);
    private static final VoxelShape AABB_NORTH = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.05F);
    private static final VoxelShape G1 = Shapes.box(0.1F, 0.0, 0.1F, 0.9F, 0.4F, 0.9F);
    private static final VoxelShape G2 = Shapes.box(0.2F, 0.0, 0.2F, 0.8F, 1.0, 0.8F);
    private static final VoxelShape G3 = Shapes.box(0.25, 0.0, 0.25, 0.75, 0.3125, 0.75);
    private static final VoxelShape G4 = Shapes.box(0.1F, 0.3F, 0.1F, 0.9F, 1.0, 0.9F);

    public BlockTaintFibre(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false).setValue(GROWTH1, false).setValue(GROWTH2, false)
                .setValue(GROWTH3, false).setValue(GROWTH4, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, GROWTH1, GROWTH2, GROWTH3, GROWTH4);
    }

    /** Свет наростов (getLightValue оригинала). */
    public static int lightOf(BlockState state) {
        return state.getValue(GROWTH3) ? 12 : state.getValue(GROWTH2) || state.getValue(GROWTH4) ? 6 : 0;
    }

    @Override
    public void die(Level world, BlockPos pos, BlockState state) {
        world.removeBlock(pos, false);
    }

    private static boolean drawAt(BlockGetter world, BlockPos pos, Direction side) {
        BlockState b = world.getBlockState(pos);
        return b.getBlock() != BlocksTC.taintFibre.get() && b.getBlock() != BlocksTC.taintFeature.get() && b.isFaceSturdy(world, pos, side.getOpposite());
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

    /** getActualState оригинала: стороны — по твёрдым граням соседей, нарост — по координатам (детерминированно). */
    public BlockState withConnections(BlockState state, BlockGetter world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            state = state.setValue(prop(d), drawAt(world, pos.relative(d), d));
        }
        boolean d = state.getValue(DOWN);
        boolean u = state.getValue(UP);
        int growth = 0;
        int q = new java.util.Random(pos.asLong()).nextInt(50);
        if (d) {
            if (q < 4) {
                growth = 1;
            } else if (q == 4 || q == 5) {
                growth = 2;
            } else if (q == 6) {
                growth = 3;
            }
        }
        if (u && q > 47) {
            growth = 4;
        }
        return state.setValue(GROWTH1, growth == 1).setValue(GROWTH2, growth == 2).setValue(GROWTH3, growth == 3).setValue(GROWTH4, growth == 4);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.withConnections(this.defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        BlockState actual = this.withConnections(state, level, pos);
        if (actual != state) {
            level.setBlock(pos, actual, 2);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return this.withConnections(state, level, pos);
    }

    private static boolean hasGrowth(BlockState s) {
        return s.getValue(GROWTH1) || s.getValue(GROWTH2) || s.getValue(GROWTH3) || s.getValue(GROWTH4);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!hasGrowth(state) && isOnlyAdjacentToTaint(world, pos)) {
            this.die(world, pos, state);
        } else if (!TaintHelper.isNearTaintSeed(world, pos)) {
            this.die(world, pos, state);
        } else {
            TaintHelper.spreadFibres(world, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!world.isClientSide && !hasGrowth(state) && isOnlyAdjacentToTaint(world, pos)) {
            world.removeBlock(pos, false);
        }
    }

    /** У клетки нет ни одной твёрдой грани, кроме порчи. */
    public static boolean isOnlyAdjacentToTaint(Level world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos p = pos.relative(dir);
            BlockState s = world.getBlockState(p);
            if (!s.isAir() && !TaintHelper.isTaint(s) && s.isFaceSturdy(world, p, dir.getOpposite())) {
                return false;
            }
        }
        return true;
    }

    /** Порчи вокруг больше, чем пустоты и нетвёрдых граней. */
    public static boolean isHemmedByTaint(Level world, BlockPos pos) {
        int c = 0;
        for (Direction dir : Direction.values()) {
            BlockPos p = pos.relative(dir);
            BlockState block = world.getBlockState(p);
            if (TaintHelper.isTaint(block)) {
                c++;
            } else if (block.isAir()) {
                c--;
            } else if (block.getFluidState().isEmpty() && !block.isFaceSturdy(world, p, dir.getOpposite())) {
                c--;
            }
        }
        return c > 0;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide && entity instanceof LivingEntity le && !le.isInvertedHealAndHarm() && world.random.nextInt(750) == 0) {
            le.addEffect(new MobEffectInstance(ConfigPotions.FLUX_TAINT, 200, 0, false, true));
        }
        super.stepOn(world, pos, state, entity);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level world, BlockPos pos, int id, int param) {
        if (id == 1) {
            if (world.isClientSide) {
                world.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 0.1F, 0.9F + world.random.nextFloat() * 0.2F, false);
            }
            return true;
        }
        return super.triggerEvent(state, world, pos, id, param);
    }

    /** dropBlockAsItemWithChance оригинала: только нарост 3 — кристалл Выброса (если random(5) ≤ удача) и Выброс в ауру. */
    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, BlockEntity te, ItemStack tool) {
        super.playerDestroy(world, player, pos, state, te, tool);
        if (!world.isClientSide && state.getValue(GROWTH3)) {
            int fortune = EnchantmentHelper.getItemEnchantmentLevel(world.registryAccess().holderOrThrow(Enchantments.FORTUNE), tool);
            if (world.random.nextInt(5) <= fortune) {
                Block.popResource(world, pos, ThaumcraftApiHelper.makeCrystal(Aspect.FLUX));
            }
            AuraHelper.polluteAura(world, pos, 1.0F, true);
        }
    }

    private VoxelShape plates(BlockState s) {
        VoxelShape v = Shapes.empty();
        if (s.getValue(UP)) {
            v = Shapes.or(v, AABB_UP);
        }
        if (s.getValue(DOWN)) {
            v = Shapes.or(v, AABB_DOWN);
        }
        if (s.getValue(EAST)) {
            v = Shapes.or(v, AABB_EAST);
        }
        if (s.getValue(WEST)) {
            v = Shapes.or(v, AABB_WEST);
        }
        if (s.getValue(SOUTH)) {
            v = Shapes.or(v, AABB_SOUTH);
        }
        if (s.getValue(NORTH)) {
            v = Shapes.or(v, AABB_NORTH);
        }
        return v;
    }

    /** Рамка выделения: нарост или плёнки (в 1.12 — плёнка под курсором, RayTracer codechicken). */
    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (s.getValue(GROWTH1)) {
            return G1;
        }
        if (s.getValue(GROWTH2)) {
            return G2;
        }
        if (s.getValue(GROWTH3)) {
            return G3;
        }
        if (s.getValue(GROWTH4)) {
            return G4;
        }
        VoxelShape p = this.plates(s);
        return p.isEmpty() ? Shapes.box(0.0, 0.0, 0.0, 1.0, 0.05F, 1.0) : p;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return this.plates(s);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return true;
    }
}
