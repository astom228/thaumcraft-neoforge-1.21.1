package thaumcraft.api.aspects;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.lib.events.EssentiaHandler;

/**
 * Взять эссенцию из ближайших источников (IAspectSource) вокруг блока. В оригинале вызов шёл через отражение,
 * чтобы API не зависел от мода; в порте API и мод — одна сборка, вызов прямой.
 */
public class AspectSourceHelper {

    /** @param direction сторона поиска (null — во все стороны), range — радиус */
    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range) {
        return EssentiaHandler.drainEssentia(tile, aspect, direction, range, 0);
    }

    public static boolean findEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range) {
        return EssentiaHandler.findEssentia(tile, aspect, direction, range, false);
    }
}
