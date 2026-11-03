package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;

/** Порт AidPortal: портал рядом со столом — в Край, в Незер или малый портал культистов. */
public class AidPortal implements ITheorycraftAid {
    final Object portal;

    public AidPortal(Object o) {
        this.portal = o;
    }

    @Override
    public Object getAidObject() {
        return this.portal;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends TheorycraftCard>[] getCards() {
        return new Class[] {CardPortal.class};
    }

    public static class AidPortalEnd extends AidPortal {
        public AidPortalEnd() {
            super(Blocks.END_PORTAL);
        }
    }

    public static class AidPortalCrimson extends AidPortal {
        public AidPortalCrimson() {
            super(EntityCultistPortalLesser.class);
        }
    }

    public static class AidPortalNether extends AidPortal {
        public AidPortalNether() {
            super(Blocks.NETHER_PORTAL);
        }
    }
}
