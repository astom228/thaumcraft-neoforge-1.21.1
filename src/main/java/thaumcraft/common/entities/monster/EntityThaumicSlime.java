package thaumcraft.common.entities.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Таумический слизень (порт EntityThaumicSlime): слизень Выброса размера 1/2/4 (чаще крупный на высокой сложности).
 * Крупнее 2 — раз в 5 с плюётся в игрока дальше 4 блоков маленьким слизнем (сам уменьшаясь). Умирая, рассыпается на
 * «размер» слизней размера 1; крупный роняет кристалл Выброса. Сам не появляется — только из флюкс-слизи.
 */
public class EntityThaumicSlime extends Slime implements ITaintedMob {
    int launched = 10;
    int spitCounter = 100;

    public EntityThaumicSlime(EntityType<? extends EntityThaumicSlime> type, Level world) {
        super(type, world);
        int i = 1 << 1 + this.random.nextInt(3);
        this.setSize(i, true);
    }

    /** Слизень-плевок от shooter в сторону target (второй конструктор оригинала). */
    public static EntityThaumicSlime spit(Level world, LivingEntity shooter, LivingEntity target) {
        EntityThaumicSlime s = ConfigEntities.THAUMIC_SLIME.get().create(world);
        if (s == null) {
            return null;
        }
        s.setSize(1, true);
        double y = (shooter.getBoundingBox().minY + shooter.getBoundingBox().maxY) / 2.0;
        double dx = target.getX() - shooter.getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - y;
        double dz = target.getZ() - shooter.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d >= 1.0E-7) {
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            float pitch = (float) (-(Math.atan2(dy, d) * 180.0 / Math.PI));
            s.moveTo(shooter.getX() + dx / d, y, shooter.getZ() + dz / d, yaw, pitch);
            s.shoot(dx, dy + d * 0.2F, dz, 1.5F, 1.0F);
        } else {
            s.moveTo(shooter.getX(), y, shooter.getZ(), 0.0F, 0.0F);
        }
        return s;
    }

    public void shoot(double x, double y, double z, float speed, float spread) {
        float len = Mth.sqrt((float) (x * x + y * y + z * z));
        x = x / len + this.random.nextGaussian() * 0.0075F * spread;
        y = y / len + this.random.nextGaussian() * 0.0075F * spread;
        z = z / len + this.random.nextGaussian() * 0.0075F * spread;
        this.setDeltaMovement(x * speed, y * speed, z * speed);
        double h = Math.sqrt(x * x + z * z);
        this.setYRot((float) (Math.atan2(x, z) * 180.0 / Math.PI));
        this.setXRot((float) (Math.atan2(y, h) * 180.0 / Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        int i = this.random.nextInt(3);
        if (i < 2 && this.random.nextFloat() < 0.5F * difficulty.getSpecialMultiplier()) {
            i++;
        }
        SpawnGroupData r = super.finalizeSpawn(level, difficulty, reason, data);
        this.setSize(1 << i, true);
        return r;
    }

    @Override
    public void setSize(int size, boolean resetHealth) {
        super.setSize(size, resetHealth);
        this.xpReward = size + 2;
    }

    /** Брызги приземления — свои (slimeJumpFX: фиолетовые осколки слизи). */
    @Override
    protected boolean spawnCustomParticles() {
        if (this.level().isClientSide) {
            int i = this.getSize();
            for (int j = 0; j < i * 2; j++) {
                IFXDispatcher.get().slimeJumpFX(this, i);
            }
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        int i = this.getSize();
        if (this.level().isClientSide) {
            if (this.launched > 0) {
                this.launched--;
                for (int j = 0; j < i * (this.launched + 1); j++) {
                    IFXDispatcher.get().slimeJumpFX(this, i);
                }
            }
        } else if (this.isAlive()) {
            Player player = this.level().getNearestPlayer(this, 16.0);
            if (player != null) {
                if (this.spitCounter > 0) {
                    this.spitCounter--;
                }
                this.lookAt(player, 10.0F, 20.0F);
                if (this.distanceTo(player) > 4.0F && this.spitCounter <= 0 && this.getSize() > 2) {
                    this.spitCounter = 101;
                    EntityThaumicSlime fly = spit(this.level(), this, player);
                    if (fly != null) {
                        this.level().addFreshEntity(fly);
                    }
                    this.playSound(SoundsTC.gore.get(), 1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
                    this.setSize(this.getSize() - 1, true);
                }
            }
        }
    }

    /** setDead оригинала: мёртвый крупный слизень распадается на «размер» слизней размера 1 (а не на половинки, как ванильный). */
    @Override
    public void remove(RemovalReason reason) {
        int i = this.getSize();
        if (!this.level().isClientSide && i > 1 && this.isDeadOrDying()) {
            for (int k = 0; k < i; k++) {
                float f = (k % 2 - 0.5F) * i / 4.0F;
                float f1 = (k / 2 - 0.5F) * i / 4.0F;
                EntityThaumicSlime s = ConfigEntities.THAUMIC_SLIME.get().create(this.level());
                if (s != null) {
                    s.setSize(1, true);
                    s.moveTo(this.getX() + f, this.getY() + 0.5, this.getZ() + f1, this.random.nextFloat() * 360.0F, 0.0F);
                    this.level().addFreshEntity(s);
                }
            }
        }
        this.setRemoved(reason);
    }

    @Override
    protected float getAttackDamage() {
        return this.getSize() + 1;
    }

    @Override
    protected boolean isDealsDamage() {
        return true;
    }

    /** dealDamage: дальность удара 0.6·размер (+2 сразу после плевка). */
    @Override
    protected void dealDamage(LivingEntity target) {
        int i = this.getSize();
        if (this.launched > 0) {
            i += 2;
        }
        if (this.hasLineOfSight(target) && this.distanceToSqr(target) < 0.6 * i * 0.6 * i
                && target.hurt(this.damageSources().mobAttack(this), this.getAttackDamage())) {
            this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            if (this.level() instanceof ServerLevel sl) {
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(sl, target, this.damageSources().mobAttack(this));
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        if (this.getSize() > 1) {
            this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(Aspect.FLUX), this.getBbHeight() / 2.0F);
        }
    }
}
