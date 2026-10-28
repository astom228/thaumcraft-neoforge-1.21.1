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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.particles.ICustomRenderParticle;
import thaumcraft.client.fx.particles.IEngineParticle;
import thaumcraft.client.lib.obj.WavefrontObject;

/**
 * Вспышка рунической защиты (порт FXShieldRunes): полусфера hemis.obj с анимированной текстурой (15 кадров) вокруг
 * сущности, повёрнутая к источнику удара; у монстров — «рябь» (ripple) вполсилы. Аддитивно, без отсечения граней.
 */
public class FXShieldRunes extends Particle implements IEngineParticle, ICustomRenderParticle {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "models/obj/hemis.obj");
    /** Модель грузится при первом показе (только клиент, главный поток). */
    private static WavefrontObject model;
    private static final ResourceLocation[] TEX1 = new ResourceLocation[15];
    private static final ResourceLocation[] TEX2 = new ResourceLocation[15];

    static {
        for (int a = 0; a < 15; a++) {
            TEX1[a] = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/ripple" + (a + 1) + ".png");
            TEX2[a] = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/hemis" + (a + 1) + ".png");
        }
    }

    private final Entity target;
    private final float yaw;
    private final float pitch;

    public FXShieldRunes(ClientLevel world, Entity target, int age, float yaw, float pitch) {
        super(world, target.getX(), (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0, target.getZ(), 0.0, 0.0, 0.0);
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.gravity = 0.0F;
        this.xd = this.yd = this.zd = 0.0;
        this.lifetime = age + this.random.nextInt(age / 2);
        this.setSize(0.01F, 0.01F);
        this.target = target;
        this.yaw = yaw;
        this.pitch = pitch;
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
        this.y = (this.target.getBoundingBox().minY + this.target.getBoundingBox().maxY) / 2.0;
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
        float b = 1.0F;
        int frame = Math.min(15, (int) (14.0F * fade) + 1);
        ResourceLocation tex;
        // TODO этап 16: кроме культистов (EntityCultist) — у них руны, как у игрока
        if (this.target instanceof Monster) {
            tex = TEX1[frame - 1];
            b = 0.5F;
        } else {
            tex = TEX2[frame - 1];
        }
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - this.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-this.pitch));
        float s = 0.2F * this.target.getBbHeight();
        pose.scale(s, s, s);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        model.renderAll(buf, pose.last().pose(), b, b, b, Math.min(1.0F, (1.0F - fade) * 3.0F));
        MeshData mesh = buf.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
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
