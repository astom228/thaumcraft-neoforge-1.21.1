package thaumcraft.common.lib.events;

import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thaumcraft.Thaumcraft;

/** Команды оператора только для отдельного стенда build/kt-server. */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class DebugAcceptance {
    private static boolean enabled;
    private static final java.util.Map<net.minecraft.world.level.ChunkPos, Float> lockedAura = new java.util.LinkedHashMap<>();
    private static final java.util.Set<net.minecraft.core.BlockPos> watched = new java.util.LinkedHashSet<>();
    @SubscribeEvent public static void started(ServerStartedEvent event) throws Exception {
        enabled = Files.exists(Path.of("tc-acceptance"));
        if (enabled) Files.writeString(Path.of("server.pid"), "" + ProcessHandle.current().pid());
    }
    @SubscribeEvent public static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (!enabled || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)
                || !java.util.Set.of("KtOne", "KtTwo").contains(p.getGameProfile().getName())) return;
        p.getServer().getPlayerList().op(p.getGameProfile());
        if (!p.getPersistentData().getBoolean("tcKtInitialized")) {
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            p.teleportTo(p.serverLevel(), 0.5, 121, 5.5, 180, 15);
            p.getPersistentData().putBoolean("tcKtInitialized", true);
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if (!enabled) return;
        var server = event.getServer();
        for (var entry : lockedAura.entrySet()) {
            var aura = thaumcraft.common.world.aura.AuraHandler.getAuraChunk(server.overworld(), entry.getKey().x, entry.getKey().z);
            if (aura != null) aura.setVis(entry.getValue());
        }
        if (server.getTickCount() % 20 == 0) {
            try { snapshot(server); }
            catch (java.io.IOException error) { org.slf4j.LoggerFactory.getLogger(DebugAcceptance.class).warn("KT snapshot unavailable", error); }
        }
        if (server.getTickCount() % 10 != 0) return;
        Path request = Path.of("commands.txt");
        if (!Files.exists(request)) return;
        var commands = Files.readAllLines(request);
        Files.delete(request);
        for (String command : commands) {
            if (command.isBlank() || command.startsWith("#")) continue;
            if (command.startsWith("kt:aura ")) {
                String[] parts = command.split(" +");
                lockedAura.put(new net.minecraft.world.level.ChunkPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])), Float.parseFloat(parts[3]));
                continue;
            }
            if (command.equals("kt:aura-clear")) { lockedAura.clear(); continue; }
            if (command.equals("kt:engine basic")) { KtEngineChecks.run(server); continue; }
            if (command.equals("kt:engine structures")) { KtEngineChecks.runStructures(server); continue; }
            if (command.startsWith("kt:watch ")) {
                String[] parts = command.split(" +");
                watched.add(new net.minecraft.core.BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                continue;
            }
            event.getServer().getCommands().performPrefixedCommand(event.getServer().createCommandSourceStack(), command);
        }
        Files.writeString(Path.of("commands.done"), String.join("\n", commands));
    }

    private static void snapshot(net.minecraft.server.MinecraftServer server) throws java.io.IOException {
        var state = new java.util.LinkedHashMap<String, Object>();
        state.put("tick", server.getTickCount());
        var players = new java.util.LinkedHashMap<String, Object>();
        for (var player : server.getPlayerList().getPlayers()) {
            var data = new java.util.LinkedHashMap<String, Object>();
            data.put("knowledge", thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player).serializeNBT(server.registryAccess()).toString());
            data.put("warp", thaumcraft.api.capabilities.ThaumcraftCapabilities.getWarp(player).serializeNBT(server.registryAccess()).toString());
            data.put("position", player.position().toString());
            data.put("health", player.getHealth());
            data.put("menu", player.containerMenu.getClass().getName());
            data.put("carried", player.containerMenu.getCarried().saveOptional(server.registryAccess()).toString());
            data.put("slots", player.containerMenu.slots.stream().map(s -> s.index + ":" + s.getItem().saveOptional(server.registryAccess())).toList());
            players.put(player.getGameProfile().getName(), data);
        }
        state.put("players", players);
        var blocks = new java.util.LinkedHashMap<String, String>();
        for (var pos : watched) {
            if (!server.overworld().hasChunkAt(pos)) continue;
            var tile = server.overworld().getBlockEntity(pos);
            blocks.put(pos.toShortString(), tile == null ? server.overworld().getBlockState(pos).toString() : tile.saveWithFullMetadata(server.registryAccess()).toString());
        }
        state.put("blocks", blocks);
        var aura = new java.util.LinkedHashMap<String, Object>();
        for (var pos : watched) {
            if (!server.overworld().hasChunkAt(pos)) continue;
            var chunk = new net.minecraft.world.level.ChunkPos(pos);
            aura.put(chunk.toString(), java.util.Map.of(
                    "vis", thaumcraft.common.world.aura.AuraHandler.getVis(server.overworld(), pos),
                    "flux", thaumcraft.common.world.aura.AuraHandler.getFlux(server.overworld(), pos)));
        }
        state.put("aura", aura);
        Path pending = Path.of("server-state.pending.json");
        Files.writeString(pending, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(state));
        try {
            Files.move(pending, Path.of("server-state.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException error) {
            Files.move(pending, Path.of("server-state.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
