package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerResearchTable;

/**
 * Клиент → сервер: начать новую теорию на столе с выбранными помощниками.
 * Раздел 7: только для стола, меню которого открыто у игрока; помощники — только из найденных сервером вокруг стола
 * (в оригинале сервер доверял списку клиента).
 */
public record PacketStartTheoryToServer(BlockPos pos, Set<String> aids) implements CustomPacketPayload {
    public static final Type<PacketStartTheoryToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "start_theory"));
    public static final StreamCodec<ByteBuf, PacketStartTheoryToServer> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketStartTheoryToServer::pos,
            ByteBufCodecs.<ByteBuf, String, Set<String>>collection(HashSet::new, ByteBufCodecs.stringUtf8(256), 64), PacketStartTheoryToServer::aids,
            PacketStartTheoryToServer::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketStartTheoryToServer message, ServerPlayer player) {
        if (player == null || !(player.containerMenu instanceof ContainerResearchTable menu) || !menu.getPos().equals(message.pos)
                || !menu.stillValid(player)) {
            return;
        }
        Set<String> allowed = menu.tileEntity.checkSurroundingAids();
        Set<String> aids = new HashSet<>(message.aids);
        aids.retainAll(allowed);
        menu.tileEntity.startNewTheory(player, aids);
    }
}
