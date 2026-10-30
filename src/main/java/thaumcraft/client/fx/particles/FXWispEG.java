package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Тёмная дымка потустороннего стража (порт FXWispEG): почти чёрные клочья (кадры 0–12 строки 3 сетки 64) стекают вниз от
 * пояса, следуя за движением стража; тают с возрастом и с расстоянием до камеры (32 блока). Слой 1, полная яркость.
 */
public class FXWispEG extends Particle implements IEngineParticle {
    private static final Vector3f SCRATCH = new Vector3f();
    private final Entity target;
    private final float particleScale;

    public FXWispEG(ClientLevel world, double x, double y, double z, Entity target) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.target = target;
        this.xd = this.random.nextGaussian() * 0.03;
        this.yd = -0.05;
        this.zd = this.random.nextGaussian() * 0.03;
        this.particleScale = (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F * 0.4F;
        this.lifetime = (int) (40.0 / (Math.random() * 0.3 + 0.7));
        this.setSize(0.01F, 0.01F);
        this.rCol = this.random.nextFloat() * 0.05F;
        this.gCol = this.random.nextFloat() * 0.05F;
        this.bCol = this.random.nextFloat() * 0.05F;
        this.gravity = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.target != null && !this.onGround) {
            Vec3 m = this.target.getDeltaMovement();
            this.x += m.x;
            this.z += m.z;
        }
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float f) {
        Entity e = Minecraft.getInstance().getCameraEntity();
        if (e == null) {
            return;
        }
        float agescale = 1.0F - (float) this.age / this.lifetime;
        float d6 = 1024.0F;
        double dist = e.position().distanceToSqr(this.x, this.y, this.z);
        float base = (float) (1.0 - Math.min(d6, dist) / d6);
        float size = 0.5F * this.particleScale;
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(f, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(f, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(f, this.zo, this.z) - cam.z());
        float u0 = this.age % 13 / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = 0.046875F;
        float v1 = v0 + 0.015625F;
        float a = 0.2F * agescale * base;
        Quaternionf q = camera.rotation();
        v(wr, q, px, py, pz, -size, -size, u1, v1, a);
        v(wr, q, px, py, pz, -size, size, u1, v0, a);
        v(wr, q, px, py, pz, size, size, u0, v0, a);
        v(wr, q, px, py, pz, size, -size, u0, v1, a);
    }

    private void v(VertexConsumer wr, Quaternionf q, float px, float py, float pz, float dx, float dy, float u, float v, float a) {
        Vector3f p = SCRATCH.set(dx, dy, 0.0F).rotate(q);
        wr.addVertex(px + p.x(), py + p.y(), pz + p.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, a).setLight(240);
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
