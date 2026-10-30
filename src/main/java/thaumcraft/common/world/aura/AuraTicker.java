package thaumcraft.common.world.aura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ModConfig;

/**
 * Обработка ауры (порт AuraThread). В оригинале — отдельный поток на измерение, раз в 1000 мс обходивший все загруженные чанки
 * (только если время мира изменилось). В порте — серверный тик: каждый чанк обрабатывается раз в 20 тиков,
 * обход одного цикла распределён по 20 тикам. Алгоритм и числа — без изменений.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class AuraTicker {
    private static final int CYCLE = 20;
    /** Раздел 7: предел обработанных чанков за тик (при большем числе загруженных чанков цикл растягивается). */
    private static final int MAX_PER_TICK = 512;
    /** Отладка: переменная окружения THAUMCRAFT_DEBUG_AURA — раз в 200 тиков сводка ауры в лог. */
    private static final boolean DEBUG = System.getenv("THAUMCRAFT_DEBUG_AURA") != null;
    private static final float[] phaseTable = {0.25F, 0.15F, 0.1F, 0.05F, 0.0F, 0.05F, 0.1F, 0.15F};
    private static final float[] maxTable = {0.15F, 0.05F, 0.0F, -0.05F, -0.15F, -0.05F, 0.0F, 0.05F};

    private static final class State {
        final Random rand = new Random(System.currentTimeMillis());
        float phaseVis;
        float phaseFlux;
        float phaseMax;
        List<AuraChunk> cycle = List.of();
        int index;
        int tick;
    }

    private static final Map<ResourceKey<Level>, State> STATES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        AuraWorld auraWorld = AuraHandler.getAuraWorld(level);
        if (auraWorld == null) {
            return;
        }
        State st = STATES.computeIfAbsent(level.dimension(), k -> new State());
        if (st.tick % CYCLE == 0 && st.index >= st.cycle.size()) {
            // начало цикла: фаза луны и снимок загруженных чанков
            int moon = level.getMoonPhase();
            st.phaseVis = phaseTable[moon];
            st.phaseMax = 1.0F + maxTable[moon];
            st.phaseFlux = 0.25F - st.phaseVis;
            st.cycle = new ArrayList<>(auraWorld.auraChunks.values());
            st.index = 0;
        }
        int perTick = Math.min(MAX_PER_TICK, (st.cycle.size() + CYCLE - 1) / CYCLE);
        for (int i = 0; i < perTick && st.index < st.cycle.size(); i++, st.index++) {
            processAuraChunk(level, auraWorld, st.cycle.get(st.index), st);
        }
        st.tick++;
        if (DEBUG && st.tick % 200 == 0) {
            float vis = 0, flux = 0, base = 0;
            for (AuraChunk ac : auraWorld.auraChunks.values()) {
                vis += ac.getVis();
                flux += ac.getFlux();
                base += ac.getBase();
            }
            Thaumcraft.log.info("[aura debug] {}: chunks={} base={} vis={} flux={} moon={}", level.dimension().location(),
                    auraWorld.auraChunks.size(), base, vis, flux, level.getMoonPhase());
        }
        // ServerEvents.worldTick оригинала: раз в 20 тиков — разлом флюкса на отмеченном месте
        if (st.tick % CYCLE == 0) {
            BlockPos rift = AuraHandler.riftTrigger.remove(level.dimension());
            if (rift != null && !ModConfig.CONFIG_MISC.wussMode.get()) {
                thaumcraft.common.entities.EntityFluxRift.createRift(level, rift);
            }
        }
    }

    private static void processAuraChunk(ServerLevel level, AuraWorld auraWorld, AuraChunk auraChunk, State st) {
        List<Integer> directions = Arrays.asList(0, 1, 2, 3);
        Collections.shuffle(directions, st.rand);
        int x = auraChunk.loc.x;
        int y = auraChunk.loc.z;
        float base = auraChunk.getBase() * st.phaseMax;
        boolean dirty = false;
        float currentVis = auraChunk.getVis();
        float currentFlux = auraChunk.getFlux();
        AuraChunk neighbourVisChunk = null;
        AuraChunk neighbourFluxChunk = null;
        float lowestVis = Float.MAX_VALUE;
        float lowestFlux = Float.MAX_VALUE;

        for (Integer a : directions) {
            Direction dir = Direction.from2DDataValue(a);
            AuraChunk n = auraWorld.getAuraChunkAt(x + dir.getStepX(), y + dir.getStepZ());
            if (n != null) {
                if ((neighbourVisChunk == null || lowestVis > n.getVis()) && n.getVis() + n.getFlux() < n.getBase() * st.phaseMax) {
                    neighbourVisChunk = n;
                    lowestVis = n.getVis();
                }
                if (neighbourFluxChunk == null || lowestFlux > n.getFlux()) {
                    neighbourFluxChunk = n;
                    lowestFlux = n.getFlux();
                }
            }
        }

        if (neighbourVisChunk != null && lowestVis < currentVis && lowestVis / currentVis < 0.75) {
            float inc = Math.min(currentVis - lowestVis, 1.0F);
            currentVis -= inc;
            neighbourVisChunk.setVis(lowestVis + inc);
            dirty = true;
            AuraHandler.markDirty(level, neighbourVisChunk);
        }

        if (neighbourFluxChunk != null && currentFlux > Math.max(5.0F, auraChunk.getBase() / 10.0F) && lowestFlux < currentFlux / 1.75) {
            float inc = Math.min(currentFlux - lowestFlux, 1.0F);
            currentFlux -= inc;
            neighbourFluxChunk.setFlux(lowestFlux + inc);
            dirty = true;
            AuraHandler.markDirty(level, neighbourFluxChunk);
        }

        if (currentVis + currentFlux < base) {
            float inc = Math.min(base - (currentVis + currentFlux), st.phaseVis);
            currentVis += inc;
            dirty = true;
        } else if (currentVis > base * 1.25 && st.rand.nextFloat() < 0.1) {
            currentFlux += st.phaseFlux;
            currentVis -= st.phaseFlux;
            dirty = true;
        } else if (currentVis <= base * 0.1 && currentVis >= currentFlux && st.rand.nextFloat() < 0.1) {
            currentFlux += st.phaseFlux;
            dirty = true;
        }

        if (dirty) {
            auraChunk.setVis(currentVis);
            auraChunk.setFlux(currentFlux);
            AuraHandler.markDirty(level, auraChunk);
        }

        if (currentFlux > base * 0.75 && st.rand.nextFloat() < currentFlux / 500.0F / 10.0F) {
            AuraHandler.riftTrigger.put(level.dimension(), new BlockPos(x * 16, 0, y * 16));
        }
    }
}
