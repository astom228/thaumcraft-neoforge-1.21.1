package thaumcraft.api.blocks;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import thaumcraft.common.blocks.basic.BlockCandle;
import thaumcraft.common.blocks.basic.BlockTable;
import thaumcraft.common.blocks.crafting.BlockArcaneWorkbench;
import thaumcraft.common.blocks.devices.BlockWaterJug;
import thaumcraft.common.blocks.essentia.BlockJar;
import thaumcraft.common.blocks.essentia.BlockTube;
import thaumcraft.common.blocks.devices.BlockMirror;
import thaumcraft.common.blocks.devices.BlockPedestal;
import thaumcraft.common.blocks.basic.BlockPillar;
import thaumcraft.common.blocks.crafting.BlockInfusionMatrix;
import thaumcraft.common.blocks.devices.BlockBrainBox;
import thaumcraft.common.blocks.crafting.BlockThaumatorium;
import thaumcraft.common.blocks.devices.BlockMirrorItem;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorEssentia;
import thaumcraft.common.blocks.essentia.BlockEssentiaTransport;
import thaumcraft.common.tiles.essentia.TileEssentiaInput;
import thaumcraft.common.tiles.essentia.TileEssentiaOutput;
import thaumcraft.common.blocks.essentia.BlockAlembic;
import thaumcraft.common.blocks.essentia.BlockCentrifuge;
import thaumcraft.common.blocks.essentia.BlockSmelter;
import thaumcraft.common.blocks.essentia.BlockSmelterAux;
import thaumcraft.common.blocks.essentia.BlockSmelterVent;
import thaumcraft.common.tiles.essentia.TileTube;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;
import thaumcraft.common.tiles.essentia.TileTubeFilter;
import thaumcraft.common.tiles.essentia.TileTubeOneway;
import thaumcraft.common.tiles.essentia.TileTubeRestrict;
import thaumcraft.common.tiles.essentia.TileTubeValve;
import thaumcraft.common.blocks.essentia.BlockJarBrainItem;
import thaumcraft.common.blocks.essentia.BlockJarItem;
import thaumcraft.common.tiles.essentia.TileJarBrain;
import thaumcraft.common.tiles.essentia.TileJarFillable;
import thaumcraft.common.tiles.essentia.TileJarFillableVoid;
import thaumcraft.common.blocks.crafting.BlockArcaneWorkbenchCharger;
import thaumcraft.common.blocks.crafting.BlockCrucible;
import thaumcraft.common.blocks.crafting.BlockResearchTable;
import thaumcraft.common.blocks.basic.BlockMetalTC;
import thaumcraft.common.blocks.basic.BlockPlanksTC;
import thaumcraft.common.blocks.basic.BlockSlabTC;
import thaumcraft.common.blocks.basic.BlockStairsTC;
import thaumcraft.common.blocks.basic.BlockStonePorous;
import thaumcraft.common.blocks.basic.BlockStoneTC;
import thaumcraft.common.blocks.basic.BlockTranslucent;
import thaumcraft.common.blocks.misc.BlockFlesh;
import thaumcraft.common.blocks.misc.BlockNitor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.world.ore.BlockCrystal;
import thaumcraft.common.blocks.world.ore.BlockOreTC;
import thaumcraft.common.blocks.world.plants.BlockLeavesTC;
import thaumcraft.common.blocks.world.plants.BlockLogsTC;
import thaumcraft.common.blocks.world.plants.BlockPlantCinderpearl;
import thaumcraft.common.blocks.world.plants.BlockPlantShimmerleaf;
import thaumcraft.common.blocks.world.plants.BlockPlantVishroom;
import thaumcraft.common.blocks.world.plants.BlockSaplingTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.SoundsTC;

/**
 * Блоки Thaumcraft (в оригинале — статические поля, заполняемые ConfigBlocks.initBlocks). Имена полей и порядок — как в оригинале.
 * Сопротивление взрыву пересчитано: в 1.12 эффективное значение = (поле blockResistance)/5, где setResistance(r) пишет r*3,
 * а setHardness(h) поднимает поле до max(поле, h*5); в 1.21 значение задаётся напрямую.
 */
