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
 * Дымная спираль (порт FXSmokeSpiral): точка обходит сферу радиуса radius (2 оборота по долготе, сверху вниз) вокруг центра,
 * не опускаясь ниже miny + 0.1; кадры 1–5 строки 64-сетки particles.png, прозрачность 0.66 и тает с возрастом.
 */
public class FXSmokeSpiral extends Particle implements IEngineParticle {
    private static final Vector3f SCRATCH = new Vector3f();
    private final float radius;
    private final int start;
    private final int miny;
    private final float particleScale = 1.0F;

    public FXSmokeSpiral(ClientLevel world, double x, double y, double z, float radius, int start, int miny) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.gravity = -0.01F;
        this.xd = this.yd = this.zd = 0.0;
        this.lifetime = 20 + world.random.nextInt(10);
        this.setSize(0.01F, 0.01F);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.radius = radius;
        this.start = start;
        this.miny = miny;
    }

    public void setRBGColorF(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    @Override
    public void tick() {
        this.alpha = (float) (this.lifetime - this.age) / this.lifetime;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float f) {
        int particle = (int) (1.0F + (float) this.age / this.lifetime * 4.0F);
        float r1 = this.start + 720.0F * ((this.age + f) / this.lifetime);
        float r2 = 90.0F - 180.0F * ((this.age + f) / this.lifetime);
        float mX = -Mth.sin(r1 / 180.0F * (float) Math.PI) * Mth.cos(r2 / 180.0F * (float) Math.PI) * this.radius;
        float mZ = Mth.cos(r1 / 180.0F * (float) Math.PI) * Mth.cos(r2 / 180.0F * (float) Math.PI) * this.radius;
        float mY = -Mth.sin(r2 / 180.0F * (float) Math.PI) * this.radius;
        float u0 = particle % 16 / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = particle / 16 / 64.0F;
        float v1 = v0 + 0.015625F;
        float size = 0.15F * this.particleScale;
        Vec3 cam = camera.getPosition();
        float px = (float) (this.x + mX - cam.x());
        float py = (float) (Math.max(this.y + mY, this.miny + 0.1F) - cam.y());
        float pz = (float) (this.z + mZ - cam.z());
        int light = this.getLightColor(f);
        float a = 0.66F * this.alpha;
        Quaternionf q = camera.rotation();
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
