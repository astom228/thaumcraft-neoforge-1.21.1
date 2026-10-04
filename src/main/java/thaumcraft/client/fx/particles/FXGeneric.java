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
 * Основная частица Thaumcraft (порт FXGeneric): кадр из листа textures/misc/particles.png (сетка gridSize×gridSize),
 * ключи прозрачности и размера по времени жизни, переход цвета, ветер, вращение, «наклонённый» режим (setAngles).
 * Рисуется движком {@link thaumcraft.client.fx.ParticleEngine}, а не ванильным.
 */
public class FXGeneric extends Particle {
    boolean doneFrames = false;
    boolean flipped = false;
    double windX;
    double windZ;
    int layer = 0;
    float dr = 0.0F;
    float dg = 0.0F;
    float db = 0.0F;
    boolean loop = false;
    float rotationSpeed = 0.0F;
    int startParticle = 0;
    int numParticles = 1;
    int particleInc = 1;
    float[] scaleKeys = new float[]{1.0F};
    float[] scaleFrames = new float[]{0.0F};
    float[] alphaKeys = new float[]{1.0F};
    float[] alphaFrames = new float[]{0.0F};
    double slowDown = 0.98F;
    float randomX;
    float randomY;
    float randomZ;
    int[] finalFrames = null;
    boolean angled = false;
    float angleYaw;
    float anglePitch;
    int gridSize = 64;
    /** particleScale / particleTextureIndexX/Y оригинала. */
    protected float particleScale;
    protected int particleTextureIndexX;
    protected int particleTextureIndexY;

    public FXGeneric(ClientLevel world, double x, double y, double z, double xx, double yy, double zz) {
        this(world, x, y, z);
        this.xd = xx;
        this.yd = yy;
        this.zd = zz;
    }

