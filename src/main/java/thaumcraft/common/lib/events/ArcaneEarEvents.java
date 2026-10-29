package thaumcraft.common.lib.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.Thaumcraft;

/**
 * Ноты, сыгранные нотными блоками за текущий тик (TileArcaneEar.noteBlockEvents оригинала: заполняет WorldEvents.noteEvent,
 * очищает ServerEvents в конце тика мира). Только сервер, по измерениям; очищается и при остановке сервера.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ArcaneEarEvents {
    private static final Map<ResourceKey<Level>, List<int[]>> NOTES = new HashMap<>();

    private ArcaneEarEvents() {
    }

    /** Записи {x, y, z, инструмент, нота}. */
    public static List<int[]> getNotes(Level level) {
        List<int[]> l = NOTES.get(level.dimension());
        return l == null ? List.of() : l;
    }

    @SubscribeEvent
    public static void noteEvent(NoteBlockEvent.Play event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            NOTES.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(new int[] {event.getPos().getX(), event.getPos().getY(),
                    event.getPos().getZ(), event.getInstrument().ordinal(), event.getVanillaNoteId()});
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide) {
            List<int[]> l = NOTES.get(event.getLevel().dimension());
            if (l != null) {
                l.clear();
            }
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        NOTES.clear();
    }
}
