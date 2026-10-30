package thaumcraft.api.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;

/** Заявка на доставку предмета (порт ProvisionRequest): от печати, блока или существа; id — хэш источника и предмета. */
public class ProvisionRequest {
    private ISealEntity seal;
    private Entity entity;
    private BlockPos pos;
    private Direction side;
    private final ItemStack stack;
    private int id;
    private Task linkedTask;
    private boolean invalid;

    ProvisionRequest(ISealEntity seal, ItemStack stack) {
        this.seal = seal;
        this.stack = stack.copy();
        this.id = (seal.getSealPos().pos.toString() + seal.getSealPos().face.name() + stack + stack.getComponentsPatch()).hashCode();
    }

    ProvisionRequest(BlockPos pos, Direction side, ItemStack stack) {
        this.pos = pos;
        this.side = side;
        this.stack = stack.copy();
        this.id = (pos.toString() + side.name() + stack + stack.getComponentsPatch()).hashCode();
    }

    ProvisionRequest(Entity entity, ItemStack stack) {
        this.entity = entity;
        this.stack = stack.copy();
        this.id = (entity.getId() + stack.toString() + stack.getComponentsPatch()).hashCode();
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ISealEntity getSeal() {
        return this.seal;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public Direction getSide() {
        return this.side;
    }

    public void setSide(Direction side) {
        this.side = side;
    }

    public Task getLinkedTask() {
        return this.linkedTask;
    }

    public void setLinkedTask(Task linkedTask) {
        this.linkedTask = linkedTask;
    }

    public boolean isInvalid() {
        return this.invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ProvisionRequest pr && this.id == pr.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}
