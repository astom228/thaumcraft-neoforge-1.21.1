package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Блок, помнящий прежнее состояние на своём месте (порт TileMemory; в 1.12 — id блока и metadata, здесь — состояние). */
public class TileMemory extends BlockEntity {
    public BlockState oldblock = Blocks.AIR.defaultBlockState();

    public TileMemory(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.oldblock = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), nbt.getCompound("oldblock"));
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.put("oldblock", NbtUtils.writeBlockState(this.oldblock));
    }
}
