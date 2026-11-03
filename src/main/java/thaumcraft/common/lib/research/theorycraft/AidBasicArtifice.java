package thaumcraft.common.lib.research.theorycraft;

import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** Матрица наполнения рядом со столом: карточки артефактики. */
public class AidBasicArtifice implements ITheorycraftAid {
    @Override
    public Object getAidObject() {
        return BlocksTC.infusionMatrix.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends TheorycraftCard>[] getCards() {
        return new Class[] {CardCalibrate.class, CardTinker.class, CardMindOverMatter.class};
    }
}
