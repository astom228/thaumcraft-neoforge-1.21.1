package thaumcraft.common.lib.research.theorycraft;

import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** Фокусный манипулятор рядом со столом: карточки ауромантии. */
public class AidBasicAuromancy implements ITheorycraftAid {
    @Override
    public Object getAidObject() {
        return BlocksTC.wandWorkbench.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends TheorycraftCard>[] getCards() {
        return new Class[] {CardFocus.class, CardAwareness.class, CardSpellbinding.class};
    }
}