    public FXGeneric(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
        this.setSize(0.1F, 0.1F);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        // Particle 1.12: particleScale = (rand*0.5 + 0.5) * 2
        this.particleScale = (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
    }

    void calculateFrames() {
        this.doneFrames = true;
        if (this.alphaKeys == null) {
            this.setAlphaF(1.0F);
        }
        this.alphaFrames = new float[this.lifetime + 1];
        float inc = (float) (this.alphaKeys.length - 1) / this.lifetime;
        float is = 0.0F;
        for (int a = 0; a <= this.lifetime; a++) {
            int isF = Mth.floor(is);
            float diff = isF < this.alphaKeys.length - 1 ? this.alphaKeys[isF + 1] - this.alphaKeys[isF] : 0.0F;
            float pa = is - isF;
            this.alphaFrames[a] = this.alphaKeys[isF] + diff * pa;
            is += inc;
        }
        if (this.scaleKeys == null) {
            this.setScale(1.0F);
        }
        this.scaleFrames = new float[this.lifetime + 1];
        inc = (float) (this.scaleKeys.length - 1) / this.lifetime;
        is = 0.0F;
        for (int a = 0; a <= this.lifetime; a++) {
            int isF = Mth.floor(is);
            float diff = isF < this.scaleKeys.length - 1 ? this.scaleKeys[isF + 1] - this.scaleKeys[isF] : 0.0F;
            float pa = is - isF;
            this.scaleFrames[a] = this.scaleKeys[isF] + diff * pa;
            is += inc;
        }
    }

    @Override
    public void tick() {
        if (!this.doneFrames) {
            this.calculateFrames();
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.oRoll = this.roll;
        this.roll = this.roll + (float) Math.PI * this.rotationSpeed * 2.0F;
        this.yd = this.yd - 0.04 * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd = this.xd * this.slowDown;
        this.yd = this.yd * this.slowDown;
        this.zd = this.zd * this.slowDown;
        this.xd = this.xd + this.random.nextGaussian() * this.randomX;
        this.yd = this.yd + this.random.nextGaussian() * this.randomY;
        this.zd = this.zd + this.random.nextGaussian() * this.randomZ;
        this.xd = this.xd + this.windX;
        this.zd = this.zd + this.windZ;
        if (this.onGround && this.slowDown != 1.0) {
            this.xd *= 0.7F;
            this.zd *= 0.7F;
        }
    }

    /** renderParticle оригинала: выбор кадра, прозрачность и размер по времени жизни. */
    public void renderParticle(VertexConsumer wr, Camera camera, float partialTicks) {
        if (!this.doneFrames) {
            this.calculateFrames();
        }
        if (this.loop) {
            this.setParticleTextureIndex(this.startParticle + this.age / this.particleInc % this.numParticles);
        } else {
            float fs = (float) this.age / this.lifetime;
            this.setParticleTextureIndex((int) (this.startParticle + Math.min(this.numParticles * fs, this.numParticles - 1)));
        }
        if (this.finalFrames != null && this.age > this.lifetime - this.finalFrames.length) {
            int frame = this.lifetime - this.age;
            this.setParticleTextureIndex(this.finalFrames[frame]);
        }
        this.alpha = this.alphaFrames[Math.min(this.age, this.alphaFrames.length - 1)];
        this.particleScale = this.scaleFrames[Math.min(this.age, this.scaleFrames.length - 1)];
        this.draw(wr, camera, partialTicks);
    }

    public void draw(VertexConsumer wr, Camera camera, float partialTicks) {
        float tx1 = (float) this.particleTextureIndexX / this.gridSize;
        float tx2 = tx1 + 1.0F / this.gridSize;
        float ty1 = (float) this.particleTextureIndexY / this.gridSize;
        float ty2 = ty1 + 1.0F / this.gridSize;
        float ts = 0.1F * this.particleScale;
        if (this.flipped) {
            float t = tx1;
            tx1 = tx2;
            tx2 = t;
        }
        float fs = Mth.clamp((this.age + partialTicks) / this.lifetime, 0.0F, 1.0F);
        float pr = this.rCol + (this.dr - this.rCol) * fs;
        float pg = this.gCol + (this.dg - this.gCol) * fs;
        float pb = this.bCol + (this.db - this.bCol) * fs;
        int light = this.getLightColor(partialTicks);
        Vec3 cam = camera.getPosition();
        float f5 = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float f6 = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float f7 = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());
        float roll = Mth.lerp(partialTicks, this.oRoll, this.roll);

        if (this.angled) {
            // GL: translate → rotate Y(-yaw+90) → rotate X(pitch+90) → rotate Z(угол); вершины (±ts, ±ts, 0)
            Quaternionf q = new Quaternionf()
                    .rotateY((float) Math.toRadians(-this.angleYaw + 90.0F))
                    .rotateX((float) Math.toRadians(this.anglePitch + 90.0F));
            if (this.roll != 0.0F) {
                q.rotateZ(roll);
            }
            vertex(wr, q, f5, f6, f7, -ts, -ts, tx2, ty2, pr, pg, pb, light);
            vertex(wr, q, f5, f6, f7, -ts, ts, tx2, ty1, pr, pg, pb, light);
            vertex(wr, q, f5, f6, f7, ts, ts, tx1, ty1, pr, pg, pb, light);
            vertex(wr, q, f5, f6, f7, ts, -ts, tx1, ty2, pr, pg, pb, light);
        } else {
            // Квад, повёрнутый к камере (как ванильные частицы)
            Quaternionf q = new Quaternionf(camera.rotation());
            if (this.roll != 0.0F) {
                q.rotateZ(roll);
            }
            vertex(wr, q, f5, f6, f7, ts, -ts, tx2, ty2, pr, pg, pb, light);
            vertex(wr, q, f5, f6, f7, ts, ts, tx2, ty1, pr, pg, pb, light);
            vertex(wr, q, f5, f6, f7, -ts, ts, tx1, ty1, pr, pg, pb, light);
            vertex(wr, q, f5, f6, f7, -ts, -ts, tx1, ty2, pr, pg, pb, light);
        }
    }

    private void vertex(VertexConsumer wr, Quaternionf q, float cx, float cy, float cz, float lx, float ly, float u, float v,
                        float r, float g, float b, int light) {
        Vector3f p = new Vector3f(lx, ly, 0.0F).rotate(q);
        wr.addVertex(cx + p.x(), cy + p.y(), cz + p.z()).setUv(u, v).setColor(r, g, b, this.alpha).setLight(light);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // Рисует ParticleEngine Thaumcraft
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    public boolean isFlipped() {
        return this.flipped;
    }

    public void setFlipped(boolean flip) {
        this.flipped = flip;
    }

    /** Ветер зависит от фазы луны. */
    public void setWind(double d) {
        int m = this.level.getMoonPhase();
        Vec3 vtar = new Vec3(0.1, 0.0, 0.0).yRot(m * (40 + this.random.nextInt(10)) / 180.0F * (float) Math.PI);
        this.windX = vtar.x * d;
        this.windZ = vtar.z * d;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public void setRBGColorF(float r, float g, float b) {
        this.setColor(r, g, b);
        this.dr = r;
        this.dg = g;
        this.db = b;
    }

    public void setRBGColorF(float r, float g, float b, float r2, float g2, float b2) {
        this.setColor(r, g, b);
        this.dr = r2;
        this.dg = g2;
        this.db = b2;
    }

    public int getFXLayer() {
        return this.layer;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setRotationSpeed(float rot) {
        this.rotationSpeed = (float) (rot * 0.017453292519943);
    }

    public void setRotationSpeed(float start, float rot) {
        this.roll = (float) (start * Math.PI * 2.0);
        this.rotationSpeed = (float) (rot * 0.017453292519943);
    }

    public void setMaxAge(int max) {
        this.lifetime = max;
    }

    public void setParticles(int startParticle, int numParticles, int particleInc) {
        this.numParticles = numParticles;
        this.particleInc = particleInc;
        this.startParticle = startParticle;
        this.setParticleTextureIndex(startParticle);
    }

    public void setParticle(int startParticle) {
        this.numParticles = 1;
        this.particleInc = 1;
        this.startParticle = startParticle;
        this.setParticleTextureIndex(startParticle);
    }

    public void setScale(float... scale) {
        this.particleScale = scale[0];
        this.scaleKeys = scale;
    }

    public void setAlphaF(float... a1) {
        this.alpha = a1[0];
        this.alphaKeys = a1;
    }

    public void setAlphaF(float a1) {
        this.alpha = a1;
        this.alphaKeys = new float[]{a1};
    }

    public void setSlowDown(double slowDown) {
        this.slowDown = slowDown;
    }

    public void setRandomMovementScale(float x, float y, float z) {
        this.randomX = x;
        this.randomY = y;
        this.randomZ = z;
    }

    public void setFinalFrames(int... frames) {
        this.finalFrames = frames;
    }

    public void setAngles(float yaw, float pitch) {
        this.angleYaw = yaw;
        this.anglePitch = pitch;
        this.angled = true;
    }

    public void setGravity(float g) {
        this.gravity = g;
    }

    public void setParticleTextureIndex(int particleTextureIndex) {
        this.particleTextureIndexX = particleTextureIndex % this.gridSize;
        this.particleTextureIndexY = particleTextureIndex / this.gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    public void setNoClip(boolean clip) {
        this.hasPhysics = clip;
    }
}
