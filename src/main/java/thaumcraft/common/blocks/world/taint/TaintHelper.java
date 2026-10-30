package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.world.aura.AuraHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Распространение порчи (порт TaintHelper). Семена порчи регистрируют себя в списке своего измерения (только сервер);
 * волокна, заражённые блоки и семена разрастаются лишь в радиусе taintSpreadArea от живого семени.
 */
public class TaintHelper {
    /** Только сервер: позиции семян по измерениям. */
    private static final Map<ResourceKey<Level>, List<BlockPos>> taintSeeds = new ConcurrentHashMap<>();

    public static void addTaintSeed(Level world, BlockPos pos) {
        taintSeeds.computeIfAbsent(world.dimension(), k -> new CopyOnWriteArrayList<>()).add(pos);
    }

    public static void removeTaintSeed(Level world, BlockPos pos) {
        List<BlockPos> locs = taintSeeds.get(world.dimension());
        if (locs != null) {
            locs.remove(pos);
        }
    }

    public static void clear(Level world) {
        taintSeeds.remove(world.dimension());
    }

    /** MATERIAL_TAINT оригинала: блоки порчи и флюкс-слизь. */
    public static boolean isTaint(BlockState state) {
        return state.getBlock() instanceof ITaintBlock || state.getBlock() == BlocksTC.fluxGoo.get();
    }

