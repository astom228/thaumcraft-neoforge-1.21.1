package thaumcraft.common.blocks.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemGenericEssentiaContainer;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCTags;

/**
 * Пористый камень (оригинал: прочность 1.0, сопротивление → 3.0). Дроп: с шансом (rand(15) + удача > 13) — случайная находка
 * по весам (кристаллы составных аспектов, янтарь, кластеры, алмаз, изумруд…), иначе — гравий.
 * В оригинале шёлковое касание срабатывало по умолчанию (полный куб) — блок роняет сам себя.
 */
public class BlockStonePorous extends Block {
    private static final Random r = new Random(System.currentTimeMillis());
    private static List<WeightedEntry.Wrapper<ItemStack>> pdrops = null;

    public BlockStonePorous(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        int fortune = 0;
        if (tool != null && !tool.isEmpty()) {
            var lookup = params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> silk = lookup.getOrThrow(Enchantments.SILK_TOUCH);
            if (tool.getEnchantmentLevel(silk) > 0) {
                return List.of(new ItemStack(this));
            }
            fortune = tool.getEnchantmentLevel(lookup.getOrThrow(Enchantments.FORTUNE));
        }
        List<ItemStack> ret = new ArrayList<>();
        int rr = r.nextInt(15) + fortune;
        if (rr > 13) {
            if (pdrops == null || pdrops.isEmpty()) {
                createDrops();
            }
            WeightedRandom.getRandomItem(net.minecraft.util.RandomSource.create(r.nextLong()), pdrops)
                    .ifPresent(w -> ret.add(w.data().copy()));
        } else {
            ret.add(new ItemStack(Items.GRAVEL));
        }
        return ret;
    }

    private static void add(ItemStack stack, int weight) {
        pdrops.add(WeightedEntry.wrap(stack, weight));
    }

    private static boolean tagExists(String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        return BuiltInRegistries.ITEM.getTagOrEmpty(tag).iterator().hasNext();
    }

    private static void createDrops() {
        pdrops = new ArrayList<>();
        for (Aspect aspect : Aspect.getCompoundAspects()) {
            ItemStack is = new ItemStack(ItemsTC.crystalEssence.get());
            ((ItemGenericEssentiaContainer) ItemsTC.crystalEssence.get())
                    .setAspects(is, new AspectList().add(aspect, aspect == Aspect.FLUX ? 100 : (aspect.isPrimal() ? 20 : 1)));
            add(is, 1);
        }
        add(new ItemStack(ItemsTC.amber.get()), 20);
        add(new ItemStack(ItemsTC.clusterIron.get()), 20);
        add(new ItemStack(ItemsTC.clusterGold.get()), 10);
        add(new ItemStack(ItemsTC.clusterCinnabar.get()), 10);
        // ModConfig.foundCopperIngot и т.п. — есть ли такие слитки (медь в 1.21 ванильная)
        add(new ItemStack(ItemsTC.clusterCopper.get()), 10);
        if (tagExists("ingots/tin")) {
            add(new ItemStack(ItemsTC.clusterTin.get()), 10);
        }
        if (tagExists("ingots/silver")) {
            add(new ItemStack(ItemsTC.clusterSilver.get()), 8);
        }
        if (tagExists("ingots/lead")) {
            add(new ItemStack(ItemsTC.clusterLead.get()), 10);
        }
        add(new ItemStack(Items.DIAMOND), 2);
        add(new ItemStack(Items.EMERALD), 4);
        add(new ItemStack(Items.REDSTONE), 8);
        add(new ItemStack(Items.PRISMARINE_CRYSTALS), 3);
        add(new ItemStack(Items.PRISMARINE_SHARD), 3);
        add(new ItemStack(Items.CLAY_BALL), 30);
        add(new ItemStack(Items.QUARTZ), 15);
    }

    /** Сброс кеша находок (при смене данных: теги других модов). */
    public static void resetDrops() {
        pdrops = null;
    }
}
