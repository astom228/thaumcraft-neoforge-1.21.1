package thaumcraft.common.lib.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.api.crafting.Part;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.events.ToolEvents;
import thaumcraft.common.lib.utils.BlockUtils;

/**
 * Многоблочная структура от соли мира (порт DustTriggerMultiblock): схема (слои сверху вниз) ищется вокруг блока под солью
 * во всех четырёх поворотах; при совпадении каждая клетка с целью превращается с задержкой priority тиков, а до того
 * её нельзя сломать.
 */
public class DustTriggerMultiblock implements IDustTrigger {
    /** EnumFacing.HORIZONTALS 1.12 (порядок поиска поворота). */
    private static final Direction[] HORIZONTALS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    Part[][][] blueprint;
    String research;
    int ySize;
    int xSize;
    int zSize;

    public DustTriggerMultiblock(String research, Part[][][] blueprint) {
        this.blueprint = blueprint;
        this.research = research;
        this.ySize = this.blueprint.length;
        this.xSize = this.blueprint[0].length;
        this.zSize = this.blueprint[0][0].length;
    }

    @Override
    public Placement getValidFace(Level world, Player player, BlockPos pos, Direction face) {
        if (this.research != null && !ThaumcraftCapabilities.knowsResearch(player, this.research)) {
            return null;
        }
        for (int yy = -this.ySize; yy <= 0; yy++) {
            for (int xx = -this.xSize; xx <= 0; xx++) {
                for (int zz = -this.zSize; zz <= 0; zz++) {
                    BlockPos p2 = pos.offset(xx, yy, zz);
                    Direction f = this.fitMultiblock(world, p2);
                    if (f != null) {
                        return new Placement(xx, yy, zz, f);
                    }
                }
            }
        }
        return null;
    }

    /** Совпадает ли состояние блока с источником клетки. */
    @SuppressWarnings("unchecked")
    static boolean sourceMatches(Object source, BlockState bsWo) {
        if (source instanceof Block b) {
            return bsWo.getBlock() == b;
        }
        if (source instanceof TagKey<?> tag) {
            return bsWo.getFluidState().is((TagKey<Fluid>) tag);
        }
        if (source instanceof ItemStack is) {
            return bsWo.getBlock() == Block.byItem(is.getItem());
        }
        if (source instanceof BlockState st) {
            return bsWo == st;
        }
        return true;
    }

    private Direction fitMultiblock(Level world, BlockPos pos) {
        outer:
        for (Direction face : HORIZONTALS) {
            for (int y = 0; y < this.ySize; y++) {
                Matrix matrix = new Matrix(this.blueprint[y]);
                matrix.Rotate90DegRight(3 - face.get2DDataValue());
                for (int x = 0; x < matrix.rows; x++) {
                    for (int z = 0; z < matrix.cols; z++) {
                        Part part = matrix.matrix[x][z];
                        if (part != null && !sourceMatches(part.getSource(), world.getBlockState(pos.offset(x, -y + (this.ySize - 1), z)))) {
                            continue outer;
                        }
                    }
                }
            }
            return face;
        }
        return null;
    }

    @Override
    public List<BlockPos> sparkle(Level world, Player player, BlockPos pos, Placement placement) {
        BlockPos p2 = pos.offset(placement.xOffset, placement.yOffset, placement.zOffset);
        List<BlockPos> list = new ArrayList<>();
        for (int y = 0; y < this.ySize; y++) {
            Matrix matrix = new Matrix(this.blueprint[y]);
            matrix.Rotate90DegRight(3 - placement.facing.get2DDataValue());
            for (int x = 0; x < matrix.rows; x++) {
                for (int z = 0; z < matrix.cols; z++) {
                    if (matrix.matrix[x][z] != null) {
                        BlockPos p3 = p2.offset(x, -y + (this.ySize - 1), z);
                        if (matrix.matrix[x][z].getSource() != null && BlockUtils.isBlockExposed(world, p3)) {
                            list.add(p3);
                        }
                    }
                }
            }
        }
        return list;
    }

    @Override
    public void execute(Level world, Player player, BlockPos pos, Placement placement, Direction side) {
        if (world.isClientSide) {
            return;
        }
        // TODO этап 14: в оригинале здесь для любой структуры вызывалось событие «создан предмет» с адской печью
        BlockPos p2 = pos.offset(placement.xOffset, placement.yOffset, placement.zOffset);
        for (int y = 0; y < this.ySize; y++) {
            Matrix matrix = new Matrix(this.blueprint[y]);
            matrix.Rotate90DegRight(3 - placement.facing.get2DDataValue());
            for (int x = 0; x < matrix.rows; x++) {
                for (int z = 0; z < matrix.cols; z++) {
                    Part part = matrix.matrix[x][z];
                    if (part == null || part.getTarget() == null) {
                        continue;
                    }
                    BlockPos p3 = p2.offset(x, -y + (this.ySize - 1), z);
                    BlockState targetState = null;
                    ItemStack targetStack = ItemStack.EMPTY;
                    if (part.getTarget() instanceof Block tb) {
                        targetState = tb.defaultBlockState();
                        if (targetState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                            // сторона клика по вертикальной грани недопустима для горизонтального свойства — DEVIATIONS 32
                            Direction d = part.getApplyPlayerFacing()
                                    ? (side.getAxis().isHorizontal() ? side : player.getDirection().getOpposite())
                                    : (part.isOpp() ? placement.facing.getOpposite() : placement.facing);
                            targetState = targetState.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
                        }
                    } else if (part.getTarget() instanceof BlockState st) {
                        targetState = st;
                    } else if (part.getTarget() instanceof ItemStack is) {
                        targetStack = is.copy();
                    }
                    Predicate<BlockState> source;
                    Object src = part.getSource();
                    if (src instanceof Block) {
                        BlockState now = world.getBlockState(p3);
                        source = s -> s == now;
                    } else if (src instanceof TagKey<?> || src instanceof BlockState) {
                        source = s -> sourceMatches(src, s);
                    } else {
                        source = null;
                    }
                    final BlockState fTarget = targetState;
                    final ItemStack fStack = targetStack;
                    ToolEvents.addBlockedBlock(world, p3);
                    ServerEvents.addRunnableServer(world, () -> {
                        if (fTarget != null) {
                            ServerEvents.addSwapper(world, p3, source, fTarget, player, true, false, -9999);
                        } else {
                            ServerEvents.addSwapper(world, p3, source, fStack, false, 0, player, true, false, -9999, false, false, 0,
                                    ServerEvents.DEFAULT_PREDICATE, 0.0F);
                        }
                        ToolEvents.clearBlockedBlock(world, p3);
                    }, part.getPriority());
                }
            }
        }
    }
}
