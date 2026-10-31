package thaumcraft.common.lib.network.misc;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.common.container.ContainerLogistics;

/**
 * Клиент → сервер (порт PacketLogisticsRequestToServer): заказать доставку предмета из окна логистики. Куда везти —
 * из серверного меню (в оригинале координаты присылал клиент); количество — не больше, чем есть у печатей снабжения
 * игрока; заказ разбивается на стопки.
 */
public record PacketLogisticsRequestToServer(ItemStack stack, int size) implements CustomPacketPayload {
    public static final Type<PacketLogisticsRequestToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "logistics_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketLogisticsRequestToServer> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> {
                ItemStack.STREAM_CODEC.encode(buf, m.stack);
                buf.writeVarInt(m.size);
            },
            buf -> new PacketLogisticsRequestToServer(ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketLogisticsRequestToServer msg, ServerPlayer player) {
        if (!(player.containerMenu instanceof ContainerLogistics menu) || msg.stack.isEmpty() || msg.size <= 0) {
            return;
        }
        int left = Math.min(msg.size, menu.available(msg.stack));
        while (left > 0) {
            ItemStack s = msg.stack.copy();
            s.setCount(Math.min(left, s.getMaxStackSize()));
            left -= s.getCount();
            if (menu.target != null) {
                GolemHelper.requestProvisioning(player.level(), menu.target, menu.side, s);
            } else {
                GolemHelper.requestProvisioning(player.level(), player, s);
            }
        }
    }
}
