package thaumcraft.common.items.curios;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.common.lib.SoundsTC;

/**
 * Реликт (порт ItemCurio; meta → отдельный предмет): правый клик — наблюдение и теория в своей категории и в случайной,
 * у мистического реликта и «Багровых обрядов» — ещё Искажение. Тип по meta оригинала:
 * 0 arcane (Ауромантия), 1 preserved (Алхимия), 2 ancient (Големостроение), 3 eldritch (Мистика), 4 knowledge (Наполнение),
 * 5 twisted (Артефакторика), 6 rites (Багровые обряды).
 */
public class ItemCurio extends Item {
    private final int type;

    public ItemCurio(int type, Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON));
        this.type = type;
    }

    /** Вариант оригинала: arcane, preserved, ancient, eldritch, knowledge, twisted, rites. */
    public static final String[] VARIANTS = {"arcane", "preserved", "ancient", "eldritch", "knowledge", "twisted", "rites"};

    public String getVariant() {
        return VARIANTS[this.type];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.curio.text"));
    }

    private static void know(Player player, String cat, int oProg, int tProg) {
        RandomSource r = player.getRandom();
        ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory(cat),
                Mth.randomBetweenInclusive(r, oProg / 3, oProg / 2));
        ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY, ResearchCategories.getResearchCategory(cat),
                Mth.randomBetweenInclusive(r, tProg / 5, tProg / 4));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player player, InteractionHand hand) {
        worldIn.playSound(null, player.getX(), player.getY(), player.getZ(), SoundsTC.learn.get(), SoundSource.NEUTRAL, 0.5F,
                0.4F / (worldIn.random.nextFloat() * 0.4F + 0.8F));
        if (!worldIn.isClientSide) {
            int oProg = IPlayerKnowledge.EnumKnowledgeType.OBSERVATION.getProgression();
            int tProg = IPlayerKnowledge.EnumKnowledgeType.THEORY.getProgression();
            switch (this.type) {
                case 1 -> know(player, "ALCHEMY", oProg, tProg);
                case 2 -> know(player, "GOLEMANCY", oProg, tProg);
                case 3 -> {
                    know(player, "ELDRITCH", oProg, tProg);
                    ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.NORMAL);
                    ThaumcraftApi.internalMethods.addWarpToPlayer(player, 5, IPlayerWarp.EnumWarpType.TEMPORARY);
                }
                case 4 -> know(player, "INFUSION", oProg, tProg);
                case 5 -> know(player, "ARTIFICE", oProg, tProg);
                case 6 -> {
                    int aw = ThaumcraftApi.internalMethods.getActualWarp(player);
                    if (aw <= 20) {
                        player.sendSystemMessage(Component.translatable("fail.crimsonrites").withStyle(ChatFormatting.DARK_PURPLE));
                        return InteractionResultHolder.pass(player.getItemInHand(hand));
                    }
                    if (!ThaumcraftCapabilities.getKnowledge(player).isResearchKnown("CrimsonRites")) {
                        ThaumcraftApi.internalMethods.completeResearch(player, "CrimsonRites");
                    }
                    know(player, "ELDRITCH", oProg, tProg);
                    ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.NORMAL);
                    ThaumcraftApi.internalMethods.addWarpToPlayer(player, 5, IPlayerWarp.EnumWarpType.TEMPORARY);
                    if (player.getRandom().nextBoolean()) {
                        ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.PERMANENT);
                    }
                }
                default -> know(player, "AUROMANCY", oProg, tProg);
            }
            ResearchCategory[] rc = ResearchCategories.researchCategories.values().toArray(new ResearchCategory[0]);
            RandomSource r = player.getRandom();
            ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, rc[r.nextInt(rc.length)],
                    Mth.randomBetweenInclusive(r, oProg / 3, oProg / 2));
            ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY, rc[r.nextInt(rc.length)],
                    Mth.randomBetweenInclusive(r, tProg / 5, tProg / 4));
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
