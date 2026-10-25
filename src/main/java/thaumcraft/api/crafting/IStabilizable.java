package thaumcraft.api.crafting;

/** Устройство, которому стабилизатор (этап 14) может добавлять стабильность (руническая матрица). */
public interface IStabilizable {
    void addStability(int multiples);

    EnumStability getStability();

    enum EnumStability {
        VERY_STABLE,
        STABLE,
        UNSTABLE,
        VERY_UNSTABLE
    }
}
