package thaumcraft.common.lib.research.theorycraft;

import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** Тигель рядом со столом: карточки алхимии. */
public class AidBasicAlchemy implements ITheorycraftAid {
    @Override
    public Object getAidObject() {
        return BlocksTC.crucible.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends TheorycraftCard>[] getCards() {
        return new Class[] {CardConcentrate.class, CardReactions.class, CardSynthesis.class};
    }
}
