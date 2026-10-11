package thaumcraft.common.blocks.world.ore;

import thaumcraft.api.aspects.Aspect;

/** Виды вис-кристаллов (порт ShardType; номер — бывшая metadata, порядок слотов кристаллов в магическом верстаке). */
public enum ShardType {
    AIR(0, "air", Aspect.AIR),
    FIRE(1, "fire", Aspect.FIRE),
    WATER(2, "water", Aspect.WATER),
    EARTH(3, "earth", Aspect.EARTH),
    ORDER(4, "order", Aspect.ORDER),
    ENTROPY(5, "entropy", Aspect.ENTROPY),
    FLUX(6, "flux", Aspect.FLUX);

    private final int metadata;
    private final String name;
    private final Aspect aspect;

    ShardType(int metadata, String unlocalizedName, Aspect aspect) {
        this.metadata = metadata;
        this.name = unlocalizedName;
        this.aspect = aspect;
    }

    public int getMetadata() {
        return this.metadata;
    }

    public Aspect getAspect() {
        return this.aspect;
    }

    public String getUnlocalizedName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.getUnlocalizedName();
    }

    public static int getMetaByAspect(Aspect a) {
        for (ShardType st : values()) {
            if (st.getAspect() == a) {
                return st.metadata;
            }
        }
        return -1;
    }
}
