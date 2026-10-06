package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Основа BlockEntity Thaumcraft (порт TileThaumcraft): readSyncNBT/writeSyncNBT — данные, которые и сохраняются, и уходят
 * клиенту (пакет обновления блока); syncTile — отправить их клиентам, следящим за чанком.
 */
public class TileThaumcraft extends BlockEntity {
    public TileThaumcraft(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
    }

    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        return nbt;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.readSyncNBT(nbt, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        this.writeSyncNBT(nbt, registries);
    }

    /** Отправить данные синхронизации клиентам (rerender — также перерисовать блок). */
    public void syncTile(boolean rerender) {
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, 2 + (rerender ? 4 : 0));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.writeSyncNBT(new CompoundTag(), registries);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.readSyncNBT(tag, registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        this.readSyncNBT(pkt.getTag(), registries);
    }
}
