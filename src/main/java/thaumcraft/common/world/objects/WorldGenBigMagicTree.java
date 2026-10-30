package thaumcraft.common.world.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Большое магическое дерево Магического леса (порт WorldGenBigMagicTree): ванильный «большой дуб» 1.12 из дуба и дубовой
 * листвы с другими пропорциями (высота 11–21, шире крона, ниже ствол).
 */
public class WorldGenBigMagicTree {
    private Random rand;
    private TreeWriter world;
    private BlockPos basePos = BlockPos.ZERO;
    int heightLimit;
    int height;
    double heightAttenuation = 0.6618;
    double branchSlope = 0.381;
    double scaleWidth = 1.25;
    double leafDensity = 0.9;
    int trunkSize = 1;
    int heightLimitLimit = 11;
    int leafDistanceLimit = 4;
    List<FoliageCoordinates> foliageCoords;

    private record FoliageCoordinates(BlockPos pos, int branchBase) {
    }

    private static BlockPos addF(BlockPos p, double x, double y, double z) {
        return BlockPos.containing(p.getX() + x, p.getY() + y, p.getZ() + z);
    }

    void generateLeafNodeList() {
        this.height = (int) (this.heightLimit * this.heightAttenuation);
        if (this.height >= this.heightLimit) {
            this.height = this.heightLimit - 1;
        }
        int i = (int) (1.382 + Math.pow(this.leafDensity * this.heightLimit / 13.0, 2.0));
        if (i < 1) {
            i = 1;
        }
        int j = this.basePos.getY() + this.height;
        int k = this.heightLimit - this.leafDistanceLimit;
        this.foliageCoords = new ArrayList<>();
        this.foliageCoords.add(new FoliageCoordinates(this.basePos.above(k), j));
        for (; k >= 0; k--) {
            float f = this.layerSize(k);
            if (f >= 0.0F) {
                for (int l = 0; l < i; l++) {
                    double d0 = this.scaleWidth * f * (this.rand.nextFloat() + 0.328);
                    double d1 = this.rand.nextFloat() * 2.0F * Math.PI;
                    double d2 = d0 * Math.sin(d1) + 0.5;
                    double d3 = d0 * Math.cos(d1) + 0.5;
                    BlockPos blockpos = addF(this.basePos, d2, k - 1, d3);
                    BlockPos blockpos1 = blockpos.above(this.leafDistanceLimit);
                    if (this.checkBlockLine(blockpos, blockpos1) == -1) {
                        int i1 = this.basePos.getX() - blockpos.getX();
                        int j1 = this.basePos.getZ() - blockpos.getZ();
                        double d4 = blockpos.getY() - Math.sqrt(i1 * i1 + j1 * j1) * this.branchSlope;
                        int k1 = d4 > j ? j : (int) d4;
                        BlockPos blockpos2 = new BlockPos(this.basePos.getX(), k1, this.basePos.getZ());
                        if (this.checkBlockLine(blockpos2, blockpos) == -1) {
                            this.foliageCoords.add(new FoliageCoordinates(blockpos, blockpos2.getY()));
                        }
                    }
                }
            }
        }
    }

    void crossSection(BlockPos pos, float size, BlockState state) {
        int i = (int) (size + 0.618);
        for (int j = -i; j <= i; j++) {
            for (int k = -i; k <= i; k++) {
                if (Math.pow(Math.abs(j) + 0.5, 2.0) + Math.pow(Math.abs(k) + 0.5, 2.0) <= size * size) {
                    BlockPos p = pos.offset(j, 0, k);
                    BlockState s = this.world.get(p);
                    if (this.world.isAir(p) || s.is(net.minecraft.tags.BlockTags.LEAVES)) {
                        this.world.set(p, state);
                    }
                }
            }
        }
    }

    float layerSize(int y) {
        if (y < this.heightLimit * 0.3F) {
            return -1.0F;
        }
        float f = this.heightLimit / 2.0F;
        float f1 = f - y;
        float f2 = Mth.sqrt(f * f - f1 * f1);
        if (f1 == 0.0F) {
            f2 = f;
        } else if (Math.abs(f1) >= f) {
            return 0.0F;
        }
        return f2 * 0.5F;
    }

