package thaumcraft.client.fx.other;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.particles.ICustomRenderParticle;
import thaumcraft.client.fx.particles.IEngineParticle;
import thaumcraft.client.lib.obj.WavefrontObject;

/**
 * Звуковая волна вопля (порт FXSonic): вытянутая полусфера hemis.obj с «рябью» (ripple1–15) перед лицом источника,
 * следует за ним; яркость 0.5, обычное смешивание, без записи глубины.
 */
public class FXSonic extends Particle implements IEngineParticle, ICustomRenderParticle {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "models/obj/hemis.obj");
    private static WavefrontObject model;
    private static final ResourceLocation[] TEX = new ResourceLocation[15];

    static {
        for (int a = 0; a < 15; a++) {
            TEX[a] = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/ripple" + (a + 1) + ".png");
        }
    }

    private final Entity target;
    private final float yaw;
    private final float pitch;

    public FXSonic(ClientLevel world, Entity target, int age) {
        super(world, target.getX(), target.getY() + target.getEyeHeight(), target.getZ(), 0.0, 0.0, 0.0);
        this.gravity = 0.0F;
        this.xd = this.yd = this.zd = 0.0;
        this.lifetime = age + this.random.nextInt(age / 2);
        this.setSize(0.01F, 0.01F);
        this.target = target;
        this.yaw = target instanceof LivingEntity le ? le.getYHeadRot() : target.getYRot();
        this.pitch = target.getXRot();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.x = this.target.getX();
        this.y = this.target.getY() + this.target.getEyeHeight();
        this.z = this.target.getZ();
    }

    @Override
    public void renderCustom(PoseStack pose, Camera camera, float f) {
        if (model == null) {
            model = WavefrontObject.load(MODEL);
        }
        float fade = (this.age + f) / this.lifetime;
        Vec3 cam = camera.getPosition();
        pose.pushPose();
        pose.translate(Mth.lerp(f, this.xo, this.x) - cam.x, Mth.lerp(f, this.yo, this.y) - cam.y, Mth.lerp(f, this.zo, this.z) - cam.z);
        int frame = Math.min(15, (int) (14.0F * fade) + 1);
        pose.mulPose(Axis.YP.rotationDegrees(-this.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(this.pitch));
        float h = this.target.getBbHeight();
        pose.translate(0.0, 0.0, 2.0F * h + this.target.getBbWidth() / 2.0F);
        pose.scale(0.25F * h, 0.25F * h, -1.0F * h);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEX[frame - 1]);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        model.renderAll(buf, pose.last().pose(), 0.5F, 0.5F, 0.5F, 1.0F);
        MeshData mesh = buf.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
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

    @Override
    public int getFXLayer() {
        return 0;
    }
}
