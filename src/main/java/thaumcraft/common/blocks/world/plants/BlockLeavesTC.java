package thaumcraft.common.blocks.world.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Листва (горючесть 60, огонь 30). Дроп (саженец 1/100 и 1/75, капля ртути с серебряного дерева) — в таблицах добычи.
 * Листва серебряного дерева на случайном тике добавляет 0.01 Вис в ауру, если Вис ниже базы
 * (в 1.12 листва тикала всегда; ванильная листва 1.21 тикает только при распаде — для серебряной включено всегда, если не постоянная).
 */
public class BlockLeavesTC extends LeavesBlock {
    public BlockLeavesTC(Properties properties) {
        super(properties);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return this == BlocksTC.leafSilverwood.get() ? !state.getValue(PERSISTENT) : super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this == BlocksTC.leafSilverwood.get() && !state.getValue(PERSISTENT)
                && AuraHandler.getVis(level, pos) < AuraHandler.getAuraBase(level, pos)) {
            AuraHandler.addVis(level, pos, 0.01F);
        }
        super.randomTick(state, level, pos, random);
    }
}
