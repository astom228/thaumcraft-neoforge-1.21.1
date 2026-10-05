package thaumcraft.common.lib.network.playerdata;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;

/**
 * Сервер → клиент: изменившиеся исследования и очки знаний (раздел 7: дельты вместо полного NBT; в оригинале
 * при любом изменении отправлялся PacketSyncKnowledge целиком). Полная синхронизация — при входе, возрождении, смене измерения.
 */
public record PacketSyncKnowledgeDelta(List<ResearchDelta> research, List<KnowledgeDelta> knowledge) implements CustomPacketPayload {
    public static final Type<PacketSyncKnowledgeDelta> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_knowledge_delta"));

    public record ResearchDelta(String key, boolean present, int stage, int flags) {
        static final StreamCodec<ByteBuf, ResearchDelta> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ResearchDelta::key,
                ByteBufCodecs.BOOL, ResearchDelta::present,
                ByteBufCodecs.VAR_INT, ResearchDelta::stage,
                ByteBufCodecs.VAR_INT, ResearchDelta::flags,
                ResearchDelta::new);
    }

    public record KnowledgeDelta(String key, int amount) {
        static final StreamCodec<ByteBuf, KnowledgeDelta> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, KnowledgeDelta::key,
                ByteBufCodecs.VAR_INT, KnowledgeDelta::amount,
                KnowledgeDelta::new);
    }

    public static final StreamCodec<ByteBuf, PacketSyncKnowledgeDelta> STREAM_CODEC = StreamCodec.composite(
            ResearchDelta.CODEC.apply(ByteBufCodecs.list()), PacketSyncKnowledgeDelta::research,
            KnowledgeDelta.CODEC.apply(ByteBufCodecs.list()), PacketSyncKnowledgeDelta::knowledge,
            PacketSyncKnowledgeDelta::new);

    /** Снимок изменений; затем, как при полной синхронизации, на сервере снимаются флаги POPUP (тост показывается один раз). */
    public static PacketSyncKnowledgeDelta create(PlayerKnowledge pk) {
        List<ResearchDelta> research = new ArrayList<>();
        for (String key : pk.getDirtyResearch()) {
            boolean present = pk.getResearchList().contains(key);
            research.add(new ResearchDelta(key, present, present ? Math.max(0, pk.getResearchStage(key)) : 0, pk.getFlagBits(key)));
        }
        List<KnowledgeDelta> knowledge = new ArrayList<>();
        for (String key : pk.getDirtyKnowledge()) {
            knowledge.add(new KnowledgeDelta(key, pk.getKnowledgeRawByKey(key)));
        }
        for (ResearchDelta d : research) {
            pk.clearResearchFlag(d.key(), IPlayerKnowledge.EnumResearchFlag.POPUP);
        }
        return new PacketSyncKnowledgeDelta(research, knowledge);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
