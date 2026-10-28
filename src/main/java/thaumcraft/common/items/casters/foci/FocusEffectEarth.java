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

/** Эффект «Земля» (порт FocusEffectEarth): урон 2·сила; мягкий блок (прочность ≤ урон / 25) ломается за игрока. */
public class FocusEffectEarth extends FocusEffect {
    @Override
    public String getResearch() {
        return "FOCUSELEMENTAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.EARTH";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.EARTH;
    }

    @Override
    public int getComplexity() {
        return this.getSettingValue("power") * 3;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return 2 * this.getSettingValue("power") * finalPower;
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = this.getPackage().world;
        FocusFX.impact(world, target.getLocation(), this.getKey());
        if (target instanceof EntityHitResult eh && eh.getEntity() != null) {
            Entity hit = eh.getEntity();
            hit.hurt(FocusFX.source(world, DamageTypes.THROWN, hit, this.getPackage().getCaster()), this.getDamageForDisplay(finalPower));
            return true;
        }
        if (target instanceof BlockHitResult bh && target.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bh.getBlockPos();
            if (this.getPackage().getCaster() instanceof Player player
                    && world.getBlockState(pos).getDestroySpeed(world, pos) <= this.getDamageForDisplay(finalPower) / 25.0F) {
                thaumcraft.common.lib.events.ServerEvents.addBreaker(world, pos, world.getBlockState(pos), player, false, false, 0, 1.0F, 0.0F, 1.0F, num, 0.1F,
                        null);
            }
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
        pp.grav = 0.4F;
        pp.layer = 1;
        pp.age = 20 + world.random.nextInt(10);
        pp.alpha = new float[] {1.0F, 0.0F};
        pp.partStart = 75 + world.random.nextInt(4);
        pp.partInc = 1;
        pp.partNum = 1;
        pp.slowDown = 0.9;
        pp.rot = (float) world.random.nextGaussian();
        float s = (float) (1.0 + world.random.nextGaussian() * 0.2F);
        pp.scale = new float[] {s, s / 2.0F};
        IFXDispatcher.get().drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.25F,
                1.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
    }
}
