package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.lib.utils.Utils;

/**
 * Заражённые блоки (порт BlockTaint): почва, камень, корка, гейзер. Вдали от семени порчи (1/10 на тик) отмирают: камень →
 * пористый камень, почва → земля, корка и гейзер → флюкс-слизь. Камень распространяет волокна; корка, если держится только
 * на порче, падает (вниз или сползает вбок по уступу); гейзер подбавляет Выброс (рой порчи — этап 16). Иногда заражает
 * идущих по ним (не нежить). Звук заражения — событие блока 1.
 */
public class BlockTaint extends Block implements ITaintBlock {
    public enum Kind { SOIL, ROCK, CRUST, GEYSER }

    private final Kind kind;

    public BlockTaint(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    @Override
    public void die(Level world, BlockPos pos, BlockState state) {
        switch (this.kind) {
            case ROCK -> world.setBlockAndUpdate(pos, BlocksTC.stonePorous.get().defaultBlockState());
            case SOIL -> world.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            default -> world.setBlockAndUpdate(pos, BlocksTC.fluxGoo.get().defaultBlockState());
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!TaintHelper.isNearTaintSeed(world, pos) && random.nextInt(10) == 0) {
            this.die(world, pos, state);
            return;
        }
        if (this.kind == Kind.ROCK) {
            TaintHelper.spreadFibres(world, pos);
        }
        if (this.kind == Kind.CRUST) {
            if (this.tryToFall(world, pos, pos)) {
                return;
            }
            if (world.isEmptyBlock(pos.above())) {
                boolean doIt = true;
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                for (int a = 1; a < 4; a++) {
                    if (!world.isEmptyBlock(pos.relative(dir).below(a))) {
                        doIt = false;
                        break;
                    }
                    if (world.getBlockState(pos.below(a)).getBlock() != this) {
                        doIt = false;
                        break;
                    }
                }
                if (doIt && this.tryToFall(world, pos, pos.relative(dir))) {
                    return;
                }
            }
        } else if (this.kind == Kind.GEYSER) {
            if (world.random.nextFloat() < 0.2 && world.hasNearbyAlivePlayer(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 32.0)) {
                // TODO этап 16: рой порчи (EntityTaintSwarm), если рядом его нет
            } else if (AuraHelper.getFlux(world, pos) < 2.0F) {
                AuraHelper.polluteAura(world, pos, 0.25F, true);
            }
        }
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide && entity instanceof LivingEntity le && !le.isInvertedHealAndHarm() && world.random.nextInt(250) == 0) {
            le.addEffect(new MobEffectInstance(ConfigPotions.FLUX_TAINT, 200, 0, false, true));
        }
        super.stepOn(world, pos, state, entity);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level world, BlockPos pos, int id, int param) {
        if (id == 1) {
            if (world.isClientSide) {
                world.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 0.1F, 0.9F + world.random.nextFloat() * 0.2F, false);
            }
            return true;
        }
        return super.triggerEvent(state, world, pos, id, param);
    }

    /** Корка может упасть сквозь клетку: воздух, огонь, волокна, заменяемое, вода, лава, жидкая слизь; не рядом с брёвнами. */
    public static boolean canFallBelow(Level world, BlockPos pos) {
        for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
                for (int yy = -1; yy <= 1; yy++) {
                    if (Utils.isWoodLog(world, pos.offset(xx, yy, zz))) {
                        return false;
                    }
                }
            }
        }
        BlockState bs = world.getBlockState(pos);
        if (bs.isAir()) {
            return true;
        }
        if (bs.getBlock() == BlocksTC.fluxGoo.get() && bs.getValue(BlockFluxGoo.LEVEL) >= 4) {
            return false;
        }
        if (bs.is(Blocks.FIRE) || bs.getBlock() == BlocksTC.taintFibre.get() || bs.canBeReplaced()) {
            return true;
        }
        return bs.getFluidState().is(Fluids.WATER) || bs.getFluidState().is(Fluids.LAVA);
    }

    /** Падение корки: падающий блок (ванильный; в 1.12 — EntityFallingTaint), а если вокруг не загружено — сразу вниз. */
    private boolean tryToFall(ServerLevel world, BlockPos pos, BlockPos pos2) {
        if (!BlockTaintFibre.isOnlyAdjacentToTaint(world, pos)) {
            return false;
        }
        if (canFallBelow(world, pos2.below()) && pos2.getY() >= world.getMinBuildHeight()) {
            if (world.isAreaLoaded(pos2, 32)) {
                BlockState state = world.getBlockState(pos);
                if (!pos2.equals(pos)) {
                    world.removeBlock(pos, false);
                }
                FallingBlockEntity f = FallingBlockEntity.fall(world, pos2, state);
                f.dropItem = false;
                return true;
            }
            world.removeBlock(pos, false);
            BlockPos p2 = pos2;
            while (canFallBelow(world, p2.below()) && p2.getY() > world.getMinBuildHeight()) {
                p2 = p2.below();
            }
            if (p2.getY() > world.getMinBuildHeight()) {
                world.setBlockAndUpdate(p2, BlocksTC.taintCrust.get().defaultBlockState());
            }
        }
        return false;
    }

    /** getDrops оригинала: заражённый камень с шансом (random(15) + удача > 13) роняет кристалл Выброса вместо себя. */
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        if (this.kind == Kind.ROCK) {
            net.minecraft.world.item.ItemStack tool = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
            int fortune = tool == null ? 0 : net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    params.getLevel().registryAccess().holderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), tool);
            if (params.getLevel().random.nextInt(15) + fortune > 13) {
                return java.util.List.of(thaumcraft.api.ThaumcraftApiHelper.makeCrystal(thaumcraft.api.aspects.Aspect.FLUX));
            }
        }
        return super.getDrops(state, params);
    }

    public static boolean isFaceSolid(BlockGetter w, BlockPos p, Direction d) {
        return w.getBlockState(p).isFaceSturdy(w, p, d);
    }
}
