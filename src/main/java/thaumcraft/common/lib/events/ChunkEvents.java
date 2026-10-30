package thaumcraft.common.lib.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.capabilities.ThaumcraftAttachments;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * События чанков и миров (порт ChunkEvents/WorldEvents). Аура хранится в Data Attachment чанка и сохраняется вместе с ним;
 * в оригинале — в NBT чанка "Thaumcraft". Печати (seals) и регенерация мира — на этапах 17–18.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ChunkEvents {
    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            if (!chunk.hasData(ThaumcraftAttachments.AURA)) {
                AuraHandler.generateAura(level, chunk, level.random);
            }
            AuraHandler.addAuraChunk(level, chunk);
            thaumcraft.common.entities.construct.golem.seals.SealHandler.readChunk(level, chunk);
        }
    }

    @SubscribeEvent
    public static void chunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AuraHandler.removeAuraChunk(level, event.getChunk().getPos());
            if (event.getChunk() instanceof LevelChunk lc) {
                thaumcraft.common.entities.construct.golem.seals.SealHandler.unloadChunk(level, lc);
            }
        }
    }

    @SubscribeEvent
    public static void worldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AuraHandler.addAuraWorld(level);
        }
    }

    @SubscribeEvent
    public static void worldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AuraHandler.removeAuraWorld(level);
            thaumcraft.common.blocks.world.taint.TaintHelper.clear(level);
            thaumcraft.common.entities.construct.golem.seals.SealHandler.clear(level);
            thaumcraft.common.entities.construct.golem.tasks.TaskHandler.clear(level);
            thaumcraft.api.golems.GolemHelper.provisionRequests.remove(level.dimension());
        }
    }

    /** chunkWatch оригинала: игрок начал видеть чанк — прислать ему печати этого чанка. */
    @SubscribeEvent
    public static void chunkWatch(net.neoforged.neoforge.event.level.ChunkWatchEvent.Sent event) {
        thaumcraft.common.entities.construct.golem.seals.SealHandler.sendChunkTo(event.getPlayer(), event.getPos());
    }

    /** Сохранение мира: печати загруженных чанков → вложения чанков. */
    @SubscribeEvent
    public static void worldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level) {
            thaumcraft.common.entities.construct.golem.seals.SealHandler.saveAll(level);
        }
    }
}
