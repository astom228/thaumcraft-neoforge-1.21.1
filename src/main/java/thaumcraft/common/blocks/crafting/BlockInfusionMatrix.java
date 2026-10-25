package thaumcraft.common.blocks.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

/** Руническая матрица (порт BlockInfusionMatrix). TODO этап 11.4: BlockEntity (TileInfusionMatrix), 11.5: рендер вместо модели. */
public class BlockInfusionMatrix extends Block {
    public static final MapCodec<BlockInfusionMatrix> CODEC = simpleCodec(BlockInfusionMatrix::new);

    public BlockInfusionMatrix(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
