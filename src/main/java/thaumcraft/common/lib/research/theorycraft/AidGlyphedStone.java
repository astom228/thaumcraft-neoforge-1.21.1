package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** Камень с глифами рядом со столом. */
public class AidGlyphedStone implements ITheorycraftAid {
    @Override
    public Object getAidObject() {
        return new ItemStack(BlocksTC.stoneAncientGlyphed.get());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends TheorycraftCard>[] getCards() {
        return new Class[] {CardGlyphs.class};
    }
}
