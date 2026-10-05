package thaumcraft.common.lib.network.playerdata;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Клиент → сервер: начать исследование (first = true, с карты исследований) или перейти к следующей стадии (first = false,
 * со страницы исследования, с проверкой и списанием требований).
 * Раздел 7 (проверка на сервере): ключ должен быть существующим исследованием (в оригинале клиент мог добавить себе
 * любой ключ, например отметку «предмет создан»); без проверки требований разрешено только начать исследование.
 */
public record PacketSyncProgressToServer(String key, boolean first, boolean checks, boolean noFlags) implements CustomPacketPayload {
    public static final Type<PacketSyncProgressToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_progress"));
    public static final StreamCodec<ByteBuf, PacketSyncProgressToServer> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), PacketSyncProgressToServer::key,
            ByteBufCodecs.BOOL, PacketSyncProgressToServer::first,
            ByteBufCodecs.BOOL, PacketSyncProgressToServer::checks,
            ByteBufCodecs.BOOL, PacketSyncProgressToServer::noFlags,
            PacketSyncProgressToServer::new);

    public PacketSyncProgressToServer(String key, boolean first) {
        this(key, first, false, true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** onMessage оригинала (главный поток сервера). */
    public static void handle(PacketSyncProgressToServer message, ServerPlayer player) {
        if (player == null || ResearchCategories.getResearch(message.key) == null) {
            return;
        }
        if (message.first != ThaumcraftCapabilities.knowsResearch(player, message.key)) {
            boolean checks = message.checks || !message.first;
            if (checks && !checkRequisites(player, message.key)) {
                return;
            }
            if (message.noFlags) {
                ResearchManager.noFlags = true;
            }
            ResearchManager.progressResearch(player, message.key);
            // флаг одноразовый: если продвижения не было, он не должен перейти к следующему вызову
            ResearchManager.noFlags = false;
        }
    }

    private static boolean checkRequisites(ServerPlayer player, String key) {
        ResearchEntry research = ResearchCategories.getResearch(key);
        if (research.getStages() == null) {
            return true;
        }
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        int currentStage = knowledge.getResearchStage(key) - 1;
        if (currentStage < 0) {
            return false;
        }
        if (currentStage >= research.getStages().length) {
            return true;
        }
        ResearchStage stage = research.getStages()[currentStage];
        ItemStack[] o = stage.getObtain();
        if (o != null) {
            for (ItemStack itemStack : o) {
                if (!InventoryUtils.isPlayerCarryingAmount(player, itemStack, false)) {
                    return false;
                }
            }
            for (ItemStack itemStack : o) {
                InventoryUtils.consumePlayerItem(player, itemStack, true, false);
            }
        }
        ItemStack[] c = stage.getCraft();
        if (c != null) {
            for (int a = 0; a < c.length; a++) {
                if (!knowledge.isResearchKnown("[#]" + stage.getCraftReference()[a])) {
                    return false;
                }
            }
        }
        String[] r = stage.getResearch();
        if (r != null) {
            for (String s : r) {
                if (!ThaumcraftCapabilities.knowsResearchStrict(player, s)) {
                    return false;
                }
            }
        }
        ResearchStage.Knowledge[] k = stage.getKnow();
        if (k != null) {
            for (ResearchStage.Knowledge value : k) {
                if (knowledge.getKnowledge(value.type, value.category) < value.amount) {
                    return false;
                }
            }
            for (ResearchStage.Knowledge value : k) {
                ResearchManager.addKnowledge(player, value.type, value.category, -value.amount * value.type.getProgression());
            }
        }
        return true;
    }
}
