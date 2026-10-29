package thaumcraft.common.entities.projectile;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.capabilities.ThaumcraftAttachments;

/**
 * Крюк магического крюкомёта (порт EntityGrapple): летит 30 тиков (гравитация 0.03); зацепившись, тянет бросившего к себе
 * (быстрее вблизи, рывок вверх в начале, без урона от падения), Shift — отцепиться. У игрока одновременно один крюк:
 * в 1.12 — статическая карта grapples (общая для сторон), в порте — несохраняемое вложение игрока thaumcraft:grapple (id крюка).
 * Движение игрока задаёт клиент, поэтому тяга считается на обеих сторонах, как в оригинале.
 */
public class EntityGrapple extends ThrowableProjectile implements IEntityWithComplexSpawn {
    public InteractionHand hand = InteractionHand.MAIN_HAND;
    private boolean pulling = false;
    private boolean boost;
    private boolean added = false;
    /** Колебание верёвки (только клиент). */
    public float ampl = 0.0F;

    public EntityGrapple(EntityType<? extends EntityGrapple> type, Level level) {
        super(type, level);
    }

    public EntityGrapple(Level level, LivingEntity thrower, InteractionHand hand) {
        super(ConfigEntities.GRAPPLE.get(), thrower, level);
        this.hand = hand;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, 0.0F);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf data) {
        data.writeBoolean(this.hand == InteractionHand.MAIN_HAND);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf data) {
        this.hand = data.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public LivingEntity getThrower() {
        return this.getOwner() instanceof LivingEntity l ? l : null;
    }

    @Override
    protected double getDefaultGravity() {
        return this.pulling ? 0.0 : 0.03F;
    }

    public void setPulling() {
        this.pulling = true;
    }

    public boolean getPulling() {
        return this.pulling;
    }

    /** Текущий крюк игрока (или −1). */
    public static int getGrapple(Player player) {
        return player.getData(ThaumcraftAttachments.GRAPPLE);
    }

    private static void setGrapple(LivingEntity thrower, int id) {
        if (thrower instanceof Player p) {
            p.setData(ThaumcraftAttachments.GRAPPLE, id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity thrower = this.getThrower();
        if (!this.pulling && this.isAlive() && (this.tickCount > 30 || thrower == null)) {
            if (thrower != null && thrower instanceof Player p && getGrapple(p) == this.getId()) {
                setGrapple(thrower, -1);
            }
            this.discard();
        }
        if (thrower == null) {
            return;
        }
        if (!this.level().isClientSide && this.isAlive() && !this.added) {
            if (thrower instanceof Player p) {
                int old = getGrapple(p);
                if (old != this.getId() && this.level().getEntity(old) instanceof EntityGrapple g) {
                    g.discard();
                }
            }
            setGrapple(thrower, this.getId());
            this.added = true;
        }
        if (!this.level().isClientSide && thrower instanceof Player p && getGrapple(p) != this.getId()) {
            this.discard();
        }
        double dis = thrower.distanceTo(this);
        if (this.pulling && this.isAlive()) {
            if (thrower.isShiftKeyDown()) {
                setGrapple(thrower, -1);
                this.discard();
            } else {
                if (thrower instanceof ServerPlayer sp) {
                    sp.connection.aboveGroundTickCount = 0;
                }
                thrower.fallDistance = 0.0F;
                double mx = this.getX() - thrower.getX();
                double my = this.getY() - thrower.getY();
                double mz = this.getZ() - thrower.getZ();
                double dd = dis;
                if (dis < 8.0) {
                    dd = dis * (8.0 - dis);
                }
                dd = Math.max(1.0E-9, dd);
                mx /= dd * 5.0;
                my /= dd * 5.0;
                mz /= dd * 5.0;
                Vec3 v2 = new Vec3(mx, my, mz);
                if (v2.length() > 0.25) {
                    v2 = v2.normalize();
                    mx = v2.x / 4.0;
                    my = v2.y / 4.0;
                    mz = v2.z / 4.0;
                }
                Vec3 m = thrower.getDeltaMovement();
                double boostY = 0.0;
                if (!this.boost) {
                    boostY = 0.4F;
                    this.boost = true;
                }
                thrower.setDeltaMovement(m.x + mx, m.y + my + 0.033 + boostY, m.z + mz);
            }
        }
        if (this.level().isClientSide) {
            if (!this.pulling) {
                this.ampl += 0.02F;
            } else {
                this.ampl *= 0.66F;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 6) {
            this.setPulling();
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void onHit(HitResult mop) {
        if (!this.level().isClientSide) {
            this.setPulling();
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(mop.getLocation());
            this.level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != this.getOwner();
    }
}
