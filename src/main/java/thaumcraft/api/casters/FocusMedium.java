package thaumcraft.api.casters;

/** Носитель фокуса (порт FocusMedium): получает траекторию, даёт цели. */
public abstract class FocusMedium extends FocusNode {
    @Override
    public EnumUnitType getType() {
        return EnumUnitType.MEDIUM;
    }

    @Override
    public final EnumSupplyType[] mustBeSupplied() {
        return this instanceof FocusMediumRoot ? null : new EnumSupplyType[] {EnumSupplyType.TRAJECTORY};
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] {EnumSupplyType.TARGET};
    }

    /** true — остаток пакета выполнит позже промежуточная сущность (снаряд, облако, мина). */
    public boolean hasIntermediary() {
        return false;
    }

    public boolean execute(Trajectory trajectory) {
        return true;
    }

    public boolean isExclusive() {
        return false;
    }
}
