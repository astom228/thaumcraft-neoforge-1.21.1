package thaumcraft.api.golems.tasks;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.ProvisionRequest;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;

/**
 * Задача голема (порт Task): к блоку (тип 0) или существу (тип 1), от печати или без неё; бронь, приостановка,
 * завершение, срок жизни (300 тиков, бронь +120).
 */
public class Task {
    private UUID golemUUID;
    private int id;
    private byte type;
    private SealPos sealPos;
    private BlockPos pos;
    private Entity entity;
    private boolean reserved;
    private boolean suspended;
    private boolean completed;
    private int data;
    private ProvisionRequest linkedProvision;
    private short lifespan;
    private byte priority = 0;

    public Task(SealPos sealPos, BlockPos pos) {
        this.sealPos = sealPos;
        this.pos = pos;
        if (sealPos == null) {
            this.id = (System.currentTimeMillis() + "/BNPOS/" + pos).hashCode();
        } else {
            this.id = (System.currentTimeMillis() + "/B/" + sealPos.face + "/" + sealPos.pos + "/" + pos).hashCode();
        }
        this.type = 0;
        this.lifespan = 300;
    }

    public Task(SealPos sealPos, Entity entity) {
        this.sealPos = sealPos;
        this.entity = entity;
        if (sealPos == null) {
            this.id = (System.currentTimeMillis() + "/ENPOS/" + entity.getId()).hashCode();
        } else {
            this.id = (System.currentTimeMillis() + "/E/" + sealPos.face + "/" + sealPos.pos + "/" + entity.getId()).hashCode();
        }
        this.type = 1;
        this.lifespan = 300;
    }

    public byte getPriority() {
        return this.priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public void setCompletion(boolean fulfilled) {
        this.completed = fulfilled;
        this.lifespan++;
    }

    public UUID getGolemUUID() {
        return this.golemUUID;
    }

    public void setGolemUUID(UUID golemUUID) {
        this.golemUUID = golemUUID;
    }

    public BlockPos getPos() {
        return this.type == 1 ? this.entity.blockPosition() : this.pos;
    }

    public byte getType() {
        return this.type;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public int getId() {
        return this.id;
    }

    public boolean isReserved() {
        return this.reserved;
    }

    public void setReserved(boolean res) {
        this.reserved = res;
        this.lifespan = (short) (this.lifespan + 120);
    }

    public boolean isSuspended() {
        return this.suspended;
    }

    public void setSuspended(boolean suspended) {
        this.setLinkedProvision(null);
        this.suspended = suspended;
    }

    public SealPos getSealPos() {
        return this.sealPos;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Task t && t.id == this.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public long getLifespan() {
        return this.lifespan;
    }

    public void setLifespan(short ls) {
        this.lifespan = ls;
    }

    /** Может ли голем взяться: цвета голема и печати совпадают (или один без цвета) и печать согласна. */
    public boolean canGolemPerformTask(IGolemAPI golem) {
        ISealEntity se = GolemHelper.getSealEntity(golem.getGolemWorld().dimension(), this.sealPos);
        if (se != null) {
            return (golem.getGolemColor() <= 0 || se.getColor() <= 0 || golem.getGolemColor() == se.getColor()) && se.getSeal().canGolemPerformTask(golem, this);
        }
        return true;
    }

    public int getData() {
        return this.data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public ProvisionRequest getLinkedProvision() {
        return this.linkedProvision;
    }

    public void setLinkedProvision(ProvisionRequest linkedProvision) {
        this.linkedProvision = linkedProvision;
    }
}
