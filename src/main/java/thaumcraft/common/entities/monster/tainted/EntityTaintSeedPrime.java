package thaumcraft.common.entities.monster.tainted;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;

/** Главное семя порчи (порт EntityTaintSeedPrime): больше (2×2), область ×2, 150 здоровья, удар 7, до 3 кристаллов Выброса. */
public class EntityTaintSeedPrime extends EntityTaintSeed {
    public EntityTaintSeedPrime(EntityType<? extends EntityTaintSeedPrime> type, Level world) {
        super(type, world);
        this.xpReward = 12;
    }

    @Override
    protected int getArea() {
        return 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 150.0).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(Aspect.FLUX), this.getBbHeight() / 2.0F);
        if (this.random.nextBoolean()) {
            this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(Aspect.FLUX), this.getBbHeight() / 2.0F);
        }
        if (this.random.nextBoolean()) {
            this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(Aspect.FLUX), this.getBbHeight() / 2.0F);
        }
    }
}
