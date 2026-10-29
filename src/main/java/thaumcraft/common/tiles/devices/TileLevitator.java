package thaumcraft.common.tiles.devices;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Левитатор (порт TileLevitator): толкает предметы и сущности в сторону FACING на 4/8/16/32 блока (до первого непрозрачного
 * блока), тратя Вис: 1 Вис ауры = 1200 единиц, по 2·дальность за сущность за тик. Вверх присевших не поднимает (только тормозит
 * падение), сбоку — поддерживает в воздухе. Работает на обеих сторонах (движение игрока задаёт клиент), Вис тратит сервер.
 */
public class TileLevitator extends TileThaumcraft {
    private static final int[] RANGES = {4, 8, 16, 32};
    private int range = 1;
    private int rangeActual = 0;
    private int counter = 0;
    private int vis = 0;

    public TileLevitator(BlockPos pos, BlockState state) {
        super(ConfigBlocks.LEVITATOR_TILE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileLevitator tile) {
        Direction facing = state.getValue(BlockTCDeviceFacing.FACING);
        if (tile.rangeActual > RANGES[tile.range]) {
            tile.rangeActual = 0;
        }
        int p = tile.counter % RANGES[tile.range];
        BlockPos probe = pos.relative(facing, 1 + p);
        if (level.isLoaded(probe) && level.getBlockState(probe).isSolidRender(level, probe)) {
            if (1 + p < tile.rangeActual) {
                tile.rangeActual = 1 + p;
            }
            tile.counter = -1;
        } else if (1 + p > tile.rangeActual) {
            tile.rangeActual = 1 + p;
        }
        tile.counter++;
        if (!level.isClientSide && tile.vis < 10) {
            tile.vis = (int) (tile.vis + AuraHelper.drainVis(level, pos, 1.0F, false) * 1200.0F);
            tile.setChanged();
            tile.syncTile(false);
        }
        if (tile.rangeActual > 0 && tile.vis > 0 && state.getValue(BlockTCDeviceFacing.ENABLED)) {
            int r = tile.rangeActual;
            AABB box = new AABB(pos.getX() - (facing.getStepX() < 0 ? r : 0), pos.getY() - (facing.getStepY() < 0 ? r : 0),
                    pos.getZ() - (facing.getStepZ() < 0 ? r : 0), pos.getX() + 1 + (facing.getStepX() > 0 ? r : 0),
                    pos.getY() + 1 + (facing.getStepY() > 0 ? r : 0), pos.getZ() + 1 + (facing.getStepZ() > 0 ? r : 0));
            List<Entity> targets = level.getEntitiesOfClass(Entity.class, box);
            boolean lifted = false;
            for (Entity e : targets) {
                if (e instanceof ItemEntity || e.isPushable() || e instanceof AbstractHorse) {
                    lifted = true;
                    tile.drawFXAt(e);
                    tile.drawFX(facing, 0.6);
                    Vec3 m = e.getDeltaMovement();
                    double mx = m.x;
                    double my = m.y;
                    double mz = m.z;
                    if (!e.isShiftKeyDown() || facing != Direction.UP) {
                        mx += 0.1F * facing.getStepX();
                        my += 0.1F * facing.getStepY();
                        mz += 0.1F * facing.getStepZ();
                        if (facing.getAxis() != Direction.Axis.Y && !e.onGround()) {
                            if (my < 0.0) {
                                my *= 0.9F;
                            }
                            my += 0.08F;
                        }
                        mx = Math.max(-0.35F, Math.min(0.35F, mx));
                        my = Math.max(-0.35F, Math.min(0.35F, my));
                        mz = Math.max(-0.35F, Math.min(0.35F, mz));
                    } else if (my < 0.0) {
                        my *= 0.9F;
                    }
                    e.setDeltaMovement(mx, my, mz);
                    e.fallDistance = 0.0F;
                    tile.vis -= tile.getCost();
                    if (tile.vis <= 0) {
                        break;
                    }
                }
            }
            tile.drawFX(facing, 0.1);
            if (lifted && !level.isClientSide && tile.counter % 20 == 0) {
                tile.setChanged();
            }
        }
    }

    private void drawFX(Direction facing, double c) {
        if (this.level.isClientSide && this.level.random.nextFloat() < c) {
            float x = this.worldPosition.getX() + 0.25F + this.level.random.nextFloat() * 0.5F;
            float y = this.worldPosition.getY() + 0.25F + this.level.random.nextFloat() * 0.5F;
            float z = this.worldPosition.getZ() + 0.25F + this.level.random.nextFloat() * 0.5F;
            IFXDispatcher.get().drawLevitatorParticles(x, y, z, facing.getStepX() / 50.0, facing.getStepY() / 50.0, facing.getStepZ() / 50.0);
        }
    }

    private void drawFXAt(Entity e) {
        if (this.level.isClientSide && this.level.random.nextFloat() < 0.1F) {
            float x = (float) (e.getX() + (this.level.random.nextFloat() - this.level.random.nextFloat()) * e.getBbWidth());
            float y = (float) (e.getY() + this.level.random.nextFloat() * e.getBbHeight());
            float z = (float) (e.getZ() + (this.level.random.nextFloat() - this.level.random.nextFloat()) * e.getBbWidth());
            IFXDispatcher.get().drawLevitatorParticles(x, y, z, (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.01,
                    (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.01, (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.01);
        }
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.range = Math.max(0, Math.min(RANGES.length - 1, nbt.getByte("range")));
        this.vis = nbt.getInt("vis");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putByte("range", (byte) this.range);
        nbt.putInt("vis", this.vis);
        return nbt;
    }

    public int getCost() {
        return RANGES[this.range] * 2;
    }

    public void increaseRange(Player player) {
        this.rangeActual = 0;
        if (!this.level.isClientSide) {
            this.range = (this.range + 1) % RANGES.length;
            this.setChanged();
            this.syncTile(false);
            player.sendSystemMessage(Component.translatable("tc.levitator", RANGES[this.range], this.getCost()));
        }
    }
}
