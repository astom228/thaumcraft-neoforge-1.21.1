package thaumcraft.common.lib.network.playerdata;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/**
 * Клиент → сервер: флаги просмотра исследования (PAGE, POPUP, RESEARCH) после открытия его страницы.
 * Раздел 7: принимается только для исследования, которое игрок уже знает (флаги влияют лишь на подсветку в книге).
 */
public record PacketSyncResearchFlagsToServer(String key, byte flags) implements CustomPacketPayload {
    public static final Type<PacketSyncResearchFlagsToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_research_flags"));
    public static final StreamCodec<ByteBuf, PacketSyncResearchFlagsToServer> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), PacketSyncResearchFlagsToServer::key,
            ByteBufCodecs.BYTE, PacketSyncResearchFlagsToServer::flags,
            PacketSyncResearchFlagsToServer::new);

    /** Флаги снимаются с клиентских знаний игрока (Utils.pack: бит 0 — PAGE, 1 — POPUP, 2 — RESEARCH). */
    public static PacketSyncResearchFlagsToServer of(Player player, String key) {
        IPlayerKnowledge k = ThaumcraftCapabilities.getKnowledge(player);
        int b = (k.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.PAGE) ? 1 : 0)
                | (k.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP) ? 2 : 0)
                | (k.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.RESEARCH) ? 4 : 0);
        return new PacketSyncResearchFlagsToServer(key, (byte) b);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** onMessage оригинала (главный поток сервера). */
    public static void handle(PacketSyncResearchFlagsToServer message, ServerPlayer player) {
        if (player == null) {
            return;
        }
        IPlayerKnowledge k = ThaumcraftCapabilities.getKnowledge(player);
        if (!k.isResearchKnown(message.key)) {
            return;
        }
        set(k, message.key, IPlayerKnowledge.EnumResearchFlag.PAGE, (message.flags & 1) != 0);
        set(k, message.key, IPlayerKnowledge.EnumResearchFlag.POPUP, (message.flags & 2) != 0);
        set(k, message.key, IPlayerKnowledge.EnumResearchFlag.RESEARCH, (message.flags & 4) != 0);
    }

    private static void set(IPlayerKnowledge k, String key, IPlayerKnowledge.EnumResearchFlag flag, boolean on) {
        if (on) {
            k.setResearchFlag(key, flag);
        } else {
            k.clearResearchFlag(key, flag);
        }
    }
}