    float leafSize(int y) {
        return y >= 0 && y < this.leafDistanceLimit ? (y != 0 && y != this.leafDistanceLimit - 1 ? 3.0F : 2.0F) : -1.0F;
    }

    void generateLeafNode(BlockPos pos) {
        for (int i = 0; i < this.leafDistanceLimit; i++) {
            this.crossSection(pos.above(i), this.leafSize(i), Blocks.OAK_LEAVES.defaultBlockState());
        }
    }

    void limb(BlockPos a, BlockPos b) {
        BlockPos d = b.offset(-a.getX(), -a.getY(), -a.getZ());
        int i = this.getGreatestDistance(d);
        float f = (float) d.getX() / i;
        float f1 = (float) d.getY() / i;
        float f2 = (float) d.getZ() / i;
        for (int j = 0; j <= i; j++) {
            BlockPos p = addF(a, 0.5F + j * f, 0.5F + j * f1, 0.5F + j * f2);
            this.world.set(p, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, this.getLogAxis(a, p)));
        }
    }

    private int getGreatestDistance(BlockPos p) {
        int i = Mth.abs(p.getX());
        int j = Mth.abs(p.getY());
        int k = Mth.abs(p.getZ());
        return k > i && k > j ? k : (j > i ? j : i);
    }

    private Direction.Axis getLogAxis(BlockPos a, BlockPos b) {
        Direction.Axis axis = Direction.Axis.Y;
        int i = Math.abs(b.getX() - a.getX());
        int j = Math.abs(b.getZ() - a.getZ());
        int k = Math.max(i, j);
        if (k > 0) {
            if (i == k) {
                axis = Direction.Axis.X;
            } else if (j == k) {
                axis = Direction.Axis.Z;
            }
        }
        return axis;
    }

    void generateLeaves() {
        for (FoliageCoordinates fc : this.foliageCoords) {
            this.generateLeafNode(fc.pos());
        }
    }

    boolean leafNodeNeedsBase(int y) {
        return y >= this.heightLimit * 0.2;
    }

    void generateTrunk() {
        BlockPos a = this.basePos;
        BlockPos b = this.basePos.above(this.height);
        this.limb(a, b);
        if (this.trunkSize == 2) {
            this.limb(a.east(), b.east());
            this.limb(a.east().south(), b.east().south());
            this.limb(a.south(), b.south());
        }
    }

    void generateLeafNodeBases() {
        for (FoliageCoordinates fc : this.foliageCoords) {
            int i = fc.branchBase();
            BlockPos blockpos = new BlockPos(this.basePos.getX(), i, this.basePos.getZ());
            if (this.leafNodeNeedsBase(i - this.basePos.getY())) {
                this.limb(blockpos, fc.pos());
            }
        }
    }

    int checkBlockLine(BlockPos a, BlockPos b) {
        BlockPos d = b.offset(-a.getX(), -a.getY(), -a.getZ());
        int i = this.getGreatestDistance(d);
        if (i == 0) {
            return -1;
        }
        float f = (float) d.getX() / i;
        float f1 = (float) d.getY() / i;
        float f2 = (float) d.getZ() / i;
        for (int j = 0; j <= i; j++) {
            if (!this.world.logReplaceable(addF(a, 0.5F + j * f, 0.5F + j * f1, 0.5F + j * f2))) {
                return j;
            }
        }
        return -1;
    }

    public boolean generate(TreeWriter w, RandomSource random, BlockPos position) {
        this.world = w;
        this.basePos = position;
        this.rand = new Random(random.nextLong());
        if (this.heightLimit == 0) {
            this.heightLimit = 11 + this.rand.nextInt(this.heightLimitLimit);
        }
        if (!this.validTreeLocation()) {
            return false;
        }
        this.generateLeafNodeList();
        this.generateLeaves();
        this.generateTrunk();
        this.generateLeafNodeBases();
        w.finish();
        return true;
    }

    private boolean validTreeLocation() {
        if (!this.world.isSoil(this.basePos.below())) {
            return false;
        }
        int i = this.checkBlockLine(this.basePos, this.basePos.above(this.heightLimit - 1));
        if (i == -1) {
            return true;
        }
        if (i < 6) {
            return false;
        }
        this.heightLimit = i;
        return true;
    }
}
