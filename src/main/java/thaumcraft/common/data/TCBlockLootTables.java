package thaumcraft.common.data;

import java.util.Set;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigBlocks;

/** Таблицы добычи блоков (в оригинале — getDrops/getItemDropped/quantityDropped в классах блоков). */
public class TCBlockLootTables extends BlockLootSubProvider {
    // BlockLeavesTC.getSaplingDropChance: великое дерево 100, серебряное 75; удача уменьшает на 2<<fortune (минимум 10)
    private static final float[] GREATWOOD_SAPLING = {1F / 100, 1F / 96, 1F / 92, 1F / 84};
    private static final float[] SILVERWOOD_SAPLING = {1F / 75, 1F / 71, 1F / 67, 1F / 59};
    // BlockLeavesTC.dropApple: капля ртути с шансом 1/(шанс_яблока*0.75); шанс яблока 200, удача уменьшает на 10<<fortune
    private static final float[] SILVERWOOD_QUICKSILVER = {1F / 150, 1F / 135, 1F / 120, 1F / 90};

    public TCBlockLootTables(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        Holder<Enchantment> fortune = this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);

        // Руды (BlockOreTC): янтарь 1–2 шт. (+удача), кварц 1 шт. (+удача), киноварь — сам блок
        // TODO этап 7: янтарь с шансом 0.066 заменяется на «Сохранившийся реликт» (curio meta 1)
        add(BlocksTC.oreAmber.get(), b -> createSilkTouchDispatchTable(b, applyExplosionDecay(b,
                LootItem.lootTableItem(ItemsTC.amber.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                        .apply(ApplyBonusCount.addOreBonusCount(fortune)))));
        add(BlocksTC.oreQuartz.get(), b -> createOreDrop(b, Items.QUARTZ));
        dropSelf(BlocksTC.oreCinnabar.get());

        // Листва
        add(BlocksTC.leafGreatwood.get(), b -> leaves(b, BlocksTC.saplingGreatwood.get(), GREATWOOD_SAPLING, null, fortune));
        add(BlocksTC.leafSilverwood.get(), b -> leaves(b, BlocksTC.saplingSilverwood.get(), SILVERWOOD_SAPLING, ItemsTC.nuggetQuicksilver.get(), fortune));

        // Плиты: двойная плита роняет две половинки
        for (Block slab : new Block[]{BlocksTC.slabArcaneStone.get(), BlocksTC.slabArcaneBrick.get(), BlocksTC.slabAncient.get(),
                BlocksTC.slabEldritch.get(), BlocksTC.slabGreatwood.get(), BlocksTC.slabSilverwood.get()}) {
            add(slab, this::createSlabItemTable);
        }

        // Пористый камень: дроп считается в BlockStonePorous.getDrops (как getDrops оригинала)
        add(BlocksTC.stonePorous.get(), noDrop());
        // Вис-кристаллы: дроп в BlockCrystal.getDrops (рост + 1 вис-кристаллов)
        for (var c : new Block[]{BlocksTC.crystalAir.get(), BlocksTC.crystalFire.get(), BlocksTC.crystalWater.get(), BlocksTC.crystalEarth.get(),
                BlocksTC.crystalOrder.get(), BlocksTC.crystalEntropy.get(), BlocksTC.crystalTaint.get()}) {
            add(c, noDrop());
        }

        // Остальные блоки этапа роняют сами себя
        for (DeferredHolder<Block, ? extends Block> holder : ConfigBlocks.BLOCKS.getEntries()) {
            Block block = holder.get();
            if (!this.map.containsKey(block.getLootTable())) {
                dropSelf(block);
            }
        }
    }

    private LootTable.Builder leaves(Block leaves, Block sapling, float[] saplingChances, Item extra, Holder<Enchantment> fortune) {
        LootItemCondition.Builder shearsOrSilk = MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS)).or(hasSilkTouch());
        LootTable.Builder table = createSelfDropDispatchTable(leaves, shearsOrSilk,
                ((LootPoolSingletonContainer.Builder<?>) applyExplosionCondition(leaves, LootItem.lootTableItem(sapling)))
                        .when(BonusLevelTableCondition.bonusLevelFlatChance(fortune, saplingChances)));
        if (extra != null) {
            table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shearsOrSilk.invert())
                    .add(((LootPoolSingletonContainer.Builder<?>) applyExplosionCondition(leaves, LootItem.lootTableItem(extra)))
                            .when(BonusLevelTableCondition.bonusLevelFlatChance(fortune, SILVERWOOD_QUICKSILVER))));
        }
        return table;
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ConfigBlocks.BLOCKS.getEntries().stream().map(h -> (Block) h.get())::iterator;
    }
}
