package thaumcraft.common.lib.fx;

/**
 * Эффекты, вызываемые из общего кода (блоки, BlockEntity). В 1.12 такие вызовы вырезались с сервера аннотацией @SideOnly;
 * в 1.21 общий код обращается к этому интерфейсу: на клиенте его реализует FXDispatcher, на сервере вызовы пустые.
 * Методы добавляются по мере переноса FXDispatcher.
 */
public interface IFXDispatcher {
    IFXDispatcher NONE = new IFXDispatcher() {
    };

    default void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float r, float g, float b, float grav) {
    }

    default void drawNitorCore(double x, double y, double z, double x2, double y2, double z2) {
    }

    default void drawNitorFlames(double x, double y, double z, double x2, double y2, double z2, int color, int a) {
    }

    default void blockRunes(double x, double y, double z, float r, float g, float b, int dur, float grav) {
    }

    default void scanHighlight(net.minecraft.core.BlockPos p) {
    }

    default void scanHighlight(net.minecraft.world.entity.Entity e) {
    }

    /** Текущая реализация (на клиенте подставляется в ThaumcraftClient). */
    static IFXDispatcher get() {
        return Holder.instance;
    }

    static void set(IFXDispatcher dispatcher) {
        Holder.instance = dispatcher;
    }

    final class Holder {
        private static IFXDispatcher instance = NONE;
    }
}
