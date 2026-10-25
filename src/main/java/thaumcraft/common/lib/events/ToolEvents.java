package thaumcraft.common.lib.events;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import thaumcraft.Thaumcraft;

/**
 * Порт части ToolEvents: блоки, которые нельзя ломать, пока они ждут превращения (многоблочные структуры от соли мира).
 * Остальное (инструменты, зачарования наполнения) — с этапами своих систем. Только сервер; очищается при остановке.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ToolEvents {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> blockedBlocks = new ConcurrentHashMap<>();

    public static void addBlockedBlock(Level world, BlockPos pos) {
        blockedBlocks.computeIfAbsent(world.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    public static void clearBlockedBlock(Level world, BlockPos pos) {
        Set<BlockPos> list = blockedBlocks.get(world.dimension());
        if (list != null) {
            list.remove(pos);
        }
    }

    @SubscribeEvent
    public static void breakBlockEvent(BlockEvent.BreakEvent event) {
        LevelAccessor la = event.getLevel();
        if (la instanceof Level world) {
            Set<BlockPos> list = blockedBlocks.get(world.dimension());
            if (list != null && list.contains(event.getPos())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        blockedBlocks.clear();
    }
}
