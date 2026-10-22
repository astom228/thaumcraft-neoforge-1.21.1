package thaumcraft.common.entities;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ConfigEntities;

/**
 * Парящий предмет (порт EntitySpecialItem): подлетает и зависает в воздухе, не боится взрывов.
 * Появляется, когда соль мира превращает блок в предмет (полка → Таумономикон).
 */
public class EntitySpecialItem extends ItemEntity {

    public EntitySpecialItem(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    public EntitySpecialItem(Level level, double x, double y, double z, ItemStack stack) {
        this(ConfigEntities.SPECIAL_ITEM.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.setYRot((float) (Math.random() * 360.0));
        this.setDeltaMovement((float) (Math.random() * 0.2F - 0.1F), 0.2F, (float) (Math.random() * 0.2F - 0.1F));
    }

    /** Подъём гасится, а гравитация ItemEntity компенсируется (+0.04 за тик). */
    @Override
    public void tick() {
        if (this.tickCount > 1) {
            Vec3 m = this.getDeltaMovement();
            double my = m.y;
            if (my > 0.0) {
                my *= 0.9F;
            }
            my += 0.04F;
            this.setDeltaMovement(m.x, my, m.z);
            super.tick();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !source.is(DamageTypeTags.IS_EXPLOSION) && super.hurt(source, amount);
    }
}
