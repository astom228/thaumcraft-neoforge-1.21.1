package thaumcraft.common.entities.construct;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.container.ContainerTurretAdvanced;

/**
 * Улучшенный автоматический арбалет (порт EntityTurretCrossbowAdvanced): цели выбираются в окне — животные, монстры,
 * игроки (только при разрешённом PvP), «дружественные» (тогда — только свои: команда и существа хозяина). 40 здоровья,
 * броня 8, стреляет чаще.
 */
public class EntityTurretCrossbowAdvanced extends EntityTurretCrossbow {
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(EntityTurretCrossbowAdvanced.class, EntityDataSerializers.BYTE);

    public EntityTurretCrossbowAdvanced(EntityType<? extends EntityTurretCrossbowAdvanced> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0, 20, 40, 24.0F));
        this.goalSelector.addGoal(2, new WatchTarget(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestValidTarget(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS, (byte) 2);
    }

    private boolean flag(int bit) {
        return (this.entityData.get(FLAGS) & 1 << bit) != 0;
    }

    private void setFlag(int bit, boolean v) {
        byte b = this.entityData.get(FLAGS);
        this.entityData.set(FLAGS, (byte) (v ? b | 1 << bit : b & ~(1 << bit)));
        this.setTarget(null);
    }

    public boolean getTargetAnimal() {
        return this.flag(0);
    }

    public void setTargetAnimal(boolean v) {
        this.setFlag(0, v);
    }

    public boolean getTargetMob() {
        return this.flag(1);
    }

    public void setTargetMob(boolean v) {
        this.setFlag(1, v);
    }

    public boolean getTargetPlayer() {
        return this.flag(2);
    }

    public void setTargetPlayer(boolean v) {
        this.setFlag(2, v);
    }

    public boolean getTargetFriendly() {
        return this.flag(3);
    }

    public void setTargetFriendly(boolean v) {
        this.setFlag(3, v);
    }

    /** canAttackClass оригинала: животные (не монстры), монстры, игроки — по флагам; игроки — только при PvP. */
    public boolean canAttackKind(LivingEntity e) {
        boolean animal = (e instanceof Animal || e instanceof WaterAnimal || e instanceof AmbientCreature) && !(e instanceof Enemy);
        if (animal && this.getTargetAnimal()) {
            return true;
        }
        if (e instanceof Enemy && this.getTargetMob()) {
            return true;
        }
        if (!(e instanceof Player) || !this.getTargetPlayer()) {
            return false;
        }
        if (!this.level().isClientSide && this.getServer() != null && !this.getServer().isPvpAllowed() && !this.getTargetFriendly()) {
            this.setTargetPlayer(false);
            return false;
        }
        return true;
    }

    @Override
    public int getArmorValue() {
        return 8;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.getServer() != null && !this.getServer().isPvpAllowed() && this.getTarget() instanceof Player
                && this.getTarget() != this.getOwnerEntity()) {
            this.setTarget(null);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(FLAGS, nbt.getByte("targets"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putByte("targets", this.entityData.get(FLAGS));
    }

    /** В оригинале у улучшенной турели нет отскока после ранения (только у базовой). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.hurtNoJiggle(source, amount);
    }

    @Override
    public void knockback(double strength, double x, double z) {
        this.knockbackPlain(strength, x / 10.0, z / 10.0);
    }

    @Override
    public void move(MoverType t, Vec3 v) {
        this.movePlain(t, new Vec3(v.x / 15.0, v.y, v.z / 15.0));
    }

    @Override
    protected ItemStack placerItem() {
        return new ItemStack(ItemsTC.turretAdvanced.get());
    }

    @Override
    protected void openMenu(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ContainerTurretAdvanced(id, inv, this),
                Component.translatable("entity.thaumcraft.turretadvanced")), buf -> buf.writeVarInt(this.getId()));
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        float b = lootingBonus(level, source);
        if (this.random.nextFloat() < 0.2F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.mindBiothaumic.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.5F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.mechanismSimple.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.3F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.plateBrass.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.4F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.plateIron.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.4F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.plateIron.get()), 0.5F);
        }
    }

    /**
     * Ближайшая подходящая цель (EntityAINearestValidTarget улучшенной турели): по флагам вида; команда хозяина и его
     * существа — только в «дружественном» режиме (и наоборот); игрок-невидимка и крадущийся замечаются ближе.
     */
    static class NearestValidTarget extends TargetGoal {
        private final EntityTurretCrossbowAdvanced turret;
        private LivingEntity targetEntity;
        private int targetUnseenTicks;

        NearestValidTarget(EntityTurretCrossbowAdvanced turret) {
            super(turret, true, false);
            this.turret = turret;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        private double range() {
            return this.turret.getAttributeValue(Attributes.FOLLOW_RANGE);
        }

        private static UUID ownerOf(LivingEntity e) {
            return e instanceof OwnableEntity o ? o.getOwnerUUID() : null;
        }

        boolean isGoodTarget(LivingEntity posTar) {
            EntityTurretCrossbowAdvanced attacker = this.turret;
            if (posTar == null || posTar == attacker || !posTar.isAlive() || !attacker.canAttackKind(posTar)) {
                return false;
            }
            if (posTar instanceof Player p && (p.isCreative() || p.isSpectator())) {
                return false;
            }
            boolean friendly = attacker.getTargetFriendly();
            Team team = attacker.getTeam();
            Team team1 = posTar.getTeam();
            if (team != null && team1 == team && !friendly) {
                return false;
            }
            if (team != null && team1 != team && friendly) {
                return false;
            }
            UUID owner = attacker.getOwnerUUID();
            if (owner != null) {
                UUID other = ownerOf(posTar);
                if (posTar instanceof OwnableEntity && owner.equals(other) && !friendly) {
                    return false;
                }
                if (!(posTar instanceof OwnableEntity) && !(posTar instanceof Player) && friendly) {
                    return false;
                }
                if (posTar instanceof OwnableEntity && !owner.equals(other) && friendly) {
                    return false;
                }
                if (posTar == attacker.getOwnerEntity() && !friendly) {
                    return false;
                }
            }
            return attacker.getSensing().hasLineOfSight(posTar) && attacker.isWithinRestriction(posTar.blockPosition());
        }

        @Override
        public boolean canUse() {
            if (this.turret.getRandom().nextInt(5) != 0) {
                return false;
            }
            double d0 = this.range();
            LivingEntity best = null;
            double bestD = Double.MAX_VALUE;
            for (LivingEntity e : this.turret.level().getEntitiesOfClass(LivingEntity.class, this.turret.getBoundingBox().inflate(d0, 4.0, d0))) {
                if (e instanceof Player p) {
                    double dd = d0;
                    if (p.isCrouching()) {
                        dd *= 0.8F;
                    }
                    if (p.isInvisible()) {
                        dd *= 0.7F * Math.max(0.1F, p.getArmorCoverPercentage());
                    }
                    if (p.distanceTo(this.turret) > dd) {
                        continue;
                    }
                }
                if (!this.isGoodTarget(e)) {
                    continue;
                }
                double d = this.turret.distanceToSqr(e);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
            this.targetEntity = best;
            return best != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = this.turret.getTarget();
            if (t == null || !t.isAlive()) {
                return false;
            }
            boolean friendly = this.turret.getTargetFriendly();
            Team team = this.turret.getTeam();
            Team team1 = t.getTeam();
            if (team != null && team1 == team && !friendly || team != null && team1 != team && friendly) {
                return false;
            }
            double d0 = this.range();
            if (this.turret.distanceToSqr(t) > d0 * d0) {
                return false;
            }
            if (this.turret.getSensing().hasLineOfSight(t)) {
                this.targetUnseenTicks = 0;
            } else if (++this.targetUnseenTicks > 60) {
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            this.turret.setTarget(this.targetEntity);
            this.targetUnseenTicks = 0;
            super.start();
        }
    }
}
