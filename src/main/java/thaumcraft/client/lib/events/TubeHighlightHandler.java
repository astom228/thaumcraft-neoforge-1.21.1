package thaumcraft.client.lib.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.tools.ItemResonator;
import thaumcraft.common.tiles.essentia.TileTube;

/**
 * Выделение части трубки (getSelectedBoundingBox + onBlockHighlight BlockTube оригинала): с резонатором (и кастером — этап 12)
 * в руке рамка обводит только патрубок или середину под взглядом.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class TubeHighlightHandler {

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        boolean doodads = player.getMainHandItem().getItem() instanceof ItemResonator || player.getOffhandItem().getItem() instanceof ItemResonator
                || player.getMainHandItem().getItem() instanceof thaumcraft.api.casters.ICaster || player.getOffhandItem().getItem() instanceof thaumcraft.api.casters.ICaster;
        if (!doodads) {
            return;
        }
        BlockPos pos = event.getTarget().getBlockPos();
        if (!(mc.level.getBlockEntity(pos) instanceof TileTube tube)) {
            return;
        }
        TileTube.IndexedAABB hit = tube.getHitCuboid(player);
        if (hit == null) {
            return;
        }
        event.setCanceled(true);
        Vec3 cam = event.getCamera().getPosition();
        AABB box = hit.box().move(-cam.x, -cam.y, -cam.z);
        PoseStack pose = event.getPoseStack();
        LevelRenderer.renderLineBox(pose, event.getMultiBufferSource().getBuffer(RenderType.lines()), box, 0.0F, 0.0F, 0.0F, 0.4F);
    }
}
