package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Частица интерфейса (порт FXGenericGui): координаты x, y — пиксели экрана, z — глубина поверх интерфейса.
 * Рисуется слоями 4–5 ParticleEngine в ортопроекции; исчезает, если открыт экран при создании и игрок вернулся в игру.
 * Столкновения с миром отключены: в оригинале частица проверяла блоки мира по экранным координатам (у начала мира).
 */
public class FXGenericGui extends FXGeneric {
    boolean inGame;
    private final Matrix4f scratch = new Matrix4f();

    public FXGenericGui(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
        this.inGame = Minecraft.getInstance().mouseHandler.isMouseGrabbed();
        this.hasPhysics = false;
    }

    public FXGenericGui(ClientLevel world, double x, double y, double z, double xx, double yy, double zz) {
        super(world, x, y, z, xx, yy, zz);
        this.inGame = Minecraft.getInstance().mouseHandler.isMouseGrabbed();
        this.hasPhysics = false;
    }

    @Override
    public void setNoClip(boolean clip) {
        // столкновения всегда отключены (см. описание класса)
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.inGame && Minecraft.getInstance().mouseHandler.isMouseGrabbed()) {
            this.remove();
        }
    }

    /** Буфер — POSITION_TEX_COLOR (шейдер position_tex_color), камера не используется. */
    @Override
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
        float f5 = (float) Mth.lerp(partialTicks, this.xo, this.x);
        float f6 = (float) Mth.lerp(partialTicks, this.yo, this.y);
        float f7 = (float) Mth.lerp(partialTicks, this.zo, this.z);
        Matrix4f m = this.scratch.identity().translate(f5, f6, -90.0F + f7);
        if (this.angled) {
            m.rotateY((float) Math.toRadians(-this.angleYaw + 90.0F));
            m.rotateX((float) Math.toRadians(this.anglePitch + 90.0F));
        }
        if (this.roll != 0.0F) {
            // как в оригинале: angle + (angle - prevAngle) * partialTicks
            m.rotateZ(this.roll + (this.roll - this.oRoll) * partialTicks);
        }
        wr.addVertex(m, -ts, -ts, 0.0F).setUv(tx2, ty2).setColor(pr, pg, pb, this.alpha);
        wr.addVertex(m, -ts, ts, 0.0F).setUv(tx2, ty1).setColor(pr, pg, pb, this.alpha);
        wr.addVertex(m, ts, ts, 0.0F).setUv(tx1, ty1).setColor(pr, pg, pb, this.alpha);
        wr.addVertex(m, ts, -ts, 0.0F).setUv(tx1, ty2).setColor(pr, pg, pb, this.alpha);
    }
}
