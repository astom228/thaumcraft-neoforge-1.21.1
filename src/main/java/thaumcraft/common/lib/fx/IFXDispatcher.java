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

    /** Молния от (x, y, z) к (tx, ty, tz) (arcLightning оригинала). */
    default void arcLightning(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float h) {
    }

    /** Частицы наполнения: предмет-не-блок с пьедестала летит к матрице. */
    default void drawInfusionParticles1(double x, double y, double z, net.minecraft.core.BlockPos pos, net.minecraft.world.item.ItemStack stack) {
    }

    /** Частицы наполнения: блок с пьедестала летит к матрице. */
    default void drawInfusionParticles2(double x, double y, double z, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }

    /** Частицы наполнения: искры с пьедестала к матрице. */
    default void drawInfusionParticles3(double x, double y, double z, int x2, int y2, int z2) {
    }

    /** Частицы наполнения: опыт игрока к матрице. */
    default void drawInfusionParticles4(double x, double y, double z, int x2, int y2, int z2) {
    }

    /** Частицы эффектов фокусов (renderParticleFX: мороз, разрушение, исцеление, Выброс, проклятие, обмен, разлом). */
    default void focusFrostFX(double x, double y, double z, double mx, double my, double mz) {
    }

    default void focusBreakFX(double x, double y, double z, double mx, double my, double mz) {
    }

    default void focusHealFX(double x, double y, double z, double vx, double vy, double vz) {
    }

    default void focusFluxFX(double x, double y, double z, double vx, double vy, double vz) {
    }

    default void focusCurseFX(double x, double y, double z, double mx, double my, double mz) {
    }

    default void focusExchangeFX(double x, double y, double z, double vx, double vy, double vz) {
    }

    default void focusRiftFX(double x, double y, double z, double mx, double my, double mz) {
    }

    /** Искра Вис от (x, y, z) к (x2, y2, z2) цвета color (visSparkle). */
    default void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
    }

    /** Искорка (sparkle): в 2/3 случаев — мерцающая частица 320–335 цвета (r, g, b). */
    default void sparkle(float x, float y, float z, float r, float g, float b) {
    }

    /** Обобщённая частица по параметрам GenPart (drawGenericParticles). */
    default void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, GenPart part) {
    }

    /** Клуб облака фокуса цвета c (drawFocusCloudParticle). */
    default void drawFocusCloudParticle(double x, double y, double z, double mx, double my, double mz, int c) {
    }

    /** Огонёк (drawFireMote): в половине случаев втрое меньше и на слое 1. */
    default void drawFireMote(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
    }

    /** Разряд от (x, y, z) к (tx, ty, tz) (arcBolt, FXBolt). */
    default void arcBolt(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
    }

    /** Клуб Выброса в блоке p (drawPollutionParticles). */
    default void drawPollutionParticles(net.minecraft.core.BlockPos p) {
    }

    /** Росчерк удара от (x, y, z) к (x2, y2, z2) за dur тиков (drawSlash). */
    default void drawSlash(double x, double y, double z, double x2, double y2, double z2, int dur) {
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

    default void drawBamf(double x, double y, double z, float r, float g, float b, boolean sound, boolean flair, net.minecraft.core.Direction side) {
    }

    default void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
    }

    default void drawAlumentum(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
    }

    default void drawLevitatorParticles(double x, double y, double z, double x2, double y2, double z2) {
    }

    default void smokeSpiral(double x, double y, double z, float rad, int start, int miny, int color) {
    }

    default void drawBlockMistParticles(net.minecraft.core.BlockPos p, int c) {
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
