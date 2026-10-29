package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Вис-батарея (порт BlockVisBattery): заряд 0–10 (он же яркость и сигнал компаратора). Без сигнала редстоуна набирает Вис,
 * пока аура выше 90% нормы, и отдаёт, когда ниже 75%; под сигналом разряжается в ауру по 1 Вис раз в 5 тиков.
 * Случайный и запланированный тики — одна и та же логика (updateTick 1.12).
 */
public class BlockVisBattery extends Block {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 10);

    public BlockVisBattery(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.update(state, level, pos, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.update(state, level, pos, random);
    }

    private void update(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        int charge = state.getValue(CHARGE);
        if (world.hasNeighborSignal(pos)) {
            if (charge > 0) {
                AuraHandler.addVis(world, pos, 1.0F);
                world.setBlockAndUpdate(pos, state.setValue(CHARGE, charge - 1));
                world.scheduleTick(pos, this, 5);
            }
        } else {
            float aura = AuraHandler.getVis(world, pos);
            int base = AuraHandler.getAuraBase(world, pos);
            if (charge < 10 && aura > base * 0.9 && aura > 1.0F) {
                AuraHandler.drainVis(world, pos, 1.0F, false);
                world.setBlockAndUpdate(pos, state.setValue(CHARGE, charge + 1));
                world.scheduleTick(pos, this, 100 + rand.nextInt(100));
            } else if (charge > 0 && aura < base * 0.75) {
                AuraHandler.addVis(world, pos, 1.0F);
                world.setBlockAndUpdate(pos, state.setValue(CHARGE, charge - 1));
                world.scheduleTick(pos, this, 20 + rand.nextInt(20));
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (world.hasNeighborSignal(pos)) {
            world.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return state.getValue(CHARGE);
    }
}
