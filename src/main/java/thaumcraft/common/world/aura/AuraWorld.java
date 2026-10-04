package thaumcraft.common.world.aura;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.ChunkPos;

/** Ауры загруженных чанков одного измерения (порт AuraWorld). Ключ — ChunkPos.toLong(). */
public class AuraWorld {
    final ConcurrentHashMap<Long, AuraChunk> auraChunks = new ConcurrentHashMap<>();

    public ConcurrentHashMap<Long, AuraChunk> getAuraChunks() {
        return this.auraChunks;
    }

    public AuraChunk getAuraChunkAt(int x, int z) {
        return this.auraChunks.get(ChunkPos.asLong(x, z));
    }
}
