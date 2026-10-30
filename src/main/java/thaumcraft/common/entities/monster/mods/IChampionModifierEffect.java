package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.common.lib.fx.IFXDispatcher;

/** Эффект модификатора чемпиона (порт IChampionModifierEffect). */
public interface IChampionModifierEffect {
    /** Тип 0 — каждый тик (target и source — null), 1 — при ударе чемпиона, 2 — при ударе по чемпиону; возвращает новый урон. */
    float performEffect(LivingEntity mob, LivingEntity target, DamageSource source, float amount);

    /** Частицы вокруг чемпиона (только клиент). */
    void showFX(LivingEntity mob);

    /** preRender оригинала: цвет модели (glColor4f) как 0xRRGGBB; 0xFFFFFF — без изменений. */
    default int getTint() {
        return 0xFFFFFF;
    }

    /** Частица FXGeneric в случайной точке рамки существа (высота — до hFrac от роста). */
    static void particle(LivingEntity boss, float hFrac, double mx, double my, double mz, float r, float g, float b, float alpha, boolean loop, int start,
            int num, int inc, int age, float scale, float rot, int layer) {
        var rand = boss.level().random;
        float w = rand.nextFloat() * boss.getBbWidth();
        float d = rand.nextFloat() * boss.getBbWidth();
        float h = rand.nextFloat() * boss.getBbHeight() * hFrac;
        var bb = boss.getBoundingBox();
        IFXDispatcher.get().drawGenericParticles(bb.minX + w, bb.minY + h, bb.minZ + d, mx, my, mz, r, g, b, alpha, loop, start, num, inc, age, 0, scale, rot, layer);
    }
}
