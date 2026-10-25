package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.essentia.BlockTube;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.SoundsTC;

/** Клапан: перекрывает всасывание (ПКМ или сигнал красного камня); сторона facing — маховик, к ней ничего не подключается. */
public class TileTubeValve extends TileTube {
    public boolean allowFlow = true;
    boolean wasPoweredLastTick = false;
    public float rotation = 0.0F;

    public TileTubeValve(BlockPos pos, BlockState state) {
        super(ConfigBlocks.TUBE_VALVE_TILE.get(), pos, state);
    }

    @Override
    public void update() {
        if (!this.level.isClientSide && this.count % 5 == 0) {
            boolean gettingPower = this.gettingPower();
            if (this.wasPoweredLastTick && !gettingPower && !this.allowFlow) {
                this.allowFlow = true;
                this.level.playSound(null, this.worldPosition, SoundsTC.squeek.get(), SoundSource.BLOCKS, 0.7F, 0.9F + this.level.random.nextFloat() * 0.2F);
                this.syncTile(true);
                this.setChanged();
            }
            if (!this.wasPoweredLastTick && gettingPower && this.allowFlow) {
                this.allowFlow = false;
                this.level.playSound(null, this.worldPosition, SoundsTC.squeek.get(), SoundSource.BLOCKS, 0.7F, 0.9F + this.level.random.nextFloat() * 0.2F);
                this.syncTile(true);
                this.setChanged();
            }
            this.wasPoweredLastTick = gettingPower;
        }
        if (this.level.isClientSide) {
            if (!this.allowFlow && this.rotation < 360.0F) {
                this.rotation += 20.0F;
            } else if (this.allowFlow && this.rotation > 0.0F) {
                this.rotation -= 20.0F;
            }
        }
        super.update();
    }

    @Override
    public boolean onCasterRightClick(Level world, ItemStack wandstack, Player player, BlockPos bp, Direction side, InteractionHand hand) {
        int hit = this.getSubHit(player);
        if (hit < 0) {
            return false;
        }
        if (hit < 6) {
            world.playLocalSound(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), SoundsTC.tool.get(), SoundSource.BLOCKS, 0.5F,
                    0.9F + world.random.nextFloat() * 0.2F, false);
            player.swing(hand);
            this.setChanged();
            this.openSides[hit] = !this.openSides[hit];
            this.syncTile(true);
            Direction dir = Direction.values()[hit];
            if (world.getBlockEntity(this.worldPosition.relative(dir)) instanceof TileTube tile) {
                tile.openSides[dir.getOpposite().ordinal()] = this.openSides[hit];
                tile.syncTile(true);
                tile.setChanged();
                BlockTube.refreshConnections(world, tile.getBlockPos());
            }
            BlockTube.refreshConnections(world, this.worldPosition);
            return true;
        }
        world.playLocalSound(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), SoundsTC.tool.get(), SoundSource.BLOCKS, 0.5F,
                0.9F + world.random.nextFloat() * 0.2F, false);
        player.swing(hand);
        int a = this.facing.ordinal();
        this.setChanged();
        while (++a < 20) {
            if (!this.canConnectSide(Direction.values()[a % 6])) {
                a %= 6;
                this.facing = Direction.values()[a];
                this.syncTile(true);
                this.setChanged();
                BlockTube.refreshConnections(world, this.worldPosition);
                break;
            }
        }
        return true;
    }

    @Override
    public void readSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        super.readSyncNBT(nbttagcompound, registries);
        this.allowFlow = nbttagcompound.getBoolean("flow");
        this.wasPoweredLastTick = nbttagcompound.getBoolean("hadpower");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        nbttagcompound = super.writeSyncNBT(nbttagcompound, registries);
        nbttagcompound.putBoolean("flow", this.allowFlow);
        nbttagcompound.putBoolean("hadpower", this.wasPoweredLastTick);
        return nbttagcompound;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face != this.facing && super.isConnectable(face);
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        if (this.allowFlow) {
            super.setSuction(aspect, amount);
        }
    }

    /** getRedstonePowerFromNeighbors > 0. */
    @Override
    public boolean gettingPower() {
        return this.level.getBestNeighborSignal(this.worldPosition) > 0;
    }
}
