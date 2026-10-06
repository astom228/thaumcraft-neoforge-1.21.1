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

    /** Раздел 7: инвентари блоков — возможность IItemHandler. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RESEARCH_TABLE_TILE.get(), (tile, side) -> tile.getItemHandler());
    }
}
