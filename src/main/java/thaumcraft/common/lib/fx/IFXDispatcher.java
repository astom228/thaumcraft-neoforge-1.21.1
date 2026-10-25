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

    default void researchTableSparkle(net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource rand) {
    }

    default void scanHighlight(net.minecraft.core.BlockPos p) {
    }

    default void scanHighlight(net.minecraft.world.entity.Entity e) {
    }

    default void drawSimpleSparkle(net.minecraft.util.RandomSource rand, double x, double y, double z, double x2, double y2, double z2, float scale,
                                   float r, float g, float b, int delay, float decay, float grav, int baseAge) {
    }

    default void drawBlockSparkles(net.minecraft.core.BlockPos p, net.minecraft.world.phys.Vec3 start) {
    }

    default void drawBamf(double x, double y, double z, int color, boolean sound, boolean flair, net.minecraft.core.Direction side) {
    }

    default void drawBamf(double x, double y, double z, boolean sound, boolean flair, net.minecraft.core.Direction side) {
    }

    default void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
    }

    default void crucibleBoil(net.minecraft.core.BlockPos pos, float fluidHeight, thaumcraft.api.aspects.AspectList aspects, int j) {
    }

    default void crucibleFroth(float x, float y, float z) {
    }

    default void crucibleFrothDown(float x, float y, float z) {
    }

    default void spark(double d, double e, double f, float size, float r, float g, float b, float a) {
    }

    default void jarSplashFx(double x, double y, double z) {
    }

    default void waterTrailFx(net.minecraft.core.BlockPos p1, net.minecraft.core.BlockPos p2, int count, int color, float scale) {
    }

    default void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color) {
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
