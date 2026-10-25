package thaumcraft.common.lib.research.theorycraft;

import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** Мозг в банке рядом со столом: «Тёмный шёпот». */
public class AidBrainInAJar implements ITheorycraftAid {
    @Override
    public Object getAidObject() {
        return BlocksTC.jarBrain.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends TheorycraftCard>[] getCards() {
        return new Class[] {CardDarkWhispers.class};
    }
}
