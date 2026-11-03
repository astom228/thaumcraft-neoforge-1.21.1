package thaumcraft.common.entities.construct;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.container.ContainerTurretBasic;
import thaumcraft.common.lib.SoundsTC;

/**
 * Автоматический арбалет (порт EntityTurretCrossbow): стреляет стрелами из «руки» по монстрам в 24 блоках; стрелы берёт
 * из раздатчика под собой (смотрящего вверх). Неподвижен (сдвиг /20), лечится раз в 4 с; на включённых магических
 * рельсах активатора отключается. Хозяин: Shift — разобрать в предмет, иначе — окно с ячейкой стрел.
 */
public class EntityTurretCrossbow extends EntityOwnedConstruct implements RangedAttackMob {
    int loadProgressInt = 0;
    boolean isLoadInProgress = false;
    float loadProgress = 0.0F;
    float prevLoadProgress = 0.0F;
    public float loadProgressForRender = 0.0F;

    public EntityTurretCrossbow(EntityType<? extends EntityTurretCrossbow> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0, 20, 60, 24.0F));
        this.goalSelector.addGoal(2, new WatchTarget(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, e -> e instanceof Enemy
                && this.isWithinRestriction(e.blockPosition())));
    }

    /** Стрела из «руки» (с её зельем), подбирать нельзя. */
    @Override
    public void performRangedAttack(LivingEntity target, float range) {
        ItemStack ammo = this.getMainHandItem();
        if (ammo.isEmpty()) {
            return;
        }
        ItemStack one = ammo.copy();
        one.setCount(1);
        Arrow arrow = new Arrow(this.level(), this, one, null);
        arrow.setBaseDamage(2.25 + range * 2.0F + this.random.nextGaussian() * 0.25);
        Vec3 look = this.getViewVector(1.0F);
        if (!this.isPassenger()) {
            arrow.setPos(arrow.getX() - look.x * 0.9F, arrow.getY() - look.y * 0.9F, arrow.getZ() - look.z * 0.9F);
        } else {
            arrow.setPos(arrow.getX() + look.x * 1.75, arrow.getY() + look.y * 1.75, arrow.getZ() + look.z * 1.75);
        }
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double d0 = target.getX() - this.getX();
        double d1 = target.getBoundingBox().minY + target.getEyeHeight() + range * range * 3.0F - arrow.getY();
        double d2 = target.getZ() - this.getZ();
        arrow.shoot(d0, d1, d2, 2.0F, 2.0F);
        this.level().addFreshEntity(arrow);
        this.level().broadcastEntityEvent(this, (byte) 16);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        ammo.shrink(1);
        if (ammo.isEmpty()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            if (!this.swinging) {
                this.swingTime = -1;
                this.swinging = true;
            }
        } else if (id == 17) {
            if (!this.isLoadInProgress) {
                this.loadProgressInt = -1;
                this.isLoadInProgress = true;
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    public float getLoadProgress(float pt) {
        float f1 = this.loadProgress - this.prevLoadProgress;
        if (f1 < 0.0F) {
            f1++;
        }
        return this.prevLoadProgress + f1 * pt;
    }

    /** Взмах (выстрел) — 6 тиков, перезарядка — 10 (только клиент). */
    @Override
    protected void updateSwingTime() {
        if (this.swinging) {
            this.swingTime++;
            if (this.swingTime >= 6) {
                this.swingTime = 0;
                this.swinging = false;
            }
        } else {
            this.swingTime = 0;
        }
        this.attackAnim = this.swingTime / 6.0F;
        if (this.isLoadInProgress) {
            this.loadProgressInt++;
            if (this.loadProgressInt >= 10) {
                this.loadProgressInt = 0;
                this.isLoadInProgress = false;
            }
        } else {
            this.loadProgressInt = 0;
        }
        this.loadProgress = this.loadProgressInt / 10.0F;
    }

    @Override
    public void baseTick() {
        this.prevLoadProgress = this.loadProgress;
        if (!this.level().isClientSide && this.getMainHandItem().isEmpty()) {
            BlockPos p = this.blockPosition().below();
            BlockState bs = this.level().getBlockState(p);
            if (bs.getBlock() instanceof DispenserBlock && bs.getValue(DispenserBlock.FACING) == Direction.UP
                    && this.level().getBlockEntity(p) instanceof DispenserBlockEntity d) {
                for (int a = 0; a < d.getContainerSize(); a++) {
                    ItemStack s = d.getItem(a);
                    if (!s.isEmpty() && s.getItem() instanceof ArrowItem) {
                        this.setItemInHand(InteractionHand.MAIN_HAND, d.removeItem(a, s.getCount()));
                        this.playSound(SoundsTC.ticks.get(), 1.0F, 1.0F);
                        this.level().broadcastEntityEvent(this, (byte) 17);
                        break;
                    }
                }
            }
        }
        super.baseTick();
    }

    @Override
    public int getArmorValue() {
        return 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getTarget() != null && this.isAlliedTo(this.getTarget())) {
            this.setTarget(null);
        }
        if (!this.level().isClientSide) {
            this.setYRot(this.getYHeadRot());
            if (this.tickCount % 80 == 0) {
                this.heal(1.0F);
            }
            BlockPos bp = this.blockPosition();
            if (BaseRailBlock.isRail(this.level(), bp.below())) {
                bp = bp.below();
            }
            BlockState rail = this.level().getBlockState(bp);
            if (rail.is(BlocksTC.activatorRail.get())) {
                this.setNoAi(rail.getValue(PoweredRailBlock.POWERED));
            }
        } else {
            this.updateSwingTime();
        }
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        this.setYRot((float) (this.getYRot() + this.getRandom().nextGaussian() * 45.0));
        this.setXRot((float) (this.getXRot() + this.getRandom().nextGaussian() * 20.0));
        return super.hurt(source, amount);
    }

    @Override
    public void knockback(double strength, double x, double z) {
        super.knockback(strength, x / 10.0, z / 10.0);
        Vec3 m = this.getDeltaMovement();
        if (m.y > 0.1) {
            this.setDeltaMovement(m.x, 0.1, m.z);
        }
    }

    /** Предмет, в который разбирается турель. */
    protected ItemStack placerItem() {
        return new ItemStack(ItemsTC.turretBasic.get());
    }

    protected void openMenu(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ContainerTurretBasic(id, inv, this), Component.translatable("entity.thaumcraft.turretbasic")),
                buf -> buf.writeVarInt(this.getId()));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && this.isOwner(player) && this.isAlive()) {
            if (player.isShiftKeyDown()) {
                this.playSound(SoundsTC.zap.get(), 1.0F, 1.0F);
                this.dropAmmo();
                this.spawnAtLocation(this.placerItem(), 0.5F);
                this.discard();
                player.swing(hand, true);
            } else if (player instanceof ServerPlayer sp) {
                this.openMenu(sp);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.level().isClientSide && this.isOwner(player)) {
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void move(MoverType mt, Vec3 v) {
        super.move(mt, new Vec3(v.x / 20.0, v.y, v.z / 20.0));
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide) {
            this.dropAmmo();
        }
    }

    protected void dropAmmo() {
        if (!this.getMainHandItem().isEmpty()) {
            this.spawnAtLocation(this.getMainHandItem(), 0.5F);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    /** Боеприпасы выбрасывает dropAmmo; ванильный выброс снаряжения не нужен. */
    @Override
    protected void dropEquipment() {
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        float b = lootingBonus(level, source);
        if (this.random.nextFloat() < 0.2F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.mindClockwork.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.5F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.mechanismSimple.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.5F + b) {
            this.spawnAtLocation(new ItemStack(BlocksTC.plankGreatwood.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.5F + b) {
            this.spawnAtLocation(new ItemStack(BlocksTC.plankGreatwood.get()), 0.5F);
        }
    }

    /** 0.15 за уровень «Добычи» оружия убийцы. */
    protected static float lootingBonus(ServerLevel level, DamageSource source) {
        if (source.getEntity() instanceof LivingEntity killer) {
            return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer) * 0.15F;
        }
        return 0.0F;
    }

    @Override
    public int getMaxHeadXRot() {
        return 20;
    }

    /** Следить за целью (EntityAIWatchTarget): 2–4 с смотрит на неё, пока она в радиусе. */
    protected static class WatchTarget extends Goal {
        protected final Mob theWatcher;
        @Nullable
        protected LivingEntity closestEntity;
        private int lookTime;

        public WatchTarget(Mob entityIn) {
            this.theWatcher = entityIn;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.theWatcher.getTarget() != null) {
                this.closestEntity = this.theWatcher.getTarget();
            }
            return this.closestEntity != null;
        }

        @Override
        public boolean canContinueToUse() {
            float d = (float) this.theWatcher.getAttributeValue(Attributes.FOLLOW_RANGE);
            return this.closestEntity != null && this.closestEntity.isAlive() && this.theWatcher.distanceToSqr(this.closestEntity) <= d * d && this.lookTime > 0;
        }

        @Override
        public void start() {
            this.lookTime = 40 + this.theWatcher.getRandom().nextInt(40);
        }

        @Override
        public void stop() {
            this.closestEntity = null;
        }

        @Override
        public void tick() {
            if (this.closestEntity != null) {
                this.theWatcher.getLookControl().setLookAt(this.closestEntity.getX(), this.closestEntity.getY() + this.closestEntity.getEyeHeight(),
                        this.closestEntity.getZ(), 10.0F, this.theWatcher.getMaxHeadXRot());
            }
            this.lookTime--;
        }
    }

    /** Ванильные hurt/knockback/move без поправок базовой турели (у улучшенной — свои). */
    protected final boolean hurtNoJiggle(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    protected final void knockbackPlain(double strength, double x, double z) {
        super.knockback(strength, x, z);
    }

    protected final void movePlain(MoverType mt, Vec3 v) {
        super.move(mt, v);
    }
}
