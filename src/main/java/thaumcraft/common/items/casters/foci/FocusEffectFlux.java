package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.lib.fx.GenPart;
import thaumcraft.common.lib.fx.IFXDispatcher;

/** Эффект «Выброс» (порт FocusEffectFlux): урон магией (3 + сила). */
public class FocusEffectFlux extends FocusEffect {
    @Override
    public String getResearch() {
        return "FOCUSFLUX";
    }

    @Override
    public String getKey() {
        return "thaumcraft.FLUX";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.FLUX;
    }

    @Override
    public int getComplexity() {
        return this.getSettingValue("power") * 3;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return (3 + this.getSettingValue("power")) * finalPower;
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = this.getPackage().world;
        FocusFX.impact(world, target.getLocation(), this.getKey());
        if (target instanceof EntityHitResult eh && eh.getEntity() != null) {
            Entity hit = eh.getEntity();
            hit.hurt(FocusFX.source(world, DamageTypes.INDIRECT_MAGIC, hit, this.getPackage().getCaster()), this.getDamageForDisplay(finalPower));
        }
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 2.0F,
                2.0F + (float) (caster.level().random.nextGaussian() * 0.1F));
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double vx, double vy, double vz) {
        IFXDispatcher.get().focusFluxFX(x, y, z, vx, vy, vz);
    }
}
