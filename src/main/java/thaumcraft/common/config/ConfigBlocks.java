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
import thaumcraft.common.tiles.essentia.TileJarFillable;
import thaumcraft.common.tiles.essentia.TileJarFillableVoid;
import thaumcraft.common.tiles.crafting.TileCrucible;
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
    public static final Supplier<BlockEntityType<TileArcaneWorkbench>> ARCANE_WORKBENCH_TILE = TILES.register("arcane_workbench",
            () -> BlockEntityType.Builder.of(TileArcaneWorkbench::new, BlocksTC.arcaneWorkbench.get()).build(null));

    /** Раздел 7: инвентари блоков — возможность IItemHandler. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RESEARCH_TABLE_TILE.get(), (tile, side) -> tile.getItemHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CRUCIBLE_TILE.get(), (tile, side) -> tile.tank);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, EVERFULL_URN_TILE.get(), TileWaterJug::capability);
    }
}
