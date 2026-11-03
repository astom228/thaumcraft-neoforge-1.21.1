package thaumcraft.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/** Только наблюдение: состояние клиента для сопоставления со скриншотами и сервером. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class DebugAcceptanceClient {
    private static int ticks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws Exception {
        if (++ticks % 20 != 0) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.getUser().getName().startsWith("Kt")) return;
        mc.getWindow().setTitle("Minecraft KT - " + mc.getUser().getName());
        var state = new LinkedHashMap<String, Object>();
        state.put("player", mc.getUser().getName());
        state.put("screen", mc.screen == null ? "world" : mc.screen.getClass().getName());
        state.put("knowledge", ThaumcraftCapabilities.getKnowledge(mc.player).serializeNBT(mc.player.registryAccess()).toString());
        state.put("warp", ThaumcraftCapabilities.getWarp(mc.player).serializeNBT(mc.player.registryAccess()).toString());
        state.put("position", mc.player.position().toString());
        state.put("dimension", mc.level.dimension().location().toString());
        state.put("hit", String.valueOf(mc.hitResult));
        state.put("fps", mc.getFps());
        var slots = new ArrayList<String>();
        for (var slot : mc.player.containerMenu.slots) slots.add(slot.index + ":" + slot.getItem() + ":" + slot.getItem().getComponentsPatch());
        state.put("slots", slots);
        Files.writeString(Path.of("state.json"), new GsonBuilder().setPrettyPrinting().create().toJson(state));
    }
}
