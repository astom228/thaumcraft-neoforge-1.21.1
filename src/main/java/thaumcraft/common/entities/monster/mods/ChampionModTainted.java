package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.config.ConfigEntities;

/**
 * Заражённый (порт ChampionModTainted): существо, почти убитое заражением Выбросом, становится враждебным — поведение
 * мирного заменяется поведением монстра (один раз; отметка в TAINTED_MOD временная, поэтому после загрузки мира замена
 * повторяется — как сброс отметки в entitySpawns оригинала). Поверх модели — слой волокон заражения (LayerTainted,
 * клиент). Заражение Выбросом его лечит (PotionFluxTaint).
 */
public class ChampionModTainted implements IChampionModifierEffect {
    public static final AttributeModifier TAINTED = new AttributeModifier(ChampionModifier.id("is_tainted"), 1.0, AttributeModifier.Operation.ADD_VALUE);

    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss instanceof PathfinderMob critter && !boss.level().isClientSide) {
            resetAI(critter);
        }
        return amount;
    }

    public static void resetAI(PathfinderMob critter) {
        var modai = critter.getAttribute(ConfigEntities.TAINTED_MOD);
        if (!(critter instanceof Monster) && modai != null && modai.getValue() == 0.0) {
            critter.goalSelector.removeAllGoals(g -> true);
            critter.targetSelector.removeAllGoals(g -> true);
            critter.goalSelector.addGoal(0, new FloatGoal(critter));
            critter.goalSelector.addGoal(2, new MeleeAttackGoal(critter, 1.2, false));
            critter.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(critter, 1.0));
            critter.goalSelector.addGoal(7, new RandomStrollGoal(critter, 1.0));
            critter.goalSelector.addGoal(8, new LookAtPlayerGoal(critter, Player.class, 8.0F));
            critter.goalSelector.addGoal(8, new RandomLookAroundGoal(critter));
            if (GoalUtils.hasGroundPathNavigation(critter)) {
                critter.goalSelector.addGoal(6, new MoveThroughVillageGoal(critter, 1.0, false, 4, () -> false));
            }
            critter.targetSelector.addGoal(1, new HurtByTargetGoal(critter, ZombifiedPiglin.class).setAlertOthers());
            critter.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(critter, Player.class, true));
            modai.removeModifier(TAINTED.id());
            modai.addTransientModifier(TAINTED);
        }
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        IChampionModifierEffect.particle(boss, 1.0F, 0.0, -0.01, 0.0, 0.1F + rand.nextFloat() * 0.2F, 0.0F, 0.1F + rand.nextFloat() * 0.1F, 0.25F, false, 1, 5, 1,
                6 + rand.nextInt(6), 2.0F + rand.nextFloat(), 0.5F, 1);
    }
}
