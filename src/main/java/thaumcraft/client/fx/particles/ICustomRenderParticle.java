package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

/**
 * Частица движка, рисующая себя сама (своя текстура и геометрия) — в 1.12 такие частицы сбрасывали общий буфер
 * (Tessellator.draw) и рисовали напрямую. Вызывается после пачки своего слоя; блендинг и глубина — слоя.
 */
public interface ICustomRenderParticle {
    /** pose — смещение от камеры уже не применено: координаты считать относительно camera.getPosition(). */
    void renderCustom(PoseStack pose, Camera camera, float partialTicks);
}
