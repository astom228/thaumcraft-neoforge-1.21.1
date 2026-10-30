package thaumcraft.common.entities.monster;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootTable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.container.ContainerPech;
import thaumcraft.common.entities.ai.pech.AINearestAttackableTargetPech;
import thaumcraft.common.entities.ai.pech.AIPechItemEntityGoto;
import thaumcraft.common.entities.ai.pech.AIPechTradePlayer;
import thaumcraft.common.items.casters.foci.FocusEffectAir;
import thaumcraft.common.items.casters.foci.FocusEffectCurse;
import thaumcraft.common.items.casters.foci.FocusEffectEarth;
import thaumcraft.common.items.casters.foci.FocusEffectFire;
import thaumcraft.common.items.casters.foci.FocusEffectFlux;
import thaumcraft.common.items.casters.foci.FocusMediumProjectile;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

/**
 * Пех (порт EntityPech): мелкий торговец из волшебных лесов. Три типа по снаряжению при появлении — собиратель (оружие
 * ближнего боя или ничего), тауматург (жезл пеха: случайный фокус-снаряд), следопыт (лук, иногда отравленные стрелы).
 * Подбирает ценные предметы (эндер-жемчуг и вещи с аспектом Desiderium) — может приручиться; приручённый торгует:
 * за ценную вещь даёт товары своего типа и вещи из рюкзака (9 ячеек). Удар игрока злит всех пехов в 32 блоках.
 */
