package thaumcraft.common.world.objects;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import thaumcraft.api.blocks.BlocksTC;

/**
 * Великое дерево (порт WorldGenGreatwoodTrees): два поставленных друг на друга «больших дуба» со стволом 2×2 (второй —
 * шире). С пауками: под деревом рассадник пещерных пауков, паутина в кроне и сундук простого подземелья ниже.
 */
public class WorldGenGreatwoodTrees {
    static final byte[] otherCoordPairs = new byte[] {2, 0, 0, 1, 2, 1};
    final Random rand = new Random();
    TreeWriter world;
    final int[] basePos = new int[] {0, 0, 0};
    int heightLimit = 0;
    int height;
    double heightAttenuation = 0.618;
    double branchSlope = 0.38;
    double scaleWidth = 1.2;
    double leafDensity = 0.9;
    int trunkSize = 2;
    int heightLimitLimit = 11;
    int leafDistanceLimit = 4;
    int[][] leafNodes;
    final boolean spiders;

    public WorldGenGreatwoodTrees(boolean notify, boolean spiders) {
        this.spiders = spiders;
    }

    private boolean airOrLeaf(BlockPos p) {
        return this.world.isAir(p) || this.world.get(p).is(BlocksTC.leafGreatwood.get());
    }

    void generateLeafNodeList() {
        this.height = (int) (this.heightLimit * this.heightAttenuation);
        if (this.height >= this.heightLimit) {
            this.height = this.heightLimit - 1;
        }
        int var1 = (int) (1.382 + Math.pow(this.leafDensity * this.heightLimit / 13.0, 2.0));
        if (var1 < 1) {
            var1 = 1;
        }
        int[][] var2 = new int[var1 * this.heightLimit][4];
        int var3 = this.basePos[1] + this.heightLimit - this.leafDistanceLimit;
        int var4 = 1;
        int var5 = this.basePos[1] + this.height;
        int var6 = var3 - this.basePos[1];
        var2[0][0] = this.basePos[0];
        var2[0][1] = var3;
        var2[0][2] = this.basePos[2];
        var2[0][3] = var5;
        var3--;
        while (var6 >= 0) {
            int var7 = 0;
            float var8 = this.layerSize(var6);
            if (var8 < 0.0F) {
                var3--;
                var6--;
            } else {
                double var9 = 0.5;
                while (var7 < var1) {
                    double var11 = this.scaleWidth * var8 * (this.rand.nextFloat() + 0.328);
                    double var13 = this.rand.nextFloat() * 2.0 * Math.PI;
                    int var15 = Mth.floor(var11 * Math.sin(var13) + this.basePos[0] + var9);
                    int var16 = Mth.floor(var11 * Math.cos(var13) + this.basePos[2] + var9);
                    int[] var17 = new int[] {var15, var3, var16};
                    int[] var18 = new int[] {var15, var3 + this.leafDistanceLimit, var16};
                    if (this.checkBlockLine(var17, var18) == -1) {
                        int[] var19 = new int[] {this.basePos[0], this.basePos[1], this.basePos[2]};
                        double var20 = Math.sqrt(Math.pow(Math.abs(this.basePos[0] - var17[0]), 2.0) + Math.pow(Math.abs(this.basePos[2] - var17[2]), 2.0));
                        double var22 = var20 * this.branchSlope;
                        if (var17[1] - var22 > var5) {
                            var19[1] = var5;
                        } else {
                            var19[1] = (int) (var17[1] - var22);
                        }
                        if (this.checkBlockLine(var19, var17) == -1) {
                            var2[var4][0] = var15;
                            var2[var4][1] = var3;
                            var2[var4][2] = var16;
                            var2[var4][3] = var19[1];
                            var4++;
                        }
                    }
                    var7++;
                }
                var3--;
                var6--;
            }
        }
        this.leafNodes = new int[var4][4];
        System.arraycopy(var2, 0, this.leafNodes, 0, var4);
    }

    void genTreeLayer(int par1, int par2, int par3, float par4, byte par5, BlockState leaf) {
        int var7 = (int) (par4 + 0.618);
        byte var8 = otherCoordPairs[par5];
        byte var9 = otherCoordPairs[par5 + 3];
        int[] var10 = new int[] {par1, par2, par3};
        int[] var11 = new int[] {0, 0, 0};
        var11[par5] = var10[par5];
        for (int var12 = -var7; var12 <= var7; var12++) {
            var11[var8] = var10[var8] + var12;
            for (int var13 = -var7; var13 <= var7; var13++) {
                double var15 = Math.pow(Math.abs(var12) + 0.5, 2.0) + Math.pow(Math.abs(var13) + 0.5, 2.0);
                if (var15 <= par4 * par4) {
                    var11[var9] = var10[var9] + var13;
                    BlockPos p = new BlockPos(var11[0], var11[1], var11[2]);
                    if (this.airOrLeaf(p) && this.world.leafReplaceable(p)) {
                        this.world.set(p, leaf);
                    }
                }
            }
        }
    }

