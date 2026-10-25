package thaumcraft.client.fx.other;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.particles.ICustomRenderParticle;
import thaumcraft.client.fx.particles.IEngineParticle;
import thaumcraft.client.lib.PolyCone;

/**
 * Поток эссенции (порт FXEssentiaStream): «змейка» от источника к цели, трубкой (PolyCone вместо GLE) с текстурой essentia.png;
 * у цели и по пути роняет капли. Повторный поток между теми же блоками того же цвета удлиняет уже идущий.
 */
public class FXEssentiaStream extends Particle implements IEngineParticle, ICustomRenderParticle {
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final double startX;
    private final double startY;
    private final double startZ;
    private int count = 0;
    public int length = 20;
    private String key = "";
    /** Активные потоки по ключу «начало-конец-цвет» (только клиент, главный поток). */
    static final Map<String, FXEssentiaStream> pt = new HashMap<>();
    private static final ResourceLocation TEX0 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/essentia.png");
    int layer = 1;
    double[][] points;
    float[][] colours;
    double[] radii;
    int growing = -1;
    float particleScale;
    final List<double[]> vecs = new ArrayList<>();

    public FXEssentiaStream(ClientLevel w, double par2, double par4, double par6, double tx, double ty, double tz, int count, int color, float scale, int extend,
                            double my) {
        super(w, par2, par4, par6, 0.0, 0.0, 0.0);
        this.particleScale = (float) (scale * (1.0 + this.random.nextGaussian() * 0.15F));
        this.length = Math.max(20, extend);
        this.count = count;
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        BlockPos bp1 = BlockPos.containing(this.x, this.y, this.z);
        BlockPos bp2 = BlockPos.containing(this.targetX, this.targetY, this.targetZ);
        net.minecraft.world.level.block.state.BlockState bs = w.getBlockState(bp1);
        if (bs.getBlock() instanceof thaumcraft.common.blocks.essentia.BlockEssentiaTransport) {
            net.minecraft.core.Direction f = bs.getValue(thaumcraft.common.blocks.essentia.BlockEssentiaTransport.FACING);
            this.x = this.x + f.getStepX() * 0.05F;
            this.y = this.y + f.getStepY() * 0.05F;
            this.z = this.z + f.getStepZ() * 0.05F;
        }
        double dx = tx - this.x;
        double dy = ty - this.y;
        double dz = tz - this.z;
        int base = (int) (Mth.sqrt((float) (dx * dx + dy * dy + dz * dz)) * 21.0F);
        if (base < 1) {
            base = 1;
        }
        this.lifetime = base;
        String k = bp1.asLong() + "" + bp2.asLong() + "" + color;
        FXEssentiaStream trail2 = pt.get(k);
        if (trail2 != null && trail2.isAlive() && trail2.vecs.size() < trail2.length) {
            trail2.length = trail2.length + Math.max(extend, 5);
            trail2.lifetime = trail2.lifetime + Math.max(extend, 5);
            this.lifetime = 0;
        }
        if (this.lifetime > 0) {
            pt.put(k, this);
            this.key = k;
        }
        this.xd = Mth.sin(count / 4.0F) * 0.015F;
        this.yd = my + Mth.sin(count / 3.0F) * 0.015F;
        this.zd = Mth.sin(count / 2.0F) * 0.015F;
        Color c = new Color(color);
        this.rCol = c.getRed() / 255.0F;
        this.gCol = c.getGreen() / 255.0F;
        this.bCol = c.getBlue() / 255.0F;
        this.gravity = 0.2F;
        this.vecs.add(new double[] {0.0, 0.0, 0.001, 0.0}); // Quat(0, 0, 0, 0.001): s = 0, z = 0.001
        this.vecs.add(new double[] {0.0, 0.0, 0.001, 0.0}); // Quat(0, 0, 0, 0.001): s = 0, z = 0.001
        this.startX = this.x;
        this.startY = this.y;
        this.startZ = this.z;
    }

    @Override
    public void renderCustom(PoseStack pose, Camera camera, float f) {
        if (this.points == null || this.points.length <= 2) {
            return;
        }
        Vec3 cam = camera.getPosition();
        pose.pushPose();
        pose.translate(this.startX - cam.x, this.startY - cam.y, this.startZ - cam.z);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEX0);
        RenderSystem.disableCull();
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        // gle: set_POLYCYL_TESS(8), __ROUND_TESS_PIECES 1, gleSetJoinStyle(1042)
        PolyCone.draw(buf, pose.last().pose(), 8, this.points, this.colours, this.radii, 0.075F,
                this.growing < 0 ? 0.0F : 0.075F * (this.age - this.growing + f));
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
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    public void setFXLayer(int l) {
        this.layer = l;
    }

