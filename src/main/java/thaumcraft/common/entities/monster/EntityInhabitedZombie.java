package thaumcraft.common.entities.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCTags;

/**
 * Одержимый зомби («Ходячая оболочка», порт EntityInhabitedZombie): зомби в латах Багрового культа, внутри — потусторонний
 * краб. Умирая, выпускает краба в шлеме (и отдаёт опыт), сам добычи не даёт. Нападает и на культистов. Не больше одного
 * в 32 блоках.
 */
public class EntityInhabitedZombie extends Zombie implements IEldritchMob {
    public EntityInhabitedZombie(EntityType<? extends EntityInhabitedZombie> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, e -> e.getType().is(TCTags.EntityTypes.CULTISTS)));
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected void randomizeReinforcementsChance() {
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity) {
        return true;
    }

    /** Латы культа: шлем всегда, нагрудник и поножи — 60% (90% на высокой сложности). */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        float d = level.getDifficulty() == Difficulty.HARD ? 0.9F : 0.6F;
        SpawnGroupData r = super.finalizeSpawn(level, diff, reason, data);
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemsTC.crimsonPlateHelm.get()));
        if (this.random.nextFloat() <= d) {
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemsTC.crimsonPlateChest.get()));
        }
        if (this.random.nextFloat() <= d) {
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ItemsTC.crimsonPlateLegs.get()));
        }
        return r;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance diff) {
    }

    /** onDeath оригинала пуст: без добычи и обычной смерти. */
    @Override
    public void die(DamageSource src) {
    }

    /** onDeathUpdate: сразу — краб в шлеме, опыт (если убит игроком), облачко; зомби исчезает. */
    @Override
    protected void tickDeath() {
        Level w = this.level();
        if (!w.isClientSide) {
            EntityEldritchCrab crab = ConfigEntities.ELDRITCH_CRAB.get().create(w);
            if (crab != null) {
                crab.moveTo(this.getX(), this.getY() + this.getEyeHeight(), this.getZ(), this.getYRot(), this.getXRot());
                crab.setHelm(true);
                w.addFreshEntity(crab);
            }
            if ((this.lastHurtByPlayerTime > 0 || this.isAlwaysExperienceDropper()) && this.shouldDropLoot()
                    && w.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT) && w instanceof ServerLevel sl) {
                ExperienceOrb.award(sl, this.position(), this.getExperienceReward(sl, this.lastHurtByPlayer));
            }
        }
        for (int i = 0; i < 20; i++) {
            w.addParticle(ParticleTypes.POOF, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), this.random.nextGaussian() * 0.02,
                    this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02);
        }
        this.remove(RemovalReason.KILLED);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.crabtalk.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.GENERIC_HURT;
    }

    /** getCanSpawnHere: в 32×16×32 вокруг нет другого одержимого зомби. */
    public static boolean checkInhabitedSpawnRules(EntityType<EntityInhabitedZombie> type, ServerLevelAccessor level, MobSpawnType reason,
                                                   net.minecraft.core.BlockPos pos, RandomSource rand) {
        return level.getEntitiesOfClass(EntityInhabitedZombie.class, new AABB(pos).inflate(32.0, 16.0, 32.0)).isEmpty()
                && checkMonsterSpawnRules(type, level, reason, pos, rand);
    }
}