public class BlocksTC {
    // --- руды (BlockTC: setResistance(2), setHardness(1.5); BlockOreTC: setResistance(5); затем setHardness → 3.0) ---
    public static final DeferredBlock<Block> oreAmber = register("amber_ore",
            () -> new BlockOreTC(UniformInt.of(1, 4), stone(1.5F, 3.0F)));
    public static final DeferredBlock<Block> oreCinnabar = register("cinnabar_ore",
            () -> new BlockOreTC(ConstantInt.of(0), stone(2.0F, 3.0F)));
    public static final DeferredBlock<Block> oreQuartz = register("quartz_ore",
            () -> new BlockOreTC(UniformInt.of(1, 4), stone(3.0F, 3.0F)));

    // --- вис-кристаллы (BlockCrystal): Material.GLASS, setHardness(0.25) → сопротивление 0.25, свет 1 ---
    public static final DeferredBlock<Block> crystalAir = register("aer_crystal", () -> new BlockCrystal(Aspect.AIR, crystal()));
    public static final DeferredBlock<Block> crystalFire = register("ignis_crystal", () -> new BlockCrystal(Aspect.FIRE, crystal()));
    public static final DeferredBlock<Block> crystalWater = register("aqua_crystal", () -> new BlockCrystal(Aspect.WATER, crystal()));
    public static final DeferredBlock<Block> crystalEarth = register("terra_crystal", () -> new BlockCrystal(Aspect.EARTH, crystal()));
    public static final DeferredBlock<Block> crystalOrder = register("ordo_crystal", () -> new BlockCrystal(Aspect.ORDER, crystal()));
    public static final DeferredBlock<Block> crystalEntropy = register("perditio_crystal", () -> new BlockCrystal(Aspect.ENTROPY, crystal()));
    public static final DeferredBlock<Block> crystalTaint = register("vitium_crystal", () -> new BlockCrystal(Aspect.FLUX, crystal()));

    // --- камни (BlockStoneTC: прочность 2.0, сопротивление → 6.0) ---
    public static final DeferredBlock<Block> stoneArcane = register("arcane_stone", () -> new BlockStoneTC(stone(2.0F, 6.0F)));
    public static final DeferredBlock<Block> stoneArcaneBrick = register("arcane_stone_bricks", () -> new BlockStoneTC(stone(2.0F, 6.0F)));
    public static final DeferredBlock<Block> stoneAncient = register("ancient_stone", () -> new BlockStoneTC(stone(2.0F, 6.0F)));
    public static final DeferredBlock<Block> stoneAncientTile = register("ancient_stone_tile", () -> new BlockStoneTC(stone(2.0F, 6.0F)));
    public static final DeferredBlock<Block> stoneAncientRock = register("ancient_rock", () -> new BlockStoneTC(stone(-1.0F, 6.0F)));
    public static final DeferredBlock<Block> stoneAncientGlyphed = register("glyphed_stone", () -> new BlockStoneTC(stone(2.0F, 6.0F)));
    public static final DeferredBlock<Block> stoneAncientDoorway = register("ancient_barrier", () -> new BlockStoneTC(stone(-1.0F, 6.0F)));
    public static final DeferredBlock<Block> stoneEldritchTile = register("eldritch_stone", () -> new BlockStoneTC(stone(15.0F, 600.0F)));
    // BlockStonePorous: setHardness(1.0), setResistance(5) → 1.0 / 3.0
    public static final DeferredBlock<Block> stonePorous = register("porous_stone", () -> new BlockStonePorous(stone(1.0F, 3.0F)));

    public static final DeferredBlock<Block> stairsArcane = register("arcane_stone_stairs",
            () -> new BlockStairsTC(stoneArcane.get().defaultBlockState(), copy(stoneArcane), false));
    public static final DeferredBlock<Block> stairsArcaneBrick = register("arcane_brick_stairs",
            () -> new BlockStairsTC(stoneArcaneBrick.get().defaultBlockState(), copy(stoneArcaneBrick), false));
    public static final DeferredBlock<Block> stairsAncient = register("ancient_stone_stairs",
            () -> new BlockStairsTC(stoneAncient.get().defaultBlockState(), copy(stoneAncient), false));

    // Плиты: setHardness(2.0).setResistance(10) → 2.0 / 6.0
    public static final DeferredBlock<Block> slabArcaneStone = register("arcane_stone_slab", () -> new BlockSlabTC(stone(2.0F, 6.0F), false));
    public static final DeferredBlock<Block> slabArcaneBrick = register("arcane_brick_slab", () -> new BlockSlabTC(stone(2.0F, 6.0F), false));
    public static final DeferredBlock<Block> slabAncient = register("ancient_stone_slab", () -> new BlockSlabTC(stone(2.0F, 6.0F), false));
    public static final DeferredBlock<Block> slabEldritch = register("eldritch_stone_slab", () -> new BlockSlabTC(stone(2.0F, 6.0F), false));

