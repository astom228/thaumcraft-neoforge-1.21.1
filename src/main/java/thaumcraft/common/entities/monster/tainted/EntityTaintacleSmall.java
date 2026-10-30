package thaumcraft.common.entities.monster.tainted;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Заражённый усик (порт EntityTaintacleSmall): малое щупальце на 10 с (5 здоровья, удар 2), без опыта и добычи. */
public class EntityTaintacleSmall extends EntityTaintacle {
    int lifetime = 200;

    public EntityTaintacleSmall(EntityType<? extends EntityTaintacleSmall> type, Level world) {
        super(type, world);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityTaintacle.createAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.lifetime-- <= 0) {
            this.hurt(this.damageSources().magic(), 10.0F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
    }
}
