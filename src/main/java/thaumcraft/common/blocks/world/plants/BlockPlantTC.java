package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Общая основа растений Thaumcraft (в оригинале — BlockBush). Форма BUSH_AABB из 1.12: 0.3..0.7 по X/Z, 0..0.6 по Y.
 * Почва: в 1.12 BlockBush.canBlockStay → soil.canSustainPlant: canSustainBush(soil) ИЛИ правило типа растения (EnumPlantType).
 */
public abstract class BlockPlantTC extends BushBlock {
    protected static final VoxelShape BUSH_AABB = Block.box(4.8, 0.0, 4.8, 11.2, 9.6, 11.2);

    protected BlockPlantTC(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends BushBlock> codec();

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BUSH_AABB;
    }

    /** Blocks.GRASS / Blocks.DIRT из 1.12 (у DIRT были варианты: обычная, каменистая земля, подзол). */
    protected static boolean isGrassOrDirt(BlockState soil) {
        return soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.COARSE_DIRT) || soil.is(Blocks.PODZOL);
    }
}
