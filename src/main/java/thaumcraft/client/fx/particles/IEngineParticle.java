package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;

/** Частица, которую рисует ParticleEngine Thaumcraft (в оригинале — любой Particle со своим renderParticle). */
public interface IEngineParticle {
    /** Слой движка: 0–3 — в мире, 4–5 — поверх интерфейса. */
    int getFXLayer();

    /** Вершины в буфер слоя (в мире — формат PARTICLE, в интерфейсе — POSITION_TEX_COLOR; camera там null). */
    void renderParticle(VertexConsumer buffer, Camera camera, float partialTicks);
}
