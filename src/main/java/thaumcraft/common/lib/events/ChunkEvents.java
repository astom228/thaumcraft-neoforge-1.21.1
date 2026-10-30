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
        }
    }

    @SubscribeEvent
    public static void chunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AuraHandler.removeAuraChunk(level, event.getChunk().getPos());
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
        }
    }
}
