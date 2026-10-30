package thaumcraft.common.entities.construct.golem.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;

/**
 * Поставленная печать (порт SealEntity): позиция и грань, тип, приоритет, цвет, замок, чувствительность к красному
 * камню, владелец, зона (по умолчанию 3×3 вдоль грани). Сигнал красного камня приостанавливает её задачи.
 */
public class SealEntity implements ISealEntity {
    SealPos sealPos;
    ISeal seal;
    byte priority = 0;
    byte color = 0;
    boolean locked = false;
    boolean redstone = false;
    String owner = "";
    boolean stopped = false;
    private BlockPos area = new BlockPos(1, 1, 1);

    public SealEntity() {
    }

    public SealEntity(SealPos sealPos, ISeal seal) {
        this.sealPos = sealPos;
        this.seal = seal;
        if (seal instanceof ISealConfigArea) {
            int x = sealPos.face.getStepX() == 0 ? 3 : 1;
            int y = sealPos.face.getStepY() == 0 ? 3 : 1;
            int z = sealPos.face.getStepZ() == 0 ? 3 : 1;
            this.area = new BlockPos(x, y, z);
        }
    }

    @Override
    public void tickSealEntity(Level world) {
        if (this.seal == null) {
            return;
        }
        if (this.isStoppedByRedstone(world)) {
            if (!this.stopped) {
                for (Task t : TaskHandler.getTasks(world.dimension()).values()) {
                    if (t.getSealPos() != null && t.getSealPos().equals(this.sealPos)) {
                        t.setSuspended(true);
                    }
                }
            }
            this.stopped = true;
            return;
        }
        this.stopped = false;
        this.seal.tickSeal(world, this);
    }

    @Override
    public boolean isStoppedByRedstone(Level world) {
        return this.isRedstoneSensitive() && (world.hasNeighborSignal(this.sealPos.pos) || world.hasNeighborSignal(this.sealPos.pos.relative(this.sealPos.face)));
    }

    @Override
    public ISeal getSeal() {
        return this.seal;
    }

    @Override
    public SealPos getSealPos() {
        return this.sealPos;
    }

    @Override
    public byte getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(byte priority) {
        this.priority = priority;
    }

    @Override
    public byte getColor() {
        return this.color;
    }

    @Override
    public void setColor(byte color) {
        this.color = color;
    }

    @Override
    public String getOwner() {
        return this.owner;
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean isRedstoneSensitive() {
        return this.redstone;
    }

    @Override
    public void setRedstoneSensitive(boolean redstone) {
        this.redstone = redstone;
    }

    @Override
    public void readNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        BlockPos p = BlockPos.of(nbt.getLong("pos"));
        Direction face = Direction.values()[Math.floorMod(nbt.getByte("face"), 6)];
        this.sealPos = new SealPos(p, face);
        this.setPriority(nbt.getByte("priority"));
        this.setColor(nbt.getByte("color"));
        this.setLocked(nbt.getBoolean("locked"));
        this.setRedstoneSensitive(nbt.getBoolean("redstone"));
        this.setOwner(nbt.getString("owner"));
        this.seal = SealHandler.newSeal(nbt.getString("type"));
        if (this.seal != null) {
            this.seal.readCustomNBT(nbt, registries);
            if (this.seal instanceof ISealConfigArea) {
                this.area = BlockPos.of(nbt.getLong("area"));
            }
            if (this.seal instanceof ISealConfigToggles t) {
                for (ISealConfigToggles.SealToggle prop : t.getToggles()) {
                    if (nbt.contains(prop.getKey())) {
                        prop.setValue(nbt.getBoolean(prop.getKey()));
                    }
                }
            }
        }
    }

    @Override
    public CompoundTag writeNBT(HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("pos", this.sealPos.pos.asLong());
        nbt.putByte("face", (byte) this.sealPos.face.ordinal());
        nbt.putString("type", this.seal == null ? "REMOVE" : this.seal.getKey());
        nbt.putByte("priority", this.getPriority());
        nbt.putByte("color", this.getColor());
        nbt.putBoolean("locked", this.isLocked());
        nbt.putBoolean("redstone", this.isRedstoneSensitive());
        nbt.putString("owner", this.getOwner());
        if (this.seal != null) {
            this.seal.writeCustomNBT(nbt, registries);
            if (this.seal instanceof ISealConfigArea) {
                nbt.putLong("area", this.area.asLong());
            }
            if (this.seal instanceof ISealConfigToggles t) {
                for (ISealConfigToggles.SealToggle prop : t.getToggles()) {
                    nbt.putBoolean(prop.getKey(), prop.getValue());
                }
            }
        }
        return nbt;
    }

    @Override
    public void syncToClient(Level world) {
        if (world instanceof ServerLevel sl) {
            SealHandler.sync(sl, this);
        }
    }

    @Override
    public BlockPos getArea() {
        return this.area;
    }

    @Override
    public void setArea(BlockPos v) {
        this.area = v;
    }
}
