package thaumcraft.common.lib.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.compat.BotaniaCompat;
import thaumcraft.common.tiles.devices.TileWaterJug;
import thaumcraft.common.tiles.devices.TileSpa;

/** Временный нагрузочный тест. Копируется скриптом, работает только в отдельной папке с маркером. */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class DebugStabilityAudit {
    private static boolean enabled, failed, completed;
    private static String phase;
    private static int duration, ticks, peakChunks, peakReceivers;
    private static long start, gcStart, heapStart;
    private static double warmupMax;
    private static final List<Double> times = new ArrayList<>();
    private static final AtomicInteger jobs = new AtomicInteger(), chain = new AtomicInteger();
    private static final List<BlockPos> urns = new ArrayList<>(), sinks = new ArrayList<>(), chests = new ArrayList<>();
    private static final BlockPos BASE = new BlockPos(0, 120, 0);
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static java.lang.reflect.Field receivers;
    private static jdk.jfr.Recording recording;

    @SubscribeEvent public static void playerJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (enabled && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            player.setHealth(player.getMaxHealth());
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            player.getPersistentData().remove("tcPerfPlaced");
        }
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) throws Exception {
        if (!Files.exists(Path.of("tc-stability-request"))) return;
        var request = Files.readAllLines(Path.of("tc-stability-request"));
        phase = request.get(0); duration = Integer.parseInt(request.get(1)); enabled = true;
        Files.writeString(Path.of("server.pid"), Long.toString(ProcessHandle.current().pid()));
        var server = event.getServer(); var world = server.overworld();
        world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        world.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        world.setDayTime(18000);
        for (int x = -1; x <= 3; x++) for (int z = -1; z <= 3; z++) world.setChunkForced(x, z, true);
        for (int i = 0; i < 64; i++) { urns.add(BASE.offset(i % 8 * 3, 1, i / 8 * 3)); sinks.add(urns.get(i).east()); }
        for (int i = 0; i < 32; i++) chests.add(BASE.offset(27, 1, i));
        receivers = TileWaterJug.class.getDeclaredField("handlers"); receivers.setAccessible(true);
        if (phase.equals("create")) {
            for (var pos : BlockPos.betweenClosed(BASE.offset(-4, 0, -4), BASE.offset(35, 0, 35))) world.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            for (int i = 0; i < urns.size(); i++) {
                world.removeBlock(urns.get(i), false); world.removeBlock(sinks.get(i), false);
                world.setBlock(urns.get(i), BlocksTC.everfullUrn.get().defaultBlockState(), 3);
                world.setBlock(sinks.get(i), BlocksTC.spa.get().defaultBlockState(), 3);
                world.setBlock(sinks.get(i).above(), Blocks.STONE.defaultBlockState(), 3);
            }
            for (int i = 0; i < chests.size(); i++) {
                world.removeBlock(chests.get(i), false); world.setBlock(chests.get(i), BlocksTC.hungryChest.get().defaultBlockState(), 3);
                var stack = new ItemStack(Items.DIAMOND, 17 + i);
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Stability marker " + i));
                var handler = world.getCapability(Capabilities.ItemHandler.BLOCK, chests.get(i), null);
                require(handler != null && handler.insertItem(0, stack, false).isEmpty(), "chest setup");
            }
            for (int i = 0; i < 64; i++) {
                var mob = thaumcraft.common.config.ConfigEntities.BRAINY_ZOMBIE.get().create(world);
                mob.moveTo(2 + i % 8 * 3, 123, 2 + i / 8 * 3, 0, 0); mob.setPersistenceRequired(); mob.setInvulnerable(true); world.addFreshEntity(mob);
            }
            for (int i = 0; i < 4; i++) {
                var rift = thaumcraft.common.config.ConfigEntities.FLUX_RIFT.get().create(world);
                rift.moveTo(3 + i * 7, 136, 8, 0, 0); rift.setRiftSize(30); rift.setRiftStability(100); world.addFreshEntity(rift);
            }
            checkBotania(world);
        } else require(Files.exists(Path.of("report-create.json")), "reload requires successful first run");
        checkSavedFixtures(world);
        require(!BotaniaCompat.isApothecary(null), "null apothecary");
        for (int i = 0; i < 20000; i++) {
            final boolean expensive = i < 1024;
            ServerEvents.addRunnableServer(world, () -> {
                if (expensive) { long until = System.nanoTime() + 250_000; while (System.nanoTime() < until) Thread.onSpinWait(); }
                jobs.incrementAndGet();
            }, 0);
        }
        Runnable[] recursive = new Runnable[1];
        recursive[0] = () -> { if (chain.incrementAndGet() < 1000) ServerEvents.addRunnableServer(world, recursive[0], 0); };
        ServerEvents.addRunnableServer(world, recursive[0], 0);
        recording = new jdk.jfr.Recording(jdk.jfr.Configuration.getConfiguration("profile"));
        recording.setDestination(Path.of("profile-" + phase + ".jfr")); recording.start();
        gcStart = gcMillis(); heapStart = heap();
        Thaumcraft.log.info("[TC-STABILITY] START {} ticks={} botania={} fixture=64 urns+64 spas+32 chests+64 mobs+4 rifts", phase, duration, ModList.get().isLoaded("botania"));
    }

    private static void checkBotania(ServerLevel world) throws Exception {
        if (!ModList.get().isLoaded("botania")) return;
        var altar = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("botania:petal_apothecary"));
        require(altar != Blocks.AIR, "Botania altar registration");
        BlockPos pos = BASE.offset(-2, 1, -2), target = pos.east();
        world.setBlock(pos, BlocksTC.everfullUrn.get().defaultBlockState(), 3); world.setBlock(target, altar.defaultBlockState(), 3);
        var urn = (TileWaterJug) world.getBlockEntity(pos); var apothecary = world.getBlockEntity(target);
        require(BotaniaCompat.isApothecary(apothecary), "Botania API recognition");
        var type = Class.forName("vazkii.botania.api.block.PetalApothecary$State");
        var get = apothecary.getClass().getMethod("getFluid"); var set = apothecary.getClass().getMethod("setFluid", type);
        Object empty = type.getField("EMPTY").get(null), water = type.getField("WATER").get(null), lava = type.getField("LAVA").get(null);
        var aura = thaumcraft.common.world.aura.AuraHandler.getAuraChunk(world, pos.getX() >> 4, pos.getZ() >> 4);
        if (aura != null) aura.setVis(0);
        var counter = TileWaterJug.class.getDeclaredField("counter"); counter.setAccessible(true);
        var zone = TileWaterJug.class.getDeclaredField("zone"); zone.setAccessible(true);
        set.invoke(apothecary, empty); urn.tank.setFluid(new FluidStack(Fluids.WATER, 999)); counter.setInt(urn, 4); zone.setInt(urn, 41); urn.update();
        require(get.invoke(apothecary) == empty && urn.tank.getFluidAmount() == 999, "apothecary rejects partial bucket");
        urn.tank.setFluid(new FluidStack(Fluids.WATER, 1000)); counter.setInt(urn, 4); urn.update();
        require(get.invoke(apothecary) == water && urn.tank.isEmpty(), "apothecary fills exactly one bucket");
        for (Object state : List.of(water, lava)) {
            set.invoke(apothecary, state); urn.tank.setFluid(new FluidStack(Fluids.WATER, 1000)); counter.setInt(urn, 4); urn.update();
            require(get.invoke(apothecary) == state && urn.tank.getFluidAmount() == 1000, "apothecary does not overwrite fluid");
        }
        set.invoke(apothecary, water);
        Thaumcraft.log.info("[TC-STABILITY] PASS Botania: empty/999/1000/full/lava");
    }

    private static void checkSavedFixtures(ServerLevel world) {
        for (int i = 0; i < chests.size(); i++) {
            var handler = world.getCapability(Capabilities.ItemHandler.BLOCK, chests.get(i), null);
            require(handler != null, "chest restored"); var stack = handler.getStackInSlot(0);
            require(stack.is(Items.DIAMOND) && stack.getCount() == 17 + i && stack.getHoverName().getString().equals("Stability marker " + i), "inventory/component round trip " + i);
        }
        for (var pos : urns) require(world.getBlockEntity(pos) instanceof TileWaterJug, "urn restored");
        for (var pos : sinks) require(world.getBlockEntity(pos) instanceof TileSpa, "spa restored");
    }

    @SubscribeEvent public static void pre(ServerTickEvent.Pre event) { if (enabled && !completed) start = System.nanoTime(); }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (!enabled || completed) return;
        try {
            var server = event.getServer(); var world = server.overworld(); ticks++;
            for (var player : server.getPlayerList().getPlayers()) if (!player.getPersistentData().getBoolean("tcPerfPlaced")) {
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                player.teleportTo(world, 12, 129, 34, 180, 10);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 6000, 0, false, false));
                player.getPersistentData().putBoolean("tcPerfPlaced", true);
            }
            double elapsed = (System.nanoTime() - start) / 1_000_000.0;
            if (ticks > 200) times.add(elapsed); else warmupMax = Math.max(warmupMax, elapsed);
            peakChunks = Math.max(peakChunks, world.getChunkSource().getLoadedChunksCount());
            if (ticks == 1) require(jobs.get() <= 512 && chain.get() <= 1, "per-tick work limit/reentrant queue");
            if (ticks == 200) require(jobs.get() == 20000, "queued work completed without losses");
            for (var pos : urns) {
                var tile = world.getBlockEntity(pos); require(tile instanceof TileWaterJug, "urn survives ticking");
                var urn = (TileWaterJug) tile;
                @SuppressWarnings("unchecked") var list = (List<Integer>) receivers.get(urn);
                peakReceivers = Math.max(peakReceivers, list.size());
                require(list.size() <= 74 && list.stream().allMatch(n -> n >= 0 && n < 75), "bounded urn receiver cache");
                if (ticks % 20 == 0) urn.tank.setFluid(new FluidStack(Fluids.WATER, 1000));
            }
            if (ticks % 20 == 0) for (var pos : sinks) ((TileSpa) world.getBlockEntity(pos)).tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            if (ticks % 200 == 0) Thaumcraft.log.info("[TC-STABILITY] tick={} chunks={} entities={} jobs={} chain={} heapMiB={}", ticks, peakChunks, world.getAllEntities().spliterator().getExactSizeIfKnown(), jobs.get(), chain.get(), heap() / 1048576);
            if (ticks >= duration || phase.equals("reload") && ticks >= 1200 && Files.exists(Path.of("../client-performance.json"))) {
                checkSavedFixtures(world); require(jobs.get() == 20000 && chain.get() == 1000, "all scheduled work completed");
                writeReport(world); completed = true; server.halt(false);
            }
        } catch (Exception e) { failed = true; completed = true; Thaumcraft.log.error("[TC-STABILITY] FAIL", e); event.getServer().halt(false); }
    }

    private static long heap() { return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(); }
    private static long gcMillis() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0, b.getCollectionTime())).sum(); }
    private static void writeReport(ServerLevel world) throws Exception {
        var sorted = times.stream().sorted().toList(); Map<String,Object> report = new LinkedHashMap<>();
        report.put("phase", phase); report.put("ticks", ticks); report.put("samples", times.size());
        report.put("mspt_mean", times.stream().mapToDouble(d -> d).average().orElse(0));
        report.put("warmup_max_ms", warmupMax);
        report.put("mspt_p50", percentile(sorted, .5)); report.put("mspt_p95", percentile(sorted, .95)); report.put("mspt_p99", percentile(sorted, .99));
        report.put("mspt_max", sorted.isEmpty() ? 0 : sorted.getLast()); report.put("ticks_over_50ms", times.stream().filter(t -> t > 50).count());
        report.put("gc_ms", gcMillis() - gcStart); report.put("heap_start_mib", heapStart / 1048576); report.put("heap_end_mib", heap() / 1048576);
        report.put("peak_chunks", peakChunks); report.put("peak_urn_receivers", peakReceivers); report.put("jobs", jobs.get()); report.put("chain", chain.get());
        report.put("botania", ModList.get().isLoaded("botania")); report.put("inventory_round_trip", true);
        Files.writeString(Path.of("report-" + phase + ".json"), JSON.toJson(report));
        recording.stop(); recording.close();
        Thaumcraft.log.info("[TC-STABILITY] PASS {} {}", phase, report);
    }
    private static double percentile(List<Double> samples, double p) { return samples.isEmpty() ? 0 : samples.get(Math.min(samples.size()-1, (int) Math.ceil(samples.size()*p)-1)); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) throws Exception {
        if (!enabled) return;
        if (completed && !failed) Files.writeString(Path.of("saved-" + phase + ".ok"), "clean server stop");
        if (recording != null) recording.close();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
