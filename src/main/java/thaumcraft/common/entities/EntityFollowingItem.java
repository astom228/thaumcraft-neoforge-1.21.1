package thaumcraft.common.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Предмет, летящий к цели (порт EntityFollowingItem): «Собиратель» тянет добычу к игроку. Скорость растёт по мере приближения
 * (age от 20 до 1), у цели летит обычным предметом. На клиенте оставляет след (type 10 — пузырьки, иначе — искры нитора).
 */
public class EntityFollowingItem extends EntitySpecialItem implements IEntityWithComplexSpawn {
    double targetX = 0.0;
    double targetY = 0.0;
    double targetZ = 0.0;
    int type = 3;
    public Entity target = null;
    int followAge = 20;
    public double gravity = 0.04F;

    public EntityFollowingItem(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack) {
        this(ConfigEntities.FOLLOWING_ITEM.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.setYRot((float) (Math.random() * 360.0));
    }

    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack, Entity target, int t) {
        this(level, x, y, z, stack);
        this.target = target;
        this.targetX = target.getX();
        this.targetY = target.getBoundingBox().minY + target.getBbHeight() / 2.0F;
        this.targetZ = target.getZ();
        this.type = t;
        this.noPhysics = true;
    }

    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack, double tx, double ty, double tz) {
        this(level, x, y, z, stack);
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
    }

    @Override
    public void tick() {
        if (this.target != null) {
            this.targetX = this.target.getX();
            this.targetY = this.target.getBoundingBox().minY + this.target.getBbHeight() / 2.0F;
            this.targetZ = this.target.getZ();
        }
        Vec3 m = this.getDeltaMovement();
        if (this.targetX == 0.0 && this.targetY == 0.0 && this.targetZ == 0.0) {
            this.setDeltaMovement(m.x, m.y - this.gravity, m.z);
        } else {
            float xd = (float) (this.targetX - this.getX());
            float yd = (float) (this.targetY - this.getY());
            float zd = (float) (this.targetZ - this.getZ());
            if (this.followAge > 1) {
                this.followAge--;
            }
            double distance = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            if (distance > 0.5) {
                distance *= this.followAge;
                this.setDeltaMovement(xd / distance, yd / distance, zd / distance);
            } else {
                this.setDeltaMovement(m.x * 0.1F, m.y * 0.1F, m.z * 0.1F);
                this.targetX = 0.0;
                this.targetY = 0.0;
                this.targetZ = 0.0;
                this.target = null;
                this.noPhysics = false;
            }
            if (this.level().isClientSide) {
                var rand = this.random;
                float h = (float) ((this.getBoundingBox().maxY - this.getBoundingBox().minY) / 2.0) + Mth.sin(this.getAge() / 10.0F + this.bobOffs) * 0.1F
                        + 0.1F;
                if (this.type != 10) {
                    IFXDispatcher.get().drawNitorCore((float) this.xo + (rand.nextFloat() - rand.nextFloat()) * 0.125F,
                            (float) this.yo + h + (rand.nextFloat() - rand.nextFloat()) * 0.125F,
                            (float) this.zo + (rand.nextFloat() - rand.nextFloat()) * 0.125F, rand.nextGaussian() * 0.01F, rand.nextGaussian() * 0.01F,
                            rand.nextGaussian() * 0.01F);
                } else {
                    IFXDispatcher.get().crucibleBubble((float) this.xo + (rand.nextFloat() - rand.nextFloat()) * 0.125F,
                            (float) this.yo + h + (rand.nextFloat() - rand.nextFloat()) * 0.125F,
                            (float) this.zo + (rand.nextFloat() - rand.nextFloat()) * 0.125F, 0.33F, 0.33F, 1.0F);
                }
            }
        }
        super.tick();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putShort("type", (short) this.type);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.type = nbt.getShort("type");
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf data) {
        data.writeInt(this.target == null ? -1 : this.target.getId());
        data.writeDouble(this.targetX);
        data.writeDouble(this.targetY);
        data.writeDouble(this.targetZ);
        data.writeByte(this.type);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf data) {
        int ent = data.readInt();
        if (ent > -1) {
            this.target = this.level().getEntity(ent);
        }
        this.targetX = data.readDouble();
        this.targetY = data.readDouble();
        this.targetZ = data.readDouble();
        this.type = data.readByte();
    }
}
