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

/** Эффект «Разлом» (порт FocusEffectRift): временная дыра вглубь блока (глубина · сила), на длительность секунд. */
public class FocusEffectRift extends FocusEffect {
    @Override
    public String getResearch() {
        return "FOCUSRIFT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.RIFT";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ELDRITCH;
    }

    @Override
    public int getComplexity() {
        return 3 + this.getSettingValue("duration") / 2 + this.getSettingValue("depth") / 4;
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        if (!(target instanceof BlockHitResult bh) || target.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        Level world = this.getPackage().world;
        // TODO этап 16: во Внешних землях (ModConfig dimensionOuterId) — отказ со звуком wandfail
        float maxdis = this.getSettingValue("depth") * finalPower;
        int dur = 20 * this.getSettingValue("duration");
        int distance;
        BlockPos pos = bh.getBlockPos();
        for (distance = 0; distance < maxdis; distance++) {
            if (!world.isLoaded(pos)) {
                break;
            }
            BlockState bi = world.getBlockState(pos);
            if (thaumcraft.common.lib.utils.BlockUtils.isPortableHoleBlackListed(bi) || bi.is(Blocks.BEDROCK)
                    || bi.is(thaumcraft.api.blocks.BlocksTC.hole.get()) || bi.isAir() || bi.getDestroySpeed(world, pos) == -1.0F) {
                break;
            }
            pos = pos.relative(bh.getDirection().getOpposite());
        }
        createHole(world, bh.getBlockPos(), bh.getDirection(), (byte) Math.round(distance + 1), dur);
        return true;
    }

    /** Поставить дыру (запомнив блок), если блок не BlockEntity, не в чёрном списке, не бедрок и разрушим. */
    public static boolean createHole(Level world, BlockPos pos, net.minecraft.core.Direction side, byte count, int max) {
        if (world.isClientSide || !world.isLoaded(pos)) {
            return false;
        }
        BlockState bs = world.getBlockState(pos);
        if (world.getBlockEntity(pos) == null && !thaumcraft.common.lib.utils.BlockUtils.isPortableHoleBlackListed(bs) && !bs.is(Blocks.BEDROCK)
                && !bs.is(thaumcraft.api.blocks.BlocksTC.hole.get()) && (bs.isAir() || !bs.canBeReplaced())
                && bs.getDestroySpeed(world, pos) != -1.0F) {
            if (world.setBlock(pos, thaumcraft.api.blocks.BlocksTC.hole.get().defaultBlockState(), 3)
                    && world.getBlockEntity(pos) instanceof thaumcraft.common.tiles.misc.TileHole ts) {
                ts.oldblock = bs;
                ts.countdownmax = (short) max;
                ts.count = count;
                ts.direction = side;
                ts.setChanged();
            }
            return true;
        }
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] depth = {8, 16, 24, 32};
        String[] depthDesc = {"8", "16", "24", "32"};
        return new NodeSetting[] {
                new NodeSetting("depth", "focus.rift.depth", new NodeSetting.NodeSettingIntList(depth, depthDesc)),
                new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(2, 10))
        };
    }

    @Override
    public void renderParticleFX(Level world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        IFXDispatcher.get().focusRiftFX(posX, posY, posZ, motionX, motionY, motionZ);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.2F, 0.7F);
    }
}
