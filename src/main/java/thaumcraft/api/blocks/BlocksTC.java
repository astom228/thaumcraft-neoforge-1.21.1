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
import thaumcraft.common.blocks.basic.BlockMetalTC;
import thaumcraft.common.blocks.basic.BlockPlanksTC;
import thaumcraft.common.blocks.basic.BlockSlabTC;
import thaumcraft.common.blocks.basic.BlockStairsTC;
import thaumcraft.common.blocks.basic.BlockStonePorous;
import thaumcraft.common.blocks.basic.BlockStoneTC;
import thaumcraft.common.blocks.basic.BlockTranslucent;
import thaumcraft.common.blocks.misc.BlockFlesh;
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
    // TODO этап 6: nitor (16 цветов) — невидимый блок, вся графика — частицы TileNitor.

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
