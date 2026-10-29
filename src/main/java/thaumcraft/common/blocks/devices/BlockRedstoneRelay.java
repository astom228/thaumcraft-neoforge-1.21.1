package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.devices.TileRedstoneRelay;

/**
 * Редстоун-реле (порт BlockRedstoneRelay): диод с задержкой 2 тика — включается, когда сигнал сзади не меньше порога «вход»,
 * и выдаёт вперёд сигнал силы «выход». Щелчок по левой ручке — «выход» +1, по правой — «вход» +1 (по кругу 1–15).
 * В 1.12 логика повторителя была переписана в блоке; в 1.21 — общий DiodeBlock (те же правила). Ручки определяются по точке
 * попадания (в 1.12 — RayTracer codechicken с подблоками); их положение — по модели блока.
 */
public class BlockRedstoneRelay extends DiodeBlock implements EntityBlock {
    public static final MapCodec<BlockRedstoneRelay> CODEC = simpleCodec(BlockRedstoneRelay::new);

    public BlockRedstoneRelay(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends DiodeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction d = ctx.getHorizontalDirection();
        Player p = ctx.getPlayer();
        return this.defaultBlockState().setValue(FACING, p != null && p.isShiftKeyDown() ? d : d.getOpposite());
    }

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    @Override
    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        int pr = level.getBlockEntity(pos) instanceof TileRedstoneRelay t ? t.getIn() : 1;
        return this.getInputSignal(level, pos, state) >= pr;
    }

    @Override
    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof TileRedstoneRelay t ? t.getOut() : 0;
    }

    /** Номер ручки под точкой попадания: 0 — «выход», 1 — «вход», −1 — мимо. */
    public static int knobAt(BlockState state, BlockPos pos, Vec3 hit) {
        // модель (фасад на юг): ручка «выход» — x,z 2..6 px, «вход» — x 6..10, z 10..14; высота 2..4 px
        double x = hit.x - pos.getX() - 0.5;
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ() - 0.5;
        // повернуть точку обратно в систему модели (blockstate: y 90 — запад, 180 — север, 270 — восток)
        int k = switch (state.getValue(FACING)) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
        for (int i = 0; i < k; i++) {
            double t = x;
            x = z;
            z = -t;
        }
        double e = 0.02;
        if (y < 0.0625 - e || y > 0.25 + e) {
            return -1;
        }
        if (x >= -0.375 - e && x <= -0.125 + e && z >= -0.375 - e && z <= -0.125 + e) {
            return 0;
        }
        if (x >= -0.125 - e && x <= 0.125 + e && z >= 0.125 - e && z <= 0.375 + e) {
            return 1;
        }
        return -1;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.mayBuild()) {
            return InteractionResult.PASS;
        }
        int knob = knobAt(state, pos, hit.getLocation());
        if (knob < 0 || !(level.getBlockEntity(pos) instanceof TileRedstoneRelay tile)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (knob == 0) {
                tile.increaseOut();
            } else {
                tile.increaseIn();
            }
            level.playSound(null, pos, SoundsTC.key.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            this.checkTickOnNeighbor(level, pos, state);
            this.updateNeighborsInFront(level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileRedstoneRelay(pos, state);
    }
}
