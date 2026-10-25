package thaumcraft.common.blocks.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.tiles.crafting.TileCrucible;

/**
 * Тигель (порт BlockCrucible): стенки и ножки для столкновений, выделение — полный блок. Предметы, упавшие в кипящий тигель,
 * растворяются; живые существа внутри получают урон огнём. ПКМ: ведро воды — налить; предметом сверху — бросить один;
 * пустой рукой с Shift — вылить содержимое (Выброс в ауру). Компаратор — по количеству аспектов.
 */
public class BlockCrucible extends BaseEntityBlock {
    public static final MapCodec<BlockCrucible> CODEC = simpleCodec(BlockCrucible::new);
    private int delay = 0;
    protected static final VoxelShape AABB_LEGS = Block.box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);
    protected static final VoxelShape AABB_WALL_NORTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0);
    protected static final VoxelShape AABB_WALL_SOUTH = Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0);
    protected static final VoxelShape AABB_WALL_EAST = Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    protected static final VoxelShape AABB_WALL_WEST = Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0);
    private static final VoxelShape COLLISION = Shapes.or(AABB_LEGS, AABB_WALL_WEST, AABB_WALL_NORTH, AABB_WALL_EAST, AABB_WALL_SOUTH);

    public BlockCrucible(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileCrucible(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ConfigBlocks.CRUCIBLE_TILE.get(), TileCrucible::tick);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!world.isClientSide) {
            TileCrucible tile = world.getBlockEntity(pos) instanceof TileCrucible t ? t : null;
            if (tile != null && entity instanceof ItemEntity item && !(entity instanceof EntitySpecialItem) && tile.heat > 150
                    && tile.tank.getFluidAmount() > 0) {
                tile.attemptSmelt(item);
            } else {
                this.delay++;
                if (this.delay < 10) {
                    return;
                }
                this.delay = 0;
                if (entity instanceof LivingEntity && tile != null && tile.heat > 150 && tile.tank.getFluidAmount() > 0) {
                    entity.hurt(world.damageSources().inFire(), 1.0F);
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F,
                            2.0F + world.random.nextFloat() * 0.4F);
                }
            }
        }
        super.entityInside(state, world, pos, entity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileCrucible tile) {
            tile.spillRemnants();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (world.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        FluidStack fs = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!fs.isEmpty() && fs.is(Fluids.WATER) && fs.getAmount() >= 1000) {
            if (world.getBlockEntity(pos) instanceof TileCrucible tile && tile.tank.getFluidAmount() < tile.tank.getCapacity()) {
                if (FluidUtil.interactWithFluidHandler(player, hand, tile.tank)) {
                    player.inventoryMenu.broadcastChanges();
                    tile.setChanged();
                    world.sendBlockUpdated(pos, state, state, 3);
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33F,
                            1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.3F);
                }
                return ItemInteractionResult.SUCCESS;
            }
        } else if (!stack.isEmpty() && !player.isShiftKeyDown() && hit.getDirection() == Direction.UP && !(stack.getItem() instanceof thaumcraft.api.casters.ICaster)) {
            if (world.getBlockEntity(pos) instanceof TileCrucible tile) {
                ItemStack ti = stack.copyWithCount(1);
                if (tile.heat > 150 && tile.tank.getFluidAmount() > 0 && tile.attemptSmelt(ti, player) == null) {
                    // в оригинале уменьшался предмет в выбранном слоте панели, даже если бросали из второй руки (DEVIATIONS.md)
                    stack.shrink(1);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Пустой рукой с Shift — вылить содержимое. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof TileCrucible tile) {
            tile.spillRemnants();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof TileCrucible te) {
            float r = te.aspects.visSize() / 500.0F;
            return Mth.floor(r * 14.0F) + (te.aspects.visSize() > 0 ? 1 : 0);
        }
        return 0;
    }

    @Override
    public void animateTick(BlockState state, Level w, BlockPos pos, RandomSource r) {
        if (r.nextInt(10) == 0 && w.getBlockEntity(pos) instanceof TileCrucible te && te.tank.getFluidAmount() > 0 && te.heat > 150) {
            w.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.1F + r.nextFloat() * 0.1F,
                    1.2F + r.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
