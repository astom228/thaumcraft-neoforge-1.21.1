package thaumcraft.api.golems.parts;

import thaumcraft.api.golems.IGolemAPI;

/** Действие детали каждый тик (на обеих сторонах, как в оригинале). */
public interface IGenericFunction {
    void onUpdateTick(IGolemAPI golem);
}
