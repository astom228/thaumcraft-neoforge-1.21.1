package thaumcraft.common.lib.events;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;

/**
 * Редкоземельные элементы из руд (HarvestDropsEvent оригинала → глобальный модификатор добычи, чтобы срабатывал при любой
 * добыче блока, как в 1.12): без шёлкового касания с шансом по руде добавляется самородок редкоземельных элементов.
 * Глубинносланцевые руды 1.21 считаются теми же рудами (DECISIONS 95).
 */
public class RareEarthsLootModifier extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Thaumcraft.MODID);

    public static final MapCodec<RareEarthsLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, RareEarthsLootModifier::new));

    public static final Supplier<MapCodec<RareEarthsLootModifier>> RARE_EARTHS = LOOT_MODIFIERS.register("rare_earths", () -> CODEC);

    public RareEarthsLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null) {
            return generatedLoot;
        }
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (tool != null && !tool.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(
                context.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), tool) > 0) {
            return generatedLoot;
        }
        float chance = chance(state.getBlock());
        if (chance > 0.0F && context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(ItemsTC.nuggetRareEarth.get()));
        }
        return generatedLoot;
    }

    private static float chance(Block b) {
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return 0.05F;
        }
        if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) {
            return 0.075F;
        }
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) {
            return 0.01F;
        }
        if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) {
            return 0.001F;
        }
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return 0.01F;
        }
        if (b == Blocks.NETHER_QUARTZ_ORE) {
            return 0.01F;
        }
        if (b == BlocksTC.oreAmber.get() || b == BlocksTC.oreQuartz.get()) {
            return 0.05F;
        }
        return 0.0F;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
