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

    /** Идёт добыча без события ломки — опыт не выпадает (ToolEvents.harvestBlockEvent). Только серверный поток. */
    private static boolean noExperience = false;

    public static boolean isHarvestingWithoutExperience() {
        return noExperience;
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
     */
    public static boolean harvestBlock(Level world, net.minecraft.world.entity.player.Player p, net.minecraft.core.BlockPos pos, boolean alwaysDrop,
                                       boolean skipEvent) {
        return harvestBlock(world, p, pos, alwaysDrop, false, 0, skipEvent);
    }

    /**
     * То же с переопределением зачарований (фокусы «Разрушение» и т.п.): шёлковое касание и/или удача уровня fortuneOverride
     * (если она выше удачи игрока) — на копии инструмента; без инструмента или при alwaysDrop — на зачарованном заменителе.
     */
    public static boolean harvestBlock(Level world, net.minecraft.world.entity.player.Player p, net.minecraft.core.BlockPos pos, boolean alwaysDrop,
                                       boolean silkOverride, int fortuneOverride, boolean skipEvent) {
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
        // эффект разрушения: остальным — из playerWillDestroy (в 1.12 его вызывал removedByPlayer), самому игроку — пакетом
        // (у него нет предсказания ломки этого блока); в оригинале — playEvent(null, 2001) для всех
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelEventPacket(2001, pos,
                net.minecraft.world.level.block.Block.getId(state), false));
        state = state.getBlock().playerWillDestroy(world, pos, state, player);
        boolean removed;
        if (player.isCreative()) {
            removed = state.onDestroyedByPlayer(world, pos, player, false, world.getFluidState(pos));
            if (removed) {
                state.getBlock().destroy(world, pos, state);
            }
        } else {
            net.minecraft.world.item.ItemStack itemstack1 = player.getMainHandItem();
            net.minecraft.world.item.ItemStack tool = itemstack1.copy();
            boolean canHarvest = alwaysDrop || state.canHarvestBlock(world, pos, player);
            var enchants = world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var fortuneE = enchants.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
            if (silkOverride || fortuneOverride > net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(fortuneE, player)) {
                if (alwaysDrop || tool.isEmpty()) {
                    tool = new net.minecraft.world.item.ItemStack(thaumcraft.api.items.ItemsTC.enchantedPlaceholder.get());
                }
                net.minecraft.world.item.enchantment.ItemEnchantments.Mutable m = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                        net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(itemstack1));
                if (silkOverride) {
                    m.set(enchants.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
                }
                int fort = Math.max(fortuneOverride, m.getLevel(fortuneE));
                if (fort > 0) {
                    m.set(fortuneE, fort);
                }
                net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(tool, m.toImmutable());
            }
            removed = state.onDestroyedByPlayer(world, pos, player, canHarvest, world.getFluidState(pos));
            if (removed) {
                state.getBlock().destroy(world, pos, state);
            }
            if (removed && canHarvest) {
                // опыт: без события ломки (skipEvent) в оригинале его не было (exp = 0)
                boolean prev = noExperience;
                noExperience = skipEvent;
                try {
                    state.getBlock().playerDestroy(world, player, pos, state, tile, tool);
                } finally {
                    noExperience = prev;
                }
            }
        }
        return removed;
    }

    /** Сквозь блок не проходит «Разлом» (тег thaumcraft:portable_hole_blacklist вместо списка строк оригинала). */
    public static boolean isPortableHoleBlackListed(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(thaumcraft.common.lib.TCTags.Blocks.PORTABLE_HOLE_BLACKLIST);
    }
}
