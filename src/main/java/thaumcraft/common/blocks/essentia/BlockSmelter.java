package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.container.ContainerSmelter;
import thaumcraft.common.tiles.essentia.TileSmelter;

/**
 * Плавильня эссенции (порт BlockSmelter): обычная, таумиевая и пустотная. Смотрит на игрока, горящая светится (13);
 * при разрушении вся эссенция внутри уходит в Выброс.
 */
public class BlockSmelter extends BaseEntityBlock {
    public static final MapCodec<BlockSmelter> CODEC = simpleCodec(BlockSmelter::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

    public BlockSmelter(Properties properties) {
        super(properties.lightLevel(s -> s.getValue(ENABLED) ? 13 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ENABLED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(ENABLED, false);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileSmelter(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ConfigBlocks.SMELTER_TILE.get(), TileSmelter::tick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof TileSmelter tile) {
            tile.checkNeighbours();
        }
    }

    /** openGui(…, 9, …) оригинала: без Shift — экран плавильни. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide && !player.isShiftKeyDown() && player instanceof ServerPlayer sp && world.getBlockEntity(pos) instanceof TileSmelter tile) {
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ContainerSmelter(id, inv, tile), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Container.calcRedstoneFromInventory. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof TileSmelter tile ? ItemHandlerHelper.calcRedstoneFromInventory(tile.getItemHandler()) : 0;
    }

    /** Включить/выключить (свечение и модель); BlockEntity при смене состояния того же блока сохраняется. */
    public static void setFurnaceState(Level world, BlockPos pos, boolean state) {
        BlockState bs = world.getBlockState(pos);
        if (bs.getBlock() instanceof BlockSmelter && bs.getValue(ENABLED) != state) {
            world.setBlock(pos, bs.setValue(ENABLED, state), 3);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof TileSmelter tile) {
                if (!world.isClientSide && tile.vis > 0) {
                    AuraHelper.polluteAura(world, pos, tile.vis, true);
                }
                tile.dropContents();
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public void animateTick(BlockState state, Level w, BlockPos pos, RandomSource r) {
        if (state.getValue(ENABLED)) {
            float f = pos.getX() + 0.5F;
            float f1 = pos.getY() + 0.2F + r.nextFloat() * 5.0F / 16.0F;
            float f2 = pos.getZ() + 0.5F;
            float f3 = 0.52F;
            float f4 = r.nextFloat() * 0.5F - 0.25F;
            switch (state.getValue(FACING)) {
                case WEST -> {
                    w.addParticle(ParticleTypes.SMOKE, f - f3, f1, f2 + f4, 0.0, 0.0, 0.0);
                    w.addParticle(ParticleTypes.FLAME, f - f3, f1, f2 + f4, 0.0, 0.0, 0.0);
                }
                case EAST -> {
                    w.addParticle(ParticleTypes.SMOKE, f + f3, f1, f2 + f4, 0.0, 0.0, 0.0);
                    w.addParticle(ParticleTypes.FLAME, f + f3, f1, f2 + f4, 0.0, 0.0, 0.0);
                }
                case NORTH -> {
                    w.addParticle(ParticleTypes.SMOKE, f + f4, f1, f2 - f3, 0.0, 0.0, 0.0);
                    w.addParticle(ParticleTypes.FLAME, f + f4, f1, f2 - f3, 0.0, 0.0, 0.0);
                }
                case SOUTH -> {
                    w.addParticle(ParticleTypes.SMOKE, f + f4, f1, f2 + f3, 0.0, 0.0, 0.0);
                    w.addParticle(ParticleTypes.FLAME, f + f4, f1, f2 + f3, 0.0, 0.0, 0.0);
                }
                default -> {
                }
            }
        }
    }
}
