package thaumcraft.common.lib.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Посевы (порт CropUtils): спелость и «растение ли» для ламп роста и големов. В 1.12 списки особых посевов хранились по
 * «ключу + metadata»; в порте — по блокам (стадии — у блоков 1.21 в состоянии). Списки заполняются при запуске.
 */
public final class CropUtils {
    /** Блоки, которые лампа роста не трогает (blacklistLamp). */
    public static final Set<Block> lampBlacklist = ConcurrentHashMap.newKeySet();

    private CropUtils() {
    }

    /**
     * Посевы, которые собирают щелчком, а не ломкой (clickableCrops оригинала; в TC — только через IMC): блок → спелое
     * состояние проверяет isGrownCrop.
     */
    public static final Set<Block> clickableCrops = ConcurrentHashMap.newKeySet();

    public static void blacklistLamp(Block block) {
        lampBlacklist.add(block);
    }

    /** TileLampGrowth.isPlant: IGrowable, кактус или растение, но не трава. */
    public static boolean isPlant(Level world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        boolean plant = b instanceof BonemealableBlock || b instanceof CactusBlock || b instanceof BushBlock || b instanceof SugarCaneBlock;
        return plant && !(b instanceof GrassBlock);
    }

    /** Спелый посев: удобрять уже нечего (кроме стеблей) или посев на последней стадии. */
    public static boolean isGrownCrop(Level world, BlockPos pos) {
        if (world.isEmptyBlock(pos)) {
            return false;
        }
        BlockState bs = world.getBlockState(pos);
        Block bi = bs.getBlock();
        if (bi instanceof CropBlock crop && crop.isMaxAge(bs)) {
            return true;
        }
        // ModConfig.postInitMisc: тыква и арбуз — «стандартные», адский нарост — спел на стадии 3, тростник и кактус — «стопкой»
        if (bi == net.minecraft.world.level.block.Blocks.MELON || bi == net.minecraft.world.level.block.Blocks.PUMPKIN
                || bi == net.minecraft.world.level.block.Blocks.NETHER_WART && bs.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) >= 3) {
            return true;
        }
        if ((bi instanceof SugarCaneBlock || bi instanceof CactusBlock) && world.getBlockState(pos.below()).is(bi)) {
            return true;
        }
        return bi instanceof BonemealableBlock g && !g.isValidBonemealTarget(world, pos, bs) && !(bi instanceof StemBlock);
    }

    public static boolean doesLampGrow(Level world, BlockPos pos) {
        return !lampBlacklist.contains(world.getBlockState(pos).getBlock());
    }
}
