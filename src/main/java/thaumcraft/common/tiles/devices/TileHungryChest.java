package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ConfigBlocks;

/** Голодный сундук (порт TileHungryChest): обычный одиночный сундук на 27 слотов (без пар), своё название. */
public class TileHungryChest extends ChestBlockEntity {
    public TileHungryChest(BlockPos pos, BlockState state) {
        super(ConfigBlocks.HUNGRY_CHEST_TILE.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.thaumcraft.hungry_chest");
    }
}
