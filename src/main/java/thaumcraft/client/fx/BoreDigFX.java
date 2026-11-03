package thaumcraft.client.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.particles.FXBoreParticles;
import thaumcraft.client.fx.particles.FXBoreSparkle;
import thaumcraft.common.lib.network.fx.PacketFXBoreDig;

/**
 * Частицы копания бура (PacketFXBoreDig.processMessage + FXDispatcher.boreDigFx оригинала): delay тиков подряд — по
 * 50/delay частиц: искры к буру (1 из 4) и осколки блока, летящие к буру. Очередь — только клиент.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class BoreDigFX {
    private record Job(BlockPos pos, Entity bore, BlockState state, int delay, int[] left) {
    }

    private static final List<Job> JOBS = new ArrayList<>();

    private BoreDigFX() {
    }

    public static void handle(PacketFXBoreDig msg) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        BlockPos pos = new BlockPos(msg.x(), msg.y(), msg.z());
        Entity entity = world.getEntity(msg.bore());
        BlockState ts = world.getBlockState(pos);
        if (entity == null || ts.isAir()) {
            return;
        }
        for (int a = 0; a < msg.delay(); a++) {
            JOBS.add(new Job(pos, entity, ts, msg.delay(), new int[] {a}));
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            JOBS.clear();
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job j = it.next();
            if (j.left[0]-- > 0) {
                continue;
            }
            it.remove();
            if (j.bore.isRemoved() || j.bore.level() != world) {
                continue;
            }
            float p = 50.0F;
            for (int a = 0; a < p / j.delay; a++) {
                double x = j.pos.getX() + world.random.nextFloat();
                double y = j.pos.getY() + world.random.nextFloat();
                double z = j.pos.getZ() + world.random.nextFloat();
                if (world.random.nextInt(4) == 0) {
                    ParticleEngine.addEffect(world, new FXBoreSparkle(world, x, y, z, j.bore));
                } else {
                    FXBoreParticles fb = new FXBoreParticles(world, x, y, z, j.bore.getX(), j.bore.getY(), j.bore.getZ(), j.state);
                    fb.setTarget(j.bore);
                    Minecraft.getInstance().particleEngine.add(fb);
                }
            }
        }
    }
}
