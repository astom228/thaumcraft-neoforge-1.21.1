package thaumcraft.common.blocks.basic;

import net.minecraft.world.level.block.Block;

/**
 * Камни Thaumcraft (оригинал: прочность 2.0, сопротивление 10 → 6.0 в 1.21, звук камня).
 * isBeaconBase → тег minecraft:beacon_base_blocks; canEntityDestroy (неразрушимые) → теги wither_immune/dragon_immune.
 */
public class BlockStoneTC extends Block {
    public BlockStoneTC(Properties properties) {
        super(properties);
    }
}
