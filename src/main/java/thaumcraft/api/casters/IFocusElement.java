package thaumcraft.api.casters;

/** Элемент фокуса (порт IFocusElement): узел или вложенный пакет. */
public interface IFocusElement {
    String getKey();

    String getResearch();

    EnumUnitType getType();

    enum EnumUnitType {
        EFFECT,
        MEDIUM,
        MOD,
        PACKAGE
    }
}
