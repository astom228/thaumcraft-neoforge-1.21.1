package thaumcraft.client.lib.events;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.UtilsFX;

/**
 * Порт RenderEventHandler (части переносятся по этапам): тик эффектов HUD и отрисовка поверх кадра
 * (RenderTickEvent END оригинала → RenderFrameEvent.Post): HUD и частицы интерфейса; подсказки очков откровения над блоком
 * под прицелом (blockHighlight оригинала → этап отрисовки мира).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class RenderEventHandler {
    /** Масштаб значков аспектов над контейнером: растёт, пока смотришь на него в очках, и тает каждый кадр (только клиент). */
    public static float tagscale = 0.0F;

    @SubscribeEvent
    public static void clientWorldTick(ClientTickEvent.Post event) {
        HudHandler.tickKnowledgeGains();
    }

    @SubscribeEvent
    public static void renderTick(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Window win = mc.getWindow();
        UtilsFX.beginFrameOverlay();
        if (mc.getCameraEntity() instanceof Player player) {
            HudHandler.renderHuds(mc, partialTicks, player, win.getGuiScaledWidth(), win.getGuiScaledHeight());
        }
        ParticleEngine.renderGuiParticles(partialTicks);
        UtilsFX.endFrameOverlay();
    }

    /**
     * blockHighlight оригинала (часть очков откровения) + убывание tagscale из renderLast. В очках над блоком под прицелом:
     * аспекты контейнера (IAspectContainer), стабильность (IStabilizable), нота нотного блока.
     * TODO этап 14: нота магического уха (TileArcaneEar); реле редстоуна (TileRedstoneRelay) — In/Out.
     */
    @SubscribeEvent
    public static void blockHighlight(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.getCameraEntity() instanceof Player player
                && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bh
                && bh.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && thaumcraft.common.lib.utils.EntityUtils.hasGoggles(player)) {
            com.mojang.blaze3d.vertex.PoseStack pose = event.getPoseStack();
            net.minecraft.world.phys.Vec3 cam = event.getCamera().getPosition();
            net.minecraft.core.BlockPos p = bh.getBlockPos();
            boolean spaceAbove = mc.level.isEmptyBlock(p.above());
            net.minecraft.world.level.block.entity.BlockEntity te = mc.level.getBlockEntity(p);
            net.minecraft.world.level.block.state.BlockState bs = mc.level.getBlockState(p);
            // в 1.12 нота читалась из TileEntityNote (с запросом PacketNote к серверу); в 1.21 она в состоянии блока
            int note = bs.getBlock() instanceof net.minecraft.world.level.block.NoteBlock ? bs.getValue(net.minecraft.world.level.block.NoteBlock.NOTE) : -1;
            if (note < 0 && te instanceof thaumcraft.api.aspects.IAspectContainer ac && ac.getAspects() != null && ac.getAspects().size() > 0) {
                if (tagscale < 0.3F) {
                    tagscale = tagscale + (0.031F - tagscale / 10.0F);
                }
                drawTagsOnContainer(pose, cam, p.getX(), p.getY() + (spaceAbove ? 0.4F : 0.0F), p.getZ(), ac.getAspects(), 220,
                        spaceAbove ? net.minecraft.core.Direction.UP : bh.getDirection());
            }
            if (te instanceof thaumcraft.api.crafting.IStabilizable st) {
                String text = net.minecraft.client.resources.language.I18n.get("stability." + st.getStability().name());
                drawTextInAir(pose, cam, p.getX(), p.getY() + 0.1, p.getZ(), text, true);
            }
            if (note >= 0) {
                drawTextInAir(pose, cam, p.getX(), p.getY() + 1, p.getZ(), "Note: " + note, false);
            }
        }
        if (tagscale > 0.0F) {
            tagscale -= 0.005F;
        }
    }

    /** Значки аспектов рядами по 5 перед гранью dir, повёрнутые к игроку, с числами (порт drawTagsOnContainer). Без глубины. */
    public static void drawTagsOnContainer(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.world.phys.Vec3 cam, double x, double y, double z,
                                           thaumcraft.api.aspects.AspectList tags, int bright, net.minecraft.core.Direction dir) {
        if (tags == null || tags.size() <= 0 || dir == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int rowsize = 5;
        int current = 0;
        float shifty = 0.0F;
        int left = tags.size();
        for (thaumcraft.api.aspects.Aspect tag : tags.getAspects()) {
            int div = Math.min(left, rowsize);
            if (current >= rowsize) {
                current = 0;
                shifty -= tagscale * 1.05F;
                left -= rowsize;
                if (left < rowsize) {
                    div = left % rowsize;
                }
            }
            float shift = (current - div / 2.0F + 0.5F) * tagscale * 4.0F;
            shift *= tagscale;
            java.awt.Color color = new java.awt.Color(tag.getColor());
            pose.pushPose();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            pose.translate(-cam.x + x + 0.5 + tagscale * 2.0F * dir.getStepX(), -cam.y + y - shifty + 0.5 + tagscale * 2.0F * dir.getStepY(),
                    -cam.z + z + 0.5 + tagscale * 2.0F * dir.getStepZ());
            float xd = (float) (cam.x - (x + 0.5));
            float zd = (float) (cam.z - (z + 0.5));
            float rotYaw = (float) (Math.atan2(xd, zd) * 180.0 / Math.PI);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotYaw + 180.0F));
            pose.translate(shift, 0.0, 0.0);
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            pose.scale(tagscale, tagscale, tagscale);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, tag.getImage());
            UtilsFX.renderQuadCentered(pose, 1, 1, 0, 1.0F, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, bright, 771, 0.75F);
            if (tags.getAmount(tag) >= 0) {
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
                String am = "" + tags.getAmount(tag);
                pose.scale(0.04F, 0.04F, 0.04F);
                pose.translate(0.0, 6.0, -0.1);
                int sw = mc.font.width(am);
                net.minecraft.client.renderer.MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
                mc.font.drawInBatch(am, 14 - sw, 1, 0xFF111111, false, pose.last().pose(), buf,
                        net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                pose.translate(0.0, 0.0, -0.1);
                mc.font.drawInBatch(am, 13 - sw, 0, 0xFFFFFFFF, false, pose.last().pose(), buf,
                        net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                buf.endBatch();
            }
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            pose.popPose();
            current++;
        }
    }

    /** Строка над блоком, повёрнутая к игроку (порт drawTextInAir); seeThrough — сквозь блоки (glDisable(GL_DEPTH_TEST) оригинала). */
    public static void drawTextInAir(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.world.phys.Vec3 cam, double x, double y, double z, String text,
                                     boolean seeThrough) {
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(-cam.x + x + 0.5, -cam.y + y + 0.5, -cam.z + z + 0.5);
        float xd = (float) (cam.x - (x + 0.5));
        float zd = (float) (cam.z - (z + 0.5));
        float rotYaw = (float) (Math.atan2(xd, zd) * 180.0 / Math.PI);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotYaw + 180.0F));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        pose.scale(0.02F, 0.02F, 0.02F);
        int sw = mc.font.width(text);
        net.minecraft.client.gui.Font.DisplayMode mode = seeThrough ? net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH
                : net.minecraft.client.gui.Font.DisplayMode.NORMAL;
        net.minecraft.client.renderer.MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        mc.font.drawInBatch(text, 1 - sw / 2, 1, 0xFF111111, false, pose.last().pose(), buf, mode, 0, 0xF000F0);
        pose.translate(0.0, 0.0, -0.1);
        mc.font.drawInBatch(text, -sw / 2, 0, 0xFFFFFFFF, false, pose.last().pose(), buf, mode, 0, 0xF000F0);
        buf.endBatch();
        pose.popPose();
    }
}
