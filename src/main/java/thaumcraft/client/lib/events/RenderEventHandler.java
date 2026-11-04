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
     * Ухо — нота; реле — «Out»/«In» над ручкой под прицелом (всегда, не только в очках).
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
                && mc.level.getBlockEntity(bh.getBlockPos()) instanceof thaumcraft.common.tiles.devices.TileRedstoneRelay relay) {
            int knob = thaumcraft.common.blocks.devices.BlockRedstoneRelay.knobAt(mc.level.getBlockState(bh.getBlockPos()), bh.getBlockPos(), bh.getLocation());
            net.minecraft.core.BlockPos rp = bh.getBlockPos();
            if (knob == 0) {
                drawTextInAir(event.getPoseStack(), event.getCamera().getPosition(), rp.getX(), rp.getY() + 0.3, rp.getZ(), "Out: " + relay.getOut(), false);
            } else if (knob == 1) {
                drawTextInAir(event.getPoseStack(), event.getCamera().getPosition(), rp.getX(), rp.getY() + 0.3, rp.getZ(), "In: " + relay.getIn(), false);
            }
        }
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
            if (te instanceof thaumcraft.common.tiles.devices.TileArcaneEar ear) {
                note = ear.note;
            }
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

    // --- печати (drawSeals / drawSealArea / renderSeal оригинала) ---
    private static final net.minecraft.resources.ResourceLocation CFRAME = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID,
            "textures/misc/frame_corner.png");
    private static final net.minecraft.resources.ResourceLocation MIDDLE = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID,
            "textures/misc/seal_area.png");
    private static final net.minecraft.core.Direction[][] ROTFACES = {
            {net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.WEST},
            {net.minecraft.core.Direction.UP, net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.WEST},
            {net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST},
            {net.minecraft.core.Direction.UP, net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST},
            {net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST},
            {net.minecraft.core.Direction.UP, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST},
            {net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST},
            {net.minecraft.core.Direction.UP, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST}};
    private static final int[][] ROTMAT = {{0, 270, 0}, {270, 180, 270}, {90, 0, 90}, {180, 90, 180}, {180, 180, 0}, {90, 270, 270}, {270, 90, 90}, {0, 0, 180}};

    /** Смена измерения или возрождение: печати прежнего мира больше не видны (в оригинале карты были по измерениям). */
    @SubscribeEvent
    public static void clearSealsOnClone(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.Clone event) {
        thaumcraft.client.lib.ClientSeals.clear();
        EssentiaSourceFXClient.clear();
        WarpFog.clear();
        thaumcraft.client.fx.ParticleEngine.clearParticles();
    }

    /** С печатью или предметом ISealDisplayer в руке видны печати в 16 блоках и их зоны; крадучись — сквозь блоки. */
    @SubscribeEvent
    public static void drawSeals(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getCameraEntity() instanceof Player player) || mc.level == null) {
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof thaumcraft.api.golems.ISealDisplayer)
                && !(player.getOffhandItem().getItem() instanceof thaumcraft.api.golems.ISealDisplayer)) {
            return;
        }
        java.util.Collection<thaumcraft.common.entities.construct.golem.seals.SealEntity> seals = thaumcraft.client.lib.ClientSeals.all();
        if (seals.isEmpty()) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        net.minecraft.world.phys.Vec3 cam = event.getCamera().getPosition();
        org.joml.Matrix4fStack modelView = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        // AFTER_PARTICLES вызывается с уже применённой LevelRenderer матрицей камеры.
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        boolean sneak = player.isShiftKeyDown();
        if (sneak) {
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (thaumcraft.api.golems.seals.ISealEntity seal : seals) {
            net.minecraft.core.BlockPos sp = seal.getSealPos().pos;
            double dis = player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(sp));
            if (dis > 256.0 || seal.getSeal() == null) {
                continue;
            }
            float alpha = 1.0F - (float) (dis / 256.0);
            boolean ia = false;
            net.minecraft.core.Direction face = seal.getSealPos().face;
            if (seal.isStoppedByRedstone(mc.level)) {
                ia = true;
                if (mc.level.random.nextFloat() < pt / 12.0F) {
                    thaumcraft.common.lib.fx.IFXDispatcher.get().spark(sp.getX() + 0.5F + face.getStepX() * 0.66F, sp.getY() + 0.5F + face.getStepY() * 0.66F,
                            sp.getZ() + 0.5F + face.getStepZ() * 0.66F, 2.0F, 0.8F - mc.level.random.nextFloat() * 0.2F, 0.0F, 0.0F, 1.0F);
                    ia = false;
                }
            }
            renderSeal(pose, sp, alpha, face, seal.getSeal().getSealIcon(), ia, sneak);
            drawSealArea(pose, player, seal, alpha, pt);
        }
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        modelView.popMatrix();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }

    private static void rotGL(com.mojang.blaze3d.vertex.PoseStack pose, float deg, float x, float y, float z) {
        org.joml.Vector3f axis = new org.joml.Vector3f(x, y, z);
        if (deg == 0.0F || axis.lengthSquared() == 0.0F) {
            return;
        }
        pose.mulPose(new org.joml.Quaternionf().rotationAxis((float) Math.toRadians(deg), axis.normalize()));
    }

    /** renderQuadCentered(texture, scale, r, g, b, 200, 771, a): квадрат по центру с полной текстурой (TexturedQuadTC). */
    private static void quadCentered(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.resources.ResourceLocation tex, float scale, float r, float g,
            float b, float a) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, tex);
        org.joml.Matrix4f m = pose.last().pose();
        float h = 0.5F * scale;
        com.mojang.blaze3d.vertex.BufferBuilder bb = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(m, -h, h, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, a);
        bb.addVertex(m, h, h, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, a);
        bb.addVertex(m, h, -h, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, a);
        bb.addVertex(m, -h, -h, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, a);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    private static void drawSealArea(com.mojang.blaze3d.vertex.PoseStack pose, Player player, thaumcraft.api.golems.seals.ISealEntity seal, float alpha,
            float pt) {
        net.minecraft.core.BlockPos sp = seal.getSealPos().pos;
        net.minecraft.core.Direction face = seal.getSealPos().face;
        float r;
        float g;
        float b;
        if (seal.getColor() > 0) {
            int c = net.minecraft.world.item.DyeColor.byId(seal.getColor() - 1).getTextureDiffuseColor();
            r = (c >> 16 & 255) / 255.0F;
            g = (c >> 8 & 255) / 255.0F;
            b = (c & 255) / 255.0F;
        } else {
            r = 0.7F + net.minecraft.util.Mth.sin((player.tickCount + pt + sp.getX()) / 4.0F) * 0.1F;
            g = 0.7F + net.minecraft.util.Mth.sin((player.tickCount + pt + sp.getY()) / 5.0F) * 0.1F;
            b = 0.7F + net.minecraft.util.Mth.sin((player.tickCount + pt + sp.getZ()) / 6.0F) * 0.1F;
        }
        pose.pushPose();
        pose.translate(sp.getX() + 0.5, sp.getY() + 0.5, sp.getZ() + 0.5);
        rotGL(pose, 90.0F, -face.getStepY(), face.getStepX(), -face.getStepZ());
        pose.translate(0.0, 0.0, face.getStepZ() < 0 ? -0.51F : 0.51F);
        rotGL(pose, player.tickCount % 360 + pt, 0.0F, 0.0F, 1.0F);
        quadCentered(pose, MIDDLE, 0.9F, r, g, b, alpha * 0.8F);
        pose.popPose();
        if (seal.getSeal() instanceof thaumcraft.api.golems.seals.ISealConfigArea) {
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            net.minecraft.core.BlockPos ar = seal.getArea();
            net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(sp).move(face.getStepX(), face.getStepY(), face.getStepZ())
                    .expandTowards(face.getStepX() != 0 ? (ar.getX() - 1) * face.getStepX() : 0.0, face.getStepY() != 0 ? (ar.getY() - 1) * face.getStepY() : 0.0,
                            face.getStepZ() != 0 ? (ar.getZ() - 1) * face.getStepZ() : 0.0)
                    .inflate(face.getStepX() == 0 ? ar.getX() - 1 : 0.0, face.getStepY() == 0 ? ar.getY() - 1 : 0.0, face.getStepZ() == 0 ? ar.getZ() - 1 : 0.0);
            double[][] locs = {{area.minX, area.minY, area.minZ}, {area.minX, area.maxY - 1.0, area.minZ}, {area.maxX - 1.0, area.minY, area.minZ},
                    {area.maxX - 1.0, area.maxY - 1.0, area.minZ}, {area.maxX - 1.0, area.minY, area.maxZ - 1.0}, {area.maxX - 1.0, area.maxY - 1.0, area.maxZ - 1.0},
                    {area.minX, area.minY, area.maxZ - 1.0}, {area.minX, area.maxY - 1.0, area.maxZ - 1.0}};
            for (int q = 0; q < locs.length; q++) {
                pose.pushPose();
                pose.translate(locs[q][0] + 0.5, locs[q][1] + 0.5, locs[q][2] + 0.5);
                for (int w = 0; w < 3; w++) {
                    net.minecraft.core.Direction f = ROTFACES[q][w];
                    pose.pushPose();
                    rotGL(pose, 90.0F, -f.getStepY(), f.getStepX(), -f.getStepZ());
                    pose.translate(0.0, 0.0, f.getStepZ() < 0 ? -0.49F : 0.49F);
                    rotGL(pose, 90.0F, 0.0F, 0.0F, -1.0F);
                    rotGL(pose, ROTMAT[q][w], 0.0F, 0.0F, 1.0F);
                    quadCentered(pose, CFRAME, 1.0F, r, g, b, alpha * 0.7F);
                    pose.popPose();
                }
                pose.popPose();
            }
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        }
    }

    /** Значок печати на грани блока: объёмный (толщина 0.1), крадучись — плоский; выключенная красным камнем — темнее. */
    private static void renderSeal(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.core.BlockPos p, float alpha, net.minecraft.core.Direction face,
            net.minecraft.resources.ResourceLocation icon, boolean ia, boolean sneak) {
        if (icon == null) {
            return;
        }
        pose.pushPose();
        float x = p.getX();
        float y = p.getY();
        float z = p.getZ();
        switch (face) {
            case UP -> {
                pose.translate(x + 0.25F, y + 1.0F, z + 0.75F);
                rotGL(pose, -90.0F, 1.0F, 0.0F, 0.0F);
            }
            case DOWN -> {
                pose.translate(x + 0.25F, y, z + 0.25F);
                rotGL(pose, 90.0F, 1.0F, 0.0F, 0.0F);
            }
            case SOUTH -> pose.translate(x + 0.25F, y + 0.25F, z + 1.0F);
            case NORTH -> {
                pose.translate(x + 0.75F, y + 0.25F, z);
                rotGL(pose, 180.0F, 0.0F, 1.0F, 0.0F);
            }
            case EAST -> {
                pose.translate(x + 1.0F, y + 0.25F, z + 0.75F);
                rotGL(pose, 90.0F, 0.0F, 1.0F, 0.0F);
            }
            case WEST -> {
                pose.translate(x, y + 0.25F, z + 0.25F);
                rotGL(pose, -90.0F, 0.0F, 1.0F, 0.0F);
            }
        }
        pose.translate(0.0F, 0.0F, 0.05F);
        pose.scale(0.5F, 0.5F, 0.5F);
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(icon);
        float c = ia ? 0.5F : 1.0F;
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
        com.mojang.blaze3d.vertex.BufferBuilder bb = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR);
        item2D(bb, pose.last().pose(), sprite.getU1(), sprite.getV0(), sprite.getU0(), sprite.getV1(), 16, 16, sneak ? 0.0F : 0.1F, c, c, c, alpha);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(bb.buildOrThrow());
        pose.popPose();
    }

    private static void vt(com.mojang.blaze3d.vertex.BufferBuilder bb, org.joml.Matrix4f m, float x, float y, float z, float u, float v, float r, float g,
            float b, float a) {
        bb.addVertex(m, x, y, z).setUv(u, v).setColor(r, g, b, a);
    }

    /** renderTextureIn3D оригинала с цветом (без освещения). */
    private static void item2D(com.mojang.blaze3d.vertex.BufferBuilder bb, org.joml.Matrix4f m, float maxu, float maxv, float minu, float minv, int width,
            int height, float t, float r, float g, float b, float a) {
        vt(bb, m, 0, 0, 0, maxu, minv, r, g, b, a);
        vt(bb, m, 1, 0, 0, minu, minv, r, g, b, a);
        vt(bb, m, 1, 1, 0, minu, maxv, r, g, b, a);
        vt(bb, m, 0, 1, 0, maxu, maxv, r, g, b, a);
        if (t <= 0.0F) {
            return;
        }
        vt(bb, m, 0, 1, -t, maxu, maxv, r, g, b, a);
        vt(bb, m, 1, 1, -t, minu, maxv, r, g, b, a);
        vt(bb, m, 1, 0, -t, minu, minv, r, g, b, a);
        vt(bb, m, 0, 0, -t, maxu, minv, r, g, b, a);
        float f5 = 0.5F * (maxu - minu) / width;
        float f6 = 0.5F * (minv - maxv) / height;
        for (int k = 0; k < width; k++) {
            float f7 = (float) k / width;
            float f8 = maxu + (minu - maxu) * f7 - f5;
            float f9 = f7 + 1.0F / width;
            vt(bb, m, f7, 0, -t, f8, minv, r, g, b, a);
            vt(bb, m, f7, 0, 0, f8, minv, r, g, b, a);
            vt(bb, m, f7, 1, 0, f8, maxv, r, g, b, a);
            vt(bb, m, f7, 1, -t, f8, maxv, r, g, b, a);
            vt(bb, m, f9, 1, -t, f8, maxv, r, g, b, a);
            vt(bb, m, f9, 1, 0, f8, maxv, r, g, b, a);
            vt(bb, m, f9, 0, 0, f8, minv, r, g, b, a);
            vt(bb, m, f9, 0, -t, f8, minv, r, g, b, a);
        }
        for (int k = 0; k < height; k++) {
            float f7 = (float) k / height;
            float f8 = minv + (maxv - minv) * f7 - f6;
            float f9 = f7 + 1.0F / height;
            vt(bb, m, 0, f9, 0, maxu, f8, r, g, b, a);
            vt(bb, m, 1, f9, 0, minu, f8, r, g, b, a);
            vt(bb, m, 1, f9, -t, minu, f8, r, g, b, a);
            vt(bb, m, 0, f9, -t, maxu, f8, r, g, b, a);
            vt(bb, m, 1, f7, 0, minu, f8, r, g, b, a);
            vt(bb, m, 0, f7, 0, maxu, f8, r, g, b, a);
            vt(bb, m, 0, f7, -t, maxu, f8, r, g, b, a);
            vt(bb, m, 1, f7, -t, minu, f8, r, g, b, a);
        }
    }
}
