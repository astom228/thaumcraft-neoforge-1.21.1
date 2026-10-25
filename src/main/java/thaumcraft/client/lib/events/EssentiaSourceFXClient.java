package thaumcraft.client.lib.events;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;

/**
 * Потоки эссенции (EssentiaHandler.sourceFX + RenderEventHandler.clientWorldTick оригинала): пакеты за тик собираются
 * (одинаковые — один поток), в начале следующего тика рисуются. Только клиент, только главный поток.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class EssentiaSourceFXClient {
    private static final Map<String, EssentiaSourceFX> sourceFX = new LinkedHashMap<>();
    private static int tickCount = 0;

    public record EssentiaSourceFX(BlockPos start, BlockPos end, int color, int ext) {
    }

    public static void handle(PacketFXEssentiaSource msg) {
        int tx = msg.x() - msg.dx();
        int ty = msg.y() - msg.dy();
        int tz = msg.z() - msg.dz();
        String key = msg.x() + ":" + msg.y() + ":" + msg.z() + ":" + tx + ":" + ty + ":" + tz + ":" + msg.color();
        EssentiaSourceFX sf = sourceFX.remove(key);
        if (sf == null) {
            sf = new EssentiaSourceFX(new BlockPos(msg.x(), msg.y(), msg.z()), new BlockPos(tx, ty, tz), msg.color(), msg.ext());
        }
        sourceFX.put(key, sf);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        tickCount++;
        if (sourceFX.isEmpty()) {
            return;
        }
        if (Minecraft.getInstance().level != null) {
            for (EssentiaSourceFX fx : sourceFX.values()) {
                // TODO этап 11: у матрицы наполнения поток идёт на блок ниже (mod = -1)
                int mod = 0;
                FXDispatcher.INSTANCE.essentiaTrailFx(fx.end(), fx.start().above(mod), tickCount, fx.color(), 0.1F, fx.ext());
            }
            sourceFX.clear();
        }
    }
}
