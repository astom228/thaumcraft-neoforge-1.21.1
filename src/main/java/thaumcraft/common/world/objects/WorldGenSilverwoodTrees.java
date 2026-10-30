package thaumcraft.common.world.objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.world.features.FeatureVegetation;

/**
 * Серебродрево (порт WorldGenSilverwoodTrees): толстый ствол-крест высотой min+rand, шар листвы у вершины, корни
 * и «рёбра» внизу и на 4 блока ниже вершины. При генерации мира вокруг сажает мерцающий лист.
 */
public class WorldGenSilverwoodTrees {
    private final int minTreeHeight;
    private final int randomTreeHeight;
    private final boolean worldgen;

    public WorldGenSilverwoodTrees(boolean doBlockNotify, int minTreeHeight, int randomTreeHeight) {
        this.worldgen = !doBlockNotify;
        this.minTreeHeight = minTreeHeight;
        this.randomTreeHeight = randomTreeHeight;
    }

    private static BlockState log(Direction.Axis axis) {
        return BlocksTC.logSilverwood.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    public boolean generate(TreeWriter w, RandomSource random, BlockPos pos) {
        int height = random.nextInt(this.randomTreeHeight) + this.minTreeHeight;
        boolean flag = true;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int minY = w.level().getMinBuildHeight();
        int maxY = w.level().getMaxBuildHeight();
        if (y < minY + 1 || y + height + 1 > maxY) {
            return false;
        }
        for (int i1 = y; i1 <= y + 1 + height; i1++) {
            int spread = 1;
            if (i1 == y) {
                spread = 0;
            }
            if (i1 >= y + 1 + height - 2) {
                spread = 3;
            }
            for (int j1 = x - spread; j1 <= x + spread && flag; j1++) {
                for (int k1 = z - spread; k1 <= z + spread && flag; k1++) {
                    BlockPos p = new BlockPos(j1, i1, k1);
                    if (!w.leafReplaceable(p) && i1 > y) {
                        flag = false;
                    }
                }
            }
        }
        if (!flag) {
            return false;
        }
        BlockPos below = new BlockPos(x, y - 1, z);
        if (!w.isSoil(below) || y >= maxY - height - 1) {
            return false;
        }
        w.soilUnder(below);
        BlockState leaf = BlocksTC.leafSilverwood.get().defaultBlockState();
        int start = y + height - 5;
        int end = y + height + 3 + random.nextInt(3);
        for (int k2 = start; k2 <= end; k2++) {
            int cty = Mth.clamp(k2, y + height - 3, y + height);
            for (int xx = x - 5; xx <= x + 5; xx++) {
                for (int zz = z - 5; zz <= z + 5; zz++) {
                    double d3 = xx - x;
                    double d4 = k2 - cty;
                    double d5 = zz - z;
                    double dist = d3 * d3 + d4 * d4 + d5 * d5;
                    BlockPos p = new BlockPos(xx, k2, zz);
                    if (dist < 10 + random.nextInt(8) && w.leafReplaceable(p)) {
                        w.set(p, leaf);
                    }
                }
            }
        }
        BlockState vy = log(Direction.Axis.Y);
        int var33;
        for (var33 = 0; var33 < height; var33++) {
            BlockPos p = new BlockPos(x, y + var33, z);
            if (w.leafReplaceable(p)) {
                w.set(p, vy);
                w.set(p.west(), vy);
                w.set(p.east(), vy);
                w.set(p.north(), vy);
                w.set(p.south(), vy);
            }
        }
        w.set(new BlockPos(x, y + var33, z), vy);
        w.set(new BlockPos(x - 1, y, z - 1), vy);
        w.set(new BlockPos(x + 1, y, z + 1), vy);
        w.set(new BlockPos(x - 1, y, z + 1), vy);
        w.set(new BlockPos(x + 1, y, z - 1), vy);
        if (random.nextInt(3) != 0) {
            w.set(new BlockPos(x - 1, y + 1, z - 1), vy);
        }
        if (random.nextInt(3) != 0) {
            w.set(new BlockPos(x + 1, y + 1, z + 1), vy);
        }
        if (random.nextInt(3) != 0) {
            w.set(new BlockPos(x - 1, y + 1, z + 1), vy);
        }
        if (random.nextInt(3) != 0) {
            w.set(new BlockPos(x + 1, y + 1, z - 1), vy);
        }
        w.set(new BlockPos(x - 2, y, z), log(Direction.Axis.X));
        w.set(new BlockPos(x + 2, y, z), log(Direction.Axis.X));
        w.set(new BlockPos(x, y, z - 2), log(Direction.Axis.Z));
        w.set(new BlockPos(x, y, z + 2), log(Direction.Axis.Z));
        w.set(new BlockPos(x - 2, y - 1, z), vy);
        w.set(new BlockPos(x + 2, y - 1, z), vy);
        w.set(new BlockPos(x, y - 1, z - 2), vy);
        w.set(new BlockPos(x, y - 1, z + 2), vy);
        w.set(new BlockPos(x - 1, y + (height - 4), z - 1), vy);
        w.set(new BlockPos(x + 1, y + (height - 4), z + 1), vy);
        w.set(new BlockPos(x - 1, y + (height - 4), z + 1), vy);
        w.set(new BlockPos(x + 1, y + (height - 4), z - 1), vy);
        if (random.nextInt(3) == 0) {
            w.set(new BlockPos(x - 1, y + (height - 5), z - 1), vy);
        }
        if (random.nextInt(3) == 0) {
            w.set(new BlockPos(x + 1, y + (height - 5), z + 1), vy);
        }
        if (random.nextInt(3) == 0) {
            w.set(new BlockPos(x - 1, y + (height - 5), z + 1), vy);
        }
        if (random.nextInt(3) == 0) {
            w.set(new BlockPos(x + 1, y + (height - 5), z - 1), vy);
        }
        w.set(new BlockPos(x - 2, y + (height - 4), z), log(Direction.Axis.X));
        w.set(new BlockPos(x + 2, y + (height - 4), z), log(Direction.Axis.X));
        w.set(new BlockPos(x, y + (height - 4), z - 2), log(Direction.Axis.Z));
        w.set(new BlockPos(x, y + (height - 4), z + 2), log(Direction.Axis.Z));
        w.finish();
        if (this.worldgen) {
            FeatureVegetation.generateFlowers(w, random, new BlockPos(x, y, z), BlocksTC.shimmerleaf.get().defaultBlockState());
        }
        return true;
    }
}
