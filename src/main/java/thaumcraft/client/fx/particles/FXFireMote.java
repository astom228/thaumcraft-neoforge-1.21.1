package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Огонёк (порт FXFireMote): кадр 7 сетки 64×64 particles.png, вращается вокруг направления взгляда, уменьшается и гаснет
 * за 16 тиков (иногда стареет вдвое быстрее). Слой 0 или 1, полная яркость.
 */
public class FXFireMote extends Particle implements IEngineParticle {
    private static final Vector3f SCRATCH = new Vector3f();
    float baseScale;
    float baseAlpha = 1.0F;
    float particleScale;
    final int glowlayer;

    public FXFireMote(ClientLevel worldIn, double x, double y, double z, double vx, double vy, double vz, float r, float g, float b, float scale, int layer) {
        super(worldIn, x, y, z, 0.0, 0.0, 0.0);
        float colorR = r > 1.0F ? r / 255.0F : r;
        float colorG = g > 1.0F ? g / 255.0F : g;
        float colorB = b > 1.0F ? b / 255.0F : b;
        this.glowlayer = layer;
        this.setColor(colorR, colorG, colorB);
        this.lifetime = 16;
        this.particleScale = scale;
        this.baseScale = scale;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.roll = (float) (Math.PI * 2);
        this.oRoll = this.roll;
    }

    public void setAlphaF(float a) {
        this.alpha = a;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.random.nextInt(6) == 0) {
            this.age++;
        }
        if (this.age >= this.lifetime) {
            this.remove();
        }
        float lifespan = (float) this.age / this.lifetime;
        this.particleScale = this.baseScale - this.baseScale * lifespan;
        this.baseAlpha = 1.0F - lifespan;
        this.oRoll = this.roll++;
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float partialTicks) {
        float u0 = 7 / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = 0.0F;
        float v1 = v0 + 0.015625F;
        float f4 = 0.1F * this.particleScale;
        Vec3 cam = camera.getPosition();
        float f5 = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float f6 = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float f7 = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());
        Quaternionf q = new Quaternionf(camera.rotation()).rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        float a = this.alpha * this.baseAlpha;
        this.v(wr, q, f5, f6, f7, -f4, -f4, u1, v1, a);
        this.v(wr, q, f5, f6, f7, -f4, f4, u1, v0, a);
        this.v(wr, q, f5, f6, f7, f4, f4, u0, v0, a);
        this.v(wr, q, f5, f6, f7, f4, -f4, u0, v1, a);
    }

    private void v(VertexConsumer wr, Quaternionf q, float cx, float cy, float cz, float lx, float ly, float u, float v, float a) {
        Vector3f p = SCRATCH.set(lx, ly, 0.0F).rotate(q);
        wr.addVertex(cx + p.x(), cy + p.y(), cz + p.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, a).setLight(240);
    }

    @Override
    public int getFXLayer() {
        return this.glowlayer;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
