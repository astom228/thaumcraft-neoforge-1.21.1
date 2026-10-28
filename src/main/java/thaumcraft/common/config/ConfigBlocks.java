package thaumcraft.common.config;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.tiles.misc.TileNitor;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;
import thaumcraft.common.tiles.devices.TileWaterJug;
import thaumcraft.common.tiles.essentia.TileJarBrain;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorEssentia;
import thaumcraft.common.tiles.essentia.TileEssentiaInput;
import thaumcraft.common.tiles.essentia.TileEssentiaOutput;
import thaumcraft.common.tiles.essentia.TileAlembic;
import thaumcraft.common.tiles.essentia.TileCentrifuge;
import thaumcraft.common.tiles.essentia.TileSmelter;
import thaumcraft.common.tiles.essentia.TileTube;
import thaumcraft.common.tiles.essentia.TileTubeValve;
import thaumcraft.common.tiles.essentia.TileTubeRestrict;
import thaumcraft.common.tiles.essentia.TileTubeOneway;
import thaumcraft.common.tiles.essentia.TileTubeFilter;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;
import thaumcraft.common.tiles.essentia.TileJarFillable;
import thaumcraft.common.tiles.essentia.TileJarFillableVoid;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.common.tiles.crafting.TilePedestal;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.common.tiles.crafting.TileThaumatoriumTop;
import thaumcraft.common.tiles.crafting.TileResearchTable;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Регистр блоков и типов BlockEntity (в оригинале — ConfigBlocks.initBlocks/initTileEntities). Блоки объявлены в BlocksTC. */
public class ConfigBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Thaumcraft.MODID);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Thaumcraft.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<TileNitor>> NITOR_TILE = TILES.register("nitor",
            () -> BlockEntityType.Builder.of(TileNitor::new, BlocksTC.nitor.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<TileResearchTable>> RESEARCH_TABLE_TILE = TILES.register("research_table",
            () -> BlockEntityType.Builder.of(TileResearchTable::new, BlocksTC.researchTable.get()).build(null));
    public static final Supplier<BlockEntityType<TileCrucible>> CRUCIBLE_TILE = TILES.register("crucible",
            () -> BlockEntityType.Builder.of(TileCrucible::new, BlocksTC.crucible.get()).build(null));
    public static final Supplier<BlockEntityType<TileJarFillable>> WARDED_JAR_TILE = TILES.register("warded_jar",
            () -> BlockEntityType.Builder.of(TileJarFillable::new, BlocksTC.jarNormal.get()).build(null));
    public static final Supplier<BlockEntityType<TileJarFillableVoid>> VOID_JAR_TILE = TILES.register("void_jar",
            () -> BlockEntityType.Builder.of(TileJarFillableVoid::new, BlocksTC.jarVoid.get()).build(null));
    public static final Supplier<BlockEntityType<TileJarBrain>> BRAIN_JAR_TILE = TILES.register("brain_jar",
            () -> BlockEntityType.Builder.of(TileJarBrain::new, BlocksTC.jarBrain.get()).build(null));
    public static final Supplier<BlockEntityType<TileWaterJug>> EVERFULL_URN_TILE = TILES.register("everfull_urn",
            () -> BlockEntityType.Builder.of(TileWaterJug::new, BlocksTC.everfullUrn.get()).build(null));
    public static final Supplier<BlockEntityType<TileTube>> TUBE_TILE = TILES.register("essentia_tube",
            () -> BlockEntityType.Builder.of(TileTube::new, BlocksTC.tube.get()).build(null));
    public static final Supplier<BlockEntityType<TileTubeValve>> TUBE_VALVE_TILE = TILES.register("essentia_valve",
            () -> BlockEntityType.Builder.of(TileTubeValve::new, BlocksTC.tubeValve.get()).build(null));
    public static final Supplier<BlockEntityType<TileTubeRestrict>> TUBE_RESTRICT_TILE = TILES.register("restricted_essentia_tube",
            () -> BlockEntityType.Builder.of(TileTubeRestrict::new, BlocksTC.tubeRestrict.get()).build(null));
    public static final Supplier<BlockEntityType<TileTubeOneway>> TUBE_ONEWAY_TILE = TILES.register("directional_essentia_tube",
            () -> BlockEntityType.Builder.of(TileTubeOneway::new, BlocksTC.tubeOneway.get()).build(null));
    public static final Supplier<BlockEntityType<TileTubeFilter>> TUBE_FILTER_TILE = TILES.register("filtered_essentia_tube",
            () -> BlockEntityType.Builder.of(TileTubeFilter::new, BlocksTC.tubeFilter.get()).build(null));
    public static final Supplier<BlockEntityType<TileTubeBuffer>> TUBE_BUFFER_TILE = TILES.register("essentia_buffer",
            () -> BlockEntityType.Builder.of(TileTubeBuffer::new, BlocksTC.tubeBuffer.get()).build(null));
    public static final Supplier<BlockEntityType<TileCentrifuge>> CENTRIFUGE_TILE = TILES.register("centrifuge",
            () -> BlockEntityType.Builder.of(TileCentrifuge::new, BlocksTC.centrifuge.get()).build(null));
    public static final Supplier<BlockEntityType<TileSmelter>> SMELTER_TILE = TILES.register("essentia_smeltery",
            () -> BlockEntityType.Builder.of(TileSmelter::new, BlocksTC.smelterBasic.get(), BlocksTC.smelterThaumium.get(), BlocksTC.smelterVoid.get())
                    .build(null));
    public static final Supplier<BlockEntityType<TileAlembic>> ALEMBIC_TILE = TILES.register("alembic",
            () -> BlockEntityType.Builder.of(TileAlembic::new, BlocksTC.alembic.get()).build(null));
    public static final Supplier<BlockEntityType<TileEssentiaInput>> ESSENTIA_INPUT_TILE = TILES.register("essentia_input",
            () -> BlockEntityType.Builder.of(TileEssentiaInput::new, BlocksTC.essentiaTransportInput.get()).build(null));
    public static final Supplier<BlockEntityType<TileEssentiaOutput>> ESSENTIA_OUTPUT_TILE = TILES.register("essentia_output",
            () -> BlockEntityType.Builder.of(TileEssentiaOutput::new, BlocksTC.essentiaTransportOutput.get()).build(null));
    public static final Supplier<BlockEntityType<TileMirror>> MIRROR_TILE = TILES.register("magic_mirror",
            () -> BlockEntityType.Builder.of(TileMirror::new, BlocksTC.mirror.get()).build(null));
    public static final Supplier<BlockEntityType<TileMirrorEssentia>> MIRROR_ESSENTIA_TILE = TILES.register("essentia_mirror",
            () -> BlockEntityType.Builder.of(TileMirrorEssentia::new, BlocksTC.mirrorEssentia.get()).build(null));
    public static final Supplier<BlockEntityType<TileThaumatorium>> THAUMATORIUM_TILE = TILES.register("thaumatorium",
            () -> BlockEntityType.Builder.of(TileThaumatorium::new, BlocksTC.thaumatorium.get()).build(null));
    public static final Supplier<BlockEntityType<TileThaumatoriumTop>> THAUMATORIUM_TOP_TILE = TILES.register("thaumatorium_top",
            () -> BlockEntityType.Builder.of(TileThaumatoriumTop::new, BlocksTC.thaumatoriumTop.get()).build(null));
    public static final Supplier<BlockEntityType<TilePedestal>> PEDESTAL_TILE = TILES.register("pedestal",
            () -> BlockEntityType.Builder.of(TilePedestal::new, BlocksTC.pedestalArcane.get(), BlocksTC.pedestalAncient.get(), BlocksTC.pedestalEldritch.get())
                    .build(null));
    public static final Supplier<BlockEntityType<TileInfusionMatrix>> INFUSION_MATRIX_TILE = TILES.register("infusion_matrix",
            () -> BlockEntityType.Builder.of(TileInfusionMatrix::new, BlocksTC.infusionMatrix.get()).build(null));
    public static final Supplier<BlockEntityType<thaumcraft.common.tiles.misc.TileHole>> HOLE_TILE = TILES.register("hole",
            () -> BlockEntityType.Builder.of(thaumcraft.common.tiles.misc.TileHole::new, BlocksTC.hole.get()).build(null));
    public static final Supplier<BlockEntityType<TileArcaneWorkbench>> ARCANE_WORKBENCH_TILE = TILES.register("arcane_workbench",
            () -> BlockEntityType.Builder.of(TileArcaneWorkbench::new, BlocksTC.arcaneWorkbench.get()).build(null));

    /** Раздел 7: инвентари блоков — возможность IItemHandler. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RESEARCH_TABLE_TILE.get(), (tile, side) -> tile.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMELTER_TILE.get(), TileSmelter::getItemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MIRROR_TILE.get(), (tile, side) -> tile.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, THAUMATORIUM_TILE.get(), (tile, side) -> tile.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PEDESTAL_TILE.get(), (tile, side) -> tile.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, THAUMATORIUM_TOP_TILE.get(), (tile, side) -> tile.getItemHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CRUCIBLE_TILE.get(), (tile, side) -> tile.tank);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, EVERFULL_URN_TILE.get(), TileWaterJug::capability);
    }
}
