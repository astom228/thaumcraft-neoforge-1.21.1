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
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.particles.ICustomRenderParticle;
import thaumcraft.client.fx.particles.IEngineParticle;
import thaumcraft.client.lib.PolyCone;

/**
 * Разряд (порт FXBolt, носитель «Разряд»): извилистая трубка от точки к цели, живёт 3 тика, извивается всё сильнее. Две трубки
 * (вторая — втрое тоньше) текстуры essentia, аддитивно, без записи глубины и отсечения граней. glePolyCone — PolyCone.
 */
public class FXBolt extends Particle implements IEngineParticle, ICustomRenderParticle {
    private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/essentia.png");
    float width;
    final List<Vec3> points = new ArrayList<>();
    final List<Float> pointsWidth = new ArrayList<>();
    float dr;
    long seed;
    private final double tX;
    private final double tY;
    private final double tZ;
    public float length;

    public FXBolt(ClientLevel world, double x, double y, double z, double tx, double ty, double tz, float red, float green, float blue, float width) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.rCol = red;
        this.gCol = green;
        this.bCol = blue;
        this.setSize(0.02F, 0.02F);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.tX = tx - x;
        this.tY = ty - y;
        this.tZ = tz - z;
        this.width = width;
        this.lifetime = 3;
        Vec3 ve = new Vec3(this.tX, this.tY, this.tZ);
        this.length = (float) (ve.length() * Math.PI);
        this.dr = (float) (this.random.nextInt(50) * Math.PI);
        this.seed = this.random.nextInt(1000);
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

    private void calcSteps(float f) {
        Random rr = new Random(this.seed);
        this.points.clear();
        this.pointsWidth.clear();
        int steps = (int) this.length;
        this.points.add(Vec3.ZERO);
        this.pointsWidth.add(this.width);
        float ampl = (this.age + f) / 10.0F;
        for (int a = 1; a < steps - 1; a++) {
            float dist = a * (this.length / steps) + this.dr;
            double dx = this.tX / steps * a + Mth.sin(dist / 4.0F) * ampl;
            double dy = this.tY / steps * a + Mth.sin(dist / 3.0F) * ampl;
            double dz = this.tZ / steps * a + Mth.sin(dist / 2.0F) * ampl;
            dx += (rr.nextFloat() - rr.nextFloat()) * 0.1F;
            dy += (rr.nextFloat() - rr.nextFloat()) * 0.1F;
            dz += (rr.nextFloat() - rr.nextFloat()) * 0.1F;
            this.points.add(new Vec3(dx, dy, dz));
            this.pointsWidth.add((rr.nextInt(4) == 0 ? 1.0F - this.age * 0.25F : 1.0F) * this.width);
        }
        this.pointsWidth.add(this.width);
        this.points.add(new Vec3(this.tX, this.tY, this.tZ));
    }

    @Override
    public void renderCustom(PoseStack pose, Camera camera, float f) {
        this.calcSteps(f);
        if (this.points.size() <= 2) {
            return;
        }
        Vec3 cam = camera.getPosition();
        pose.pushPose();
        pose.translate(Mth.lerp(f, this.xo, this.x) - cam.x, Mth.lerp(f, this.yo, this.y) - cam.y, Mth.lerp(f, this.zo, this.z) - cam.z);
        float alpha = Mth.clamp(1.0F - (float) this.age / this.lifetime, 0.1F, 1.0F);
        int n = this.points.size();
        double[][] pp = new double[n][3];
        float[][] colours = new float[n][4];
        double[] radii = new double[n];
        for (int a = 0; a < n; a++) {
            pp[a][0] = this.points.get(a).x;
            pp[a][1] = this.points.get(a).y;
            pp[a][2] = this.points.get(a).z;
            colours[a][0] = this.rCol;
            colours[a][1] = this.gCol;
            colours[a][2] = this.bCol;
            colours[a][3] = alpha;
            radii[a] = this.pointsWidth.get(a) / 10.0F;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, BEAM);
        RenderSystem.disableCull();
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        // gle: set_POLYCYL_TESS(5), gleSetJoinStyle(1042), glePolyCone(…, 1.0, 0.0) — дважды, вторая втрое тоньше
        PolyCone.draw(buf, pose.last().pose(), 5, pp, colours, radii, 1.0F, 0.0F);
        for (int a = 0; a < n; a++) {
            radii[a] /= 3.0;
        }
        PolyCone.draw(buf, pose.last().pose(), 5, pp, colours, radii, 1.0F, 0.0F);
        MeshData mesh = buf.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        pose.popPose();
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
