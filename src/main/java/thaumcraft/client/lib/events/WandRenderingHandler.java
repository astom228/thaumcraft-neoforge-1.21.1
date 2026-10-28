package thaumcraft.client.lib.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.items.IArchitect;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.casters.ItemFocusPouch;
import thaumcraft.common.lib.network.misc.PacketFocusChangeToServer;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Порт WandRenderingHandler: радиальное меню фокусов (пока зажата клавиша смены фокуса — фокусы из инвентаря, сумок и
 * пояса по кругу, курсор отпущен; наведение и отпускание клавиши или щелчок — выбор) и подсветка блоков «Плана»
 * (рамки и стрелки осей; в 1.12 рисовалась после кадра, здесь — на этапе отрисовки мира). Только клиент.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class WandRenderingHandler {
    static float radialHudScale = 0.0F;
    static final TreeMap<String, Integer> foci = new TreeMap<>();
    static final HashMap<String, ItemStack> fociItem = new HashMap<>();
    static final HashMap<String, Boolean> fociHover = new HashMap<>();
    static final HashMap<String, Float> fociScale = new HashMap<>();
    static long lastTime = 0L;
    static boolean lastState = false;
    static final ResourceLocation R1 = rl("textures/misc/radial.png");
    static final ResourceLocation R2 = rl("textures/misc/radial2.png");
    static int lastArcHash = 0;
    static List<BlockPos> architectBlocks = new ArrayList<>();
    static final HashMap<BlockPos, boolean[]> bmCache = new HashMap<>();
    static final ResourceLocation CFRAME = rl("textures/misc/frame_corner.png");
    static final ResourceLocation SFRAME = rl("textures/misc/frame_side.png");
    static final int[][] MOS = {{4, 5, 6, 7}, {0, 1, 2, 3}, {0, 1, 4, 5}, {2, 3, 6, 7}, {0, 2, 4, 6}, {1, 3, 5, 7}};
    static final int[][] ROTMAT = {{0, 90, 270, 180}, {270, 180, 0, 90}, {180, 90, 270, 0}, {0, 270, 90, 180}, {270, 180, 0, 90}, {180, 270, 90, 0}};
    static final ResourceLocation ARROWS = rl("textures/misc/architect_arrows.png");

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, p);
    }

    public static void clear() {
        foci.clear();
        fociItem.clear();
        fociHover.clear();
        fociScale.clear();
        radialHudScale = 0.0F;
        architectBlocks = new ArrayList<>();
        bmCache.clear();
    }

    private static void addPouch(ItemStack pouchStack, int pouchcount) {
        if (!(pouchStack.getItem() instanceof ItemFocusPouch pouch)) {
            return;
        }
        var inv = pouch.getInventory(pouchStack);
        for (int q = 0; q < inv.size(); q++) {
            ItemStack item = inv.get(q);
            if (item.getItem() instanceof ItemFocus f) {
                String sh = f.getSortingHelper(item);
                if (sh != null) {
                    foci.put(sh, q + pouchcount * 1000);
                    fociItem.put(sh, item.copy());
                    fociScale.put(sh, 1.0F);
                    fociHover.put(sh, false);
                }
            }
        }
    }

    /** handleFociRadial оригинала (слой интерфейса поверх всего). */
    public static void renderRadialLayer(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !KeyHandler.radialActive && radialHudScale <= 0.0F) {
            return;
        }
        long time = System.nanoTime() / 1000000L;
        if (!KeyHandler.radialActive) {
            if (mc.screen == null && lastState) {
                if (!mc.mouseHandler.isMouseGrabbed()) {
                    mc.mouseHandler.grabMouse();
                }
                lastState = false;
            }
        } else {
            if (mc.screen != null) {
                KeyHandler.radialActive = false;
                KeyHandler.radialLock = true;
                return;
            }
            if (radialHudScale == 0.0F) {
                foci.clear();
                fociItem.clear();
                fociHover.clear();
                fociScale.clear();
                int pouchcount = 0;
                var baubles = CuriosApi.getCuriosInventory(mc.player).map(h -> h.getEquippedCurios()).orElse(null);
                if (baubles != null) {
                    for (int a = 0; a < baubles.getSlots(); a++) {
                        if (baubles.getStackInSlot(a).getItem() instanceof ItemFocusPouch) {
                            addPouch(baubles.getStackInSlot(a), ++pouchcount);
                        }
                    }
                }
                for (int a = 0; a < 36; a++) {
                    ItemStack item = mc.player.getInventory().items.get(a);
                    if (item.getItem() instanceof ItemFocus f) {
                        String sh = f.getSortingHelper(item);
                        if (sh == null) {
                            continue;
                        }
                        foci.put(sh, a);
                        fociItem.put(sh, item.copy());
                        fociScale.put(sh, 1.0F);
                        fociHover.put(sh, false);
                    }
                    if (item.getItem() instanceof ItemFocusPouch) {
                        addPouch(item, ++pouchcount);
                    }
                }
                if (!foci.isEmpty() && mc.mouseHandler.isMouseGrabbed()) {
                    mc.mouseHandler.releaseMouse();
                }
            }
        }
        renderFocusRadialHUD(mc, g, g.guiWidth(), g.guiHeight(), delta.getGameTimeDeltaPartialTick(true));
        if (time > lastTime) {
            for (String key : fociHover.keySet()) {
                if (fociHover.get(key)) {
                    if (!KeyHandler.radialActive && !KeyHandler.radialLock) {
                        PacketDistributor.sendToServer(new PacketFocusChangeToServer(key));
                        KeyHandler.radialLock = true;
                    }
                    if (fociScale.get(key) < 1.3F) {
                        fociScale.put(key, fociScale.get(key) + 0.025F);
                    }
                } else if (fociScale.get(key) > 1.0F) {
                    fociScale.put(key, fociScale.get(key) - 0.025F);
                }
            }
            if (!KeyHandler.radialActive) {
                radialHudScale -= 0.05F;
            } else if (radialHudScale < 1.0F) {
                radialHudScale += 0.05F;
            }
            if (radialHudScale > 1.0F) {
                radialHudScale = 1.0F;
            }
            if (radialHudScale < 0.0F) {
                radialHudScale = 0.0F;
                KeyHandler.radialLock = false;
            }
            lastTime = time + 5L;
            lastState = KeyHandler.radialActive;
        }
    }

    private static void renderFocusRadialHUD(Minecraft mc, GuiGraphics g, double sw, double sh, float partialTicks) {
        ItemStack s = mc.player.getMainHandItem();
        if (!(s.getItem() instanceof ICaster)) {
            s = mc.player.getOffhandItem();
        }
        if (!(s.getItem() instanceof ICaster wand) || fociItem.isEmpty()) {
            return;
        }
        var win = mc.getWindow();
        int i = (int) (mc.mouseHandler.xpos() * sw / win.getScreenWidth());
        int j = (int) (mc.mouseHandler.ypos() * sh / win.getScreenHeight());
        boolean click = GLFW.glfwGetMouseButton(win.getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(sw / 2.0, sh / 2.0, 0.0);
        ItemStack tt = null;
        float width = 16.0F + fociItem.size() * 2.5F;
        float rot = partialTicks + mc.player.tickCount % 720 / 2.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(rot));
        quad(g, R1, width * 2.75F * radialHudScale, 0.5F, 0.5F, 0.5F, 0.5F);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-rot));
        quad(g, R2, width * 2.55F * radialHudScale, 0.5F, 0.5F, 0.5F, 0.5F);
        pose.popPose();
        if (wand.getFocus(s) != null) {
            ItemStack item = wand.getFocusStack(s).copy();
            g.renderItem(item, -8, -8);
            int mx = (int) (i - sw / 2.0);
            int my = (int) (j - sh / 2.0);
            if (mx >= -10 && mx <= 10 && my >= -10 && my <= 10) {
                tt = item;
            }
        }
        pose.scale(radialHudScale, radialHudScale, radialHudScale);
        float currentRot = -90.0F * radialHudScale;
        float pieSlice = 360.0F / fociItem.size();
        String key = foci.firstKey();
        for (int a = 0; a < fociItem.size() && key != null; a++) {
            double xx = Mth.cos(currentRot / 180.0F * (float) Math.PI) * width;
            double yy = Mth.sin(currentRot / 180.0F * (float) Math.PI) * width;
            currentRot += pieSlice;
            pose.pushPose();
            pose.translate(xx, yy, 100.0);
            float sc = fociScale.get(key);
            pose.scale(sc, sc, sc);
            g.renderItem(fociItem.get(key), -8, -8);
            pose.popPose();
            if (!KeyHandler.radialLock && KeyHandler.radialActive) {
                int mx = (int) (i - sw / 2.0 - xx);
                int my = (int) (j - sh / 2.0 - yy);
                if (mx >= -10 && mx <= 10 && my >= -10 && my <= 10) {
                    fociHover.put(key, true);
                    tt = fociItem.get(key);
                    if (click) {
                        KeyHandler.radialActive = false;
                        KeyHandler.radialLock = true;
                        PacketDistributor.sendToServer(new PacketFocusChangeToServer(key));
                        break;
                    }
                } else {
                    fociHover.put(key, false);
                }
            }
            key = foci.higherKey(key);
        }
        pose.popPose();
        if (tt != null) {
            List<String> lines = new ArrayList<>();
            for (Component c : tt.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL)) {
                lines.add(c.getString());
            }
            pose.pushPose();
            pose.translate(sw / 2.0, sh / 2.0, 300.0);
            UtilsFX.drawCustomTooltip(g, mc.font, lines, -4, 20, 11);
            pose.popPose();
        }
    }

    /** renderQuadCentered в интерфейсе: вся текстура квадратом size с центром в начале координат. */
    private static void quad(GuiGraphics g, ResourceLocation tex, float size, float r, float gr, float b, float a) {
        g.pose().pushPose();
        g.pose().translate(-size / 2.0F, -size / 2.0F, 0.0F);
        g.pose().scale(size / 16.0F, size / 16.0F, 1.0F);
        RenderSystem.setShaderColor(r, gr, b, a);
        g.blit(tex, 0, 0, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();
    }

    // --- подсветка «Плана» -------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void renderArchitect(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getCameraEntity() instanceof Player player) || mc.level == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem().getItem() instanceof IArchitect ? player.getMainHandItem()
                : (player.getOffhandItem().getItem() instanceof IArchitect ? player.getOffhandItem() : ItemStack.EMPTY);
        if (stack.isEmpty() || ((IArchitect) stack.getItem()).useBlockHighlight(stack)) {
            return;
        }
        HitResult target = ((IArchitect) stack.getItem()).getArchitectMOP(stack, player.level(), player);
        if (target instanceof BlockHitResult bh) {
            handleArchitectOverlay(stack, player, event.getPoseStack(), event.getCamera().getPosition(), player.tickCount, bh);
        }
    }

    static boolean handleArchitectOverlay(ItemStack stack, Player player, PoseStack pose, Vec3 cam, int playerticks, BlockHitResult target) {
        Minecraft mc = Minecraft.getInstance();
        IArchitect af = (IArchitect) stack.getItem();
        String h = target.getBlockPos().getX() + "" + target.getBlockPos().getY() + "" + target.getBlockPos().getZ() + "" + target.getDirection() + ""
                + playerticks / 5;
        int hc = h.hashCode();
        if (hc != lastArcHash) {
            lastArcHash = hc;
            bmCache.clear();
            List<BlockPos> list = af.getArchitectBlocks(stack, mc.level, target.getBlockPos(), target.getDirection(), player);
            architectBlocks = list == null ? new ArrayList<>() : list;
        }
        if (architectBlocks.isEmpty()) {
            return false;
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        drawArchitectAxis(pose, cam, buffers, target.getBlockPos(), player, af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.X),
                af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.Y),
                af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.Z));
        for (BlockPos cc : architectBlocks) {
            drawOverlayBlock(pose, cam, buffers, cc);
        }
        buffers.endBatch();
        return true;
    }

    private static boolean isConnectedBlock(BlockPos pos) {
        return architectBlocks.contains(pos);
    }

    private static boolean[] getConnectedSides(BlockPos pos) {
        return bmCache.computeIfAbsent(pos, p -> new boolean[] {
                !isConnectedBlock(p.offset(-1, 0, 0)) && !isConnectedBlock(p.offset(0, 0, -1)) && !isConnectedBlock(p.offset(0, 1, 0)),
                !isConnectedBlock(p.offset(1, 0, 0)) && !isConnectedBlock(p.offset(0, 0, -1)) && !isConnectedBlock(p.offset(0, 1, 0)),
                !isConnectedBlock(p.offset(-1, 0, 0)) && !isConnectedBlock(p.offset(0, 0, 1)) && !isConnectedBlock(p.offset(0, 1, 0)),
                !isConnectedBlock(p.offset(1, 0, 0)) && !isConnectedBlock(p.offset(0, 0, 1)) && !isConnectedBlock(p.offset(0, 1, 0)),
                !isConnectedBlock(p.offset(-1, 0, 0)) && !isConnectedBlock(p.offset(0, 0, -1)) && !isConnectedBlock(p.offset(0, -1, 0)),
                !isConnectedBlock(p.offset(1, 0, 0)) && !isConnectedBlock(p.offset(0, 0, -1)) && !isConnectedBlock(p.offset(0, -1, 0)),
                !isConnectedBlock(p.offset(-1, 0, 0)) && !isConnectedBlock(p.offset(0, 0, 1)) && !isConnectedBlock(p.offset(0, -1, 0)),
                !isConnectedBlock(p.offset(1, 0, 0)) && !isConnectedBlock(p.offset(0, 0, 1)) && !isConnectedBlock(p.offset(0, -1, 0))});
    }

    /** Рамка блока: на открытых гранях — полупрозрачная сторона (0.1) и уголки (0.66) там, где нет соседей; аддитивно, сквозь блоки. */
    private static void drawOverlayBlock(PoseStack pose, Vec3 cam, MultiBufferSource buffers, BlockPos pos) {
        boolean[] bitMatrix = getConnectedSides(pos);
        VertexConsumer side = buffers.getBuffer(TCRenderTypes.architect(SFRAME));
        VertexConsumer corner = buffers.getBuffer(TCRenderTypes.architect(CFRAME));
        pose.pushPose();
        pose.translate(pos.getX() + 0.5 - cam.x, pos.getY() + 0.5 - cam.y, pos.getZ() + 0.5 - cam.z);
        for (Direction face : Direction.values()) {
            if (isConnectedBlock(pos.relative(face))) {
                continue;
            }
            pose.pushPose();
            pose.mulPose(Axis.of(new org.joml.Vector3f(-face.getStepY(), face.getStepX(), -face.getStepZ())).rotationDegrees(90.0F));
            pose.translate(0.0, 0.0, face.getStepZ() < 0 ? -0.5 : 0.5);
            pose.mulPose(Axis.ZN.rotationDegrees(90.0F));
            quadCentered(side, pose.last().pose(), 1.0F, 1.0F, 1.0F, 0.1F);
            for (int a = 0; a < 4; a++) {
                if (bitMatrix[MOS[face.ordinal()][a]]) {
                    pose.pushPose();
                    pose.mulPose(Axis.ZP.rotationDegrees(ROTMAT[face.ordinal()][a]));
                    quadCentered(corner, pose.last().pose(), 1.0F, 1.0F, 1.0F, 0.66F);
                    pose.popPose();
                }
            }
            pose.popPose();
        }
        pose.popPose();
    }

    /** Стрелки осей, по которым меняется размер области (цвет переливается). */
    private static void drawArchitectAxis(PoseStack pose, Vec3 cam, MultiBufferSource buffers, BlockPos pos, Player player, boolean dx, boolean dy, boolean dz) {
        if (!dx && !dy && !dz) {
            return;
        }
        float r = Mth.sin(player.tickCount / 4.0F + pos.getX()) * 0.2F + 0.3F;
        float g = Mth.sin(player.tickCount / 3.0F + pos.getY()) * 0.2F + 0.3F;
        float b = Mth.sin(player.tickCount / 2.0F + pos.getZ()) * 0.2F + 0.8F;
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.architect(ARROWS));
        pose.pushPose();
        pose.translate(pos.getX() + 0.5 - cam.x, pos.getY() + 0.5 - cam.y, pos.getZ() + 0.5 - cam.z);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        if (dz) {
            pose.pushPose();
            quadCentered(vc, pose.last().pose(), r, g, b, 1.0F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            quadCentered(vc, pose.last().pose(), r, g, b, 1.0F);
            pose.popPose();
        }
        if (dx) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            quadCentered(vc, pose.last().pose(), r, g, b, 1.0F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            quadCentered(vc, pose.last().pose(), r, g, b, 1.0F);
            pose.popPose();
        }
        if (dy) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            quadCentered(vc, pose.last().pose(), r, g, b, 1.0F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            quadCentered(vc, pose.last().pose(), r, g, b, 1.0F);
            pose.popPose();
        }
        pose.popPose();
    }

    /** UtilsFX.renderQuadCentered в мире (размер 1, вся текстура; сопоставление вершин — как в оригинале). */
    private static void quadCentered(VertexConsumer vc, Matrix4f m, float r, float g, float b, float a) {
        vc.addVertex(m, -0.5F, 0.5F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, a);
        vc.addVertex(m, 0.5F, 0.5F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, a);
        vc.addVertex(m, 0.5F, -0.5F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, a);
        vc.addVertex(m, -0.5F, -0.5F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, a);
    }
}