    float layerSize(int par1) {
        if (par1 < this.heightLimit * 0.3) {
            return -1.618F;
        }
        float var2 = this.heightLimit / 2.0F;
        float var3 = this.heightLimit / 2.0F - par1;
        float var4;
        if (var3 == 0.0F) {
            var4 = var2;
        } else if (Math.abs(var3) >= var2) {
            var4 = 0.0F;
        } else {
            var4 = (float) Math.sqrt(Math.pow(Math.abs(var2), 2.0) - Math.pow(Math.abs(var3), 2.0));
        }
        return var4 * 0.5F;
    }

    float leafSize(int par1) {
        return par1 >= 0 && par1 < this.leafDistanceLimit ? (par1 != 0 && par1 != this.leafDistanceLimit - 1 ? 3.0F : 2.0F) : -1.0F;
    }

    void generateLeafNode(int par1, int par2, int par3) {
        BlockState leaf = BlocksTC.leafGreatwood.get().defaultBlockState();
        for (int var4 = par2; var4 < par2 + this.leafDistanceLimit; var4++) {
            float var6 = this.leafSize(var4 - par2);
            this.genTreeLayer(par1, var4, par3, var6, (byte) 1, leaf);
        }
    }

    void placeBlockLine(int[] a, int[] b) {
        int[] var4 = new int[] {0, 0, 0};
        byte var6 = 0;
        for (byte var5 = 0; var5 < 3; var5++) {
            var4[var5] = b[var5] - a[var5];
            if (Math.abs(var4[var5]) > Math.abs(var4[var6])) {
                var6 = var5;
            }
        }
        if (var4[var6] != 0) {
            byte var7 = otherCoordPairs[var6];
            byte var8 = otherCoordPairs[var6 + 3];
            byte var9 = (byte) (var4[var6] > 0 ? 1 : -1);
            double var10 = (double) var4[var7] / var4[var6];
            double var12 = (double) var4[var8] / var4[var6];
            int[] var14 = new int[] {0, 0, 0};
            for (int var15 = 0, var16 = var4[var6] + var9; var15 != var16; var15 += var9) {
                var14[var6] = Mth.floor(a[var6] + var15 + 0.5);
                var14[var7] = Mth.floor(a[var7] + var15 * var10 + 0.5);
                var14[var8] = Mth.floor(a[var8] + var15 * var12 + 0.5);
                Direction.Axis axis = Direction.Axis.Y;
                int var18 = Math.abs(var14[0] - a[0]);
                int var19 = Math.abs(var14[2] - a[2]);
                int var20 = Math.max(var18, var19);
                if (var20 > 0) {
                    if (var18 == var20) {
                        axis = Direction.Axis.X;
                    } else if (var19 == var20) {
                        axis = Direction.Axis.Z;
                    }
                }
                BlockPos p = new BlockPos(var14[0], var14[1], var14[2]);
                if (this.world.logReplaceable(p)) {
                    this.world.set(p, BlocksTC.logGreatwood.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
                }
            }
        }
    }

    void generateLeaves() {
        for (int[] node : this.leafNodes) {
            this.generateLeafNode(node[0], node[1], node[2]);
        }
    }

    boolean leafNodeNeedsBase(int par1) {
        return par1 >= this.heightLimit * 0.2;
    }

    void generateTrunk() {
        int var1 = this.basePos[0];
        int var2 = this.basePos[1];
        int var3 = this.basePos[1] + this.height;
        int var4 = this.basePos[2];
        int[] var5 = new int[] {var1, var2, var4};
        int[] var6 = new int[] {var1, var3, var4};
        this.placeBlockLine(var5, var6);
        if (this.trunkSize == 2) {
            var5[0]++;
            var6[0]++;
            this.placeBlockLine(var5, var6);
            var5[2]++;
            var6[2]++;
            this.placeBlockLine(var5, var6);
            var5[0]--;
            var6[0]--;
            this.placeBlockLine(var5, var6);
        }
    }

    void generateLeafNodeBases() {
        int[] var3 = new int[] {this.basePos[0], this.basePos[1], this.basePos[2]};
        for (int[] var4 : this.leafNodes) {
            int[] var5 = new int[] {var4[0], var4[1], var4[2]};
            var3[1] = var4[3];
            int var6 = var3[1] - this.basePos[1];
            if (this.leafNodeNeedsBase(var6)) {
                this.placeBlockLine(var3, var5);
            }
        }
    }

    int checkBlockLine(int[] a, int[] b) {
        int[] var3 = new int[] {0, 0, 0};
        byte var5 = 0;
        for (byte var4 = 0; var4 < 3; var4++) {
            var3[var4] = b[var4] - a[var4];
            if (Math.abs(var3[var4]) > Math.abs(var3[var5])) {
                var5 = var4;
            }
        }
        if (var3[var5] == 0) {
            return -1;
        }
        byte var6 = otherCoordPairs[var5];
        byte var7 = otherCoordPairs[var5 + 3];
        byte var8 = (byte) (var3[var5] > 0 ? 1 : -1);
        double var9 = (double) var3[var6] / var3[var5];
        double var11 = (double) var3[var7] / var3[var5];
        int[] var13 = new int[] {0, 0, 0};
        int var14 = 0;
        int var15;
        for (var15 = var3[var5] + var8; var14 != var15; var14 += var8) {
            var13[var5] = a[var5] + var14;
            var13[var6] = Mth.floor(a[var6] + var14 * var9);
            var13[var7] = Mth.floor(a[var7] + var14 * var11);
            if (!this.airOrLeaf(new BlockPos(var13[0], var13[1], var13[2]))) {
                break;
            }
        }
        return var14 == var15 ? -1 : Math.abs(var14);
    }

    boolean validTreeLocation(int x, int z) {
        int[] var1x = new int[] {this.basePos[0] + x, this.basePos[1], this.basePos[2] + z};
        int[] var2x = new int[] {this.basePos[0] + x, this.basePos[1] + this.heightLimit - 1, this.basePos[2] + z};
        if (!this.world.isSoil(new BlockPos(this.basePos[0] + x, this.basePos[1] - 1, this.basePos[2] + z))) {
            return false;
        }
        int var4 = this.checkBlockLine(var1x, var2x);
        if (var4 == -1) {
            return true;
        }
        if (var4 < 6) {
            return false;
        }
        this.heightLimit = var4;
        return true;
    }

    public boolean generate(TreeWriter w, RandomSource random, BlockPos pos) {
        this.world = w;
        this.rand.setSeed(random.nextLong());
        this.basePos[0] = pos.getX();
        this.basePos[1] = pos.getY();
        this.basePos[2] = pos.getZ();
        if (this.heightLimit == 0) {
            this.heightLimit = this.heightLimitLimit + this.rand.nextInt(this.heightLimitLimit);
        }
        boolean valid = false;
        search:
        for (int a = -1; a < 2; a++) {
            next:
            for (int b = -1; b < 2; b++) {
                for (int i = 0; i < this.trunkSize; i++) {
                    for (int j = 0; j < this.trunkSize; j++) {
                        if (!this.validTreeLocation(i + a, j + b)) {
                            continue next;
                        }
                    }
                }
                valid = true;
                this.basePos[0] += a;
                this.basePos[2] += b;
                break search;
            }
        }
        if (!valid) {
            return false;
        }
        w.set(pos, Blocks.AIR.defaultBlockState());
        this.generateLeafNodeList();
        this.generateLeaves();
        this.generateLeafNodeBases();
        this.generateTrunk();
        this.scaleWidth = 1.66;
        this.basePos[0] = pos.getX();
        this.basePos[1] = pos.getY() + this.height;
        this.basePos[2] = pos.getZ();
        this.generateLeafNodeList();
        this.generateLeaves();
        this.generateLeafNodeBases();
        this.generateTrunk();
        w.finish();
        if (this.spiders && w.canAccess(pos.below(2))) {
            var level = w.level();
            w.set(pos.below(), Blocks.SPAWNER.defaultBlockState());
            if (level.getBlockEntity(pos.below()) instanceof SpawnerBlockEntity spawner) {
                spawner.setEntityId(EntityType.CAVE_SPIDER, random);
                for (int a = 0; a < 50; a++) {
                    BlockPos p = new BlockPos(pos.getX() - 7 + random.nextInt(14), pos.getY() + random.nextInt(10), pos.getZ() - 7 + random.nextInt(14));
                    if (w.isAir(p) && touching(w, p)) {
                        w.set(p, Blocks.COBWEB.defaultBlockState());
                    }
                }
                w.set(pos.below(2), Blocks.CHEST.defaultBlockState());
                RandomizableContainer.setBlockEntityLootTable(level, random, pos.below(2), BuiltInLootTables.SIMPLE_DUNGEON);
            }
        }
        return true;
    }

    /** BlockUtils.isBlockTouching: рядом бревно или листва великого дерева. */
    private static boolean touching(TreeWriter w, BlockPos p) {
        for (Direction d : Direction.values()) {
            BlockState s = w.get(p.relative(d));
            if (s.is(BlocksTC.leafGreatwood.get()) || s.is(BlocksTC.logGreatwood.get())) {
                return true;
            }
        }
        return false;
    }
}