public class EntityPech extends Monster implements RangedAttackMob {
    public NonNullList<ItemStack> loot = NonNullList.withSize(9, ItemStack.EMPTY);
    public boolean trading = false;
    private final RangedAttackGoal aiArrowAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15.0F);
    private final RangedAttackGoal aiBlastAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15.0F);
    private final MeleeAttackGoal aiMeleeAttack = new MeleeAttackGoal(this, 0.6, false);
    private final AvoidEntityGoal<Player> aiAvoidPlayer = new AvoidEntityGoal<>(this, Player.class, 8.0F, 0.5, 0.6);
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.BOOLEAN);
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "entities/pech"));
    /** Метка предмета, выброшенного из окна торговли: пехи его не подбирают (thrower «PechDrop» оригинала). */
    public static final String PECH_DROP = "PechDrop";
    public float mumble = 0.0F;
    int chargecount = 0;

    public EntityPech(EntityType<? extends EntityPech> type, Level world) {
        super(type, world);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    /** valuedItems оригинала: эндер-жемчуг — ценность 15. */
    private static int valuedItem(ItemStack stack) {
        return stack.is(Items.ENDER_PEARL) ? 15 : 0;
    }

    @Override
    protected Component getTypeName() {
        return switch (this.getPechType()) {
            case 1 -> Component.translatable("entity.thaumcraft.pech.1");
            case 2 -> Component.translatable("entity.thaumcraft.pech.2");
            default -> super.getTypeName();
        };
    }

    @Override
    protected void registerGoals() {
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanFloat(false);
            nav.setCanOpenDoors(true);
        }
        this.setDropChance(EquipmentSlot.MAINHAND, 0.2F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.2F);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AIPechTradePlayer(this));
        this.goalSelector.addGoal(3, new AIPechItemEntityGoto(this));
        this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 4, () -> true));
        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new AINearestAttackableTargetPech(this, Player.class, true));
    }

    /** Боевое поведение по предмету в руке: лук и жезл — дальний бой, остальное — ближний; дикий пех сторонится игроков. */
    public void setCombatTask() {
        if (this.level() == null || this.level().isClientSide) {
            return;
        }
        this.goalSelector.removeGoal(this.aiMeleeAttack);
        this.goalSelector.removeGoal(this.aiArrowAttack);
        this.goalSelector.removeGoal(this.aiBlastAttack);
        ItemStack itemstack = this.getMainHandItem();
        if (itemstack.is(Items.BOW)) {
            this.goalSelector.addGoal(2, this.aiArrowAttack);
        } else if (itemstack.is(ItemsTC.pechWand.get())) {
            this.goalSelector.addGoal(2, this.aiBlastAttack);
        } else {
            this.goalSelector.addGoal(2, this.aiMeleeAttack);
        }
        this.goalSelector.removeGoal(this.aiAvoidPlayer);
        if (!this.isTamed()) {
            this.goalSelector.addGoal(4, this.aiAvoidPlayer);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (this.getPechType() == 2) {
            ItemStack bow = this.getMainHandItem();
            Arrow arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), bow.isEmpty() ? null : bow);
            if (this.random.nextFloat() < 0.2F) {
                arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 40));
            }
            double d0 = target.getX() - this.getX();
            double d1 = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - arrow.getY();
            double d2 = target.getZ() - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            arrow.shoot(d0, d1 + d3 * 0.2F, d2, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
            // сила, отдача и пламя лука в 1.21 применяются через оружие, переданное стреле
            arrow.setBaseDamage(f * 2.0F + this.random.nextGaussian() * 0.25 + this.level().getDifficulty().getId() * 0.11F);
            this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level().addFreshEntity(arrow);
        } else if (this.getPechType() == 1) {
            FocusPackage p = new FocusPackage(this);
            FocusMediumRoot root = new FocusMediumRoot();
            double off = this.distanceTo(target) / 6.0F;
            root.setupFromCasterToTarget(this, target, off);
            p.addNode(root);
            FocusMediumProjectile fp = new FocusMediumProjectile();
            fp.initialize();
            fp.getSetting("speed").setValue(2);
            p.addNode(fp);
            p.addNode(this.random.nextBoolean() ? new FocusEffectCurse()
                    : (this.random.nextBoolean() ? new FocusEffectFlux()
                            : (this.random.nextBoolean() ? new FocusEffectEarth() : (this.random.nextBoolean() ? new FocusEffectAir() : new FocusEffectFire()))));
            FocusEngine.castFocusPackage(this, p, true);
            this.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!this.level().isClientSide && slot == EquipmentSlot.MAINHAND) {
            this.setCombatTask();
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource rand, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(rand, difficulty);
        Item item = switch (rand.nextInt(20)) {
            case 0, 12 -> ItemsTC.pechWand.get();
            case 1 -> Items.STONE_SWORD;
            case 2, 4, 10, 11, 13 -> Items.BOW;
            case 3 -> Items.STONE_AXE;
            case 5 -> Items.IRON_SWORD;
            case 6 -> Items.IRON_AXE;
            case 7 -> Items.FISHING_ROD;
            case 8 -> Items.STONE_PICKAXE;
            case 9 -> Items.IRON_PICKAXE;
            default -> null;
        };
        if (item != null) {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData res = super.finalizeSpawn(level, diff, reason, data);
        ItemStack itemstack = this.getMainHandItem();
        if (itemstack.is(ItemsTC.pechWand.get())) {
            this.setPechType(1);
            this.setDropChance(EquipmentSlot.MAINHAND, 0.1F);
        } else if (itemstack.is(Items.BOW)) {
            this.setPechType(2);
        }
        float f = diff.getSpecialMultiplier();
        this.setCanPickUpLoot(this.random.nextFloat() < 0.75F * f);
        this.setCombatTask();
        return res;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, (byte) 0);
        builder.define(ANGER, 0);
        builder.define(TAMED, false);
    }

    public int getPechType() {
        return this.entityData.get(TYPE);
    }

    public int getAnger() {
        return this.entityData.get(ANGER);
    }

    public boolean isTamed() {
        return this.entityData.get(TAMED);
    }

    public void setPechType(int type) {
        this.entityData.set(TYPE, (byte) type);
    }

    public void setAnger(int anger) {
        this.entityData.set(ANGER, anger);
    }

    public void setTamed(boolean tamed) {
        this.entityData.set(TAMED, tamed);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putByte("PechType", (byte) this.getPechType());
        nbt.putShort("Anger", (short) this.getAnger());
        nbt.putBoolean("Tamed", this.isTamed());
        ContainerHelper.saveAllItems(nbt, this.loot, this.registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("PechType")) {
            this.setPechType(nbt.getByte("PechType"));
        }
        this.setAnger(nbt.getShort("Anger"));
        this.setTamed(nbt.getBoolean("Tamed"));
        this.loot = NonNullList.withSize(9, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, this.loot, this.registryAccess());
        this.setCombatTask();
    }

    /** Не исчезает, если в рюкзаке 5 вещей и больше. */
    @Override
    public boolean removeWhenFarAway(double dist) {
        int q = 0;
        for (ItemStack is : this.loot) {
            if (!is.isEmpty()) {
                q++;
            }
        }
        return q < 5;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return LOOT;
    }

    /** dropLoot: каждая вещь рюкзака выпадает с шансом 1/3, затем таблица добычи. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack stack : this.loot) {
            if (!stack.isEmpty() && this.random.nextFloat() < 0.33F) {
                this.spawnAtLocation(stack.copy(), 1.5F);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            this.mumble = (float) Math.PI;
        } else if (id == 17) {
            this.mumble = (float) (Math.PI * 2);
        } else if (id == 18) {
            this.particles(ParticleTypes.HAPPY_VILLAGER);
        }
        if (id == 19) {
            this.particles(ParticleTypes.ANGRY_VILLAGER);
            this.mumble = (float) (Math.PI * 2);
        } else if (id < 16 || id > 18) {
            super.handleEntityEvent(id);
        }
    }

    private void particles(net.minecraft.core.particles.SimpleParticleType type) {
        for (int i = 0; i < 5; i++) {
            double d0 = this.random.nextGaussian() * 0.02;
            double d1 = this.random.nextGaussian() * 0.02;
            double d2 = this.random.nextGaussian() * 0.02;
            this.level().addParticle(type, this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                    this.getY() + 0.5 + this.random.nextFloat() * this.getBbHeight(), this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                    d0, d1, d2);
        }
    }

    /** Бормотание: рядом с другим пехом — «торговый» звук (событие 17), иначе — челюсти (16). */
    @Override
    public void playAmbientSound() {
        if (!this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                for (Entity e : this.level().getEntities(this, this.getBoundingBox().inflate(4.0, 2.0, 4.0))) {
                    if (e instanceof EntityPech) {
                        this.level().broadcastEntityEvent(this, (byte) 17);
                        this.playSound(SoundsTC.pech_trade.get(), this.getSoundVolume(), this.getVoicePitch());
                        return;
                    }
                }
            }
            this.level().broadcastEntityEvent(this, (byte) 16);
        }
        super.playAmbientSound();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.pech_idle.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundsTC.pech_hit.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.pech_death.get();
    }

    private void becomeAngryAt(Entity entity) {
        if (entity instanceof Player p && p.isCreative()) {
            return;
        }
        if (this.getAnger() <= 0) {
            this.level().broadcastEntityEvent(this, (byte) 19);
            this.playSound(SoundsTC.pech_charge.get(), this.getSoundVolume(), this.getVoicePitch());
        }
        if (entity instanceof LivingEntity le) {
            this.setTarget(le);
        }
        this.setAnger(400 + this.random.nextInt(400));
        this.setTamed(false);
        this.setCombatTask();
    }

    @Override
    public int getArmorValue() {
        return Math.min(20, super.getArmorValue() + 2);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (this.isInvulnerableTo(src)) {
            return false;
        }
        Entity entity = src.getEntity();
        if (entity instanceof Player && !this.level().isClientSide) {
            for (EntityPech pech : this.level().getEntitiesOfClass(EntityPech.class, this.getBoundingBox().inflate(32.0, 16.0, 32.0), e -> e != this)) {
                pech.becomeAngryAt(entity);
            }
            this.becomeAngryAt(entity);
        }
        return super.hurt(src, amount);
    }

    @Override
    public void tick() {
        if (this.mumble > 0.0F) {
            this.mumble *= 0.75F;
        }
        if (!this.level().isClientSide) {
            if (this.getAnger() > 0) {
                this.setAnger(this.getAnger() - 1);
            }
            if (this.getAnger() > 0 && this.getTarget() != null) {
                if (this.chargecount > 0) {
                    this.chargecount--;
                }
                if (this.chargecount == 0) {
                    this.chargecount = 100;
                    this.playSound(SoundsTC.pech_charge.get(), this.getSoundVolume(), this.getVoicePitch());
                }
                this.level().broadcastEntityEvent(this, (byte) 17);
            }
        }
        if (this.level().isClientSide && this.random.nextInt(15) == 0 && this.getAnger() > 0) {
            this.particle(ParticleTypes.ANGRY_VILLAGER);
        }
        if (this.level().isClientSide && this.random.nextInt(25) == 0 && this.isTamed()) {
            this.particle(ParticleTypes.HAPPY_VILLAGER);
        }
        super.tick();
    }

    private void particle(net.minecraft.core.particles.SimpleParticleType type) {
        double d0 = this.random.nextGaussian() * 0.02;
        double d1 = this.random.nextGaussian() * 0.02;
        double d2 = this.random.nextGaussian() * 0.02;
        this.level().addParticle(type, this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                this.getY() + 0.5 + this.random.nextFloat() * this.getBbHeight(), this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                d0, d1, d2);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 40 == 0) {
            this.heal(1.0F);
        }
    }

    /** Может ли подобрать: дикий — ценное; иначе — если есть место в рюкзаке. */
    public boolean canPickup(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!this.isTamed() && valuedItem(stack) > 0) {
            return true;
        }
        for (ItemStack slot : this.loot) {
            if (slot.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(stack, slot) && stack.getCount() + slot.getCount() <= slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** Подобрать: ценное дикому — шанс приручиться (ценность из 10), съедает 1; остальное — в рюкзак. Возвращает остаток. */
    public ItemStack pickupItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!this.isTamed() && this.isValued(stack)) {
            if (this.random.nextInt(10) < this.getValue(stack)) {
                this.setTamed(true);
                this.setCombatTask();
                this.level().broadcastEntityEvent(this, (byte) 18);
            }
            stack.shrink(1);
            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        }
        for (int a = 0; a < this.loot.size() && !stack.isEmpty(); a++) {
            ItemStack slot = this.loot.get(a);
            if (!slot.isEmpty() && slot.getCount() < slot.getMaxStackSize() && ItemStack.isSameItemSameComponents(stack, slot)) {
                int sz = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(sz);
                stack.shrink(sz);
            }
        }
        for (int a = 0; a < this.loot.size() && !stack.isEmpty(); a++) {
            if (this.loot.get(a).isEmpty()) {
                this.loot.set(a, stack.copy());
                return ItemStack.EMPTY;
            }
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || player.getItemInHand(hand).getItem() instanceof NameTagItem) {
            return InteractionResult.PASS;
        }
        if (this.isTamed()) {
            if (player instanceof ServerPlayer sp) {
                sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ContainerPech(id, inv, this), this.getDisplayName()), buf -> buf.writeVarInt(this.getId()));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    public boolean isValued(ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }
        if (valuedItem(item) > 0) {
            return true;
        }
        AspectList al = ThaumcraftCraftingManager.getObjectTags(item);
        return al != null && al.getAmount(Aspect.DESIRE) > 1;
    }

    public int getValue(ItemStack item) {
        if (item.isEmpty()) {
            return 0;
        }
        int value = valuedItem(item);
        if (value == 0) {
            AspectList al = ThaumcraftCraftingManager.getObjectTags(item);
            value = al == null ? 0 : Math.min(32, al.getAmount(Aspect.DESIRE) / 2);
        }
        return value;
    }

    /** Товар: ценность (1–5) и предмет. */
    public record Trade(int value, ItemStack stack) {
    }

    private static boolean tagExists(String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        return BuiltInRegistries.ITEM.getTagOrEmpty(tag).iterator().hasNext();
    }

    /**
     * tradeInventory оригинала: товары по типу пеха. Собирается при каждой торговле (книги зачарований требуют реестров
     * мира). Тотем, компас и спектральная стрела в оригинале по ошибке добавлялись к товарам собирателя — так и оставлено.
     */
    public static List<Trade> trades(int type, RegistryAccess access) {
        List<Trade> forInv = new ArrayList<>();
        List<Trade> forMag = new ArrayList<>();
        List<Trade> forArc = new ArrayList<>();
        forInv.add(new Trade(1, new ItemStack(ItemsTC.clusterIron.get())));
        forInv.add(new Trade(1, new ItemStack(ItemsTC.clusterGold.get())));
        forInv.add(new Trade(1, new ItemStack(ItemsTC.clusterCinnabar.get())));
        forInv.add(new Trade(1, new ItemStack(ItemsTC.clusterQuartz.get())));
        forInv.add(new Trade(1, new ItemStack(ItemsTC.clusterCopper.get())));
        if (tagExists("ingots/tin")) {
            forInv.add(new Trade(1, new ItemStack(ItemsTC.clusterTin.get())));
        }
        if (tagExists("ingots/silver")) {
            forInv.add(new Trade(1, new ItemStack(ItemsTC.clusterSilver.get())));
        }
        if (tagExists("ingots/lead")) {
            forInv.add(new Trade(1, new ItemStack(ItemsTC.clusterLead.get())));
        }
        forInv.add(new Trade(2, new ItemStack(Items.BLAZE_ROD)));
        forInv.add(new Trade(2, new ItemStack(BlocksTC.saplingGreatwood.get())));
        forInv.add(new Trade(2, new ItemStack(Items.DRAGON_BREATH)));
        forInv.add(new Trade(3, new ItemStack(Items.EXPERIENCE_BOTTLE)));
        forInv.add(new Trade(3, new ItemStack(Items.EXPERIENCE_BOTTLE)));
        forInv.add(new Trade(3, new ItemStack(Items.GOLDEN_APPLE)));
        forInv.add(new Trade(4, new ItemStack(ItemsTC.thaumiumPick.get())));
        forInv.add(new Trade(4, new ItemStack(ItemsTC.thaumiumAxe.get())));
        forInv.add(new Trade(4, new ItemStack(ItemsTC.thaumiumHoe.get())));
        forInv.add(new Trade(5, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));
        forInv.add(new Trade(5, new ItemStack(BlocksTC.saplingSilverwood.get())));
        forInv.add(new Trade(5, new ItemStack(ItemsTC.curioKnowledge.get())));
        forMag.add(new Trade(1, ThaumcraftApiHelper.makeCrystal(Aspect.AIR)));
        forMag.add(new Trade(1, ThaumcraftApiHelper.makeCrystal(Aspect.EARTH)));
        forMag.add(new Trade(1, ThaumcraftApiHelper.makeCrystal(Aspect.FIRE)));
        forMag.add(new Trade(1, ThaumcraftApiHelper.makeCrystal(Aspect.WATER)));
        forMag.add(new Trade(1, ThaumcraftApiHelper.makeCrystal(Aspect.ORDER)));
        forMag.add(new Trade(1, ThaumcraftApiHelper.makeCrystal(Aspect.ENTROPY)));
        forMag.add(new Trade(2, potion(Potions.REGENERATION)));
        forMag.add(new Trade(2, potion(Potions.HEALING)));
        forMag.add(new Trade(2, ThaumcraftApiHelper.makeCrystal(Aspect.FLUX)));
        forMag.add(new Trade(3, new ItemStack(Items.EXPERIENCE_BOTTLE)));
        forMag.add(new Trade(3, new ItemStack(Items.EXPERIENCE_BOTTLE)));
        forMag.add(new Trade(3, ThaumcraftApiHelper.makeCrystal(Aspect.AURA)));
        forMag.add(new Trade(3, new ItemStack(Items.GOLDEN_APPLE)));
        forMag.add(new Trade(4, new ItemStack(ItemsTC.clothBoots.get())));
        forMag.add(new Trade(4, new ItemStack(ItemsTC.clothChest.get())));
        forMag.add(new Trade(4, new ItemStack(ItemsTC.clothLegs.get())));
        forMag.add(new Trade(5, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));
        forMag.add(new Trade(5, new ItemStack(ItemsTC.pechWand.get())));
        forMag.add(new Trade(5, new ItemStack(ItemsTC.curioKnowledge.get())));
        forMag.add(new Trade(5, new ItemStack(ItemsTC.amuletVisFound.get())));
        forInv.add(new Trade(5, new ItemStack(Items.TOTEM_OF_UNDYING)));
        // свечи 15 цветов (EnumDyeColor.byDyeDamage(0..14) — все, кроме белой)
        for (var e : BlocksTC.candles.entrySet()) {
            if (e.getKey() != net.minecraft.world.item.DyeColor.WHITE) {
                forArc.add(new Trade(1, new ItemStack(e.getValue().get())));
            }
        }
        forArc.add(new Trade(2, new ItemStack(Items.GHAST_TEAR)));
        forInv.add(new Trade(2, new ItemStack(Items.COMPASS)));
        forArc.add(new Trade(2, book(access, Enchantments.POWER)));
        forArc.add(new Trade(3, new ItemStack(Items.EXPERIENCE_BOTTLE)));
        forArc.add(new Trade(3, new ItemStack(Items.EXPERIENCE_BOTTLE)));
        forArc.add(new Trade(3, new ItemStack(Items.GOLDEN_APPLE)));
        // Око потустороннего (eldritchEye) в BETA17 не зарегистрировано — товар был пустым; не переносится (DECISIONS 135)
        forArc.add(new Trade(4, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));
        forInv.add(new Trade(4, new ItemStack(Items.SPECTRAL_ARROW)));
        forArc.add(new Trade(5, new ItemStack(ItemsTC.ringApprentice.get())));
        forArc.add(new Trade(5, book(access, Enchantments.FLAME)));
        forArc.add(new Trade(5, book(access, Enchantments.INFINITY)));
        forArc.add(new Trade(5, new ItemStack(ItemsTC.curioKnowledge.get())));
        return switch (type) {
            case 1 -> forMag;
            case 2 -> forArc;
            default -> forInv;
        };
    }

    private static ItemStack potion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> p) {
        ItemStack s = new ItemStack(Items.POTION);
        s.set(DataComponents.POTION_CONTENTS, new PotionContents(p));
        return s;
    }

    private static ItemStack book(RegistryAccess access, ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        var holder = access.registryOrThrow(Registries.ENCHANTMENT).getHolder(key);
        return holder.map(h -> EnchantedBookItem.createForEnchantment(new EnchantmentInstance(h, 1))).orElse(new ItemStack(Items.BOOK));
    }
}
