package thaumcraft.client.fx;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.client.fx.particles.FXGenericGui;
import thaumcraft.common.lib.fx.IFXDispatcher;

/** Создание эффектов (порт FXDispatcher; методы переносятся по мере надобности — тела и числа как в оригинале). */
public class FXDispatcher implements IFXDispatcher {
    public static final FXDispatcher INSTANCE = new FXDispatcher();

    public ClientLevel getWorld() {
        return Minecraft.getInstance().level;
    }

    /** Искра поверх интерфейса (слой 4). */
    public void drawSimpleSparkleGui(RandomSource rand, double x, double y, double x2, double y2, float scale, float r, float g, float b,
                                     int delay, float decay, float grav) {
        boolean sp = rand.nextFloat() < 0.2;
        FXGenericGui fb = new FXGenericGui(this.getWorld(), x, y, 0.0, x2, y2, 0.0);
        fb.setMaxAge(32 + this.getWorld().random.nextInt(8));
        fb.setRBGColorF(r, g, b);
        fb.setAlphaF(0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScale(scale, scale * 2.0F);
        fb.setNoClip(false);
        fb.setLayer(4);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(0.025F, 0.025F, 0.0F);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
    }

    public void drawWispyMotesOnBlock(BlockPos pp, int age, float grav) {
        this.drawWispyMotes(pp.getX() + this.getWorld().random.nextFloat(), pp.getY(), pp.getZ() + this.getWorld().random.nextFloat(),
                0.0, 0.0, 0.0, age,
                0.4F + this.getWorld().random.nextFloat() * 0.6F,
                0.6F + this.getWorld().random.nextFloat() * 0.4F,
                0.6F + this.getWorld().random.nextFloat() * 0.4F, grav);
    }

    public void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float grav) {
        this.drawWispyMotes(d, e, f, vx, vy, vz, age,
                0.25F + this.getWorld().random.nextFloat() * 0.75F,
                0.25F + this.getWorld().random.nextFloat() * 0.75F,
                0.25F + this.getWorld().random.nextFloat() * 0.75F, grav);
    }

    @Override
    public void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float r, float g, float b, float grav) {
        FXGeneric fb = new FXGeneric(this.getWorld(), d, e, f, vx, vy, vz);
        fb.setMaxAge((int) (age + age / 2 * this.getWorld().random.nextFloat()));
        fb.setRBGColorF(r, g, b);
        fb.setAlphaF(0.0F, 0.6F, 0.6F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(512, 16, 1);
        fb.setScale(1.0F, 0.5F);
        fb.setLoop(true);
        fb.setWind(0.001);
        fb.setGravity(grav);
        fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void drawNitorCore(double x, double y, double z, double x2, double y2, double z2) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.setMaxAge(10);
        fb.setRBGColorF(1.0F, 1.0F, 1.0F);
        fb.setAlphaF(1.0F);
        fb.setParticles(457, 1, 1);
        fb.setScale(1.0F, 1.0F + (float) this.getWorld().random.nextGaussian() * 0.1F, 1.0F);
        fb.setLayer(1);
        fb.setRandomMovementScale(2.0E-4F, 2.0E-4F, 2.0E-4F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void drawNitorFlames(double x, double y, double z, double x2, double y2, double z2, int color, int a) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.setMaxAge(10 + this.getWorld().random.nextInt(5));
        Color c = new Color(color);
        fb.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
        fb.setAlphaF(0.66F);
        fb.setLoop(true);
        fb.setGridSize(64);
        fb.setParticles(264, 8, 1);
        fb.setScale(3.0F + this.getWorld().random.nextFloat(), 0.05F);
        fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, a);
    }
}
