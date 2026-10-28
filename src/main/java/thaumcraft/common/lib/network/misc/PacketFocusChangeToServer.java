package thaumcraft.common.lib.network.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.items.casters.CasterManager;

/** Клиент → сервер: сменить фокус перчатки в руке (ключ сортировки фокуса или «REMOVE»). Сервер ищет фокус сам. */
public record PacketFocusChangeToServer(String focus) implements CustomPacketPayload {
    public static final Type<PacketFocusChangeToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus_change"));
    public static final StreamCodec<FriendlyByteBuf, PacketFocusChangeToServer> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> buf.writeUtf(m.focus, 512),
            buf -> new PacketFocusChangeToServer(buf.readUtf(512)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketFocusChangeToServer msg, ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        if (player.getMainHandItem().getItem() instanceof ICaster) {
            CasterManager.changeFocus(player.getMainHandItem(), player.level(), player, msg.focus);
        } else if (player.getOffhandItem().getItem() instanceof ICaster) {
            CasterManager.changeFocus(player.getOffhandItem(), player.level(), player, msg.focus);
        }
    }
}
