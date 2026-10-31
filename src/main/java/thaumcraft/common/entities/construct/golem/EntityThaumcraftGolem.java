package thaumcraft.common.entities.construct.golem;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;
import thaumcraft.common.entities.construct.golem.ai.AIFollowOwner;
import thaumcraft.common.entities.construct.golem.ai.AIGotoBlock;
import thaumcraft.common.entities.construct.golem.ai.AIGotoEntity;
import thaumcraft.common.entities.construct.golem.ai.AIGotoHome;
import thaumcraft.common.entities.construct.golem.ai.AIOwnerHurtByTarget;
import thaumcraft.common.entities.construct.golem.ai.AIOwnerHurtTarget;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Голем (порт EntityThaumcraftGolem). Состав — 8 байт (материал, голова, руки, ноги, дополнение, ранг) в двух
 * синхронизируемых int; третий int — цвет и флаги (0 — карабкается, 1 — следует за хозяином, 3 — в бою). Задачи берёт
 * у печатей (TaskHandler), домашняя зона — 32 блока (разведчик — 48). Несёт предметы в руках (носильщик — в обеих).
 */
public class EntityThaumcraftGolem extends EntityOwnedConstruct implements IGolemAPI, RangedAttackMob {
    int rankXp = 0;
    private static final EntityDataAccessor<Integer> PROPS1 = SynchedEntityData.defineId(EntityThaumcraftGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROPS2 = SynchedEntityData.defineId(EntityThaumcraftGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROPS3 = SynchedEntityData.defineId(EntityThaumcraftGolem.class, EntityDataSerializers.INT);
    protected Task task = null;
    protected int taskID = Integer.MAX_VALUE;
    public static final int XPM = 1000;

    /** Клиентское состояние отрисовки (в оригинале — статические карты по id сущности в деталях). */
    public float wheelRot;
    public float grinderSpeed;
    public float grinderRot;

    private final MoveControl groundMoveControl;
    private final MoveControl flyMoveControl;

    public EntityThaumcraftGolem(EntityType<? extends EntityThaumcraftGolem> type, Level world) {
        super(type, world);
        this.xpReward = 5;
        this.groundMoveControl = this.moveControl;
        this.flyMoveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.FLYING_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.removeAllGoals(g -> true);
        this.goalSelector.addGoal(2, new AIGotoEntity(this));
        this.goalSelector.addGoal(3, new AIGotoBlock(this));
        this.goalSelector.addGoal(4, new AIGotoHome(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PROPS1, 0);
        builder.define(PROPS2, 0);
        builder.define(PROPS3, 0);
    }

    @Override
    public IGolemProperties getProperties() {
        long hi = this.entityData.get(PROPS1) & 0xFFFFFFFFL;
        long lo = this.entityData.get(PROPS2) & 0xFFFFFFFFL;
        return GolemProperties.fromLong(hi << 32 | lo);
    }

    @Override
    public void setProperties(IGolemProperties prop) {
        long l = prop.toLong();
        this.entityData.set(PROPS1, (int) (l >>> 32));
        this.entityData.set(PROPS2, (int) l);
    }

    /** Байт b3 (старший) int-а PROPS3 — цвет, следующий — флаги (Utils.intToByteArray оригинала — big-endian). */
    @Override
    public byte getGolemColor() {
        return (byte) (this.entityData.get(PROPS3) >>> 24);
    }

    public void setGolemColor(byte b) {
        int v = this.entityData.get(PROPS3);
        this.entityData.set(PROPS3, v & 0x00FFFFFF | (b & 0xFF) << 24);
    }

    public byte getFlags() {
        return (byte) (this.entityData.get(PROPS3) >>> 16);
    }

    public void setFlags(byte b) {
        int v = this.entityData.get(PROPS3);
        this.entityData.set(PROPS3, v & 0xFF00FFFF | (b & 0xFF) << 16);
    }

    private boolean getFlag(int bit) {
        return (this.getFlags() & 1 << bit) != 0;
    }

    private void setFlag(int bit, boolean value) {
        byte b = this.getFlags();
        this.setFlags((byte) (value ? b | 1 << bit : b & ~(1 << bit)));
    }

    private void updateEntityAttributes() {
        IGolemProperties props = this.getProperties();
        int mh = 10 + props.getMaterial().healthMod;
        if (props.hasTrait(EnumGolemTrait.FRAGILE)) {
            mh = (int) (mh * 0.75);
        }
        mh += props.getRank();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(mh);
        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(props.hasTrait(EnumGolemTrait.WHEELED) ? 0.5 : 0.6);
        BlockPos home = this.hasRestriction() ? this.getRestrictCenter() : this.blockPosition();
        this.restrictTo(home, props.hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32);
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(props.hasTrait(EnumGolemTrait.SCOUT) ? 56.0 : 40.0);
        this.navigation = this.getGolemNavigator();
        this.moveControl = props.hasTrait(EnumGolemTrait.FLYER) ? this.flyMoveControl : this.groundMoveControl;
        if (props.hasTrait(EnumGolemTrait.FIGHTER)) {
            double da = props.getMaterial().damage;
            if (props.hasTrait(EnumGolemTrait.BRUTAL)) {
                da = Math.max(da * 1.5, da + 1.0);
            }
            da += props.getRank() * 0.25;
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(da);
        } else {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        }
        this.createAI();
    }

    private void createAI() {
        this.goalSelector.removeAllGoals(g -> true);
        this.targetSelector.removeAllGoals(g -> true);
        if (this.isFollowingOwner()) {
            this.goalSelector.addGoal(4, new AIFollowOwner(this, 1.0, 10.0F, 2.0F));
        } else {
            this.goalSelector.addGoal(3, new AIGotoEntity(this));
            this.goalSelector.addGoal(4, new AIGotoBlock(this));
            this.goalSelector.addGoal(5, new AIGotoHome(this));
        }
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        IGolemProperties props = this.getProperties();
        if (props.hasTrait(EnumGolemTrait.FIGHTER)) {
            if (this.getNavigation() instanceof GroundPathNavigation) {
                this.goalSelector.addGoal(0, new FloatGoal(this));
            }
            if (props.hasTrait(EnumGolemTrait.RANGED) && props.getArms().function != null) {
                RangedAttackGoal aa = props.getArms().function.getRangedAttackAI(this);
                if (aa != null) {
                    this.goalSelector.addGoal(1, aa);
                }
            }
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
            if (this.isFollowingOwner()) {
                this.targetSelector.addGoal(1, new AIOwnerHurtByTarget(this));
                this.targetSelector.addGoal(2, new AIOwnerHurtTarget(this));
            }
            this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        }
    }

    @Override
    public boolean onClimbable() {
        return this.isBesideClimbableBlock();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
        this.restrictTo(this.blockPosition(), 32);
        this.updateEntityAttributes();
        return data;
    }

    @Override
    public int getArmorValue() {
        int armor = this.getProperties().getMaterial().armor;
        if (this.getProperties().hasTrait(EnumGolemTrait.ARMORED)) {
            armor = (int) Math.max(armor * 1.5, armor + 1);
        }
        if (this.getProperties().hasTrait(EnumGolemTrait.FRAGILE)) {
            armor = (int) (armor * 0.75);
        }
        return armor;
    }

    @Override
    public void aiStep() {
        this.updateSwingTime();
        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();
        IGolemProperties props = this.getProperties();
        if (!this.level().isClientSide) {
            if (this.task != null && this.task.isSuspended()) {
                this.task = null;
            }
            LivingEntity target = this.getTarget();
            if (target != null && !target.isAlive()) {
                this.setTarget(null);
            }
            if (this.getTarget() != null && props.hasTrait(EnumGolemTrait.RANGED) && this.distanceToSqr(this.getTarget()) > 1024.0) {
                this.setTarget(null);
            }
            if (this.getTarget() instanceof Player && this.getServer() != null && !this.getServer().isPvpAllowed()) {
                this.setTarget(null);
            }
            if (this.tickCount % (props.hasTrait(EnumGolemTrait.REPAIR) ? 40 : 100) == 0) {
                this.heal(1.0F);
            }
            if (props.hasTrait(EnumGolemTrait.CLIMBER)) {
                this.setBesideClimbableBlock(!this.swinging && this.horizontalCollision);
            }
        }
        if (props.getHead().function != null) {
            props.getHead().function.onUpdateTick(this);
        }
        if (props.getArms().function != null) {
            props.getArms().function.onUpdateTick(this);
        }
        if (props.getLegs().function != null) {
            props.getLegs().function.onUpdateTick(this);
        }
        if (props.getAddon().function != null) {
            props.getAddon().function.onUpdateTick(this);
        }
    }

    /** Эмоции: 5 — взял задачу, 6 — не справился, 7 — (не используется), 8 — «стой», 9 — новый ранг. */
    @Override
    public void handleEntityEvent(byte id) {
        IFXDispatcher fx = IFXDispatcher.get();
        double y = this.getY() + this.getBbHeight() + 0.1;
        if (id == 5) {
            fx.drawGenericParticles(this.getX(), y, this.getZ(), 0.0, 0.0, 0.0, 1.0F, 1.0F, 1.0F, 0.5F, false, 704 + (this.random.nextBoolean() ? 0 : 3), 3, 1, 6,
                    0, 2.0F, 0.0F, 1);
        } else if (id == 6) {
            fx.drawGenericParticles(this.getX(), y, this.getZ(), 0.0, 0.025, 0.0, 0.1F, 1.0F, 1.0F, 0.5F, false, 15, 1, 1, 10, 0, 2.0F, 0.0F, 1);
        } else if (id == 7) {
            fx.drawGenericParticles(this.getX(), y, this.getZ(), 0.0, 0.05, 0.0, 1.0F, 1.0F, 1.0F, 0.5F, false, 640, 10, 1, 10, 0, 2.0F, 0.0F, 1);
        } else if (id == 8) {
            fx.drawGenericParticles(this.getX(), y, this.getZ(), 0.0, 0.01, 0.0, 1.0F, 1.0F, 0.1F, 0.5F, false, 14, 1, 1, 20, 0, 2.0F, 0.0F, 1);
        } else if (id == 9) {
            for (int a = 0; a < 5; a++) {
                fx.drawGenericParticles(this.getX(), this.getY() + this.getBbHeight(), this.getZ(), this.random.nextGaussian() * 0.01F,
                        this.random.nextFloat() * 0.02, this.random.nextGaussian() * 0.01F, 1.0F, 1.0F, 1.0F, 0.5F, false, 13, 1, 1, 20 + this.random.nextInt(20), 0,
                        0.3F + this.random.nextFloat() * 0.4F, 0.0F, 1);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    public float getGolemMoveSpeed() {
        IGolemProperties p = this.getProperties();
        return 1.0F + p.getRank() * 0.025F + (p.hasTrait(EnumGolemTrait.LIGHT) ? 0.2F : 0.0F) + (p.hasTrait(EnumGolemTrait.HEAVY) ? -0.175F : 0.0F)
                + (p.hasTrait(EnumGolemTrait.FLYER) ? -0.33F : 0.0F) + (p.hasTrait(EnumGolemTrait.WHEELED) ? 0.25F : 0.0F);
    }

    /**
     * Навигация по ногам. В оригинале — свои навигаторы (наземный с проходом через двери, воздушный); здесь — ванильные
     * аналоги 1.21 (DECISIONS).
     */
    public PathNavigation getGolemNavigator() {
        PathNavigation nav;
        if (this.getProperties().hasTrait(EnumGolemTrait.FLYER)) {
            FlyingPathNavigation fly = new FlyingPathNavigation(this, this.level());
            fly.setCanOpenDoors(false);
            fly.setCanFloat(true);
            fly.setCanPassDoors(true);
            nav = fly;
        } else if (this.getProperties().hasTrait(EnumGolemTrait.CLIMBER)) {
            nav = new WallClimberNavigation(this, this.level());
        } else {
            nav = new GroundPathNavigation(this, this.level());
        }
        if (nav instanceof GroundPathNavigation g) {
            g.setCanPassDoors(true);
        }
        return nav;
    }

    /** Полёт: ускорение 0.02 по направлению управления, торможение 0.9, без цели — медленное снижение. */
    @Override
    public void travel(Vec3 input) {
        if (this.isControlledByLocalInstance() && this.getProperties().hasTrait(EnumGolemTrait.FLYER)) {
            this.moveRelative(0.02F, input);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            Vec3 m = this.getDeltaMovement().scale(0.9F);
            if (this.getTarget() == null) {
                m = m.add(0.0, -0.005, 0.0);
            }
            this.setDeltaMovement(m);
            this.calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return this.getProperties().hasTrait(EnumGolemTrait.HEAVY) && !this.getProperties().hasTrait(EnumGolemTrait.FLYER) ? MovementEmission.ALL
                : MovementEmission.NONE;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        if (this.getProperties().hasTrait(EnumGolemTrait.FLYER) || this.getProperties().hasTrait(EnumGolemTrait.CLIMBER)) {
            return false;
        }
        return super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setProperties(GolemProperties.fromLong(nbt.getLong("props")));
        this.restrictTo(BlockPos.of(nbt.getLong("homepos")), 32);
        this.setFlags(nbt.getByte("gflags"));
        this.rankXp = nbt.getInt("rankXP");
        this.setGolemColor(nbt.getByte("color"));
        this.updateEntityAttributes();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putLong("props", this.getProperties().toLong());
        nbt.putLong("homepos", this.getRestrictCenter().asLong());
        nbt.putByte("gflags", this.getFlags());
        nbt.putInt("rankXP", this.rankXp);
        nbt.putByte("color", this.getGolemColor());
    }

    /** Огнеупорный не горит; взрывоустойчивый получает 30% (не больше половины здоровья); кактус не ранит; застряв — домой. */
    @Override
    protected void actuallyHurt(DamageSource ds, float damage) {
        if (ds.is(DamageTypeTags.IS_FIRE) && this.getProperties().hasTrait(EnumGolemTrait.FIREPROOF)) {
            return;
        }
        if (ds.is(DamageTypeTags.IS_EXPLOSION) && this.getProperties().hasTrait(EnumGolemTrait.BLASTPROOF)) {
            damage = Math.min(this.getMaxHealth() / 2.0F, damage * 0.3F);
        }
        if (ds.is(DamageTypes.CACTUS)) {
            return;
        }
        if (ds.is(DamageTypes.IN_WALL) || ds.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            BlockPos home = this.getRestrictCenter();
            this.moveTo(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5, 0.0F, 0.0F);
        }
        super.actuallyHurt(ds, damage);
    }

    /**
     * Хозяин: с Shift — разобрать в предмет (с составом и опытом); краситель — окрасить. Колокольчик (следовать/стоять)
     * добавляется вместе с самим колокольчиком.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof NameTagItem) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide && this.isOwner(player) && this.isAlive()) {
            if (player.isShiftKeyDown()) {
                this.playSound(SoundsTC.zap.get(), 1.0F, 1.0F);
                if (this.task != null) {
                    this.task.setReserved(false);
                }
                this.dropCarried();
                this.spawnAtLocation(ItemGolemPlacer.createStack(this.getProperties().toLong(), this.rankXp), 0.5F);
                this.discard();
                player.swing(hand, true);
            } else if (this.handleBell(player, held)) {
                player.swing(hand, true);
            } else if (held.getItem() instanceof DyeItem dye) {
                this.playSound(SoundsTC.zap.get(), 1.0F, 1.0F);
                this.setGolemColor((byte) (dye.getDyeColor().getId() + 1));
                held.shrink(1);
                player.swing(hand, true);
            }
            return InteractionResult.SUCCESS;
        }
        if (!this.level().isClientSide || player.isShiftKeyDown()) {
            return super.mobInteract(player, hand);
        }
        return this.isOwner(player) ? InteractionResult.SUCCESS : super.mobInteract(player, hand);
    }

    /** Колокольчик големанта + исследование GOLEMDIRECT: переключить «следовать за хозяином» / «стоять здесь». */
    protected boolean handleBell(Player player, ItemStack held) {
        return false;
    }

    protected void toggleFollow(Player player) {
        if (this.task != null) {
            this.task.setReserved(false);
        }
        this.playSound(SoundsTC.scan.get(), 1.0F, 1.0F);
        this.setFollowingOwner(!this.isFollowingOwner());
        if (this.isFollowingOwner()) {
            player.displayClientMessage(Component.translatable("golem.follow", ""), true);
            if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes.get()) {
                this.level().broadcastEntityEvent(this, (byte) 5);
            }
        } else {
            player.displayClientMessage(Component.translatable("golem.stay", ""), true);
            if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes.get()) {
                this.level().broadcastEntityEvent(this, (byte) 8);
            }
            this.restrictTo(this.blockPosition(), 32);
        }
        this.updateEntityAttributes();
    }

    @Override
    public void die(DamageSource cause) {
        if (this.task != null) {
            this.task.setReserved(false);
        }
        super.die(cause);
        if (!this.level().isClientSide) {
            this.dropCarried();
        }
    }

    protected void dropCarried() {
        for (ItemStack s : this.getCarrying()) {
            if (!s.isEmpty()) {
                this.spawnAtLocation(s, 0.25F);
            }
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    /** Несомое уже выброшено в die/dropCarried — ванильный выброс снаряжения не нужен. */
    @Override
    protected void dropEquipment() {
    }

    /** Добыча — детали сборки: каждая с шансом 30% (+15% за уровень добычи), часть количества. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        int looting = 0;
        if (source.getEntity() instanceof LivingEntity killer) {
            looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer);
        }
        float b = looting * 0.15F;
        for (ItemStack stack : this.getProperties().generateComponents()) {
            ItemStack s = stack.copy();
            if (this.random.nextFloat() < 0.3F + b) {
                if (s.getCount() > 0) {
                    s.shrink(this.random.nextInt(s.getCount()));
                }
                this.spawnAtLocation(s, 0.25F);
            }
        }
    }

    public boolean isBesideClimbableBlock() {
        return this.getFlag(0);
    }

    public void setBesideClimbableBlock(boolean climbing) {
        this.setFlag(0, climbing);
    }

    public boolean isFollowingOwner() {
        return this.getFlag(1);
    }

    public void setFollowingOwner(boolean follow) {
        this.setFlag(1, follow);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        this.setInCombat(this.getTarget() != null);
    }

    @Override
    public boolean isInCombat() {
        return this.getFlag(3);
    }

    public void setInCombat(boolean combat) {
        this.setFlag(3, combat);
    }

    /**
     * Удар: ванильный расчёт (урон атаки, чары, отбрасывание), затем действие рук; убийство — 8 опыта ранга. Ловкий голем
     * засчитывает удар хозяину (в оригинале — recentlyHit, т.е. добыча «как от игрока»).
     */
    @Override
    public boolean doHurtTarget(Entity ent) {
        boolean flag = super.doHurtTarget(ent);
        if (flag) {
            if (ent instanceof LivingEntity le && this.getProperties().hasTrait(EnumGolemTrait.DEFT) && this.getOwnerEntity() instanceof Player owner) {
                le.setLastHurtByPlayer(owner);
            }
            if (this.getProperties().getArms().function != null) {
                this.getProperties().getArms().function.onMeleeAttack(this, ent);
            }
            if (ent instanceof net.minecraft.world.entity.Mob m && !m.isAlive()) {
                this.addRankXp(8);
            }
        }
        return flag;
    }

    public Task getTask() {
        if (this.task == null && this.taskID != Integer.MAX_VALUE) {
            this.task = TaskHandler.getTask(this.level().dimension(), this.taskID);
            this.taskID = Integer.MAX_VALUE;
        }
        return this.task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public void addRankXp(int xp) {
        if (this.getProperties().hasTrait(EnumGolemTrait.SMART) && !this.level().isClientSide) {
            int rank = this.getProperties().getRank();
            if (rank < 10) {
                this.rankXp += xp;
                int xn = (rank + 1) * (rank + 1) * XPM;
                if (this.rankXp >= xn) {
                    this.rankXp -= xn;
                    IGolemProperties props = this.getProperties();
                    props.setRank(rank + 1);
                    this.setProperties(props);
                    if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes.get()) {
                        this.level().broadcastEntityEvent(this, (byte) 9);
                        this.playSound(SoundEvents.PLAYER_LEVELUP, 0.25F, 1.0F);
                    }
                }
            }
        }
    }

    private int carrySlots() {
        return this.getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1;
    }

    private static EquipmentSlot slot(int a) {
        return a == 0 ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    @Override
    public ItemStack holdItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }
        for (int a = 0; a < this.carrySlots(); a++) {
            ItemStack in = this.getItemBySlot(slot(a));
            if (in.isEmpty()) {
                this.setItemSlot(slot(a), stack);
                return ItemStack.EMPTY;
            }
            if (in.getCount() < in.getMaxStackSize() && ItemStack.isSameItemSameComponents(in, stack)) {
                int d = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
                stack.shrink(d);
                in.grow(d);
                if (stack.getCount() <= 0) {
                    stack = ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    @Override
    public ItemStack dropItem(ItemStack stack) {
        ItemStack out = ItemStack.EMPTY;
        for (int a = 0; a < this.carrySlots(); a++) {
            ItemStack in = this.getItemBySlot(slot(a));
            if (in.isEmpty()) {
                continue;
            }
            if (stack != null && !stack.isEmpty()) {
                if (ItemStack.isSameItemSameComponents(in, stack)) {
                    out = in.copy();
                    out.setCount(Math.min(stack.getCount(), out.getCount()));
                    in.shrink(stack.getCount());
                    if (in.getCount() <= 0) {
                        this.setItemSlot(slot(a), ItemStack.EMPTY);
                    }
                }
            } else {
                out = in.copy();
                this.setItemSlot(slot(a), ItemStack.EMPTY);
            }
            if (!out.isEmpty()) {
                break;
            }
        }
        if (this.getProperties().hasTrait(EnumGolemTrait.HAULER) && this.getItemBySlot(slot(0)).isEmpty() && !this.getItemBySlot(slot(1)).isEmpty()) {
            this.setItemSlot(slot(0), this.getItemBySlot(slot(1)).copy());
            this.setItemSlot(slot(1), ItemStack.EMPTY);
        }
        return out;
    }

    @Override
    public boolean canCarry(ItemStack stack, boolean partial) {
        for (int a = 0; a < this.carrySlots(); a++) {
            ItemStack in = this.getItemBySlot(slot(a));
            if (in.isEmpty()) {
                return true;
            }
            if (in.getCount() < in.getMaxStackSize() && ItemStack.isSameItemSameComponents(in, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCarrying(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (int a = 0; a < this.carrySlots(); a++) {
            ItemStack in = this.getItemBySlot(slot(a));
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NonNullList<ItemStack> getCarrying() {
        if (this.getProperties().hasTrait(EnumGolemTrait.HAULER)) {
            NonNullList<ItemStack> stacks = NonNullList.withSize(2, ItemStack.EMPTY);
            stacks.set(0, this.getItemBySlot(slot(0)));
            stacks.set(1, this.getItemBySlot(slot(1)));
            return stacks;
        }
        return NonNullList.withSize(1, this.getItemBySlot(slot(0)));
    }

    @Override
    public LivingEntity getGolemEntity() {
        return this;
    }

    @Override
    public Level getGolemWorld() {
        return this.level();
    }

    @Override
    public void swingArm() {
        if (!this.swinging || this.swingTime >= 3 || this.swingTime < 0) {
            this.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float range) {
        if (this.getProperties().getArms().function != null) {
            this.getProperties().getArms().function.onRangedAttack(this, target, range);
        }
    }
}
