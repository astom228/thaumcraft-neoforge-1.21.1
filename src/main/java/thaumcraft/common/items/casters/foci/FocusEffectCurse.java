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

/** Эффект «Проклятие» (порт FocusEffectCurse): урон магией, яд и с убывающим шансом другие недуги; по блоку — гниль вокруг. */
public class FocusEffectCurse extends FocusEffect {
    @Override
    public String getResearch() {
        return "FOCUSCURSE";
    }

    @Override
    public String getKey() {
        return "thaumcraft.CURSE";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.DEATH;
    }

    @Override
    public int getComplexity() {
        return this.getSettingValue("duration") + this.getSettingValue("power") * 3;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return (1.0F + this.getSettingValue("power")) * finalPower;
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = this.getPackage().world;
        var loc = target.getLocation();
        if (world instanceof net.minecraft.server.level.ServerLevel sl) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(sl, null, loc.x, loc.y, loc.z, 64.0,
                    new thaumcraft.common.lib.network.fx.PacketFXBlockBamf(loc.x, loc.y, loc.z, 6946821, true, true, null));
        }
        if (target instanceof EntityHitResult eh && eh.getEntity() != null) {
            Entity hit = eh.getEntity();
            float damage = this.getDamageForDisplay(finalPower);
            int duration = 20 * this.getSettingValue("duration");
            int eff = Math.max(0, (int) (this.getSettingValue("power") * finalPower / 2.0F));
            hit.hurt(FocusFX.source(world, DamageTypes.INDIRECT_MAGIC, hit, this.getPackage().getCaster()), damage);
            if (hit instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.POISON, duration, eff));
                float c = 0.85F;
                if (world.random.nextFloat() < c) {
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, eff));
                    c -= 0.15F;
                }
                if (world.random.nextFloat() < c) {
                    le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, eff));
                    c -= 0.15F;
                }
                if (world.random.nextFloat() < c) {
                    le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration * 2, eff));
                    c -= 0.15F;
                }
                if (world.random.nextFloat() < c) {
                    le.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration * 3, eff));
                    c -= 0.15F;
                }
                if (world.random.nextFloat() < c) {
                    le.addEffect(new MobEffectInstance(MobEffects.UNLUCK, duration * 3, eff));
                }
            }
        } else if (target instanceof BlockHitResult bh && target.getType() == HitResult.Type.BLOCK) {
            float f = (float) Math.min(8.0, 1.5 * this.getSettingValue("power") * finalPower);
            int fi = (int) f;
            BlockPos c = bh.getBlockPos();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-fi, -fi, -fi), c.offset(fi, fi, fi))) {
                if (p.distToCenterSqr(loc.x, loc.y, loc.z) <= f * f && world.isLoaded(p) && world.isEmptyBlock(p.above())
                        && world.getBlockState(p).isCollisionShapeFullBlock(world, p)) {
                    world.setBlockAndUpdate(p.above(), thaumcraft.api.blocks.BlocksTC.effectSap.get().defaultBlockState());
                }
            }
        }
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
                new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(1, 10))
        };
    }

    @Override
    public void renderParticleFX(Level world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        IFXDispatcher.get().focusCurseFX(posX, posY, posZ, motionX, motionY, motionZ);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.15F,
                1.0F + caster.level().random.nextFloat() / 2.0F);
    }
}
