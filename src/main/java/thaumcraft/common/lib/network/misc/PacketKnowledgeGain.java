package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Сервер → клиент: получено очко знаний (эффект на HUD и звук). cat — ключ категории или "". */
public record PacketKnowledgeGain(byte knowledgeType, String cat) implements CustomPacketPayload {
    public static final Type<PacketKnowledgeGain> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "knowledge_gain"));
    public static final StreamCodec<ByteBuf, PacketKnowledgeGain> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, PacketKnowledgeGain::knowledgeType,
            ByteBufCodecs.STRING_UTF8, PacketKnowledgeGain::cat,
            PacketKnowledgeGain::new);

    public PacketKnowledgeGain {
        cat = cat == null ? "" : cat;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
