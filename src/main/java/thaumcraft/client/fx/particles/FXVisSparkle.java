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
 * Искра Вис (порт FXVisSparkle): летит к цели, разгоняясь, у цели гаснет; кадры 0–15 строки 8 сетки 64×64, полная яркость.
 */
public class FXVisSparkle extends Particle implements IEngineParticle {
    private static final Vector3f SCRATCH = new Vector3f();
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    float sizeMod;
    float particleScale = 0.0F;

    public FXVisSparkle(ClientLevel world, double x, double y, double z, double tx, double ty, double tz) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        this.lifetime = 1000;
        float f3 = 0.01F;
        this.xd = (float) this.random.nextGaussian() * f3;
        this.yd = (float) this.random.nextGaussian() * f3;
        this.zd = (float) this.random.nextGaussian() * f3;
        this.sizeMod = 45 + this.random.nextInt(15);
        this.rCol = 0.2F;
        this.gCol = 0.6F + this.random.nextFloat() * 0.3F;
        this.bCol = 0.2F;
        this.gravity = 0.2F;
    }

    public void setRBGColorF(float r, float g, float b) {
        this.setColor(r, g, b);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.985;
        this.yd *= 0.985;
        this.zd *= 0.985;
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double d13 = 0.1F;
        double d11 = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d11 < 2.0) {
            this.particleScale *= 0.95F;
        }
        if (d11 < 0.2) {
            this.lifetime = this.age;
        }
        if (this.age < 10) {
            this.particleScale = this.age / this.sizeMod;
        }
        dx /= d11;
        dy /= d11;
        dz /= d11;
        this.xd = Mth.clamp((float) (this.xd + dx * d13), -0.1F, 0.1F);
        this.yd = Mth.clamp((float) (this.yd + dy * d13), -0.1F, 0.1F);
        this.zd = Mth.clamp((float) (this.zd + dz * d13), -0.1F, 0.1F);
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float f) {
        float bob = Mth.sin(this.age / 3.0F) * 0.3F + 6.0F;
        int part = this.age % 16;
        float u0 = part / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = 0.125F;
        float v1 = v0 + 0.015625F;
        float s = 0.1F * this.particleScale * bob;
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(f, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(f, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(f, this.zo, this.z) - cam.z());
        Quaternionf q = camera.rotation();
        this.v(wr, q, px, py, pz, -s, -s, u1, v1);
        this.v(wr, q, px, py, pz, -s, s, u1, v0);
        this.v(wr, q, px, py, pz, s, s, u0, v0);
        this.v(wr, q, px, py, pz, s, -s, u0, v1);
    }

    private void v(VertexConsumer wr, Quaternionf q, float cx, float cy, float cz, float lx, float ly, float u, float v) {
        Vector3f p = SCRATCH.set(lx, ly, 0.0F).rotate(q);
        wr.addVertex(cx + p.x(), cy + p.y(), cz + p.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, 0.5F).setLight(240);
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
