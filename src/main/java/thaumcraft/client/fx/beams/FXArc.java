package thaumcraft.client.fx.beams;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.particles.ICustomRenderParticle;
import thaumcraft.client.fx.particles.IEngineParticle;

/**
 * Молния-дуга (порт FXArc): ломаная по параболе от точки к цели с шумом, две ленты текстуры beamh (вертикальная и
 * диагональная), аддитивно, без отсечения граней, живёт 2 тика и гаснет. Ленты 1.12 (GL_TRIANGLE_STRIP) — по четырёхугольникам.
 */
public class FXArc extends Particle implements IEngineParticle, ICustomRenderParticle {
    private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/beamh.png");
    final List<Vec3> points = new ArrayList<>();
    public float length = 1.0F;

    public FXArc(ClientLevel world, double x, double y, double z, double tx, double ty, double tz, float red, float green, float blue, double hg) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.rCol = red;
        this.gCol = green;
        this.bCol = blue;
        this.setSize(0.02F, 0.02F);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        double dtx = tx - x;
        double dty = ty - y;
        double dtz = tz - z;
        this.lifetime = 2;
        double gravity = 0.115;
        double noise = 0.25;
        Vec3 vs = Vec3.ZERO;
        Vec3 ve = new Vec3(dtx, dty, dtz);
        Vec3 vc = Vec3.ZERO;
        this.length = (float) ve.length();
        Vec3 vv = calculateVelocity(vs, ve, hg, gravity);
        double l = vv.lengthSqr();
        this.points.add(vs);
        for (int c = 0; ve.distanceToSqr(vc) > l && c < 50; c++) {
            Vec3 vt = vc.add(vv.x, vv.y, vv.z);
            vc = vt;
            vt = vt.add((this.random.nextDouble() - this.random.nextDouble()) * noise, (this.random.nextDouble() - this.random.nextDouble()) * noise,
                    (this.random.nextDouble() - this.random.nextDouble()) * noise);
            this.points.add(vt);
            vv = vv.subtract(0.0, gravity / 1.9, 0.0);
        }
        this.points.add(ve);
    }

    /** Utils.calculateVelocity оригинала: начальная скорость параболы с подъёмом heightGain. */
    static Vec3 calculateVelocity(Vec3 from, Vec3 to, double heightGain, double gravity) {
        double endGain = to.y - from.y;
        double hx = to.x - from.x;
        double hz = to.z - from.z;
        double horizDist = Math.sqrt(hx * hx + hz * hz);
        double maxGain = heightGain > endGain + heightGain ? heightGain : endGain + heightGain;
        double a = -horizDist * horizDist / (4.0 * maxGain);
        double b = horizDist;
        double c = -endGain;
        double slope = -b / (2.0 * a) - Math.sqrt(b * b - 4.0 * a * c) / (2.0 * a);
        double vy = Math.sqrt(maxGain * gravity);
        double vh = vy / slope;
        double mag = Math.sqrt(hx * hx + hz * hz);
        return new Vec3(vh * hx / mag, vy, vh * hz / mag);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    @Override
    public void renderCustom(PoseStack pose, Camera camera, float f) {
        if (this.points.size() < 2) {
            return;
        }
        Vec3 cam = camera.getPosition();
        pose.pushPose();
        pose.translate(Mth.lerp(f, this.xo, this.x) - cam.x, Mth.lerp(f, this.yo, this.y) - cam.y, Mth.lerp(f, this.zo, this.z) - cam.z);
        Matrix4f m = pose.last().pose();
        float size = 0.25F;
        float alpha = 1.0F - (this.age + f) / this.lifetime;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, BEAM);
        RenderSystem.disableCull();
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int c = 0; c + 1 < this.points.size(); c++) {
            Vec3 v = this.points.get(c);
            Vec3 w = this.points.get(c + 1);
            float u0 = c / this.length;
            float u1 = (c + 1) / this.length;
            this.v(buf, m, v.x, v.y - size, v.z, u0, 1.0F, alpha);
            this.v(buf, m, v.x, v.y + size, v.z, u0, 0.0F, alpha);
            this.v(buf, m, w.x, w.y + size, w.z, u1, 0.0F, alpha);
            this.v(buf, m, w.x, w.y - size, w.z, u1, 1.0F, alpha);
        }
        for (int c = 0; c + 1 < this.points.size(); c++) {
            Vec3 v = this.points.get(c);
            Vec3 w = this.points.get(c + 1);
            float u0 = c / this.length;
            float u1 = (c + 1) / this.length;
            this.v(buf, m, v.x - size, v.y, v.z - size, u0, 1.0F, alpha);
            this.v(buf, m, v.x + size, v.y, v.z + size, u0, 0.0F, alpha);
            this.v(buf, m, w.x + size, w.y, w.z + size, u1, 0.0F, alpha);
            this.v(buf, m, w.x - size, w.y, w.z - size, u1, 1.0F, alpha);
        }
        MeshData mesh = buf.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        pose.popPose();
    }

    private void v(BufferBuilder buf, Matrix4f m, double x, double y, double z, float u, float v, float a) {
        buf.addVertex(m, (float) x, (float) y, (float) z).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, a);
    }

    @Override
    public void renderParticle(VertexConsumer buffer, Camera camera, float partialTicks) {
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