    public static boolean isNearTaintSeed(Level world, BlockPos pos) {
        double area = ModConfig.CONFIG_WORLD.taintSpreadArea.get() * ModConfig.CONFIG_WORLD.taintSpreadArea.get();
        List<BlockPos> locs = taintSeeds.get(world.dimension());
        if (locs != null) {
            for (BlockPos p : locs) {
                if (p.distSqr(pos) <= area) {
                    if (!world.isLoaded(p)) {
                        return true;
                    }
                    if (world.getEntitiesOfClass(EntityTaintSeed.class, new AABB(p).inflate(1.0)).isEmpty()) {
                        removeTaintSeed(world, p);
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAtTaintSeedEdge(Level world, BlockPos pos) {
        int a = ModConfig.CONFIG_WORLD.taintSpreadArea.get();
        double area = a * a;
        double fringe = a * 0.8 * (a * 0.8);
        List<BlockPos> locs = taintSeeds.get(world.dimension());
        if (locs != null) {
            for (BlockPos p : locs) {
                double d = p.distSqr(pos);
                if (d < area && d > fringe) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void spreadFibres(Level world, BlockPos pos) {
        spreadFibres(world, pos, false);
    }

    /**
     * Шаг распространения: случайная клетка 3×3×3 рядом. Пустое/заменяемое/растение у твёрдой опоры → волокна; листва → нарост
     * (у заражённого бревна) или волокна; окружённое порчей: бревно → заражённое, дерево/тыквы/кактусы/грибы → корка, земля/
     * песок/глина → заражённая почва, камень → заражённый камень. На краю области при Выбросе ≥ 5 — новое семя.
     */
    public static void spreadFibres(Level world, BlockPos pos, boolean ignore) {
        if (!ignore && ModConfig.CONFIG_MISC.wussMode.get()) {
            return;
        }
        float mod = 0.001F + AuraHandler.getFluxSaturation(world, pos) * 2.0F;
        double rate = ModConfig.CONFIG_WORLD.taintSpreadRate.get();
        if (!ignore && world.random.nextFloat() > rate / 100.0F * mod) {
            return;
        }
        if (!isNearTaintSeed(world, pos)) {
            return;
        }
        BlockPos t = pos.offset(world.random.nextInt(3) - 1, world.random.nextInt(3) - 1, world.random.nextInt(3) - 1);
        if (t.equals(pos) || !world.isLoaded(t)) {
            return;
        }
        BlockState bs = world.getBlockState(t);
        float bh = bs.getDestroySpeed(world, t);
        if (bh < 0.0F || bh > 10.0F) {
            return;
        }
        boolean leaves = bs.is(BlockTags.LEAVES);
        if (!leaves && bs.getFluidState().isEmpty()
                && (bs.isAir() || bs.canBeReplaced() || bs.getBlock() instanceof BushBlock)
                && isAdjacentToSolidBlock(world, t) && !BlockTaintFibre.isOnlyAdjacentToTaint(world, t)) {
            setTaint(world, t, BlocksTC.taintFibre.get().defaultBlockState());
            return;
        }
        if (leaves) {
            Direction face = null;
            if (world.random.nextFloat() < 0.6 && (face = getFaceBlockTouching(world, t, BlocksTC.taintLog.get())) != null) {
                world.setBlockAndUpdate(t, BlocksTC.taintFeature.get().defaultBlockState().setValue(BlockTaintFeature.FACING, face.getOpposite()));
            } else {
                setTaint(world, t, BlocksTC.taintFibre.get().defaultBlockState());
            }
            return;
        }
        if (BlockTaintFibre.isHemmedByTaint(world, t) && bh < 5.0F) {
            if (Utils.isWoodLog(world, t) && !isTaint(bs)) {
                Direction.Axis axis = bs.hasProperty(RotatedPillarBlock.AXIS) ? bs.getValue(RotatedPillarBlock.AXIS) : Direction.Axis.Y;
                world.setBlockAndUpdate(t, BlocksTC.taintLog.get().defaultBlockState().setValue(BlockTaintLog.AXIS, axis));
                return;
            }
            if (isCrustMaterial(bs)) {
                setTaint(world, t, BlocksTC.taintCrust.get().defaultBlockState());
                return;
            }
            if (isSoilMaterial(bs)) {
                setTaint(world, t, BlocksTC.taintSoil.get().defaultBlockState());
                return;
            }
            if (bs.instrument() == NoteBlockInstrument.BASEDRUM && !isTaint(bs)) {
                setTaint(world, t, BlocksTC.taintRock.get().defaultBlockState());
                return;
            }
        }
        if ((bs.getBlock() == BlocksTC.taintSoil.get() || bs.getBlock() == BlocksTC.taintRock.get()) && world.isEmptyBlock(t.above())
                && AuraHelper.getFlux(world, t) >= 5.0F && world.random.nextFloat() < rate / 100.0F * 0.33 && isAtTaintSeedEdge(world, t)) {
            EntityTaintSeed e = ConfigEntities.TAINT_SEED.get().create(world);
            if (e != null) {
                e.moveTo(t.getX() + 0.5F, t.above().getY(), t.getZ() + 0.5F, world.random.nextInt(360), 0.0F);
                if (e.canSpawnHere()) {
                    AuraHelper.drainFlux(world, t, 5.0F, false);
                    world.addFreshEntity(e);
                }
            }
        }
    }

    private static void setTaint(Level world, BlockPos t, BlockState state) {
        world.setBlockAndUpdate(t, state);
        world.blockEvent(t, state.getBlock(), 1, 0);
        AuraHelper.drainFlux(world, t, 0.01F, false);
    }

    /** Материалы 1.12 WOOD, GOURD, CACTUS, CORAL, SPONGE и грибные блоки → корка порчи (WOOD 1.21 — инструмент BASS). */
    private static boolean isCrustMaterial(BlockState bs) {
        if (isTaint(bs)) {
            return false;
        }
        return bs.is(Blocks.RED_MUSHROOM_BLOCK) || bs.is(Blocks.BROWN_MUSHROOM_BLOCK) || bs.is(Blocks.MUSHROOM_STEM) || bs.is(Blocks.PUMPKIN)
                || bs.is(Blocks.CARVED_PUMPKIN) || bs.is(Blocks.JACK_O_LANTERN) || bs.is(Blocks.MELON) || bs.is(Blocks.CACTUS)
                || bs.is(BlockTags.CORAL_BLOCKS) || bs.is(Blocks.SPONGE) || bs.is(Blocks.WET_SPONGE) || bs.instrument() == NoteBlockInstrument.BASS;
    }

    /** Материалы 1.12 SAND, GROUND, GRASS, CLAY → заражённая почва. */
    private static boolean isSoilMaterial(BlockState bs) {
        return bs.is(BlockTags.SAND) || bs.is(BlockTags.DIRT) || bs.is(Blocks.GRAVEL) || bs.is(Blocks.CLAY) || bs.is(Blocks.FARMLAND)
                || bs.is(Blocks.SOUL_SAND) || bs.is(Blocks.DIRT_PATH);
    }

    /** BlockUtils.isAdjacentToSolidBlock: у какой-то из 6 сторон — твёрдая к этой клетке грань. */
    public static boolean isAdjacentToSolidBlock(BlockGetter world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos p = pos.relative(d);
            if (world.getBlockState(p).isFaceSturdy(world, p, d.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /** BlockUtils.getFaceBlockTouching: сторона, с которой касается блок block (или null). */
    public static Direction getFaceBlockTouching(BlockGetter world, BlockPos pos, Block block) {
        for (Direction d : Direction.values()) {
            if (world.getBlockState(pos.relative(d)).getBlock() == block) {
                return d;
            }
        }
        return null;
    }
}
