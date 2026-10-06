package thaumcraft.api.research.theorycraft;

import net.minecraft.world.level.block.Blocks;

/** Книжная полка рядом со столом: «Баланс», 2× «Записи», 3× «Изучение». */
public class AidBookshelf implements ITheorycraftAid {
    @Override
    public Object getAidObject() {
        return Blocks.BOOKSHELF;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends TheorycraftCard>[] getCards() {
        return new Class[] {CardBalance.class, CardNotation.class, CardNotation.class, CardStudy.class, CardStudy.class, CardStudy.class};
    }
}
