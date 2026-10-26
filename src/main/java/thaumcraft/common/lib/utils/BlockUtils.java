package thaumcraft.common.lib.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Порт BlockUtils (методы добавляются по мере переноса систем). */
public class BlockUtils {

    /** Есть ли у блока хотя бы одна сторона, не закрытая непрозрачным блоком. */
    public static boolean isBlockExposed(Level world, BlockPos pos) {
        for (Direction face : Direction.values()) {
            BlockPos p = pos.relative(face);
            if (!world.getBlockState(p).isSolidRender(world, p)) {
                return true;
            }
        }
        return false;
    }

    /** Поиск самого дальнего блока того же вида (findBlocks оригинала; lastPos/lastdistance — здесь локальные). */
    private static final class Furthest {
        net.minecraft.core.BlockPos lastPos;
        double lastdistance = 0.0;
    }

    private static void findBlocks(Level world, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState block, int reach,
                                   Furthest f) {
        for (int xx = -reach; xx <= reach; xx++) {
            for (int yy = reach; yy >= -reach; yy--) {
                for (int zz = -reach; zz <= reach; zz++) {
                    if (Math.abs(f.lastPos.getX() + xx - pos.getX()) > 24 || Math.abs(f.lastPos.getY() + yy - pos.getY()) > 48
                            || Math.abs(f.lastPos.getZ() + zz - pos.getZ()) > 24) {
                        return;
                    }
                    net.minecraft.core.BlockPos p = f.lastPos.offset(xx, yy, zz);
                    if (!world.isLoaded(p)) {
                        continue;
                    }
                    net.minecraft.world.level.block.state.BlockState bs = world.getBlockState(p);
                    // damageDropped (metadata) в 1.21 — отдельные блоки, поэтому «тот же блок»
                    if (bs.getBlock() == block.getBlock() && bs.getDestroySpeed(world, p) >= 0.0F) {
                        double xd = f.lastPos.getX() + xx - pos.getX();
                        double yd = f.lastPos.getY() + yy - pos.getY();
                        double zd = f.lastPos.getZ() + zz - pos.getZ();
                        double d = xd * xd + yd * yd + zd * zd;
                        if (d > f.lastdistance) {
                            f.lastdistance = d;
                            f.lastPos = p;
                            findBlocks(world, pos, block, reach, f);
                            return;
                        }
                    }
                }
            }
        }
    }

    /** «Проходчик»: вместо сломанного блока добывается самый дальний такой же (руда/бревно); у бревна соседи потом обновляются. */
    public static boolean breakFurthestBlock(Level world, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState block,
                                             net.minecraft.world.entity.player.Player player) {
        Furthest f = new Furthest();
        f.lastPos = pos.immutable();
        int reach = thaumcraft.common.lib.utils.Utils.isWoodLog(world, pos) ? 2 : 1;
        findBlocks(world, pos, block, reach, f);
        boolean worked = harvestBlockSkipCheck(world, player, f.lastPos);
        world.sendBlockUpdated(pos, block, block, 3);
        if (worked && thaumcraft.common.lib.utils.Utils.isWoodLog(world, pos)) {
            for (int xx = -3; xx <= 3; xx++) {
                for (int yy = -3; yy <= 3; yy++) {
                    for (int zz = -3; zz <= 3; zz++) {
                        net.minecraft.core.BlockPos p = f.lastPos.offset(xx, yy, zz);
                        if (world.isLoaded(p)) {
                            world.scheduleTick(p, world.getBlockState(p).getBlock(), 50 + world.random.nextInt(75));
                        }
                    }
                }
            }
        }
        return worked;
    }

    public static boolean harvestBlockSkipCheck(Level world, net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        return harvestBlock(world, player, pos, false, true);
    }

    public static boolean harvestBlock(Level world, net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        return harvestBlock(world, player, pos, false, false);
    }

    /**
     * Добыть блок за игрока (harvestBlock оригинала): событие ломания (если не skipEvent), эффект разрушения, в творческом —
     * без выпадения; иначе выпадение с инструментом в руке, но без его износа (износ считают вызывающие).
     * TODO этап 12: alwaysDrop / silkOverride / fortuneOverride (фокусы).
     */
    public static boolean harvestBlock(Level world, net.minecraft.world.entity.player.Player p, net.minecraft.core.BlockPos pos, boolean alwaysDrop,
                                       boolean skipEvent) {
        if (world.isClientSide || !(p instanceof net.minecraft.server.level.ServerPlayer player) || !world.isLoaded(pos)) {
            return false;
        }
        net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (!skipEvent && net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(world, player.gameMode.getGameModeForPlayer(), player, pos, state)
                .isCanceled()) {
            return false;
        }
        net.minecraft.world.level.block.entity.BlockEntity tile = world.getBlockEntity(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.GameMasterBlock && !player.canUseGameMasterBlocks()) {
            world.sendBlockUpdated(pos, state, state, 3);
            return false;
        }
        world.levelEvent(null, 2001, pos, net.minecraft.world.level.block.Block.getId(state));
        boolean removed;
        if (player.isCreative()) {
            removed = state.onDestroyedByPlayer(world, pos, player, false, world.getFluidState(pos));
            if (removed) {
                state.getBlock().destroy(world, pos, state);
            }
        } else {
            net.minecraft.world.item.ItemStack tool = player.getMainHandItem().copy();
            boolean canHarvest = alwaysDrop || state.canHarvestBlock(world, pos, player);
            removed = state.onDestroyedByPlayer(world, pos, player, canHarvest, world.getFluidState(pos));
            if (removed) {
                state.getBlock().destroy(world, pos, state);
            }
            if (removed && canHarvest) {
                state.getBlock().playerDestroy(world, player, pos, state, tile, tool);
            }
        }
        return removed;
    }
}
