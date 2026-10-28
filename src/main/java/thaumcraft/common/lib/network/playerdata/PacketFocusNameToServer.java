package thaumcraft.common.lib.network.playerdata;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

/** Клиент → сервер: имя будущего фокуса (поле экрана манипулятора). Только из открытого меню этого манипулятора. */
public record PacketFocusNameToServer(long loc, String name) implements CustomPacketPayload {
    public static final Type<PacketFocusNameToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus_name"));
    public static final StreamCodec<FriendlyByteBuf, PacketFocusNameToServer> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeLong(m.loc);
                buf.writeUtf(m.name, PacketFocusNodesToServer.MAX_NAME);
            },
            buf -> new PacketFocusNameToServer(buf.readLong(), buf.readUtf(PacketFocusNodesToServer.MAX_NAME)));

    public PacketFocusNameToServer(BlockPos pos, String name) {
        this(pos.asLong(), name);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketFocusNameToServer msg, ServerPlayer player) {
        TileFocalManipulator rt = PacketFocusNodesToServer.openTable(player, msg.loc);
        if (rt != null) {
            rt.focusName = msg.name;
            rt.setChanged();
        }
    }
}
