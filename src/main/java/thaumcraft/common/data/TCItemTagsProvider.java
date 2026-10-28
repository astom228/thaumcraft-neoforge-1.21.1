package thaumcraft.common.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.TCTags;

import static thaumcraft.api.items.ItemsTC.*;

/** Теги предметов: записи OreDictionary оригинала (api/OreDictionaryEntries) и ванильные категории. */
public class TCItemTagsProvider extends ItemTagsProvider {
    public TCItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                              CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper) {
        super(output, lookup, blockTags, Thaumcraft.MODID, helper);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addTags(HolderLookup.Provider provider) {
        copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
        copy(BlockTags.PLANKS, ItemTags.PLANKS);
        copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
        copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
        copy(BlockTags.SLABS, ItemTags.SLABS);
        copy(BlockTags.STAIRS, ItemTags.STAIRS);
        copy(BlockTags.SAPLINGS, ItemTags.SAPLINGS);
        copy(BlockTags.LEAVES, ItemTags.LEAVES);
        copy(TCTags.Blocks.ORES_AMBER, TCTags.Items.ORES_AMBER);
        copy(TCTags.Blocks.ORES_CINNABAR, TCTags.Items.ORES_CINNABAR);
        copy(TCTags.Blocks.ORES_QUARTZ, TCTags.Items.ORES_QUARTZ);
        copy(Tags.Blocks.ORES, Tags.Items.ORES);
        copy(TCTags.Blocks.STORAGE_BLOCKS_THAUMIUM, TCTags.Items.STORAGE_BLOCKS_THAUMIUM);
        copy(TCTags.Blocks.STORAGE_BLOCKS_VOID, TCTags.Items.STORAGE_BLOCKS_VOID);
        copy(TCTags.Blocks.STORAGE_BLOCKS_BRASS, TCTags.Items.STORAGE_BLOCKS_BRASS);
        copy(Tags.Blocks.STORAGE_BLOCKS, Tags.Items.STORAGE_BLOCKS);

        tag(TCTags.Items.GEMS_AMBER).add(amber.get());
        tag(Tags.Items.GEMS).addTag(TCTags.Items.GEMS_AMBER);
        tag(TCTags.Items.QUICKSILVER).add(quicksilver.get());

        // инструменты и броня: ванильные категории (зачаровываемость на столе, ремонт и т.п.)
        tag(ItemTags.SWORDS).add(thaumiumSword.get(), voidSword.get());
        tag(ItemTags.PICKAXES).add(thaumiumPick.get(), voidPick.get());
        tag(ItemTags.AXES).add(thaumiumAxe.get(), voidAxe.get());
        tag(ItemTags.SHOVELS).add(thaumiumShovel.get(), voidShovel.get());
        tag(ItemTags.HOES).add(thaumiumHoe.get(), voidHoe.get());
        tag(ItemTags.HEAD_ARMOR).add(thaumiumHelm.get(), voidHelm.get(), goggles.get());
        tag(ItemTags.CHEST_ARMOR).add(thaumiumChest.get(), voidChest.get(), clothChest.get());
        tag(ItemTags.LEG_ARMOR).add(thaumiumLegs.get(), voidLegs.get(), clothLegs.get());
        tag(ItemTags.FOOT_ARMOR).add(thaumiumBoots.get(), voidBoots.get(), clothBoots.get(), travellerBoots.get());
        // безделушки: слоты Curios
        tag(TCTags.Items.CURIOS_NECKLACE).add(amuletMundane.get(), amuletFancy.get(), amuletVisFound.get(), amuletVis.get());
        tag(TCTags.Items.CURIOS_RING).add(ringMundane.get(), ringApprentice.get(), ringFancy.get());
        tag(TCTags.Items.CURIOS_HEAD).add(goggles.get());
        tag(TCTags.Items.CURIOS_BELT).add(girdleMundane.get(), girdleFancy.get(), focusPouch.get());
        // роба окрашивается, как кожаная броня
        tag(ItemTags.DYEABLE).add(clothChest.get(), clothLegs.get(), clothBoots.get());

        tag(TCTags.Items.INGOTS_THAUMIUM).add(ingotThaumium.get());
        tag(TCTags.Items.INGOTS_VOID).add(ingotVoid.get());
        tag(TCTags.Items.INGOTS_BRASS).add(ingotBrass.get());
        tag(Tags.Items.INGOTS).addTags(TCTags.Items.INGOTS_THAUMIUM, TCTags.Items.INGOTS_VOID, TCTags.Items.INGOTS_BRASS);

        tag(TCTags.Items.NUGGETS_IRON).add(nuggetIron.get());
        tag(TCTags.Items.NUGGETS_COPPER).add(nuggetCopper.get());
        tag(TCTags.Items.NUGGETS_TIN).add(nuggetTin.get());
        tag(TCTags.Items.NUGGETS_SILVER).add(nuggetSilver.get());
        tag(TCTags.Items.NUGGETS_LEAD).add(nuggetLead.get());
        tag(TCTags.Items.NUGGETS_QUICKSILVER).add(nuggetQuicksilver.get());
        tag(TCTags.Items.NUGGETS_THAUMIUM).add(nuggetThaumium.get());
        tag(TCTags.Items.NUGGETS_VOID).add(nuggetVoid.get());
        tag(TCTags.Items.NUGGETS_BRASS).add(nuggetBrass.get());
        tag(TCTags.Items.NUGGETS_QUARTZ).add(nuggetQuartz.get());
        tag(TCTags.Items.NUGGETS_MEAT).add(chunkBeef.get(), chunkChicken.get(), chunkPork.get(), chunkFish.get(), chunkRabbit.get(), chunkMutton.get());
        tag(Tags.Items.NUGGETS).addTags(TCTags.Items.NUGGETS_IRON, TCTags.Items.NUGGETS_COPPER, TCTags.Items.NUGGETS_TIN,
                TCTags.Items.NUGGETS_SILVER, TCTags.Items.NUGGETS_LEAD, TCTags.Items.NUGGETS_QUICKSILVER, TCTags.Items.NUGGETS_THAUMIUM,
                TCTags.Items.NUGGETS_VOID, TCTags.Items.NUGGETS_BRASS, TCTags.Items.NUGGETS_QUARTZ);

        tag(TCTags.Items.PLATES_IRON).add(plateIron.get());
        tag(TCTags.Items.PLATES_BRASS).add(plateBrass.get());
        tag(TCTags.Items.PLATES_THAUMIUM).add(plateThaumium.get());
        tag(TCTags.Items.PLATES_VOID).add(plateVoid.get());

        tag(TCTags.Items.CLUSTERS_IRON).add(clusterIron.get());
        tag(TCTags.Items.CLUSTERS_GOLD).add(clusterGold.get());
        tag(TCTags.Items.CLUSTERS_COPPER).add(clusterCopper.get());
        tag(TCTags.Items.CLUSTERS_TIN).add(clusterTin.get());
        tag(TCTags.Items.CLUSTERS_SILVER).add(clusterSilver.get());
        tag(TCTags.Items.CLUSTERS_LEAD).add(clusterLead.get());
        tag(TCTags.Items.CLUSTERS_CINNABAR).add(clusterCinnabar.get());
        tag(TCTags.Items.CLUSTERS_QUARTZ).add(clusterQuartz.get());

        tag(TCTags.Items.NITOR).add(thaumcraft.api.blocks.BlocksTC.nitor.values().stream().map(b -> b.get().asItem()).toArray(net.minecraft.world.item.Item[]::new));

        // ItemFood(..., isWolfFood = true): мясное угощение и мозг зомби
        tag(ItemTags.WOLF_FOOD).add(tripleMeatTreat.get(), brain.get());
    }
}
