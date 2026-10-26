package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Искра, летящая к цели (порт FXBoreSparkle): кадры 0–3 строки 4 сетки particles.png, слой 0 (аддитивно), полная яркость блока. */
public class FXBoreSparkle extends Particle implements IEngineParticle {
    private static final Vector3f SCRATCH = new Vector3f();
    private Entity target;
    private double targetX;
    private double targetY;
    private double targetZ;
    float particleScale;

    public FXBoreSparkle(ClientLevel world, double x, double y, double z, double tx, double ty, double tz) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.particleScale = this.random.nextFloat() * 0.5F + 0.5F;
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        double dx = tx - this.x;
        double dy = ty - this.y;
        double dz = tz - this.z;
        int base = (int) (Mth.sqrt((float) (dx * dx + dy * dy + dz * dz)) * 10.0F);
        if (base < 1) {
            base = 1;
        }
        this.lifetime = base / 2 + this.random.nextInt(base);
        float f3 = 0.01F;
        this.xd = (float) this.random.nextGaussian() * f3;
        this.yd = (float) this.random.nextGaussian() * f3;
        this.zd = (float) this.random.nextGaussian() * f3;
        this.rCol = 0.2F;
        this.gCol = 0.6F + this.random.nextFloat() * 0.3F;
        this.bCol = 0.2F;
        this.gravity = 0.2F;
        Entity view = Minecraft.getInstance().getCameraEntity();
        int visibleDistance = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST ? 32 : 64;
        if (view != null && view.distanceToSqr(this.x, this.y, this.z) > visibleDistance * visibleDistance) {
            this.lifetime = 0;
        }
    }

    public FXBoreSparkle(ClientLevel world, double x, double y, double z, Entity t) {
        this(world, x, y, z, t.getX(), t.getY() + t.getEyeHeight(), t.getZ());
        this.target = t;
    }

    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.target != null) {
            this.targetX = this.target.getX();
            this.targetY = this.target.getY() + this.target.getEyeHeight();
            this.targetZ = this.target.getZ();
        }
        if (this.age++ < this.lifetime && (Mth.floor(this.x) != Mth.floor(this.targetX) || Mth.floor(this.y) != Mth.floor(this.targetY)
                || Mth.floor(this.z) != Mth.floor(this.targetZ))) {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.985;
            this.yd *= 0.95;
            this.zd *= 0.985;
            double dx = this.targetX - this.x;
            double dy = this.targetY - this.y;
            double dz = this.targetZ - this.z;
            double d11 = Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
            double clamp = Math.min(0.25, d11 / 15.0);
            if (d11 < 2.0) {
                this.particleScale *= 0.9F;
            }
            dx /= d11;
            dy /= d11;
            dz /= d11;
            this.xd += dx * clamp;
            this.yd += dy * clamp;
            this.zd += dz * clamp;
            this.xd = Mth.clamp((float) this.xd, -clamp, clamp);
            this.yd = Mth.clamp((float) this.yd, -clamp, clamp);
            this.zd = Mth.clamp((float) this.zd, -clamp, clamp);
            this.xd = this.xd + this.random.nextGaussian() * 0.01;
            this.yd = this.yd + this.random.nextGaussian() * 0.01;
            this.zd = this.zd + this.random.nextGaussian() * 0.01;
        } else {
            this.remove();
        }
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float f) {
        float bob = Mth.sin(this.age / 3.0F) * 0.5F + 1.0F;
        int part = this.age % 4;
        float u0 = part / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = 0.0625F;
        float v1 = v0 + 0.015625F;
        float size = 0.1F * this.particleScale * bob;
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(f, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(f, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(f, this.zo, this.z) - cam.z());
        Quaternionf q = camera.rotation();
        // вершины как в 1.12: (-1,-1) u1v1, (-1,1) u1v0, (1,1) u0v0, (1,-1) u0v1
        this.vertex(wr, q, px, py, pz, -size, -size, u1, v1);
        this.vertex(wr, q, px, py, pz, -size, size, u1, v0);
        this.vertex(wr, q, px, py, pz, size, size, u0, v0);
        this.vertex(wr, q, px, py, pz, size, -size, u0, v1);
    }

    private void vertex(VertexConsumer wr, Quaternionf q, float px, float py, float pz, float dx, float dy, float u, float v) {
        Vector3f p = SCRATCH.set(dx, dy, 0.0F).rotate(q);
        wr.addVertex(px + p.x(), py + p.y(), pz + p.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, 1.0F).setLight(240);
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