    // --- дерево ---
    public static final DeferredBlock<Block> saplingGreatwood = register("greatwood_sapling",
            () -> new BlockSaplingTC(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<Block> saplingSilverwood = register("silverwood_sapling",
            () -> new BlockSaplingTC(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<Block> logGreatwood = register("greatwood_log", () -> new BlockLogsTC(log(0)));
    public static final DeferredBlock<Block> logSilverwood = register("silverwood_log", () -> new BlockLogsTC(log(5)));
    public static final DeferredBlock<Block> leafGreatwood = register("greatwood_leaves",
            () -> new BlockLeavesTC(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.PLANT)));
    public static final DeferredBlock<Block> leafSilverwood = register("silverwood_leaves",
            () -> new BlockLeavesTC(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.COLOR_LIGHT_BLUE)));
    // Растения (BlockBush): прочность 0; свет setLightLevel(0.4/0.5/0.4) → 6/7/6
    public static final DeferredBlock<Block> shimmerleaf = register("shimmerleaf", () -> new BlockPlantShimmerleaf(plant(6)));
    public static final DeferredBlock<Block> cinderpearl = register("cinderpearl", () -> new BlockPlantCinderpearl(plant(7)));
    public static final DeferredBlock<Block> vishroom = register("vishroom", () -> new BlockPlantVishroom(plant(6)));
    public static final DeferredBlock<Block> plankGreatwood = register("greatwood_planks", () -> new BlockPlanksTC(wood(2.0F, 2.0F)));
    public static final DeferredBlock<Block> plankSilverwood = register("silverwood_planks", () -> new BlockPlanksTC(wood(2.0F, 2.0F)));
    public static final DeferredBlock<Block> stairsGreatwood = register("greatwood_stairs",
            () -> new BlockStairsTC(plankGreatwood.get().defaultBlockState(), copy(plankGreatwood), true));
    public static final DeferredBlock<Block> stairsSilverwood = register("silverwood_stairs",
            () -> new BlockStairsTC(plankSilverwood.get().defaultBlockState(), copy(plankSilverwood), true));
    // setHardness(1.2 / 1.0).setResistance(2) → сопротивление 1.2
    public static final DeferredBlock<Block> slabGreatwood = register("greatwood_slab", () -> new BlockSlabTC(wood(1.2F, 1.2F), true));
    public static final DeferredBlock<Block> slabSilverwood = register("silverwood_slab", () -> new BlockSlabTC(wood(1.0F, 1.2F), true));

    // --- янтарь, плоть ---
    // BlockTranslucent: setHardness(0.5) → сопротивление 1.5; материал стекла, инструмент не нужен
    public static final DeferredBlock<Block> amberBlock = register("amber_block", () -> new BlockTranslucent(amber()));
    public static final DeferredBlock<Block> amberBrick = register("amber_bricks", () -> new BlockTranslucent(amber()));
    public static final DeferredBlock<Block> fleshBlock = register("flesh_block", () -> new BlockFlesh(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.25F, 1.2F).sound(SoundsTC.GORE)));

    // --- металлы (BlockMetalTC: прочность 4.0, сопротивление → 6.0) ---
    public static final DeferredBlock<Block> metalBlockBrass = register("brass_block", () -> new BlockMetalTC(metal()));
    public static final DeferredBlock<Block> metalBlockThaumium = register("thaumium_block", () -> new BlockMetalTC(metal()));
    public static final DeferredBlock<Block> metalBlockVoid = register("void_metal_block", () -> new BlockMetalTC(metal()));
    public static final DeferredBlock<Block> metalAlchemical = register("alchemical_construct", () -> new BlockMetalTC(metal()));
    public static final DeferredBlock<Block> metalAlchemicalAdvanced = register("advanced_alchemical_construct", () -> new BlockMetalTC(metal()));

    public static final DeferredBlock<Block> matrixSpeed = register("infusion_speed_stone", () -> new BlockStoneTC(stone(2.0F, 6.0F)));
    public static final DeferredBlock<Block> matrixCost = register("infusion_cost_stone", () -> new BlockStoneTC(stone(2.0F, 6.0F)));

    // Свечи (BlockCandle): setHardness(0.1) → сопротивление 1.5, звук ткани, свет 0.9375 → 14
    public static final Map<DyeColor, DeferredBlock<Block>> candles = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dye : DyeColor.values()) {
            candles.put(dye, register(dye.getSerializedName() + "_candle", () -> new BlockCandle(dye,
                    BlockBehaviour.Properties.of().mapColor(dye.getMapColor()).strength(0.1F, 1.5F).sound(SoundType.WOOL)
                            .lightLevel(state -> 14).noOcclusion().pushReaction(PushReaction.DESTROY))));
        }
    }
    // TODO этап 14: баннеры (BlockBannerTC) — в оригинале регистрируются между свечами и ниторами.

    // Ниторы (BlockNitor): setHardness(0.1) → сопротивление 1.5, звук ткани, свет 15, без коллизии
    public static final Map<DyeColor, DeferredBlock<Block>> nitor = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dye : DyeColor.values()) {
            nitor.put(dye, register(dye.getSerializedName() + "_nitor", () -> new BlockNitor(dye,
                    BlockBehaviour.Properties.of().mapColor(dye.getMapColor()).strength(0.1F, 1.5F).sound(SoundType.WOOL)
                            .lightLevel(state -> 15).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY))));
        }
    }

    private static BlockBehaviour.Properties crystal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.25F, 0.25F).sound(SoundsTC.CRYSTAL).randomTicks()
                .noCollission().noOcclusion().lightLevel(state -> 1);
    }

    private static BlockBehaviour.Properties plant(int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS)
                .lightLevel(state -> light).pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties stone(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0F, 6.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties wood(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                .strength(hardness, resistance).sound(SoundType.WOOD).ignitedByLava();
    }

    private static BlockBehaviour.Properties log(int light) {
        return wood(2.0F, 3.0F).lightLevel(state -> light);
    }

    private static BlockBehaviour.Properties amber() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.5F, 1.5F).sound(SoundType.STONE).noOcclusion()
                .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
    }

    private static BlockBehaviour.Properties copy(DeferredBlock<Block> block) {
        return BlockBehaviour.Properties.ofFullCopy(block.get());
    }

    // Столы (BlockTable): дерево — прочность 2.0, сопротивление → 2.0; камень — 2.5 / 2.5; выпадают без инструмента
    public static final DeferredBlock<Block> tableWood = register("wooden_table", () -> new BlockTable(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<Block> tableStone = register("stone_table", () -> new BlockTable(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 2.5F).sound(SoundType.STONE).noOcclusion()));
    // Стол исследований (BlockTCTile: прочность 2.0, setResistance(20) → 12)
    public static final DeferredBlock<Block> researchTable = register("research_table", () -> new BlockResearchTable(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 12.0F).sound(SoundType.WOOD).noOcclusion()));
    // Магический верстак (BlockTCDevice → BlockTCTile: прочность 2.0, сопротивление 20 → 12; Material.WOOD)
    public static final DeferredBlock<Block> arcaneWorkbench = register("arcane_workbench", () -> new BlockArcaneWorkbench(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 12.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    // Зарядник верстака (BlockTC: прочность 1.25, setResistance(10) → 6; Material.WOOD)
    public static final DeferredBlock<Block> arcaneWorkbenchCharger = register("arcane_workbench_charger", () -> new BlockArcaneWorkbenchCharger(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.25F, 6.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));

    // Тигель (BlockTCTile: прочность 2.0, сопротивление 20 → 12; Material.IRON — нужна кирка)
    public static final DeferredBlock<Block> crucible = register("crucible", () -> new BlockCrucible(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // Банки (BlockJar: Material.GLASS, прочность 0.3, сопротивление BlockTCTile 20 → 12, звук банки)
    public static final DeferredBlock<Block> jarNormal = registerWithItem("warded_jar", () -> new BlockJar(TileJarFillable::new,
            ConfigBlocks.WARDED_JAR_TILE, jar()), BlockJarItem::new);
    public static final DeferredBlock<Block> jarVoid = registerWithItem("void_jar", () -> new BlockJar(TileJarFillableVoid::new,
            ConfigBlocks.VOID_JAR_TILE, jar()), BlockJarItem::new);
    public static final DeferredBlock<Block> jarBrain = registerWithItem("brain_jar", () -> new BlockJar(TileJarBrain::new,
            ConfigBlocks.BRAIN_JAR_TILE, jar()), BlockJarBrainItem::new);

    // Неиссякаемая урна (BlockTCDevice: прочность 2.0, сопротивление 20 → 12, Material.ROCK; добывается без инструмента)
    public static final DeferredBlock<Block> everfullUrn = register("everfull_urn", () -> new BlockWaterJug(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 12.0F).sound(SoundType.STONE).noOcclusion()));

    // Трубки (BlockTube: Material.IRON, прочность 0.5, setResistance(5) → 3, звук металла; добываются без инструмента)
    public static final DeferredBlock<Block> tube = register("essentia_tube", () -> new BlockTube(TileTube::new, ConfigBlocks.TUBE_TILE, tube()));
    public static final DeferredBlock<Block> tubeValve = register("essentia_valve", () -> new BlockTube(TileTubeValve::new, ConfigBlocks.TUBE_VALVE_TILE, tube()));
    public static final DeferredBlock<Block> tubeRestrict = register("restricted_essentia_tube",
            () -> new BlockTube(TileTubeRestrict::new, ConfigBlocks.TUBE_RESTRICT_TILE, tube()));
    public static final DeferredBlock<Block> tubeOneway = register("directional_essentia_tube",
            () -> new BlockTube(TileTubeOneway::new, ConfigBlocks.TUBE_ONEWAY_TILE, tube()));
    public static final DeferredBlock<Block> tubeFilter = register("filtered_essentia_tube",
            () -> new BlockTube(TileTubeFilter::new, ConfigBlocks.TUBE_FILTER_TILE, tube()));
    public static final DeferredBlock<Block> tubeBuffer = register("essentia_buffer", () -> new BlockTube(TileTubeBuffer::new, ConfigBlocks.TUBE_BUFFER_TILE, tube()));

    // Центрифуга (BlockTCDevice: прочность 2.0, сопротивление 20 → 12, Material.WOOD; добывается без инструмента)
    public static final DeferredBlock<Block> centrifuge = register("centrifuge", () -> new BlockCentrifuge(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 12.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    // TODO этап 14: bellows (мехи)
    // Плавильни (BlockTCDevice: прочность 2.0, сопротивление 12, Material.IRON; добываются без инструмента)
    public static final DeferredBlock<Block> smelterBasic = register("essentia_smeltery", () -> new BlockSmelter(smelter()));
    public static final DeferredBlock<Block> smelterThaumium = register("thaumium_essentia_smeltery", () -> new BlockSmelter(smelter()));
    public static final DeferredBlock<Block> smelterVoid = register("void_metal_essentia_smeltery", () -> new BlockSmelter(smelter()));
    // Насос и клапан (BlockTC: прочность 1.0, setResistance(10) → 6, Material.IRON)
    public static final DeferredBlock<Block> smelterAux = register("auxiliary_slurry_pump", () -> new BlockSmelterAux(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Block> smelterVent = register("auxiliary_venting_port", () -> new BlockSmelterVent(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    // Алембик (BlockTCTile: прочность 2.0, сопротивление 12, Material.WOOD)
    public static final DeferredBlock<Block> alembic = register("alembic", () -> new BlockAlembic(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 12.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));

    // Переливатели эссенции (BlockEssentiaTransport: прочность 1.0, setResistance(10) → 6, Material.IRON)
    public static final DeferredBlock<Block> essentiaTransportInput = register("essentia_input",
            () -> new BlockEssentiaTransport(TileEssentiaInput::new, ConfigBlocks.ESSENTIA_INPUT_TILE, transport()));
    public static final DeferredBlock<Block> essentiaTransportOutput = register("essentia_output",
            () -> new BlockEssentiaTransport(TileEssentiaOutput::new, ConfigBlocks.ESSENTIA_OUTPUT_TILE, transport()));

    // Зеркала (BlockMirror: Material.IRON, прочность 0.1, звук банки; добываются без инструмента)
    public static final DeferredBlock<Block> mirror = registerWithItem("magic_mirror",
            () -> new BlockMirror(TileMirror::new, ConfigBlocks.MIRROR_TILE, mirror()), BlockMirrorItem::new);
    public static final DeferredBlock<Block> mirrorEssentia = registerWithItem("essentia_mirror",
            () -> new BlockMirror(TileMirrorEssentia::new, ConfigBlocks.MIRROR_ESSENTIA_TILE, mirror()), BlockMirrorItem::new);

    // Тауматориум: низ и верх (BlockTCDevice: прочность 2.0, сопротивление 12, Material.IRON) — без предмета, не во вкладке
    public static final DeferredBlock<Block> thaumatorium = ConfigBlocks.BLOCKS.register("thaumatorium", () -> new BlockThaumatorium(false, thaumatoriumProps()));
    public static final DeferredBlock<Block> thaumatoriumTop = ConfigBlocks.BLOCKS.register("thaumatorium_top",
            () -> new BlockThaumatorium(true, thaumatoriumProps()));
    // Мнемоническая матрица (BlockBrainBox: прочность 1.0, setResistance(10) → 6, Material.IRON)
    public static final DeferredBlock<Block> brainBox = register("mnemonic_matrix", () -> new BlockBrainBox(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));

    // Руническая матрица (BlockTCDevice: прочность 2.0, сопротивление 12, Material.ROCK, добывается без инструмента)
    public static final DeferredBlock<Block> infusionMatrix = register("infusion_matrix", () -> new BlockInfusionMatrix(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 12.0F).sound(SoundType.STONE).noOcclusion()));
    // Пьедесталы (BlockTCTile: прочность 2.0, сопротивление 12, Material.ROCK, добываются без инструмента)
    public static final DeferredBlock<Block> pedestalArcane = register("arcane_pedestal", () -> new BlockPedestal(pedestal()));
    public static final DeferredBlock<Block> pedestalAncient = register("ancient_pedestal", () -> new BlockPedestal(pedestal()));
    public static final DeferredBlock<Block> pedestalEldritch = register("eldritch_pedestal", () -> new BlockPedestal(pedestal()));
    // Колонны наполнения (BlockPillar: прочность 2.5, setResistance BlockTC 2 → по прочности 2.5, Material.ROCK — нужна кирка)
    public static final DeferredBlock<Block> pillarArcane = register("infusion_pillar", () -> new BlockPillar(pillar()));
    public static final DeferredBlock<Block> pillarAncient = register("ancient_infusion_pillar", () -> new BlockPillar(pillar()));
    public static final DeferredBlock<Block> pillarEldritch = register("eldritch_infusion_pillar", () -> new BlockPillar(pillar()));

    private static BlockBehaviour.Properties pedestal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 12.0F).sound(SoundType.STONE).noOcclusion();
    }

    private static BlockBehaviour.Properties pillar() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 2.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                .noOcclusion().pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties thaumatoriumProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 12.0F).sound(SoundType.METAL).noOcclusion();
    }

    private static BlockBehaviour.Properties mirror() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.1F, 6.0F).sound(SoundsTC.JAR).noOcclusion()
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
    }

    private static BlockBehaviour.Properties transport() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion();
    }

    private static BlockBehaviour.Properties smelter() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 12.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties tube() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F, 3.0F).sound(SoundType.METAL).noOcclusion()
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
    }

    private static BlockBehaviour.Properties jar() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F, 12.0F).sound(SoundsTC.JAR).noOcclusion()
                .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false);
    }

    /** Блок с собственным классом предмета (в оригинале — registerBlock(block, ItemBlockClass)). */
    private static DeferredBlock<Block> registerWithItem(String name, Supplier<? extends Block> block,
                                                         java.util.function.BiFunction<Block, net.minecraft.world.item.Item.Properties, ? extends net.minecraft.world.item.Item> item) {
        DeferredBlock<Block> holder = ConfigBlocks.BLOCKS.register(name, block);
        ConfigItems.ITEMS.register(name, () -> item.apply(holder.get(), new net.minecraft.world.item.Item.Properties()));
        return holder;
    }

    /** Регистрирует блок и его предмет (в оригинале — ConfigBlocks.registerBlock). */
    private static DeferredBlock<Block> register(String name, Supplier<? extends Block> block) {
        DeferredBlock<Block> holder = ConfigBlocks.BLOCKS.register(name, block);
        ConfigItems.ITEMS.registerSimpleBlockItem(holder);
        return holder;
    }

    /** Загружает класс, чтобы все блоки попали в регистр до события регистрации. */
    public static void init() {
    }
}
