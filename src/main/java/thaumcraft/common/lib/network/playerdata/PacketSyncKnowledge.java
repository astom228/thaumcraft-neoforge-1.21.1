package thaumcraft.common.lib.network.playerdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;

/** Сервер → клиент: полные знания игрока (NBT как в оригинале). */
public record PacketSyncKnowledge(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PacketSyncKnowledge> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_knowledge"));
    public static final StreamCodec<FriendlyByteBuf, PacketSyncKnowledge> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(PacketSyncKnowledge::new, PacketSyncKnowledge::data).cast();

    /** Как в оригинале: данные снимаются до отправки, затем на сервере снимаются флаги POPUP (тосты показываются один раз). */
    public static PacketSyncKnowledge create(ServerPlayer player, IPlayerKnowledge pk) {
        CompoundTag data = pk.serializeNBT(player.registryAccess());
        for (String key : pk.getResearchList()) {
            pk.clearResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
        }
        return new PacketSyncKnowledge(data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
