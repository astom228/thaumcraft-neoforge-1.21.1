package thaumcraft.common.entities.monster;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import thaumcraft.api.items.ItemsTC;

/**
 * Злобный зомби (порт EntityBrainyZombie): крепче (25 здоровья, удар 5, +1 брони), без подкреплений; часто роняет мозг
 * зомби (вместе с обычной добычей зомби). В воде не превращается в утопленника (в 1.12 этого не было).
 */
public class EntityBrainyZombie extends Zombie {
    public EntityBrainyZombie(EntityType<? extends EntityBrainyZombie> type, Level world) {
        super(type, world);
    }

    /** Броня зомби 2 + 1 (getTotalArmorValue + 1). */
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 25.0).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0)
                .add(Attributes.ARMOR, 3.0);
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected void randomizeReinforcementsChance() {
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return EntityType.ZOMBIE.getDefaultLootTable();
    }

    /** dropLoot оригинала: мозг с шансом (random(10) − добыча ≤ 4), плюс добыча зомби. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, src, recentlyHit);
        int looting = src.getEntity() instanceof LivingEntity le
                ? EnchantmentHelper.getEnchantmentLevel(level.registryAccess().holderOrThrow(Enchantments.LOOTING), le) : 0;
        if (this.random.nextInt(10) - looting <= 4) {
            this.spawnAtLocation(new ItemStack(ItemsTC.brain.get()), 1.5F);
        }
    }
}
