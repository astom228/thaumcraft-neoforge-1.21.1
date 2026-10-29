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
 * Слизистый пузырь (порт FXSlimyBubble): кадры 144–150 сетки particles.png (16 столбцов по 1/64), вздувается, всплывает
 * и лопается за 15–19 тиков. Слой 1 движка Thaumcraft.
 */
public class FXSlimyBubble extends Particle implements IEngineParticle {
    private static final Vector3f SCRATCH = new Vector3f();
    int particle = 144;
    final float particleScale;

    public FXSlimyBubble(ClientLevel world, double x, double y, double z, float scale) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.gravity = 0.0F;
        this.xd = this.yd = this.zd = 0.0;
        this.particleScale = scale;
        this.lifetime = 15 + world.random.nextInt(5);
        this.setSize(0.01F, 0.01F);
    }

    public void setAlphaF(float a) {
        this.alpha = a;
    }

    public void setRBGColorF(float r, float g, float b) {
        this.setColor(r, g, b);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        if (this.age - 1 < 6) {
            this.particle = 144 + this.age / 2;
            if (this.age == 5) {
                this.y += 0.1;
            }
        } else if (this.age < this.lifetime - 4) {
            this.yd += 0.005;
            this.particle = 147 + this.age % 4 / 2;
        } else {
            this.yd /= 2.0;
            this.particle = 150 - (this.lifetime - this.age) / 2;
        }
        this.y = this.y + this.yd;
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float partialTicks) {
        float u0 = this.particle % 16 / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = this.particle / 16 / 64.0F;
        float v1 = v0 + 0.015625F;
        float s = this.particleScale;
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());
        Quaternionf q = camera.rotation();
        int light = this.getLightColor(partialTicks);
        this.v(wr, q, px, py, pz, -s, -s, u1, v1, light);
        this.v(wr, q, px, py, pz, -s, s, u1, v0, light);
        this.v(wr, q, px, py, pz, s, s, u0, v0, light);
        this.v(wr, q, px, py, pz, s, -s, u0, v1, light);
    }

    private void v(VertexConsumer wr, Quaternionf q, float cx, float cy, float cz, float lx, float ly, float u, float v, int light) {
        Vector3f p = SCRATCH.set(lx, ly, 0.0F).rotate(q);
        wr.addVertex(cx + p.x(), cy + p.y(), cz + p.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
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
