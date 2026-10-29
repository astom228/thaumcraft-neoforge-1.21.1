package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.devices.TileInfernalFurnace;

/**
 * Адская печь (порт BlockInfernalFurnace): центр многоблока 3×3×3 (собирается солью мира из незерского кирпича, обсидиана,
 * лавы и железной решётки); большая модель рисует всю печь, остальные блоки — невидимые заполнители. Предметы в «лаве» тонут
 * и попадают внутрь, существа горят. При сломе любой части печь распадается обратно в блоки, центр — в лаву, окно — в решётку.
 */
public class BlockInfernalFurnace extends BaseEntityBlock {
    public static final MapCodec<BlockInfernalFurnace> CODEC = simpleCodec(BlockInfernalFurnace::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    /** Флаг разборки (только серверный поток): чтобы слом частей при распаде не запускал распад повторно. */
    private static boolean ignore = false;

    public BlockInfernalFurnace(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    public static boolean isIgnoring() {
        return ignore;
    }

    /** destroyFurnace оригинала: заполнители вокруг — обратно в кирпич и обсидиан, окно — решётка, центр — лава. */
    public static void destroyFurnace(Level world, BlockPos pos, BlockState state, BlockPos startpos) {
        if (ignore || world.isClientSide) {
            return;
        }
        ignore = true;
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    for (int c = -1; c <= 1; c++) {
                        BlockPos p = pos.offset(a, b, c);
                        if (p.equals(startpos)) {
                            continue;
                        }
                        BlockState bs = world.getBlockState(p);
                        if (bs.is(BlocksTC.placeholderNetherbrick.get())) {
                            world.setBlock(p, Blocks.NETHER_BRICKS.defaultBlockState(), 3);
                        }
                        if (bs.is(BlocksTC.placeholderObsidian.get())) {
                            world.setBlock(p, Blocks.OBSIDIAN.defaultBlockState(), 3);
                        }
                    }
                }
            }
            BlockPos window = pos.relative(state.getValue(FACING).getOpposite());
            if (world.isEmptyBlock(window)) {
                world.setBlock(window, Blocks.IRON_BARS.defaultBlockState(), 3);
            }
            world.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        } finally {
            ignore = false;
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TileInfernalFurnace tile) {
                tile.dropContents();
            }
            super.onRemove(state, level, pos, newState, moving);
            if (!newState.is(Blocks.LAVA)) {
                destroyFurnace(level, pos, state, pos);
            }
            return;
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        Vec3 m = entity.getDeltaMovement();
        double mx = m.x;
        double mz = m.z;
        if (entity.getX() < pos.getX() + 0.3F) {
            mx += 1.0E-4F;
        }
        if (entity.getX() > pos.getX() + 0.7F) {
            mx -= 1.0E-4F;
        }
        if (entity.getZ() < pos.getZ() + 0.3F) {
            mz += 1.0E-4F;
        }
        if (entity.getZ() > pos.getZ() + 0.7F) {
            mz -= 1.0E-4F;
        }
        entity.setDeltaMovement(mx, m.y, mz);
        if (!world.isClientSide && entity.tickCount % 10 == 0) {
            if (entity instanceof ItemEntity item) {
                item.setDeltaMovement(item.getDeltaMovement().x, 0.025F, item.getDeltaMovement().z);
                if (item.onGround() && world.getBlockEntity(pos) instanceof TileInfernalFurnace taf) {
                    ItemStack rest = taf.addItemsToInventory(item.getItem());
                    if (rest.isEmpty()) {
                        item.discard();
                    } else {
                        item.setItem(rest);
                    }
                }
            } else if (entity instanceof LivingEntity && !entity.fireImmune()) {
                entity.hurt(world.damageSources().lava(), 3.0F);
                entity.igniteForSeconds(10.0F);
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileInfernalFurnace(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ConfigBlocks.INFERNAL_FURNACE_TILE.get(), TileInfernalFurnace::tick);
    }
}