    @Override
    public int getFXLayer() {
        return this.layer;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ < this.lifetime && this.length >= 1) {
            this.yd = this.yd + 0.01 * this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.985;
            this.yd *= 0.985;
            this.zd *= 0.985;
            this.xd = Mth.clamp((float) this.xd, -0.05F, 0.05F);
            this.yd = Mth.clamp((float) this.yd, -0.05F, 0.05F);
            this.zd = Mth.clamp((float) this.zd, -0.05F, 0.05F);
            double dx = this.targetX - this.x;
            double dy = this.targetY - this.y;
            double dz = this.targetZ - this.z;
            double d13 = 0.01;
            double d11 = Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
            dx /= d11;
            dy /= d11;
            dz /= d11;
            this.xd = this.xd + dx * (d13 / Math.min(1.0, d11));
            this.yd = this.yd + dy * (d13 / Math.min(1.0, d11));
            this.zd = this.zd + dz * (d13 / Math.min(1.0, d11));
            float scale = this.particleScale * (0.75F + Mth.sin((this.count + this.age) / 2.0F) * 0.25F);
            if (d11 < 1.0) {
                float f = Mth.sin((float) (d11 * (Math.PI / 2)));
                scale *= f;
                this.particleScale *= f;
            }
            if (this.particleScale > 0.001) {
                this.vecs.add(new double[] {this.x - this.startX, this.y - this.startY, this.z - this.startZ, scale});
            } else {
                if (this.growing < 0) {
                    this.growing = this.age;
                }
                this.length--;
                FXDispatcher.INSTANCE.essentiaDropFx(this.targetX + this.random.nextGaussian() * 0.075F, this.targetY + this.random.nextGaussian() * 0.075F,
                        this.targetZ + this.random.nextGaussian() * 0.075F, this.rCol, this.gCol, this.bCol, 0.5F);
            }
            if (this.vecs.size() > this.length) {
                this.vecs.remove(0);
            }
            this.points = new double[this.vecs.size()][3];
            this.colours = new float[this.vecs.size()][4];
            this.radii = new double[this.vecs.size()];
            int c = this.vecs.size();
            for (double[] v : this.vecs) {
                c--;
                float variance = 1.0F + Mth.sin((c + this.age) / 3.0F) * 0.2F;
                float xx = Mth.sin((c + this.age) / 6.0F) * 0.03F;
                float yy = Mth.sin((c + this.age) / 7.0F) * 0.03F;
                float zz = Mth.sin((c + this.age) / 8.0F) * 0.03F;
                this.points[c][0] = v[0] + xx;
                this.points[c][1] = v[1] + yy;
                this.points[c][2] = v[2] + zz;
                this.radii[c] = v[3] * variance;
                if (c > this.vecs.size() - 10) {
                    this.radii[c] = this.radii[c] * Mth.cos((float) ((c - (this.vecs.size() - 12)) / 10.0F * (Math.PI / 2)));
                }
                if (c == 0) {
                    this.radii[c] = 0.0;
                } else if (c == 1) {
                    this.radii[c] = 0.0;
                } else if (c == 2) {
                    this.radii[c] = (this.particleScale * 0.5 + this.radii[c]) / 2.0;
                } else if (c == 3) {
                    this.radii[c] = (this.particleScale + this.radii[c]) / 2.0;
                } else if (c == 4) {
                    this.radii[c] = (this.particleScale + this.radii[c] * 2.0) / 3.0;
                }
                float v2 = 1.0F - Mth.sin((c + this.age) / 2.0F) * 0.1F;
                this.colours[c][0] = this.rCol * v2;
                this.colours[c][1] = this.gCol * v2;
                this.colours[c][2] = this.bCol * v2;
                this.colours[c][3] = 1.0F;
            }
            if (this.vecs.size() > 2 && this.random.nextBoolean()) {
                int q = this.random.nextInt(3);
                if (this.random.nextBoolean()) {
                    q = this.vecs.size() - 2;
                }
                double[] v = this.vecs.get(q);
                FXDispatcher.INSTANCE.essentiaDropFx(v[0] + this.startX, v[1] + this.startY, v[2] + this.startZ, this.rCol, this.gCol, this.bCol, 0.5F);
            }
        } else {
            this.remove();
            FXEssentiaStream cur = pt.get(this.key);
            if (cur != null && !cur.isAlive()) {
                pt.remove(this.key);
            }
        }
    }

    public void setGravity(float value) {
        this.gravity = value;
    }
}
