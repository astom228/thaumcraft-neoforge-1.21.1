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

/** Эффект «Огонь» (порт FocusEffectFire): урон огнём (3 + сила) и поджог; по блоку при длительности > 0 — огонь у грани. */
public class FocusEffectFire extends FocusEffect {
    @Override
    public String getResearch() {
        return "BASEAUROMANCY";
    }

    @Override
    public String getKey() {
        return "thaumcraft.FIRE";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.FIRE;
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
            if (hit.fireImmune()) {
                return false;
            }
            float fire = 1 + this.getSettingValue("duration") * this.getSettingValue("duration");
            float damage = this.getDamageForDisplay(finalPower);
            fire *= finalPower;
            hit.hurt(FocusFX.source(world, DamageTypes.FIREBALL, hit, this.getPackage().getCaster()), damage);
            if (fire > 0.0F) {
                hit.igniteForSeconds(Math.round(fire));
            }
            return true;
        }
        if (target instanceof BlockHitResult bh && target.getType() == HitResult.Type.BLOCK && this.getSettingValue("duration") > 0) {
            BlockPos pos = bh.getBlockPos().relative(bh.getDirection());
            if (world.isEmptyBlock(pos) && world.random.nextFloat() < finalPower) {
                world.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, world.random.nextFloat() * 0.4F + 0.8F);
                world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
                return true;
            }
        }
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
                new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("duration", "focus.fire.burn", new NodeSetting.NodeSettingIntRange(0, 5))
        };
    }

    @Override
    public void renderParticleFX(Level world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        GenPart pp = new GenPart();
        pp.grav = -0.2F;
        pp.age = 10;
        pp.alpha = new float[] {0.7F};
        pp.partStart = 640;
        pp.partInc = 1;
        pp.partNum = 10;
        pp.slowDown = 0.75;
        pp.scale = new float[] {(float) (1.5 + world.random.nextGaussian() * 0.2F)};
        IFXDispatcher.get().drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F,
                1.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
    }
}
