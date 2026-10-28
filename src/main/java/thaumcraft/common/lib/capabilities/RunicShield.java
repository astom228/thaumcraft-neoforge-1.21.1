package thaumcraft.common.lib.capabilities;

/**
 * Состояние рунической защиты игрока (в оригинале — статические карты PlayerEvents по id сущности: runicInfo, lastMaxCharge,
 * lastCharge, nextCycle). Только на сервере, не сохраняется (как и в 1.12 — пересчитывается раз в секунду).
 */
public class RunicShield {
    /** runicInfo: сумма зарядов надетых вещей; 0 — защиты нет. */
    public int max;
    /** lastMaxCharge; −1 — нет записи. */
    public int lastMaxCharge = -1;
    /** lastCharge; −1 — нет записи. */
    public int lastCharge = -1;
    /** nextCycle: игровое время (тики), раньше которого заряд не восстанавливается. */
    public long nextCycle;
}
