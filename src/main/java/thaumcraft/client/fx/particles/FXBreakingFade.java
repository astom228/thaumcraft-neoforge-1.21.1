package thaumcraft.client.fx.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Осколок предмета, тающий к концу жизни (порт FXBreakingFade — ParticleBreaking с прозрачностью × (1 − возраст/жизнь)):
 * кусочек ¼×¼ значка предмета, ванильный движок частиц (полупрозрачный лист).
 */
public class FXBreakingFade extends TextureSheetParticle {
    private final float uo;
    private final float vo;

    public FXBreakingFade(ClientLevel world, double x, double y, double z, ItemStack stack) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setSprite(Minecraft.getInstance().getItemRenderer().getModel(stack, world, null, 0).getParticleIcon());
        this.gravity = 1.0F;
        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
    }

    public void setParticleMaxAge(int age) {
        this.lifetime = age;
    }

    public void setRBGColorF(float r, float g, float b) {
        this.setColor(r, g, b);
    }

    public void setAlphaF(float a) {
        this.alpha = a;
    }

    public void setSpeed(double x, double y, double z) {
        this.xd = x;
        this.yd = y;
        this.zd = z;
    }

    /** Разлёт: скорость нормируется к 0.15·(1…3) и чуть вверх. */
    public void boom() {
        float f = (float) (Math.random() + Math.random() + 1.0) * 0.15F;
        float f1 = Mth.sqrt((float) (this.xd * this.xd + this.yd * this.yd + this.zd * this.zd));
        this.xd = this.xd / f1 * f * 0.9640000000596046;
        this.yd = this.yd / f1 * f * 0.9640000000596046 + 0.1F;
        this.zd = this.zd / f1 * f * 0.9640000000596046;
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float pt) {
        float a = this.alpha;
        this.alpha = a * (1.0F - (float) this.age / this.lifetime);
        super.render(buffer, camera, pt);
        this.alpha = a;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
