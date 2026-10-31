package thaumcraft.common.lib.network.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.common.container.ContainerGolemBuilder;
import thaumcraft.common.entities.construct.golem.GolemProperties;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

/**
 * Клиент → сервер: собрать голема заданного состава на прессе. Принимается только из открытого окна этого пресса, если
 * пресс свободен, состав допустим (существующие детали, ранг 0, лишние байты пусты) и все детали изучены игроком —
 * в оригинале сервер не проверял ничего, кроме наличия деталей.
 */
public record PacketStartGolemCraftToServer(long pos, long golem) implements CustomPacketPayload {
    public static final Type<PacketStartGolemCraftToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "start_golem_craft"));
    public static final StreamCodec<FriendlyByteBuf, PacketStartGolemCraftToServer> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                buf.writeLong(m.pos);
                buf.writeLong(m.golem);
            },
            buf -> new PacketStartGolemCraftToServer(buf.readLong(), buf.readLong()));

    public PacketStartGolemCraftToServer(BlockPos pos, long golem) {
        this(pos.asLong(), golem);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Байты состава: 0 материал, 1 голова, 2 руки, 3 ноги, 4 дополнение (5 — ранг, 6–7 не используются). */
    private static int part(long data, int index) {
        return (int) (data >>> (8 * (7 - index))) & 0xFF;
    }

    public static boolean isValidCraft(ServerPlayer player, long id) {
        if ((id & 0xFFFFFFL) != 0L) {
            return false;
        }
        if (part(id, 0) >= GolemMaterial.getMaterials().length || part(id, 1) >= GolemHead.getHeads().length || part(id, 2) >= GolemArm.getArms().length
                || part(id, 3) >= GolemLeg.getLegs().length || part(id, 4) >= GolemAddon.getAddons().length) {
            return false;
        }
        IGolemProperties props = GolemProperties.fromLong(id);
        return ThaumcraftCapabilities.knowsResearchStrict(player, props.getMaterial().research)
                && ThaumcraftCapabilities.knowsResearchStrict(player, props.getHead().research)
                && ThaumcraftCapabilities.knowsResearchStrict(player, props.getArms().research)
                && ThaumcraftCapabilities.knowsResearchStrict(player, props.getLegs().research)
                && ThaumcraftCapabilities.knowsResearchStrict(player, props.getAddon().research);
    }

    public static void handle(PacketStartGolemCraftToServer msg, ServerPlayer player) {
        if (!(player.containerMenu instanceof ContainerGolemBuilder menu) || !menu.stillValid(player)) {
            return;
        }
        TileGolemBuilder tile = menu.getTile();
        if (tile.getBlockPos().asLong() != msg.pos || tile.isBusy() || !isValidCraft(player, msg.golem)) {
            return;
        }
        tile.startCraft(msg.golem, player);
    }
}
