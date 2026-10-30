package thaumcraft.common.entities.monster.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.common.entities.monster.boss.EntityCultistLeader;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Культист Багрового культа (порт EntityCultist): основа рыцаря и клирика. Ломает двери, снаряжение выпадает с шансом 5%,
 * свет для появления не важен. Свои — культисты и претор. Добыча — таблица thaumcraft:entities/cultist.
 */
public class EntityCultist extends Monster {
    /** Вспышка появления ждёт, пока сущность появится у клиентов (событие сразу после создания до них не доходит). */
    private boolean pendingSpawnFx;

    public EntityCultist(EntityType<? extends EntityCultist> type, Level world) {
        super(type, world);
        this.xpReward = 10;
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
        this.setDropChance(EquipmentSlot.CHEST, 0.05F);
        this.setDropChance(EquipmentSlot.FEET, 0.05F);
        this.setDropChance(EquipmentSlot.HEAD, 0.05F);
        this.setDropChance(EquipmentSlot.LEGS, 0.05F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> LOOT = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "entities/cultist"));

    @Override
    protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getDefaultLootTable() {
        return LOOT;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    /** setLoot оригинала: снаряжение при появлении. */
    protected void setLoot(DifficultyInstance diff) {
    }

    /** setEnchantmentBasedOnDifficulty оригинала. */
    protected void enchantGear(RandomSource rand, DifficultyInstance diff) {
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource rand, DifficultyInstance diff) {
    }

    @Override
    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource rand, DifficultyInstance diff) {
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        this.setLoot(diff);
        this.enchantGear(this.random, diff);
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("HomeD")) {
            this.restrictTo(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")), nbt.getInt("HomeD"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.hasRestriction() && this.getRestrictRadius() > 0.0F) {
            nbt.putInt("HomeD", (int) this.getRestrictRadius());
            nbt.putInt("HomeX", this.getRestrictCenter().getX());
            nbt.putInt("HomeY", this.getRestrictCenter().getY());
            nbt.putInt("HomeZ", this.getRestrictCenter().getZ());
        }
    }

    @Override
    public boolean isAlliedTo(Entity el) {
        return el instanceof EntityCultist || el instanceof EntityCultistLeader || super.isAlliedTo(el);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof EntityCultist) && !(target instanceof EntityCultistLeader) && super.canAttack(target);
    }

    /** Облачко появления (spawnExplosionParticle оригинала): на сервере — событие 20, на клиенте — красные вспышки. */
    @Override
    public void spawnAnim() {
        if (this.level().isClientSide) {
            cultistSpawnFX(this);
        } else {
            this.pendingSpawnFx = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.pendingSpawnFx && !this.level().isClientSide && this.tickCount >= 2) {
            this.pendingSpawnFx = false;
            this.level().broadcastEntityEvent(this, (byte) 20);
        }
    }

    public static void cultistSpawnFX(LivingEntity e) {
        var rand = e.getRandom();
        for (int i = 0; i < 20; i++) {
            double d0 = rand.nextGaussian() * 0.05;
            double d1 = rand.nextGaussian() * 0.05;
            double d2 = rand.nextGaussian() * 0.05;
            double d3 = 2.0;
            IFXDispatcher.get().cultistSpawn(e.getX() + rand.nextFloat() * e.getBbWidth() * 2.0F - e.getBbWidth() + d0 * d3,
                    e.getY() + rand.nextFloat() * e.getBbHeight() + d1 * d3, e.getZ() + rand.nextFloat() * e.getBbWidth() * 2.0F - e.getBbWidth() + d2 * d3, d0, d1, d2);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 20) {
            cultistSpawnFX(this);
        } else {
            super.handleEntityEvent(id);
        }
    }
}
