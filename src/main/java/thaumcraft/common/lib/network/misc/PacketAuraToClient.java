package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.aura.AuraChunk;

/** Сервер → клиент: аура чанка под игроком для HUD (шлют таумометр и кастер, когда их держат в руке). */
public record PacketAuraToClient(short base, float vis, float flux) implements CustomPacketPayload {
    public static final Type<PacketAuraToClient> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "aura_to_client"));
    public static final StreamCodec<ByteBuf, PacketAuraToClient> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT, PacketAuraToClient::base,
            ByteBufCodecs.FLOAT, PacketAuraToClient::vis,
            ByteBufCodecs.FLOAT, PacketAuraToClient::flux,
            PacketAuraToClient::new);

    public PacketAuraToClient(AuraChunk ac) {
        this(ac.getBase(), ac.getVis(), ac.getFlux());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
