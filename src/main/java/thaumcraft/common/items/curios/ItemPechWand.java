package thaumcraft.common.items.curios;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.common.lib.SoundsTC;

/**
 * Жезл пехов (порт ItemPechWand): реликт; после «Основ ауромантии» ПКМ расходует его — открывает исследование FOCUSPECH и даёт
 * наблюдения (⅓–½ уровня) и теорию (⅕–¼ уровня) случайных разделов.
 */
public class ItemPechWand extends Item {
    public ItemPechWand(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.curio.text"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (!knowledge.isResearchKnown("BASEAUROMANCY")) {
            if (!world.isClientSide) {
                player.sendSystemMessage(Component.translatable("not.pechwand").withStyle(ChatFormatting.RED));
            }
            return super.use(world, player, hand);
        }
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundsTC.learn.get(), SoundSource.NEUTRAL, 0.5F,
                0.4F / (world.random.nextFloat() * 0.4F + 0.8F));
        if (!world.isClientSide) {
            if (!knowledge.isResearchKnown("FOCUSPECH")) {
                ThaumcraftApi.internalMethods.progressResearch(player, "FOCUSPECH");
                player.sendSystemMessage(Component.translatable("got.pechwand").withStyle(ChatFormatting.DARK_PURPLE));
            }
            ResearchCategory[] rc = ResearchCategories.researchCategories.values().toArray(new ResearchCategory[0]);
            int oProg = IPlayerKnowledge.EnumKnowledgeType.OBSERVATION.getProgression();
            ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, rc[player.getRandom().nextInt(rc.length)],
                    Mth.nextInt(player.getRandom(), oProg / 3, oProg / 2));
            int tProg = IPlayerKnowledge.EnumKnowledgeType.THEORY.getProgression();
            ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY, rc[player.getRandom().nextInt(rc.length)],
                    Mth.nextInt(player.getRandom(), tProg / 5, tProg / 4));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide);
    }
}
