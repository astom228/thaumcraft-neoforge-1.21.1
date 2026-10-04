package thaumcraft.common.blocks.world.ore;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.DropExperienceBlock;

/**
 * Руды. Дроп — в таблицах добычи (янтарь 1–2 шт., кварц 1 шт., киноварь — сам блок; удача как в оригинале).
 * Опыт 1–4 за янтарь и кварц, если выпал не сам блок (в 1.21 при шёлковом касании опыт не даётся).
 */
public class BlockOreTC extends DropExperienceBlock {
    public BlockOreTC(IntProvider xpRange, Properties properties) {
        super(xpRange, properties);
    }
}
