package thaumcraft.client.lib.events;

import thaumcraft.common.world.aura.AuraChunk;

/** Порт HudHandler (состояние и отрисовка HUD; отрисовка — с таумометром/кастером на этапах 7 и 12). */
public class HudHandler {
    /** Последняя полученная аура (PacketAuraToClient). */
    public static AuraChunk currentAura = new AuraChunk(null, (short) 0, 0.0F, 0.0F);
}
