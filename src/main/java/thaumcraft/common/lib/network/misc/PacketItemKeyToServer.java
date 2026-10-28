package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.items.casters.CasterManager;

/**
 * Клиент → сервер: клавиша предмета. key 1 — «разное» перчатки (mod: 0 — размер области «Плана», 1 — ось (Ctrl), 2 — Shift).
 * TODO этап 13: ориентация стихийной лопаты; этап 18: key 0 — колокольчик голема.
 */
public record PacketItemKeyToServer(byte key, byte mod) implements CustomPacketPayload {
    public static final Type<PacketItemKeyToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "item_key"));
    public static final StreamCodec<ByteBuf, PacketItemKeyToServer> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeByte(m.key);
                buf.writeByte(m.mod);
            },
            buf -> new PacketItemKeyToServer(buf.readByte(), buf.readByte()));

    public PacketItemKeyToServer(int key, int mod) {
        this((byte) key, (byte) mod);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketItemKeyToServer msg, ServerPlayer player) {
        if (msg.key == 1) {
            if (player.getMainHandItem().getItem() instanceof ICaster) {
                CasterManager.toggleMisc(player.getMainHandItem(), player.level(), player, msg.mod);
            } else if (player.getOffhandItem().getItem() instanceof ICaster) {
                CasterManager.toggleMisc(player.getOffhandItem(), player.level(), player, msg.mod);
            }
        }
    }
}
