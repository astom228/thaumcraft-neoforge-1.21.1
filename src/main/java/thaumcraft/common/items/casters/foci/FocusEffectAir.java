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

/** Эффект «Воздух» (порт FocusEffectAir): урон (1 + сила) и отбрасывание по траектории. */
public class FocusEffectAir extends FocusEffect {
    @Override
    public String getResearch() {
        return "FOCUSELEMENTAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.AIR";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.AIR;
    }

    @Override
    public int getComplexity() {
        return this.getSettingValue("power") * 2;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return (1 + this.getSettingValue("power")) * finalPower;
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = this.getPackage().world;
        FocusFX.impact(world, target.getLocation(), this.getKey());
        world.playSound(null, target.getLocation().x, target.getLocation().y, target.getLocation().z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS,
                0.5F, 0.66F);
        if (target instanceof EntityHitResult eh && eh.getEntity() != null) {
            Entity hit = eh.getEntity();
            float damage = this.getDamageForDisplay(finalPower);
            hit.hurt(FocusFX.source(world, DamageTypes.THROWN, hit, this.getPackage().getCaster()), damage);
            if (hit instanceof LivingEntity le) {
                if (trajectory != null) {
                    le.knockback(damage * 0.25F, -trajectory.direction.x, -trajectory.direction.z);
                } else {
                    le.knockback(damage * 0.25F, -Mth.sin(hit.getYRot() * (float) (Math.PI / 180.0)), Mth.cos(hit.getYRot() * (float) (Math.PI / 180.0)));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
    }

    @Override
    public void renderParticleFX(Level world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        GenPart pp = new GenPart();
        pp.grav = -0.1F;
        pp.age = 20 + world.random.nextInt(10);
        pp.alpha = new float[] {0.5F, 0.0F};
        pp.grid = 32;
        pp.partStart = 337;
        pp.partInc = 1;
        pp.partNum = 5;
        pp.slowDown = 0.75;
        pp.rot = (float) world.random.nextGaussian() / 2.0F;
        float s = (float) (2.0 + world.random.nextGaussian() * 0.5);
        pp.scale = new float[] {s, s * 2.0F};
        IFXDispatcher.get().drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), thaumcraft.common.lib.SoundsTC.wind.get(), SoundSource.PLAYERS, 0.125F, 2.0F);
    }
}
