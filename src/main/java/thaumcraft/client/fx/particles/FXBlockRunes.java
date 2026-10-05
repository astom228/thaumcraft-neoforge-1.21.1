package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Руна на грани блока (порт FXBlockRunes): кадр 224–239 листа частиц, проявляется и гаснет, полная яркость. Слой 0. */
public class FXBlockRunes extends Particle implements IEngineParticle {
    double ofx = 0.0;
    double ofy = 0.0;
    float rotation = 0.0F;
    int runeIndex = 0;
    float particleScale;
    private final Matrix4f scratch = new Matrix4f();

    public FXBlockRunes(ClientLevel world, double d, double d1, double d2, float f1, float f2, float f3, int m) {
        super(world, d, d1, d2, 0.0, 0.0, 0.0);
        if (f1 == 0.0F) {
            f1 = 1.0F;
        }
        this.rotation = this.random.nextInt(4) * 90;
        this.rCol = f1;
        this.gCol = f2;
        this.bCol = f3;
        this.gravity = 0.0F;
        this.xd = this.yd = this.zd = 0.0;
        this.lifetime = 3 * m;
        this.setSize(0.01F, 0.01F);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.runeIndex = (int) (Math.random() * 16.0 + 224.0);
        this.ofx = this.random.nextFloat() * 0.2;
        this.ofy = -0.3 + this.random.nextFloat() * 0.6;
        this.particleScale = (float) (1.0 + this.random.nextGaussian() * 0.1F);
        this.alpha = 0.0F;
    }

    @Override
    public int getFXLayer() {
        return 0;
    }

    @Override
    public void renderParticle(VertexConsumer wr, Camera camera, float f) {
        Vec3 cam = camera.getPosition();
        float var13 = (float) (Mth.lerp(f, this.xo, this.x) - cam.x());
        float var14 = (float) (Mth.lerp(f, this.yo, this.y) - cam.y());
        float var15 = (float) (Mth.lerp(f, this.zo, this.z) - cam.z());
        Matrix4f m = this.scratch.identity().translate(var13, var14, var15)
                .rotateY((float) Math.toRadians(this.rotation))
                .rotateZ((float) Math.toRadians(90.0F))
                .translate((float) this.ofx, (float) this.ofy, -0.51F);
        float var8x = this.runeIndex % 16 / 64.0F;
        float var9 = var8x + 0.015625F;
        float var10 = 0.09375F;
        float var11 = var10 + 0.015625F;
        float var12 = 0.3F * this.particleScale;
        float a = this.alpha / 2.0F;
        int light = 240;
        wr.addVertex(m, -0.5F * var12, 0.5F * var12, 0.0F).setUv(var9, var11).setColor(this.rCol, this.gCol, this.bCol, a).setLight(light);
        wr.addVertex(m, 0.5F * var12, 0.5F * var12, 0.0F).setUv(var9, var10).setColor(this.rCol, this.gCol, this.bCol, a).setLight(light);
        wr.addVertex(m, 0.5F * var12, -0.5F * var12, 0.0F).setUv(var8x, var10).setColor(this.rCol, this.gCol, this.bCol, a).setLight(light);
        wr.addVertex(m, -0.5F * var12, -0.5F * var12, 0.0F).setUv(var8x, var11).setColor(this.rCol, this.gCol, this.bCol, a).setLight(light);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float threshold = this.lifetime / 5.0F;
        if (this.age <= threshold) {
            this.alpha = this.age / threshold;
        } else {
            this.alpha = (float) (this.lifetime - this.age) / this.lifetime;
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.yd = this.yd - 0.04 * this.gravity;
        this.x = this.x + this.xd;
        this.y = this.y + this.yd;
        this.z = this.z + this.zd;
    }

    public void setGravity(float value) {
        this.gravity = value;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // Рисует ParticleEngine Thaumcraft
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
