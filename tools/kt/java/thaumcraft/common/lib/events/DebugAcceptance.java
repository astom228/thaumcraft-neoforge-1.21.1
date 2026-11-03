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
        if (!enabled || event.getServer().getTickCount() % 10 != 0) return;
        Path request = Path.of("commands.txt");
        if (!Files.exists(request)) return;
        var commands = Files.readAllLines(request);
        Files.delete(request);
        for (String command : commands) {
            if (command.isBlank() || command.startsWith("#")) continue;
            event.getServer().getCommands().performPrefixedCommand(event.getServer().createCommandSourceStack(), command);
        }
        Files.writeString(Path.of("commands.done"), String.join("\n", commands));
    }
}
