package thaumcraft.common.lib.network.playerdata;

import io.netty.handler.codec.DecoderException;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.tiles.crafting.FocusElementNode;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

/**
 * Клиент → сервер: дерево узлов и имя фокуса из экрана манипулятора. Принимается, только если у игрока открыто меню
 * именно этого манипулятора, он не работает и дерево корректно (в оригинале проверок не было — раздел 7).
 */
public record PacketFocusNodesToServer(long loc, HashMap<Integer, FocusElementNode> data, String name) implements CustomPacketPayload {
    public static final int MAX_NAME = 50;
    public static final Type<PacketFocusNodesToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus_nodes"));
    public static final StreamCodec<FriendlyByteBuf, PacketFocusNodesToServer> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeLong(m.loc);
                buf.writeVarInt(m.data.size());
                for (FocusElementNode node : m.data.values()) {
                    buf.writeNbt(node.serialize());
                }
                buf.writeUtf(m.name, MAX_NAME);
            },
            buf -> {
                long loc = buf.readLong();
                int m = buf.readVarInt();
                if (m < 0 || m > FocusElementNode.MAX_NODES) {
                    throw new DecoderException("Too many focus nodes: " + m);
                }
                HashMap<Integer, FocusElementNode> data = new HashMap<>();
                for (int a = 0; a < m; a++) {
                    CompoundTag t = buf.readNbt();
                    FocusElementNode node = new FocusElementNode();
                    if (t != null) {
                        node.deserialize(t);
                    }
                    data.put(node.id, node);
                }
                return new PacketFocusNodesToServer(loc, data, buf.readUtf(MAX_NAME));
            });

    public PacketFocusNodesToServer(BlockPos pos, HashMap<Integer, FocusElementNode> data, String name) {
        this(pos.asLong(), data, name);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Открытое меню этого манипулятора, игрок рядом, манипулятор не работает. */
    static TileFocalManipulator openTable(ServerPlayer player, long loc) {
        if (player.containerMenu instanceof ContainerFocalManipulator c && c.stillValid(player) && c.getTile().getBlockPos().asLong() == loc
                && c.getTile().vis <= 0.0F) {
            return c.getTile();
        }
        return null;
    }

    public static void handle(PacketFocusNodesToServer msg, ServerPlayer player) {
        TileFocalManipulator rt = openTable(player, msg.loc);
        if (rt == null || !FocusElementNode.isStructureValid(msg.data)) {
            return;
        }
        rt.data.clear();
        rt.data = msg.data;
        rt.focusName = msg.name;
        rt.setChanged();
    }
}
