package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Сервер → клиент (порт PacketMiscEvent): 0 — событие Искажения (сердцебиение), 1 — долгий туман, 2 — короткий туман;
 * 3, 4 — звуки-пугалки Искажения (шипение крипера, далёкий взрыв): клиент сам решает по настройке nostress (в оригинале
 * сервер читал графическую настройку, на выделенном сервере её нет).
 */
public record PacketMiscEvent(byte kind) implements CustomPacketPayload {
    public static final byte WARP_EVENT = 0;
    public static final byte MIST_EVENT = 1;
    public static final byte MIST_EVENT_SHORT = 2;
    public static final byte WARP_SOUND_PRIMED = 3;
    public static final byte WARP_SOUND_EXPLODE = 4;
    public static final Type<PacketMiscEvent> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "misc_event"));
    public static final StreamCodec<ByteBuf, PacketMiscEvent> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BYTE, PacketMiscEvent::kind,
            PacketMiscEvent::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
