package thaumcraft.common.world.aura;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.capabilities.ThaumcraftAttachments;
import thaumcraft.common.world.biomes.BiomeHandler;

/**
 * Аура мира (порт AuraHandler). В оригинале — статические таблицы по номеру измерения, заполняемые при загрузке чанков;
 * в порте — по ключу измерения, сами данные хранятся в Data Attachment чанка (thaumcraft:aura).
 * Работает только на сервере: на клиенте таблиц нет, как и в оригинале.
 */
public class AuraHandler {
    public static final int AURA_CEILING = 500;
    static final Map<ResourceKey<Level>, AuraWorld> auras = new ConcurrentHashMap<>();
    /** Где должен появиться разлом флюкса (заполняется обработкой ауры). */
    public static final Map<ResourceKey<Level>, BlockPos> riftTrigger = new ConcurrentHashMap<>();

    public static AuraWorld getAuraWorld(Level level) {
        return auras.get(level.dimension());
    }

    public static AuraChunk getAuraChunk(Level level, int x, int z) {
        AuraWorld aw = auras.get(level.dimension());
        return aw != null ? aw.getAuraChunkAt(x, z) : null;
    }

    public static void addAuraWorld(Level level) {
        auras.computeIfAbsent(level.dimension(), k -> new AuraWorld());
    }

    public static void removeAuraWorld(Level level) {
        auras.remove(level.dimension());
        riftTrigger.remove(level.dimension());
    }

    public static void addAuraChunk(Level level, LevelChunk chunk) {
        AuraChunk ac = chunk.getData(ThaumcraftAttachments.AURA);
        ac.setLoc(chunk.getPos());
        auras.computeIfAbsent(level.dimension(), k -> new AuraWorld()).auraChunks.put(chunk.getPos().toLong(), ac);
    }

    public static void removeAuraChunk(Level level, ChunkPos pos) {
        AuraWorld aw = auras.get(level.dimension());
        if (aw != null) {
            aw.auraChunks.remove(pos.toLong());
        }
    }

    private static AuraChunk at(Level level, BlockPos pos) {
        return getAuraChunk(level, pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static float getTotalAura(Level level, BlockPos pos) {
        AuraChunk ac = at(level, pos);
        return ac != null ? ac.getVis() + ac.getFlux() : 0.0F;
    }

    public static float getFluxSaturation(Level level, BlockPos pos) {
        AuraChunk ac = at(level, pos);
        return ac != null ? ac.getFlux() / ac.getBase() : 0.0F;
    }

    public static float getVis(Level level, BlockPos pos) {
        AuraChunk ac = at(level, pos);
        return ac != null ? ac.getVis() : 0.0F;
    }

    public static float getFlux(Level level, BlockPos pos) {
        AuraChunk ac = at(level, pos);
        return ac != null ? ac.getFlux() : 0.0F;
    }

    public static int getAuraBase(Level level, BlockPos pos) {
        AuraChunk ac = at(level, pos);
        return ac != null ? ac.getBase() : 0;
    }

    public static boolean shouldPreserveAura(Level level, Player player, BlockPos pos) {
        return (player == null || ThaumcraftCapabilities.getKnowledge(player).isResearchComplete("AURAPRESERVE"))
                && getVis(level, pos) / getAuraBase(level, pos) < 0.1;
    }

    public static void addVis(Level level, BlockPos pos, float amount) {
        if (!(amount < 0.0F)) {
            AuraChunk ac = at(level, pos);
            modifyVisInChunk(level, ac, amount, true);
        }
    }

    public static void addFlux(Level level, BlockPos pos, float amount) {
        if (!(amount < 0.0F)) {
            AuraChunk ac = at(level, pos);
            modifyFluxInChunk(level, ac, amount, true);
        }
    }

    public static float drainVis(Level level, BlockPos pos, float amount, boolean simulate) {
        AuraChunk ac = at(level, pos);
        if (ac == null) {
            return 0.0F;
        }
        if (amount > ac.getVis()) {
            amount = ac.getVis();
        }
        return modifyVisInChunk(level, ac, -amount, !simulate) ? amount : 0.0F;
    }

    public static float drainFlux(Level level, BlockPos pos, float amount, boolean simulate) {
        AuraChunk ac = at(level, pos);
        if (ac == null) {
            return 0.0F;
        }
        if (amount > ac.getFlux()) {
            amount = ac.getFlux();
        }
        return modifyFluxInChunk(level, ac, -amount, !simulate) ? amount : 0.0F;
    }

    public static boolean modifyVisInChunk(Level level, AuraChunk ac, float amount, boolean doit) {
        if (ac == null) {
            return false;
        }
        if (doit) {
            ac.setVis(Math.max(0.0F, ac.getVis() + amount));
            markDirty(level, ac);
        }
        return true;
    }

    private static boolean modifyFluxInChunk(Level level, AuraChunk ac, float amount, boolean doit) {
        if (ac == null) {
            return false;
        }
        if (doit) {
            ac.setFlux(Math.max(0.0F, ac.getFlux() + amount));
            markDirty(level, ac);
        }
        return true;
    }

    /** Пометить чанк для сохранения (в оригинале — dirtyChunks + markChunkDirty раз в секунду). */
    static void markDirty(Level level, AuraChunk ac) {
        if (ac.loc != null && level instanceof ServerLevel sl) {
            LevelChunk chunk = sl.getChunkSource().getChunkNow(ac.loc.x, ac.loc.z);
            if (chunk != null) {
                chunk.setUnsaved(true);
            }
        }
    }

    /** Биом в точке без загрузки чанков (в 1.12 World.getBiome для незагруженных чанков брал биом из BiomeProvider). */
    private static Holder<Biome> biomeAt(ServerLevel level, int x, int y, int z) {
        return level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), level.getChunkSource().randomState().sampler());
    }

    /** Аура нового чанка: среднее по биому чанка и четырёх соседей × 500 × шум (±10%), 0..500. */
    public static void generateAura(ServerLevel level, LevelChunk chunk, RandomSource rand) {
        ChunkPos cp = chunk.getPos();
        Holder<Biome> bgb = biomeAt(level, cp.x * 16 + 8, 50, cp.z * 16 + 8);
        // TODO: BiomeHandler.getBiomeBlacklist (IMC других модов) — в порте не используется
        float life = BiomeHandler.getBiomeAuraModifier(bgb);
        for (int a = 0; a < 4; a++) {
            Direction dir = Direction.from2DDataValue(a);
            Holder<Biome> bgb2 = biomeAt(level, (cp.x + dir.getStepX()) * 16 + 8, 50, (cp.z + dir.getStepZ()) * 16 + 8);
            life += BiomeHandler.getBiomeAuraModifier(bgb2);
        }
        life /= 5.0F;
        float noise = (float) (1.0 + rand.nextGaussian() * 0.1F);
        short base = (short) (life * 500.0F * noise);
        base = (short) Mth.clamp(base, 0, 500);
        chunk.setData(ThaumcraftAttachments.AURA, new AuraChunk(cp, base, base, 0.0F));
        chunk.setUnsaved(true);
    }
}
