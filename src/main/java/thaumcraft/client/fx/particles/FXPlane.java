package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Росчерк удара (порт FXPlane): плоский квадрат, летящий от одной точки к другой за life тиков, повёрнутый по направлению
 * полёта; кадры 22–30 строки 10 сетки 32×32 particles.png, слой 0 (аддитивно). Без отсечения граней — обе стороны.
 */
public class FXPlane extends Particle implements IEngineParticle {
    private static final Vector3f SCRATCH = new Vector3f();
    float angle;
    float angleYaw;
    float anglePitch;
    float particleAlpha;

    public FXPlane(ClientLevel world, double x, double y, double z, double m, double m1, double m2, int life) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.gravity = 0.0F;
        this.lifetime = life;
        this.setSize(0.01F, 0.01F);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.particleAlpha = 0.0F;
        double dx = m - this.x;
        double dy = m1 - this.y;
        double dz = m2 - this.z;
        this.xd = dx / this.lifetime;
        this.yd = dy / this.lifetime;
        this.zd = dz / this.lifetime;
        double d3 = Mth.sqrt((float) (dx * dx + dz * dz));
        this.angleYaw = 0.0F;
        this.anglePitch = 0.0F;
        if (d3 >= 1.0E-7) {
            this.angleYaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            this.anglePitch = (float) (-(Mth.atan2(dy, d3) * 180.0 / Math.PI));
        }
        this.angle = (float) (this.random.nextGaussian() * 20.0);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float threshold = this.lifetime / 5.0F;
        if (this.age <= threshold) {
            this.particleAlpha = this.age / threshold;
        } else {
            this.particleAlpha = (float) (this.lifetime - this.age) / this.lifetime;
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float f) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(f, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(f, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(f, this.zo, this.z) - cam.z());
        Quaternionf q = new Quaternionf(Axis.YP.rotationDegrees(-this.angleYaw + 90.0F)).mul(Axis.ZP.rotationDegrees(this.anglePitch + 90.0F))
                .mul(Axis.YP.rotationDegrees(this.angle));
        int tix = 22 + Math.round((this.age + f) / this.lifetime * 8.0F);
        float u0 = tix / 32.0F;
        float u1 = u0 + 0.03125F;
        float v0 = 10 / 32.0F;
        float v1 = v0 + 0.03125F;
        float s = 0.5F * (0.5F + (this.age + f) / this.lifetime);
        float a = this.particleAlpha / 2.0F;
        // лицевая сторона и обратная (в 1.12 — glDisable(GL_CULL_FACE))
        this.v(wr, q, px, py, pz, -s, s, u1, v1, a);
        this.v(wr, q, px, py, pz, s, s, u1, v0, a);
        this.v(wr, q, px, py, pz, s, -s, u0, v0, a);
        this.v(wr, q, px, py, pz, -s, -s, u0, v1, a);
        this.v(wr, q, px, py, pz, -s, -s, u0, v1, a);
        this.v(wr, q, px, py, pz, s, -s, u0, v0, a);
        this.v(wr, q, px, py, pz, s, s, u1, v0, a);
        this.v(wr, q, px, py, pz, -s, s, u1, v1, a);
    }

    private void v(VertexConsumer wr, Quaternionf q, float px, float py, float pz, float dx, float dy, float u, float v, float a) {
        Vector3f p = SCRATCH.set(dx, dy, 0.0F).rotate(q);
        wr.addVertex(px + p.x(), py + p.y(), pz + p.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, a).setLight(240);
    }

    @Override
    public int getFXLayer() {
        return 0;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
