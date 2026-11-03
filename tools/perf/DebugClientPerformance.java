package thaumcraft.client.fx;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.particles.FXGeneric;

/** Временный клиентский замер. Не подключается к обычному запуску без файла-маркера. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class DebugClientPerformance {
    private static final boolean ENABLED = Files.exists(Path.of("tc-client-performance"));
    private static final List<List<Double>> frames = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    private static int ticks, peakActive, peakDelayed, inactiveFrames;
    private static long lastFrame;
    private static boolean done;
    private static java.lang.reflect.Field live, delayed;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!ENABLED || done || mc.level == null || mc.player == null || mc.screen != null || !mc.player.isSpectator()) return;
        try {
            ticks++;
            if (ticks == 1) {
                live = ParticleEngine.class.getDeclaredField("particles"); live.setAccessible(true);
                delayed = ParticleEngine.class.getDeclaredField("particlesDelayed"); delayed.setAccessible(true);
                Thaumcraft.log.info("[TC-CLIENT-PERF] START warmup=200 baseline=200 load=600 recovery=200");
            }
            if (ticks >= 400 && ticks < 1000) {
                int count = ticks % 100 == 0 ? 5000 : 100;
                var eye = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(8));
                for (int i = 0; i < count; i++) {
                    var fx = new FXGeneric(mc.level, eye.x + (mc.level.random.nextDouble() - .5) * 8, eye.y + (mc.level.random.nextDouble() - .5) * 5, eye.z + (mc.level.random.nextDouble() - .5) * 8);
                    fx.setMaxAge(40); fx.setScale(.3F); fx.setAlphaF(.6F); fx.setRBGColorF(.4F, .1F, .8F); fx.setLayer(i % 4); fx.setParticles(320, 16, 1);
                    ParticleEngine.addEffect(mc.level, fx);
                }
                if (ticks % 100 == 0) for (int i = 0; i < 5000; i++) ParticleEngine.addEffectWithDelay(mc.level, new FXGeneric(mc.level, 12, 130, 20), 10000);
            }
            @SuppressWarnings("unchecked") var maps = (Map<ResourceKey<Level>, List<Particle>>[]) live.get(null);
            int active = 0; for (var map : maps) active += map.getOrDefault(mc.level.dimension(), List.of()).size();
            int pending = ((java.util.Collection<?>) delayed.get(null)).size();
            peakActive = Math.max(active, peakActive); peakDelayed = Math.max(pending, peakDelayed);
            if (active > 4000 || pending > 4000) throw new IllegalStateException("Particle budget exceeded: " + active + "/" + pending);
            if (ticks == 1000) ParticleEngine.clearParticles();
            if (ticks % 200 == 0) Thaumcraft.log.info("[TC-CLIENT-PERF] ticks={} fps={} active={} pending={}", ticks, mc.getFps(), active, pending);
            if (ticks >= 1200) {
                Map<String,Object> report = new LinkedHashMap<>();
                report.put("resolution", mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
                report.put("renderer", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
                report.put("vendor", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR));
                report.put("peak_active", peakActive); report.put("peak_delayed", peakDelayed); report.put("inactive_frames", inactiveFrames);
                for (int i = 0; i < 3; i++) {
                    var values = frames.get(i).stream().sorted().toList(); Map<String,Object> summary = new LinkedHashMap<>();
                    double average = values.stream().mapToDouble(d -> d).average().orElse(0);
                    summary.put("frames", values.size()); summary.put("mean_ms", average); summary.put("mean_fps", average > 0 ? 1000 / average : 0);
                    summary.put("p95_ms", percentile(values, .95)); summary.put("p99_ms", percentile(values, .99));
                    summary.put("max_ms", values.isEmpty() ? 0 : values.getLast()); summary.put("frames_over_50ms", values.stream().filter(v -> v > 50).count());
                    summary.put("frames_over_250ms", values.stream().filter(v -> v > 250).count());
                    report.put(List.of("baseline", "particle_load", "recovery").get(i), summary);
                }
                Files.writeString(Path.of("../client-performance.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
                Thaumcraft.log.info("[TC-CLIENT-PERF] PASS {}", report); done = true; mc.stop();
            }
        } catch (Exception e) { done = true; Thaumcraft.log.error("[TC-CLIENT-PERF] FAIL", e); mc.stop(); }
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!ENABLED || done || mc.level == null || mc.screen != null || ticks < 200) { lastFrame = 0; return; }
        long now = System.nanoTime();
        if (!mc.isWindowActive()) inactiveFrames++;
        if (lastFrame != 0) frames.get(ticks < 400 ? 0 : ticks < 1000 ? 1 : 2).add((now - lastFrame) / 1_000_000.0);
        lastFrame = now;
    }
    private static double percentile(List<Double> list, double fraction) { return list.isEmpty() ? 0 : list.get(Math.min(list.size()-1, (int) Math.ceil(list.size()*fraction)-1)); }
}
