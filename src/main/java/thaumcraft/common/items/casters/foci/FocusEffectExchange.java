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

/** Эффект «Обмен» (порт FocusEffectExchange): блок заменяется выбранным перчаткой (Shift + ПКМ по образцу). */
public class FocusEffectExchange extends FocusEffect implements thaumcraft.api.casters.IFocusBlockPicker {
    @Override
    public String getResearch() {
        return "FOCUSEXCHANGE";
    }

    @Override
    public String getKey() {
        return "thaumcraft.EXCHANGE";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.EXCHANGE;
    }

    /** Как в оригинале: из-за приоритета операций — (удача + 1) · 3 (DECISIONS 99). */
    @Override
    public int getComplexity() {
        return 5 + this.getSettingValue("silk") * 4 + this.getSettingValue("fortune") == 0 ? 0 : (this.getSettingValue("fortune") + 1) * 3;
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        if (!(target instanceof BlockHitResult bh) || target.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        net.minecraft.world.item.ItemStack casterStack = FocusFX.casterStack(this.getPackage().getCaster());
        if (casterStack.isEmpty() || !(casterStack.getItem() instanceof thaumcraft.common.items.casters.ItemCaster caster)) {
            return false;
        }
        boolean silk = this.getSettingValue("silk") > 0;
        int fortune = this.getSettingValue("fortune");
        net.minecraft.world.item.ItemStack picked = caster.getPickedBlock(casterStack);
        if (this.getPackage().getCaster() instanceof Player player && picked != null && !picked.isEmpty()) {
            Level world = this.getPackage().world;
            BlockState src = world.getBlockState(bh.getBlockPos());
            thaumcraft.common.lib.events.ServerEvents.addSwapper(world, bh.getBlockPos(), st -> st == src, picked, true, 0, player, true, false, 8038177, true,
                    silk, fortune, thaumcraft.common.lib.events.ServerEvents.DEFAULT_PREDICATE, 0.25F + (silk ? 0.25F : 0.0F) + fortune * 0.1F);
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
                new NodeSetting("fortune", "focus.common.fortune", new NodeSetting.NodeSettingIntList(fortune, fortuneDesc)),
                new NodeSetting("silk", "focus.common.silk", new NodeSetting.NodeSettingIntList(silk, silkDesc))
        };
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double vx, double vy, double vz) {
        IFXDispatcher.get().focusExchangeFX(x, y, z, vx, vy, vz);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.2F,
                2.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
    }
}
