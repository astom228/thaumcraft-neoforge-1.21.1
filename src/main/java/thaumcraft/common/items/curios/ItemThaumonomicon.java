package thaumcraft.common.items.curios;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.gui.ResearchScreens;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.CommandThaumcraft;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * Таумономикон (порт ItemThaumonomicon; meta 0 — обычный, meta 1 — «шпаргалка» cheaters_thaumonomicon).
 * Правый клик: на сервере — открыть «братьев» известных исследований (шпаргалка при разрешении в конфиге — все исследования),
 * синхронизация знаний; на клиенте — звук страницы и карта исследований.
 */
public class ItemThaumonomicon extends Item {
    private final boolean cheat;

    public ItemThaumonomicon(boolean cheat, Properties properties) {
        super(properties.stacksTo(1).rarity(cheat ? Rarity.EPIC : Rarity.UNCOMMON));
        this.cheat = cheat;
    }

    public boolean isCheat() {
        return this.cheat;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (this.cheat) {
            tooltip.add(Component.translatable("item.thaumcraft.cheaters_thaumonomicon.desc").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide) {
            if (ModConfig.CONFIG_MISC.allowCheatSheet.get() && this.cheat) {
                for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
                    for (ResearchEntry ri : cat.research.values()) {
                        CommandThaumcraft.giveRecursiveResearch(player, ri.getKey());
                    }
                }
            } else {
                for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
                    for (ResearchEntry ri : cat.research.values()) {
                        if (ThaumcraftCapabilities.knowsResearch(player, ri.getKey()) && ri.getSiblings() != null) {
                            for (String sib : ri.getSiblings()) {
                                if (!ThaumcraftCapabilities.knowsResearch(player, sib)) {
                                    ResearchManager.completeResearch(player, sib);
                                }
                            }
                        }
                    }
                }
            }
            if (player instanceof ServerPlayer sp) {
                ThaumcraftCapabilities.getKnowledge(player).sync(sp);
            }
        } else {
            world.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundsTC.page.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
            ResearchScreens.openBrowser();
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
