package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Струйка пара (порт FXVent): быстро растёт и тает, кадры 1–5 строки 64-сетки particles.png. */
public class FXVent extends Particle implements IEngineParticle {
    /** Только главный поток отрисовки — без выделений на вершину. */
    private static final Vector3f SCRATCH = new Vector3f();
    float psm = 1.0F;
    float particleScale;

    public FXVent(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, int color) {
        super(world, x, y, z, vx, vy, vz);
        this.setSize(0.02F, 0.02F);
        this.particleScale = this.random.nextFloat() * 0.1F + 0.05F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        Color c = new Color(color);
        this.rCol = c.getRed() / 255.0F;
        this.bCol = c.getBlue() / 255.0F;
        this.gCol = c.getGreen() / 255.0F;
        this.setHeading(this.xd, this.yd, this.zd, 0.125F, 5.0F);
        // в 1.12 при дальности > 50 (25 в быстрой графике) ставился particleMaxAge = 0, но onUpdate его не проверял — без эффекта
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    public void setScale(float f) {
        this.particleScale *= f;
        this.psm *= f;
    }

    public void setAlphaF(float a) {
        this.alpha = a;
    }

    public void setHeading(double par1, double par3, double par5, float par7, float par8) {
        float f2 = Mth.sqrt((float) (par1 * par1 + par3 * par3 + par5 * par5));
        par1 /= f2;
        par3 /= f2;
        par5 /= f2;
        par1 += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075F * par8;
        par3 += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075F * par8;
        par5 += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075F * par8;
        this.xd = par1 * par7;
        this.yd = par3 * par7;
        this.zd = par5 * par7;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.particleScale >= this.psm) {
            this.remove();
        }
        this.yd += 0.0025;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.8500000190734863;
        this.yd *= 0.8500000190734863;
        this.zd *= 0.8500000190734863;
        if (this.particleScale < this.psm) {
            this.particleScale = (float) (this.particleScale * 1.15);
        }
        if (this.particleScale > this.psm) {
            this.particleScale = this.psm;
        }
        if (this.onGround) {
            this.xd *= 0.7F;
            this.zd *= 0.7F;
        }
    }

    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float f) {
        int part = (int) (1.0F + this.particleScale / this.psm * 4.0F);
        float u0 = part % 16 / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = part / 64 / 64.0F;
        float v1 = v0 + 0.015625F;
        float size = 0.3F * this.particleScale;
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(f, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(f, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(f, this.zo, this.z) - cam.z());
        int light = this.getLightColor(f);
        float a = this.alpha * ((this.psm - this.particleScale) / this.psm);
        Quaternionf q = camera.rotation();
        // вершины как в 1.12: (-1,-1) u1v1, (-1,1) u1v0, (1,1) u0v0, (1,-1) u0v1
        vertex(wr, q, px, py, pz, -size, -size, u1, v1, a, light);
        vertex(wr, q, px, py, pz, -size, size, u1, v0, a, light);
        vertex(wr, q, px, py, pz, size, size, u0, v0, a, light);
        vertex(wr, q, px, py, pz, size, -size, u0, v1, a, light);
    }

    private void vertex(VertexConsumer wr, Quaternionf q, float px, float py, float pz, float dx, float dy, float u, float v, float a, int light) {
        Vector3f p = SCRATCH.set(dx, dy, 0.0F).rotate(q);
        wr.addVertex(px + p.x(), py + p.y(), pz + p.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, a).setLight(light);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
