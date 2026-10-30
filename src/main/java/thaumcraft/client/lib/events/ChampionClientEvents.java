package thaumcraft.client.lib.events;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.common.entities.monster.mods.ChampionModifier;

/**
 * Клиентская часть чемпионов (livingTick и renderLivingPre RenderEventHandler оригинала): частицы модификатора каждый
 * тик и окраска модели (glColor4f в 1.12 — здесь множитель цвета вершин) (слой волокон заражения — ClientRegistration.addLayers).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ChampionClientEvents {
    /** Повторный вызов рендера с окрашенным буфером — только в потоке рендера. */
    private static boolean tinting = false;

    private static int championMod(LivingEntity e) {
        if (!(e instanceof PathfinderMob) || !e.isAlive()) {
            return -1;
        }
        Integer t = ThaumcraftApiHelper.getChampionMod(e);
        return t == null || t >= ChampionModifier.mods.length ? -1 : t;
    }

    @SubscribeEvent
    public static void livingTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide && event.getEntity() instanceof LivingEntity mob) {
            int t = championMod(mob);
            if (t >= 0) {
                ChampionModifier.mods[t].effect.showFX(mob);
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void renderLivingPre(RenderLivingEvent.Pre event) {
        if (tinting) {
            return;
        }
        LivingEntity mob = event.getEntity();
        int t = championMod(mob);
        if (t < 0) {
            return;
        }
        int tint = ChampionModifier.mods[t].effect.getTint();
        if (tint == 0xFFFFFF) {
            return;
        }
        event.setCanceled(true);
        MultiBufferSource src = event.getMultiBufferSource();
        float r = (tint >> 16 & 255) / 255.0F;
        float g = (tint >> 8 & 255) / 255.0F;
        float b = (tint & 255) / 255.0F;
        MultiBufferSource tinted = type -> new Tinted(src.getBuffer(type), r, g, b);
        float yaw = Mth.lerp(event.getPartialTick(), mob.yRotO, mob.getYRot());
        tinting = true;
        try {
            ((LivingEntityRenderer) event.getRenderer()).render(mob, yaw, event.getPartialTick(), event.getPoseStack(), tinted, event.getPackedLight());
        } finally {
            tinting = false;
        }
    }

    /** Буфер, умножающий цвет вершин на (r, g, b). */
    private record Tinted(VertexConsumer parent, float r, float g, float b) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.parent.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.parent.setColor((int) (red * this.r), (int) (green * this.g), (int) (blue * this.b), alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.parent.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.parent.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.parent.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.parent.setNormal(x, y, z);
            return this;
        }
    }
}
