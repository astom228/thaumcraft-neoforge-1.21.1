package thaumcraft.common.lib.research.theorycraft;

import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** Пресс големов рядом со столом: карточки големантии. */
public class AidBasicGolemancy implements ITheorycraftAid {
    @Override
    public Object getAidObject() {
        return BlocksTC.golemBuilder.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends TheorycraftCard>[] getCards() {
        return new Class[] {CardSculpting.class, CardScripting.class, CardSynergy.class};
    }
}
