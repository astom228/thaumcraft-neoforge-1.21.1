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

/** Эффект «Мороз» (порт FocusEffectFrost): урон (3 + сила) и замедление; по блоку — вода в радиусе 2·сила превращается в лёд. */
public class FocusEffectFrost extends FocusEffect {
    @Override
    public String getResearch() {
        return "FOCUSELEMENTAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.FROST";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.COLD;
    }

    @Override
    public int getComplexity() {
        return this.getSettingValue("duration") + this.getSettingValue("power") * 2;
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
            float damage = this.getDamageForDisplay(finalPower);
            int duration = 20 * this.getSettingValue("duration");
            int potency = (int) (1.0F + this.getSettingValue("power") * finalPower / 3.0F);
            hit.hurt(FocusFX.source(world, DamageTypes.THROWN, hit, this.getPackage().getCaster()), damage);
            if (hit instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, potency));
            }
        } else if (target instanceof BlockHitResult bh && target.getType() == HitResult.Type.BLOCK) {
            float f = Math.min(16.0F, 2 * this.getSettingValue("power") * finalPower);
            int fi = (int) f;
            BlockPos c = bh.getBlockPos();
            BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-fi, -fi, -fi), c.offset(fi, fi, fi))) {
                if (p.distToCenterSqr(target.getLocation().x, target.getLocation().y, target.getLocation().z) <= f * f && world.isLoaded(p)) {
                    BlockState s = world.getBlockState(p);
                    if (s.is(Blocks.WATER) && s.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) == 0 && ice.canSurvive(world, p)
                            && world.isUnobstructed(ice, p, net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                        world.setBlockAndUpdate(p, ice);
                        world.scheduleTick(p.immutable(), Blocks.FROSTED_ICE, Mth.nextInt(world.random, 60, 120));
                    }
                }
            }
        }
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
                new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(2, 10))
        };
    }

    @Override
    public void renderParticleFX(Level world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        IFXDispatcher.get().focusFrostFX(posX, posY, posZ, motionX, motionY, motionZ);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.2F,
                1.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
    }
}
