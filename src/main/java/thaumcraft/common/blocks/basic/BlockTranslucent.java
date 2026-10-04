package thaumcraft.common.blocks.basic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HalfTransparentBlock;

/**
 * Янтарные блоки: полупрозрачные (слой translucent задан в модели), грани между одинаковыми блоками не рисуются,
 * добываются без инструмента, основа маяка.
 */
public class BlockTranslucent extends HalfTransparentBlock {
    public static final MapCodec<BlockTranslucent> CODEC = simpleCodec(BlockTranslucent::new);

    public BlockTranslucent(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HalfTransparentBlock> codec() {
        return CODEC;
    }
}
