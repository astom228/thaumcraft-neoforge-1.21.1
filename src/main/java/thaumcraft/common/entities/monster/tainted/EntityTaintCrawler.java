package thaumcraft.common.entities.monster.tainted;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.world.taint.BlockTaintFibre;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.config.ConfigPotions;

/**
 * Заражённый ползун (порт EntityTaintCrawler): мелкая членистоногая тварь скверны. Раз в 2 с оставляет на новом месте
 * заражённые волокна; укус на нормальной/высокой сложности иногда отравляет скверной. 1/8 — кристалл Выброса.
 */
public class EntityTaintCrawler extends Monster implements ITaintedMob {
    BlockPos lastPos = BlockPos.ZERO;

    public EntityTaintCrawler(EntityType<? extends EntityTaintCrawler> type, Level world) {
        super(type, world);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.275).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof ITaintedMob) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof ITaintedMob || super.isAlliedTo(other);
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SILVERFISH_STEP, 0.15F, 1.0F);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public void tick() {
        Level w = this.level();
        BlockPos p = this.blockPosition();
        if (!w.isClientSide && this.tickCount % 40 == 0 && !this.lastPos.equals(p)) {
            this.lastPos = p;
            BlockState bs = w.getBlockState(p);
            if (!bs.is(BlockTags.LEAVES) && bs.getFluidState().isEmpty() && !TaintHelper.isTaint(bs)
                    && (bs.isAir() || bs.canBeReplaced() || bs.getBlock() instanceof BushBlock)
                    && TaintHelper.isAdjacentToSolidBlock(w, p) && !BlockTaintFibre.isOnlyAdjacentToTaint(w, p)) {
                w.setBlockAndUpdate(p, BlocksTC.taintFibre.get().defaultBlockState());
            }
        }
        super.tick();
    }

    /** onInitialSpawn оригинала ничего не делает. Членистоногое — тег сущностей minecraft:arthropod. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        return data;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        if (this.random.nextInt(8) == 0) {
            this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(Aspect.FLUX), this.getBbHeight() / 2.0F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        if (super.doHurtTarget(victim)) {
            if (victim instanceof LivingEntity le) {
                int b0 = this.level().getDifficulty() == Difficulty.NORMAL ? 3 : this.level().getDifficulty() == Difficulty.HARD ? 6 : 0;
                if (b0 > 0 && this.random.nextInt(b0 + 1) > 2) {
                    le.addEffect(new MobEffectInstance(ConfigPotions.FLUX_TAINT, b0 * 20, 0));
                }
            }
            return true;
        }
        return false;
    }
}
