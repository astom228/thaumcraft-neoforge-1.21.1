package thaumcraft.api.golems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Черта голема (порт EnumGolemTrait): иконка и противоположная черта (неуклюжий/ловкий, тяжёлый/лёгкий, хрупкий/бронированный). */
public enum EnumGolemTrait {
    SMART, DEFT, CLUMSY, FIGHTER, WHEELED, FLYER, CLIMBER, HEAVY, LIGHT, FRAGILE, REPAIR, SCOUT, ARMORED, BRUTAL, FIREPROOF, BREAKER, HAULER, RANGED,
    BLASTPROOF;

    public final ResourceLocation icon;
    public EnumGolemTrait opposite;

    EnumGolemTrait() {
        this.icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_" + this.name().toLowerCase() + ".png");
    }

    public Component getLocalizedName() {
        return Component.translatable("golem.trait." + this.name().toLowerCase());
    }

    public Component getLocalizedDescription() {
        return Component.translatable("golem.trait.text." + this.name().toLowerCase());
    }

    static {
        CLUMSY.opposite = DEFT;
        DEFT.opposite = CLUMSY;
        HEAVY.opposite = LIGHT;
        LIGHT.opposite = HEAVY;
        FRAGILE.opposite = ARMORED;
        ARMORED.opposite = FRAGILE;
    }
}
