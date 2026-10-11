package thaumcraft.common.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.TCTags;

import static thaumcraft.api.blocks.BlocksTC.*;

/**
 * Теги блоков. Инструменты: в 1.12 камень/металл (Material.ROCK/IRON) — кирка любого уровня, дерево — топор;
 * setHarvestLevel: янтарная руда — кирка 1 (каменная), киноварь — кирка 2 (железная).
 */
public class TCBlockTagsProvider extends BlockTagsProvider {
    public TCBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
        super(output, lookup, Thaumcraft.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(oreAmber.get(), oreCinnabar.get(), oreQuartz.get(),
                stoneArcane.get(), stoneArcaneBrick.get(), stoneAncient.get(), stoneAncientTile.get(), stoneAncientRock.get(),
                stoneAncientGlyphed.get(), stoneAncientDoorway.get(), stoneEldritchTile.get(), stonePorous.get(),
                stairsArcane.get(), stairsArcaneBrick.get(), stairsAncient.get(),
                slabArcaneStone.get(), slabArcaneBrick.get(), slabAncient.get(), slabEldritch.get(),
                metalBlockBrass.get(), metalBlockThaumium.get(), metalBlockVoid.get(), metalAlchemical.get(), metalAlchemicalAdvanced.get(),
                matrixSpeed.get(), matrixCost.get(), tableStone.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(oreAmber.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(oreCinnabar.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(plankGreatwood.get(), plankSilverwood.get(), stairsGreatwood.get(), stairsSilverwood.get(),
                slabGreatwood.get(), slabSilverwood.get(), tableWood.get(), researchTable.get(),
                arcaneWorkbench.get(), arcaneWorkbenchCharger.get());

        // isBeaconBase = true: BlockStoneTC, BlockMetalTC, BlockTranslucent
        tag(BlockTags.BEACON_BASE_BLOCKS).add(stoneArcane.get(), stoneArcaneBrick.get(), stoneAncient.get(), stoneAncientTile.get(),
                stoneAncientRock.get(), stoneAncientGlyphed.get(), stoneAncientDoorway.get(), stoneEldritchTile.get(), matrixSpeed.get(), matrixCost.get(),
                metalBlockBrass.get(), metalBlockThaumium.get(), metalBlockVoid.get(), metalAlchemical.get(), metalAlchemicalAdvanced.get(),
                amberBlock.get(), amberBrick.get());
        // BlockStoneTC.canEntityDestroy: прочность < 0 — не разрушаются мобами
        tag(BlockTags.WITHER_IMMUNE).add(stoneAncientRock.get(), stoneAncientDoorway.get());
        tag(BlockTags.DRAGON_IMMUNE).add(stoneAncientRock.get(), stoneAncientDoorway.get());

        // OreDictionary: logWood, plankWood, slabWood, treeSapling, treeLeaves (logs нужны и для распада листвы в 1.21)
        tag(BlockTags.LOGS_THAT_BURN).add(logGreatwood.get(), logSilverwood.get());
        tag(BlockTags.PLANKS).add(plankGreatwood.get(), plankSilverwood.get());
        tag(BlockTags.WOODEN_SLABS).add(slabGreatwood.get(), slabSilverwood.get());
        tag(BlockTags.WOODEN_STAIRS).add(stairsGreatwood.get(), stairsSilverwood.get());
        tag(BlockTags.SLABS).add(slabArcaneStone.get(), slabArcaneBrick.get(), slabAncient.get(), slabEldritch.get());
        tag(BlockTags.STAIRS).add(stairsArcane.get(), stairsArcaneBrick.get(), stairsAncient.get());
        tag(BlockTags.SAPLINGS).add(saplingGreatwood.get(), saplingSilverwood.get());
        tag(BlockTags.LEAVES).add(leafGreatwood.get(), leafSilverwood.get());

        // OreDictionary: oreAmber, oreCinnabar, oreQuartz, blockThaumium, blockVoid, blockBrass
        tag(TCTags.Blocks.ORES_AMBER).add(oreAmber.get());
        tag(TCTags.Blocks.ORES_CINNABAR).add(oreCinnabar.get());
        tag(TCTags.Blocks.ORES_QUARTZ).add(oreQuartz.get());
        tag(Tags.Blocks.ORES).addTags(TCTags.Blocks.ORES_AMBER, TCTags.Blocks.ORES_CINNABAR, TCTags.Blocks.ORES_QUARTZ);
        tag(TCTags.Blocks.STORAGE_BLOCKS_THAUMIUM).add(metalBlockThaumium.get());
        tag(TCTags.Blocks.STORAGE_BLOCKS_VOID).add(metalBlockVoid.get());
        tag(TCTags.Blocks.STORAGE_BLOCKS_BRASS).add(metalBlockBrass.get());
        tag(Tags.Blocks.STORAGE_BLOCKS).addTags(TCTags.Blocks.STORAGE_BLOCKS_THAUMIUM, TCTags.Blocks.STORAGE_BLOCKS_VOID, TCTags.Blocks.STORAGE_BLOCKS_BRASS);
    }
}
