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

/** Эффект «Разрушение» (порт FocusEffectBreak): блок постепенно ломается за игрока (сила, удача, шёлковое касание); плата — Вис ауры. */
public class FocusEffectBreak extends FocusEffect {
    @Override
    public String getResearch() {
        return "FOCUSBREAK";
    }

    @Override
    public String getKey() {
        return "thaumcraft.BREAK";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ENTROPY;
    }

    @Override
    public int getComplexity() {
        return this.getSettingValue("power") * 3 + this.getSettingValue("silk") * 4
                + (this.getSettingValue("fortune") == 0 ? 0 : (this.getSettingValue("fortune") + 1) * 3);
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        if (!(target instanceof BlockHitResult bh) || target.getType() != HitResult.Type.BLOCK) {
            return true;
        }
        Level world = this.getPackage().world;
        BlockPos pos = bh.getBlockPos();
        FocusFX.impact(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.getKey());
        boolean silk = this.getSettingValue("silk") > 0;
        int fortune = this.getSettingValue("fortune");
        float strength = this.getSettingValue("power") * finalPower;
        float dur = world.getBlockState(pos).getDestroySpeed(world, pos) * 100.0F;
        dur = (float) Math.sqrt(dur);
        if (this.getPackage().getCaster() instanceof Player player) {
            thaumcraft.common.lib.events.ServerEvents.addBreaker(world, pos, world.getBlockState(pos), player, true, silk, fortune, strength, dur, dur,
                    (int) (dur / strength / 3.0F * num), 0.25F + (silk ? 0.25F : 0.0F) + fortune * 0.1F, null);
        }
        return true;
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] silk = {0, 1};
        String[] silkDesc = {"focus.common.no", "focus.common.yes"};
        int[] fortune = {0, 1, 2, 3, 4};
        String[] fortuneDesc = {"focus.common.no", "I", "II", "III", "IV"};
        return new NodeSetting[] {
                new NodeSetting("power", "focus.break.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("fortune", "focus.common.fortune", new NodeSetting.NodeSettingIntList(fortune, fortuneDesc)),
                new NodeSetting("silk", "focus.common.silk", new NodeSetting.NodeSettingIntList(silk, silkDesc))
        };
    }

    @Override
    public void renderParticleFX(Level world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        IFXDispatcher.get().focusBreakFX(posX, posY, posZ, motionX, motionY, motionZ);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.END_GATEWAY_SPAWN, SoundSource.PLAYERS, 0.1F,
                2.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
    }
}
