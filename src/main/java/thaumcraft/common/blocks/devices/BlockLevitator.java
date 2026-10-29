package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.devices.TileLevitator;

/**
 * Левитатор (порт BlockLevitator): блок без 1/8 со стороны выхода; щелчок по ручке на рабочей стороне — следующая дальность
 * (в 1.12 — RayTracer codechicken с подблоком; здесь — точка попадания в границах ручки). Под сигналом выключен.
 */
public class BlockLevitator extends BlockTCDeviceFacing {
    public static final MapCodec<BlockLevitator> CODEC = simpleCodec(BlockLevitator::new);

    public BlockLevitator(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f = state.getValue(FACING);
        double g = 0.125;
        return Shapes.box(f.getStepX() > 0 ? g : 0.0, f.getStepY() > 0 ? g : 0.0, f.getStepZ() > 0 ? g : 0.0, 1.0 - (f.getStepX() < 0 ? g : 0.0),
                1.0 - (f.getStepY() < 0 ? g : 0.0), 1.0 - (f.getStepZ() < 0 ? g : 0.0));
    }

    /** getCuboidByFacing оригинала: ручка регулировки на стороне выхода. */
    public static AABB knob(Direction facing) {
        return switch (facing) {
            case DOWN -> new AABB(0.375, 0.875, 0.375, 0.625, 0.9375, 0.625);
            case EAST -> new AABB(0.0625, 0.375, 0.375, 0.125, 0.625, 0.625);
            case WEST -> new AABB(0.875, 0.375, 0.375, 0.9375, 0.625, 0.625);
            case SOUTH -> new AABB(0.375, 0.375, 0.0625, 0.625, 0.625, 0.125);
            case NORTH -> new AABB(0.375, 0.375, 0.875, 0.625, 0.625, 0.9375);
            default -> new AABB(0.375, 0.0625, 0.375, 0.625, 0.125, 0.625);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        if (knob(state.getValue(FACING)).inflate(0.02).contains(local) && world.getBlockEntity(pos) instanceof TileLevitator tile) {
            tile.increaseRange(player);
            world.playSound(null, pos, SoundsTC.key.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileLevitator(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ConfigBlocks.LEVITATOR_TILE.get(), TileLevitator::tick);
    }
}
