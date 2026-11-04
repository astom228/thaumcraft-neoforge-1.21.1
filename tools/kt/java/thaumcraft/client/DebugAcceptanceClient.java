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
    private static final java.util.ArrayDeque<String> sounds = new java.util.ArrayDeque<>();
    @SubscribeEvent public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent event) {
        var sound = event.getSound();
        if (sound != null && sound.getLocation().getNamespace().equals("thaumcraft")) {
            if (sounds.size() >= 64) sounds.removeFirst();
            sounds.addLast(sound.getLocation() + " volume=" + sound.getVolume() + " tick=" + ticks);
        }
    }
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
        state.put("hit", mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit ? hit.getBlockPos() + ":" + hit.getDirection() : String.valueOf(mc.hitResult));
        state.put("fps", mc.getFps());
        state.put("guiScaleOption", mc.options.guiScale().get());
        state.put("guiScaleActual", mc.getWindow().getGuiScale());
        if (mc.screen instanceof thaumcraft.client.gui.GuiResearchBrowser browser) {
            var view = new LinkedHashMap<String, Object>();
            view.put("width", browser.width); view.put("height", browser.height);
            for (String name : java.util.List.of("screenZoom", "guiMapX", "guiMapY", "guiCenterX", "guiCenterY",
                    "mapFitsX", "mapFitsY", "guiBoundsLeft", "guiBoundsRight", "guiBoundsTop", "guiBoundsBottom")) {
                var field = browser.getClass().getDeclaredField(name);
                field.setAccessible(true);
                view.put(name, field.get(browser));
            }
            state.put("researchView", view);
        }
        state.put("rotation", java.util.List.of(mc.player.getYRot(), mc.player.getXRot()));
        state.put("playedSounds", new java.util.ArrayList<>(sounds));
        if (mc.player.containerMenu instanceof thaumcraft.common.container.ContainerArcaneWorkbench bench) {
            state.put("workbenchVis", bench.getTile().auraVisClient);
        }
        state.put("carried", mc.player.containerMenu.getCarried() + ":" + mc.player.containerMenu.getCarried().getComponentsPatch());
        var slots = new ArrayList<String>();
        for (var slot : mc.player.containerMenu.slots) slots.add(slot.index + ":" + slot.getItem() + ":" + slot.getItem().getComponentsPatch());
        state.put("slots", slots);
        state.put("slotData", mc.player.containerMenu.slots.stream().map(s -> s.index + ":" + s.getItem().saveOptional(mc.player.registryAccess())).toList());
        Files.writeString(Path.of("state.json"), new GsonBuilder().setPrettyPrinting().create().toJson(state));
    }
}
