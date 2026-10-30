package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.common.lib.SoundsTC;

import java.util.ArrayList;
import java.util.List;

/**
 * Мошка роя (порт FXSwarm): кружит вокруг цели (подлетает, а у цели или когда цель ранена — отлетает), кадры 7–14 строки 1
 * сетки 64, полная яркость, слой 1. После смерти цели падает и гаснет за 50 тиков. Изредка жужжит (не больше 3 звуков разом).
 */
public class FXSwarm extends Particle implements IEngineParticle {
    private static final Vector3f SCRATCH = new Vector3f();
    /** Только клиент: время окончания текущих звуков жужжания. */
    private static final List<Long> BUZZCOUNT = new ArrayList<>();
    private final Entity target;
    private float turnSpeed = 10.0F;
    private float speed = 0.2F;
    int deathtimer = 0;
    float rotationPitch;
    float rotationYaw;
    float particleScale;

    public FXSwarm(ClientLevel world, double x, double y, double z, Entity target, float r, float g, float b, float sp, float ts, float pg) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setColor(r, g, b);
        this.particleScale = this.random.nextFloat() * 0.5F + 1.0F;
        this.target = target;
        float f3 = 0.2F;
        this.xd = (this.random.nextFloat() - this.random.nextFloat()) * f3;
        this.yd = (this.random.nextFloat() - this.random.nextFloat()) * f3;
        this.zd = (this.random.nextFloat() - this.random.nextFloat()) * f3;
        this.speed = sp;
        this.turnSpeed = ts;
        this.gravity = pg;
        this.lifetime = Integer.MAX_VALUE;
    }

    private boolean targetDead() {
        return this.target == null || !this.target.isAlive() || this.target instanceof LivingEntity le && le.deathTime > 0;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.targetDead()) {
            this.deathtimer++;
            this.xd *= 0.9;
            this.zd *= 0.9;
            this.yd -= this.gravity / 2.0F;
            if (this.deathtimer > 50) {
                this.remove();
            }
        } else {
            this.yd += this.gravity;
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.985;
        this.yd *= 0.985;
        this.zd *= 0.985;
        if (!this.targetDead()) {
            boolean hurt = this.target instanceof LivingEntity le && le.hurtTime > 0;
            int half = (int) (this.turnSpeed / 2.0F);
            if (new Vec3(this.x, this.y, this.z).distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ()) > this.target.getBbWidth() && !hurt) {
                this.faceEntity(this.target, this.turnSpeed / 2.0F + this.random.nextInt(half), this.turnSpeed / 2.0F + this.random.nextInt(half));
            } else {
                this.faceEntity(this.target, -(this.turnSpeed / 2.0F + this.random.nextInt(half)), -(this.turnSpeed / 2.0F + this.random.nextInt(half)));
            }
            double mx = -Mth.sin(this.rotationYaw / 180.0F * (float) Math.PI) * Mth.cos(this.rotationPitch / 180.0F * (float) Math.PI);
            double mz = Mth.cos(this.rotationYaw / 180.0F * (float) Math.PI) * Mth.cos(this.rotationPitch / 180.0F * (float) Math.PI);
            double my = -Mth.sin(this.rotationPitch / 180.0F * (float) Math.PI);
            this.setHeading(mx, my, mz, this.speed, 15.0F);
        }
        if (BUZZCOUNT.size() < 3 && this.random.nextInt(50) == 0 && this.level.getNearestPlayer(this.x, this.y, this.z, 8.0, false) != null) {
            this.level.playLocalSound(this.x, this.y, this.z, SoundsTC.fly.get(), SoundSource.HOSTILE, 0.03F, 0.5F + this.random.nextFloat() * 0.4F, false);
            BUZZCOUNT.add(System.nanoTime() + 1500000L);
        }
        if (BUZZCOUNT.size() >= 3 && BUZZCOUNT.get(0) < System.nanoTime()) {
            BUZZCOUNT.remove(0);
        }
    }

    private void faceEntity(Entity e, float yawStep, float pitchStep) {
        double d0 = e.getX() - this.x;
        double d1 = e.getZ() - this.z;
        double d2 = (e.getBoundingBox().minY + e.getBoundingBox().maxY) / 2.0 - (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0;
        double d3 = Math.sqrt(d0 * d0 + d1 * d1);
        float f2 = (float) (Math.atan2(d1, d0) * 180.0 / Math.PI) - 90.0F;
        float f3 = (float) (-(Math.atan2(d2, d3) * 180.0 / Math.PI));
        this.rotationPitch = updateRotation(this.rotationPitch, f3, pitchStep);
        this.rotationYaw = updateRotation(this.rotationYaw, f2, yawStep);
    }

    private static float updateRotation(float cur, float target, float max) {
        float f3 = Mth.wrapDegrees(target - cur);
        if (f3 > max) {
            f3 = max;
        }
        if (f3 < -max) {
            f3 = -max;
        }
        return cur + f3;
    }

    private void setHeading(double x, double y, double z, float speed, float spread) {
        float f2 = Mth.sqrt((float) (x * x + y * y + z * z));
        x /= f2;
        y /= f2;
        z /= f2;
        x += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075F * spread;
        y += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075F * spread;
        z += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075F * spread;
        this.xd = x * speed;
        this.yd = y * speed;
        this.zd = z * speed;
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float f) {
        float bob = Mth.sin(this.age / 3.0F) * 0.25F + 1.0F;
        int part = 7 + this.age % 8;
        float u0 = part / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = 0.0625F;
        float v1 = v0 + 0.015625F;
        float size = 0.1F * this.particleScale * bob;
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(f, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(f, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(f, this.zo, this.z) - cam.z());
        float trans = (50.0F - this.deathtimer) / 50.0F;
        float dd = this.target instanceof LivingEntity le && le.hurtTime > 0 ? 2.0F : 1.0F;
        float r = this.rCol;
        float g = this.gCol / dd;
        float b = this.bCol / dd;
        Quaternionf q = camera.rotation();
        v(wr, q, px, py, pz, -size, -size, u1, v1, r, g, b, trans);
        v(wr, q, px, py, pz, -size, size, u1, v0, r, g, b, trans);
        v(wr, q, px, py, pz, size, size, u0, v0, r, g, b, trans);
        v(wr, q, px, py, pz, size, -size, u0, v1, r, g, b, trans);
    }

    private static void v(VertexConsumer wr, Quaternionf q, float px, float py, float pz, float dx, float dy, float u, float v, float r, float g, float b, float a) {
        Vector3f p = SCRATCH.set(dx, dy, 0.0F).rotate(q);
        wr.addVertex(px + p.x(), py + p.y(), pz + p.z()).setUv(u, v).setColor(r, g, b, a).setLight(240);
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
