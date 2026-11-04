package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.items.tools.ItemResonator;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.essentia.TileTube;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;
import thaumcraft.common.tiles.essentia.TileTubeFilter;
import thaumcraft.common.tiles.essentia.TileTubeValve;

/**
 * Трубка эссенции и её разновидности (порт BlockTube). Свойства north…down — подключения к соседям (в 1.12 вычислялись
 * в getActualState; в 1.21 хранятся в состоянии и пересчитываются при изменении соседей и сторон трубки).
 * ПКМ по клапану — открыть/закрыть; по фильтру предметом с эссенцией — задать фильтр, с Shift — снять.
 */
public class BlockTube extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    /** Порядок Direction.values(): down, up, north, south, west, east. */
    private static final BooleanProperty[] BY_DIR = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
    public static final MapCodec<BlockTube> CODEC = simpleCodec(p -> new BlockTube(TileTube::new, thaumcraft.common.config.ConfigBlocks.TUBE_TILE, p));

    private final BiFunction<BlockPos, BlockState, ? extends TileTube> factory;
    private final Supplier<? extends BlockEntityType<? extends TileTube>> type;

    public BlockTube(BiFunction<BlockPos, BlockState, ? extends TileTube> factory, Supplier<? extends BlockEntityType<? extends TileTube>> type,
                     Properties properties) {
        super(properties);
        this.factory = factory;
        this.type = type;
        BlockState s = this.stateDefinition.any();
        for (BooleanProperty p : BY_DIR) {
            s = s.setValue(p, false);
        }
        this.registerDefaultState(s);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        return t == this.type.get() ? (l, p, s, be) -> TileTube.tick(l, p, s, (TileTube) be) : null;
    }

    /** makeConnections оригинала: сторона открыта у трубки, и сосед готов соединиться. */
    private BlockState withConnections(BlockState state, BlockGetter world, BlockPos pos) {
        IEssentiaTransport tube = world.getBlockEntity(pos) instanceof IEssentiaTransport t ? t : null;
        for (Direction face : Direction.values()) {
            boolean c = (tube == null || tube.isConnectable(face)) && ThaumcraftApiHelper.getConnectableTile(world, pos, face) != null;
            state = state.setValue(BY_DIR[face.ordinal()], c);
        }
        return state;
    }

    /** Пересчитать подключения (после изменения сторон трубки кастером, поворота клапана). */
    public static void refreshConnections(Level world, BlockPos pos) {
        BlockState st = world.getBlockState(pos);
        if (st.getBlock() instanceof BlockTube bt) {
            BlockState ns = bt.withConnections(st, world, pos);
            if (ns != st) {
                world.setBlock(pos, ns, 3);
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.withConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return this.withConnections(state, world, pos);
    }

    /** onBlockPlacedBy: facing — по направлению взгляда игрока (EnumFacing.getDirectionFromEntityLiving). */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer != null && world.getBlockEntity(pos) instanceof TileTube tile) {
            tile.facing = Direction.orderedByNearest(placer)[0].getOpposite();
            tile.setChanged();
            refreshConnections(world, pos);
        }
        super.setPlacedBy(world, pos, state, placer, stack);
    }

    /** collisionRayTrace оригинала: точные кубоиды, включая закрытые, но доступные перчатке патрубки. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext context) {
        if (source.getBlockEntity(pos) instanceof TileTube tube) {
            var cuboids = new ArrayList<TileTube.IndexedAABB>();
            tube.addTraceableCuboids(cuboids);
            VoxelShape selection = Shapes.empty();
            for (var cuboid : cuboids) {
                selection = Shapes.or(selection, Shapes.create(cuboid.box().move(-pos.getX(), -pos.getY(), -pos.getZ())));
            }
            return selection;
        }
        return this.collisionShape(source, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext context) {
        return this.collisionShape(source, pos);
    }

    /** getBoundingBox оригинала: сердцевина 5/16..11/16 и подключаемые соседи. */
    private VoxelShape collisionShape(BlockGetter source, BlockPos pos) {
        double minx = 0.3125;
        double maxx = 0.6875;
        double miny = 0.3125;
        double maxy = 0.6875;
        double minz = 0.3125;
        double maxz = 0.6875;
        for (Direction fd : Direction.values()) {
            if (ThaumcraftApiHelper.getConnectableTile(source, pos, fd) != null) {
                switch (fd) {
                    case DOWN -> miny = 0.0;
                    case UP -> maxy = 1.0;
                    case NORTH -> minz = 0.0;
                    case SOUTH -> maxz = 1.0;
                    case WEST -> minx = 0.0;
                    case EAST -> maxx = 1.0;
                }
            }
        }
        return Shapes.box(minx, miny, minz, maxx, maxy, maxz);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof TileTubeBuffer b) {
            float r = b.aspects.visSize() / 10.0F;
            return Mth.floor(r * 14.0F) + (b.aspects.visSize() > 0 ? 1 : 0);
        }
        return 0;
    }

    /**
     * breakBlock: эссенция в трубке уходит в ауру Выбросом, на клиенте — шипение и пар. Как в оригинале, затем ещё раз
     * то же количество (BlockTCTile.breakBlock для любого IEssentiaTransport).
     */
    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof TileTube te && te.getEssentiaAmount(Direction.UP) > 0) {
            int ess = te.getEssentiaAmount(Direction.UP);
            if (!world.isClientSide) {
                AuraHelper.polluteAura(world, pos, ess, true);
                AuraHelper.polluteAura(world, pos, ess, true);
            } else {
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.1F,
                        1.0F + world.random.nextFloat() * 0.1F, false);
                for (int a = 0; a < 5; a++) {
                    IFXDispatcher.get().drawVentParticles(pos.getX() + 0.33 + world.random.nextFloat() * 0.33, pos.getY() + 0.33 + world.random.nextFloat() * 0.33,
                            pos.getZ() + 0.33 + world.random.nextFloat() * 0.33, 0.0, 0.0, 0.0, Aspect.FLUX.getColor());
                }
            }
        }
        super.onRemove(state, world, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        return this.activate(state, world, pos, player, stack) ? ItemInteractionResult.sidedSuccess(world.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return this.activate(state, world, pos, player, ItemStack.EMPTY) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
    }

    private boolean activate(BlockState state, Level world, BlockPos pos, Player player, ItemStack held) {
        if (state.is(BlocksTC.tubeValve.get())) {
            if (held.getItem() instanceof ItemResonator || held.getItem() instanceof thaumcraft.api.casters.ICaster || held.is(this.asItem())) {
                return false;
            }
            if (world.getBlockEntity(pos) instanceof TileTubeValve te) {
                te.allowFlow = !te.allowFlow;
                world.sendBlockUpdated(pos, state, state, 3);
                te.setChanged();
                if (!world.isClientSide) {
                    world.playSound(null, pos, SoundsTC.squeek.get(), SoundSource.BLOCKS, 0.7F, 0.9F + world.random.nextFloat() * 0.2F);
                }
                return true;
            }
        }
        if (state.is(BlocksTC.tubeFilter.get()) && world.getBlockEntity(pos) instanceof TileTubeFilter te) {
            if (player.isShiftKeyDown() && te.aspectFilter != null) {
                te.aspectFilter = null;
                world.sendBlockUpdated(pos, state, state, 3);
                te.setChanged();
                if (world.isClientSide) {
                    world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundsTC.key.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
                }
                return true;
            }
            if (te.aspectFilter == null && held.getItem() instanceof IEssentiaContainerItem ec) {
                if (ec.getAspects(held) != null) {
                    te.aspectFilter = ec.getAspects(held).getAspects()[0];
                    world.sendBlockUpdated(pos, state, state, 3);
                    te.setChanged();
                    if (world.isClientSide) {
                        world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundsTC.key.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
