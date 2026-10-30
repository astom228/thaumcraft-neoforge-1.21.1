package thaumcraft.common.world.objects;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

/**
 * Запись деревьев (замена setBlockAndNotifyAdequately WorldGenAbstractTree). При генерации мира пишет только в 3×3 чанка
 * вокруг текущего (за границей — как препятствие); у саженца ограничений нет. После постройки выставляет листве
 * расстояние до бревна (в 1.21 листва без него осыпается), как TreeFeature.
 */
public class TreeWriter {
    private final WorldGenLevel level;
    private final ChunkPos center;
    private final int flags;
    private final Set<BlockPos> logs = new HashSet<>();
    private final Set<BlockPos> leaves = new HashSet<>();

    /** center — текущий чанк генерации или null (рост саженца, обычный мир). */
    public TreeWriter(WorldGenLevel level, ChunkPos center) {
        this.level = level;
        this.center = center;
        this.flags = center == null ? 3 : 2;
    }

    public WorldGenLevel level() {
        return this.level;
    }

    public boolean canAccess(BlockPos p) {
        if (this.level.isOutsideBuildHeight(p)) {
            return false;
        }
        if (this.center == null) {
            // рост саженца: только загруженные чанки (без подгрузки ради расчётов)
            return this.level.hasChunkAt(p);
        }
        return Math.abs((p.getX() >> 4) - this.center.x) <= 1 && Math.abs((p.getZ() >> 4) - this.center.z) <= 1;
    }

    /** Состояние блока; недоступный блок считается камнем (препятствием). */
    public BlockState get(BlockPos p) {
        return this.canAccess(p) ? this.level.getBlockState(p) : Blocks.STONE.defaultBlockState();
    }

    public boolean isAir(BlockPos p) {
        return this.canAccess(p) && this.level.isEmptyBlock(p);
    }

    /** canBeReplacedByLeaves: воздух, листва, заменяемые деревом растения. */
    public boolean leafReplaceable(BlockPos p) {
        return this.canAccess(p) && TreeFeature.validTreePos(this.level, p);
    }

    /** WorldGenAbstractTree.canGrowInto: воздух, листва, трава/земля, брёвна, саженцы, лианы. */
    public boolean logReplaceable(BlockPos p) {
        if (!this.canAccess(p)) {
            return false;
        }
        BlockState s = this.level.getBlockState(p);
        return TreeFeature.validTreePos(this.level, p) || s.is(BlockTags.LOGS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.DIRT);
    }

    /** Почва для дерева (canSustainPlant саженца). */
    public boolean isSoil(BlockPos p) {
        return this.canAccess(p) && this.level.getBlockState(p).is(BlockTags.DIRT);
    }

    public void set(BlockPos p, BlockState state) {
        if (!this.canAccess(p)) {
            return;
        }
        BlockPos ip = p.immutable();
        this.level.setBlock(ip, state, this.flags);
        if (state.getBlock() instanceof LeavesBlock) {
            this.leaves.add(ip);
            this.logs.remove(ip);
        } else {
            this.leaves.remove(ip);
            if (state.is(BlockTags.LOGS)) {
                this.logs.add(ip);
            } else {
                this.logs.remove(ip);
            }
        }
    }

    /** onPlantGrow: трава под стволом становится землёй. */
    public void soilUnder(BlockPos p) {
        if (this.canAccess(p) && this.level.getBlockState(p).is(Blocks.GRASS_BLOCK)) {
            this.level.setBlock(p, Blocks.DIRT.defaultBlockState(), this.flags);
        }
    }

    /** Расстояние листвы до ближайшего бревна (поиск в ширину до 6); дальше — 7 (осыпется, как у ванильных деревьев). */
    public void finish() {
        Map<BlockPos, Integer> dist = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : this.logs) {
            for (Direction d : Direction.values()) {
                BlockPos n = log.relative(d);
                if (this.leaves.contains(n) && !dist.containsKey(n)) {
                    dist.put(n, 1);
                    queue.add(n);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int dp = dist.get(p);
            if (dp >= 6) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (this.leaves.contains(n) && !dist.containsKey(n)) {
                    dist.put(n, dp + 1);
                    queue.add(n);
                }
            }
        }
        for (BlockPos p : this.leaves) {
            BlockState s = this.level.getBlockState(p);
            if (s.hasProperty(LeavesBlock.DISTANCE)) {
                this.level.setBlock(p, s.setValue(LeavesBlock.DISTANCE, dist.getOrDefault(p, 7)), this.flags);
            }
        }
    }
}
