package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.api.items.IArchitect;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Носитель «План» (порт FocusMediumPlan): цели — все блоки области (размеры и ось задаются клавишами перчатки) у блока, на
 * который смотрит заклинатель (до 16 блоков). Способ: «целиком» — объём вглубь от грани, «поверхность» — связные открытые
 * блоки того же вида в плоскости грани. Ближние блоки — первыми. Исключительный носитель.
 */
public class FocusMediumPlan extends FocusMedium implements IArchitect {
    final ArrayList<BlockPos> checked = new ArrayList<>();

    @Override
    public String getResearch() {
        return "FOCUSPLAN";
    }

    @Override
    public String getKey() {
        return "thaumcraft.PLAN";
    }

    @Override
    public int getComplexity() {
        return 4;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.CRAFT;
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] method = {0, 1};
        String[] methodDesc = {"focus.plan.full", "focus.plan.surface"};
        return new NodeSetting[] {new NodeSetting("method", "focus.plan.method", new NodeSetting.NodeSettingIntList(method, methodDesc))};
    }

    @Override
    public HitResult[] supplyTargets() {
        if (this.getParent() == null || !(this.getPackage().getCaster() instanceof Player player)) {
            return new HitResult[0];
        }
        ArrayList<HitResult> targets = new ArrayList<>();
        ItemStack casterStack = ItemStack.EMPTY;
        if (player.getMainHandItem().getItem() instanceof ICaster) {
            casterStack = player.getMainHandItem();
        } else if (player.getOffhandItem().getItem() instanceof ICaster) {
            casterStack = player.getOffhandItem();
        }
        for (Trajectory sT : this.getParent().supplyTrajectories()) {
            Vec3 end = sT.direction.normalize().scale(16.0).add(sT.source);
            BlockHitResult target = EntityUtils.rayTraceBlocks(this.getPackage().world, sT.source, end);
            if (target != null) {
                ArrayList<BlockPos> usl = this.getArchitectBlocks(casterStack, this.getPackage().world, target.getBlockPos(), target.getDirection(), player);
                BlockPos origin = target.getBlockPos();
                usl.sort(Comparator.comparingDouble(origin::distSqr));
                for (BlockPos p : usl) {
                    targets.add(new BlockHitResult(new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5), target.getDirection(), p, false));
                }
            }
        }
        return targets.toArray(new HitResult[0]);
    }

    @Override
    public HitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player) {
        Vec3 start = player.position().add(0.0, player.getEyeHeight(), 0.0);
        Vec3 end = player.getLookAngle().scale(16.0).add(start);
        return EntityUtils.rayTraceBlocks(world, start, end);
    }

    @Override
    public boolean useBlockHighlight(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public boolean showAxis(ItemStack stack, Level world, Player player, Direction side, EnumAxis axis) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        int dim = CasterManager.getAreaDim(stack);
        if (this.getSettingValue("method") == 0) {
            return switch (axis) {
                case Y -> dim == 0 || dim == 3;
                case Z -> dim == 0 || dim == 2;
                case X -> dim == 0 || dim == 1;
            };
        }
        return switch (side.getAxis()) {
            case Y -> axis == EnumAxis.X && (dim == 0 || dim == 1) || axis == EnumAxis.Z && (dim == 0 || dim == 2);
            case Z -> axis == EnumAxis.Y && (dim == 0 || dim == 1) || axis == EnumAxis.X && (dim == 0 || dim == 2);
            case X -> axis == EnumAxis.Y && (dim == 0 || dim == 1) || axis == EnumAxis.Z && (dim == 0 || dim == 2);
        };
    }

    @Override
    public ArrayList<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos pos, Direction side, Player player) {
        ArrayList<BlockPos> out = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return out;
        }
        this.checked.clear();
        if (this.getSettingValue("method") == 0) {
            this.checkNeighboursFull(world, pos, pos.immutable(), side, CasterManager.getAreaX(stack), CasterManager.getAreaY(stack),
                    CasterManager.getAreaZ(stack), out, player);
        } else {
            BlockState bi = world.getBlockState(pos);
            if (side.getAxis() == Direction.Axis.Z) {
                this.checkNeighboursSurface(world, pos, bi, pos.immutable(), side, CasterManager.getAreaZ(stack), CasterManager.getAreaY(stack),
                        CasterManager.getAreaX(stack), out, player);
            } else {
                this.checkNeighboursSurface(world, pos, bi, pos.immutable(), side, CasterManager.getAreaX(stack), CasterManager.getAreaY(stack),
                        CasterManager.getAreaZ(stack), out, player);
            }
        }
        return out;
    }

    public void checkNeighboursFull(Level world, BlockPos pos1, BlockPos pos2, Direction side, int sizeX, int sizeY, int sizeZ, ArrayList<BlockPos> list,
                                    Player player) {
        if (this.checked.contains(pos2)) {
            return;
        }
        this.checked.add(pos2);
        if (world.isLoaded(pos2) && !world.isEmptyBlock(pos2) && world.getBlockState(pos2).getDestroySpeed(world, pos2) >= 0.0F
                && world.mayInteract(player, pos2)) {
            list.add(pos2);
        }
        int xs = -sizeX + pos1.getX();
        int xe = sizeX + pos1.getX();
        int ys = -sizeY + pos1.getY();
        int ye = sizeY + pos1.getY();
        int zs = -sizeZ + pos1.getZ();
        int ze = sizeZ + pos1.getZ();
        xs -= sizeX * side.getStepX();
        xe -= sizeX * side.getStepX();
        ys -= sizeY * side.getStepY();
        ye -= sizeY * side.getStepY();
        zs -= sizeZ * side.getStepZ();
        ze -= sizeZ * side.getStepZ();
        for (Direction dir : Direction.values()) {
            BlockPos q = pos2.relative(dir);
            if (q.getX() >= xs && q.getX() <= xe && q.getY() >= ys && q.getY() <= ye && q.getZ() >= zs && q.getZ() <= ze) {
                this.checkNeighboursFull(world, pos1, q, side, sizeX, sizeY, sizeZ, list, player);
            }
        }
    }

    public void checkNeighboursSurface(Level world, BlockPos pos1, BlockState bi, BlockPos pos2, Direction side, int sizeX, int sizeY, int sizeZ,
                                       ArrayList<BlockPos> list, Player player) {
        if (this.checked.contains(pos2)) {
            return;
        }
        this.checked.add(pos2);
        switch (side.getAxis()) {
            case Y -> {
                if (Math.abs(pos2.getX() - pos1.getX()) > sizeX || Math.abs(pos2.getZ() - pos1.getZ()) > sizeZ) {
                    return;
                }
            }
            case Z -> {
                if (Math.abs(pos2.getX() - pos1.getX()) > sizeX || Math.abs(pos2.getY() - pos1.getY()) > sizeZ) {
                    return;
                }
            }
            case X -> {
                if (Math.abs(pos2.getY() - pos1.getY()) > sizeX || Math.abs(pos2.getZ() - pos1.getZ()) > sizeZ) {
                    return;
                }
            }
        }
        if (world.isLoaded(pos2) && world.getBlockState(pos2) == bi && BlockUtils.isBlockExposed(world, pos2) && !world.isEmptyBlock(pos2)
                && world.getBlockState(pos2).getDestroySpeed(world, pos2) >= 0.0F && world.mayInteract(player, pos2)) {
            list.add(pos2);
            for (Direction dir : Direction.values()) {
                if (dir != side && dir.getOpposite() != side) {
                    this.checkNeighboursSurface(world, pos1, bi, pos2.relative(dir), side, sizeX, sizeY, sizeZ, list, player);
                }
            }
        }
    }
}
