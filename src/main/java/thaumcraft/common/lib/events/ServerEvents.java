package thaumcraft.common.lib.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Серверные задачи по мирам (порт части ServerEvents): отложенные действия (addRunnableServer) и «виртуальная» замена блоков
 * (addSwapper — соль мира, позже фокус обмена). Очереди только серверные, по измерениям; очищаются при остановке сервера.
 * Поломка блоков (addBreaker — фокусы «Разрушение», «Земля»). Регенерация чанков и печати — со своими системами.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ServerEvents {
    public static final Predicate<SwapperPredicate> DEFAULT_PREDICATE = p -> true;
    private static final Map<ResourceKey<Level>, Deque<RunnableEntry>> serverRunList = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Deque<VirtualSwapper>> swapList = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Deque<BreakData>> breakList = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void worldTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Deque<RunnableEntry> rlist = serverRunList.get(level.dimension());
        if (rlist != null && !rlist.isEmpty()) {
            Deque<RunnableEntry> temp = new ArrayDeque<>();
            RunnableEntry current;
            while ((current = rlist.poll()) != null) {
                if (current.delay > 0) {
                    current.delay--;
                    temp.offer(current);
                } else {
                    try {
                        current.runnable.run();
                    } catch (Exception e) {
                        Thaumcraft.log.warn("Server task failed", e);
                    }
                }
            }
            rlist.addAll(temp);
        }
        tickBlockSwap(level);
        tickBlockBreak(level);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        serverRunList.clear();
        swapList.clear();
        breakList.clear();
        thaumcraft.api.casters.FocusEngine.clearServerState();
        thaumcraft.common.items.casters.CasterManager.clearServer();
        thaumcraft.common.entities.projectile.EntityFocusCloud.clearServer();
    }

    private static void tickBlockSwap(ServerLevel world) {
        Deque<VirtualSwapper> queue = swapList.get(world.dimension());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        Deque<VirtualSwapper> queue2 = new ArrayDeque<>();
        VirtualSwapper vs;
        while ((vs = queue.poll()) != null) {
            BlockState bs = world.getBlockState(vs.pos);
            boolean allow = bs.getDestroySpeed(world, vs.pos) >= 0.0F;
            if (vs.source != null && !vs.source.test(bs)) {
                allow = false;
            }
            if (vs.visCost > 0.0F && AuraHelper.getVis(world, vs.pos) < vs.visCost) {
                allow = false;
            }
            if (world.mayInteract(vs.player, vs.pos) && allow
                    && (vs.targetState != null ? vs.targetState != bs : (vs.target == null || vs.target.isEmpty() || !ItemStack.isSameItem(vs.target, new ItemStack(bs.getBlock()))))
                    && !EventHooks.onBlockPlace(vs.player, BlockSnapshot.create(world.dimension(), world, vs.pos), Direction.UP)
                    && vs.allowSwap.test(new SwapperPredicate(world, vs.player, vs.pos))) {
                int slot;
                if (vs.consumeTarget && vs.target != null && !vs.target.isEmpty()) {
                    slot = InventoryUtils.getPlayerSlotFor(vs.player, vs.target);
                } else {
                    slot = 1;
                }
                if (vs.player.getAbilities().instabuild) {
                    slot = 1;
                }
                boolean matches = vs.source != null && vs.source.test(bs);
                if ((vs.source == null || matches) && slot >= 0) {
                    if (!vs.player.getAbilities().instabuild) {
                        if (vs.consumeTarget) {
                            vs.player.getInventory().removeItem(slot, 1);
                        }
                        if (vs.pickup) {
                            List<ItemStack> ret = new ArrayList<>();
                            if (vs.silk && !bs.getBlock().asItem().getDefaultInstance().isEmpty()) {
                                ret.add(new ItemStack(bs.getBlock()));
                            } else {
                                // TODO этап 12 (фокус обмена): удача (fortune) при выпадении
                                ret = Block.getDrops(bs, world, vs.pos, world.getBlockEntity(vs.pos), vs.player, ItemStack.EMPTY);
                            }
                            for (ItemStack is : ret) {
                                if (!vs.player.getInventory().add(is)) {
                                    world.addFreshEntity(new ItemEntity(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.5, vs.pos.getZ() + 0.5, is));
                                }
                            }
                        }
                        if (vs.visCost > 0.0F) {
                            ThaumcraftApi.internalMethods.drainVis(world, vs.pos, vs.visCost, false);
                        }
                    }
                    if (vs.targetState != null) {
                        world.setBlock(vs.pos, vs.targetState, 3);
                    } else if (vs.target != null && !vs.target.isEmpty()) {
                        Block tb = Block.byItem(vs.target.getItem());
                        if (tb != Blocks.AIR) {
                            world.setBlock(vs.pos, tb.defaultBlockState(), 3);
                        } else {
                            world.removeBlock(vs.pos, false);
                            EntitySpecialItem entityItem = new EntitySpecialItem(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.1, vs.pos.getZ() + 0.5,
                                    vs.target.copy());
                            entityItem.setDeltaMovement(0.0, 0.0, 0.0);
                            world.addFreshEntity(entityItem);
                        }
                    } else {
                        world.removeBlock(vs.pos, false);
                    }
                    if (vs.fx) {
                        PacketDistributor.sendToPlayersNear(world, null, vs.pos.getX(), vs.pos.getY(), vs.pos.getZ(), 32.0,
                                new PacketFXBlockBamf(vs.pos, vs.color, true, vs.fancy, null));
                    }
                    if (vs.lifespan > 0) {
                        for (int xx = -1; xx <= 1; xx++) {
                            for (int yy = -1; yy <= 1; yy++) {
                                for (int zz = -1; zz <= 1; zz++) {
                                    BlockPos np = vs.pos.offset(xx, yy, zz);
                                    boolean m = vs.source != null && vs.source.test(world.getBlockState(np));
                                    if ((xx != 0 || yy != 0 || zz != 0) && m && BlockUtils.isBlockExposed(world, np)) {
                                        queue2.offer(new VirtualSwapper(np, vs.source, vs.target, vs.targetState, vs.consumeTarget, vs.lifespan - 1, vs.player, vs.fx,
                                                vs.fancy, vs.color, vs.pickup, vs.silk, vs.fortune, vs.allowSwap, vs.visCost));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        queue.addAll(queue2);
    }

    /**
     * @param source что должно стоять на месте (null — что угодно); в оригинале — IBlockState или Material
     * @param target во что превратить (предмет-не-блок появляется парящим EntitySpecialItem); пусто — убрать блок
     * @param color  цвет облачка или −9999 (по умолчанию)
     */
    public static void addSwapper(Level world, BlockPos pos, Predicate<BlockState> source, ItemStack target, boolean consumeTarget, int life, Player player,
                                  boolean fx, boolean fancy, int color, boolean pickup, boolean silk, int fortune, Predicate<SwapperPredicate> allowSwap,
                                  float visCost) {
        if (world.isClientSide) {
            return;
        }
        swapList.computeIfAbsent(world.dimension(), k -> new ArrayDeque<>())
                .offer(new VirtualSwapper(pos, source, target, null, consumeTarget, life, player, fx, fancy, color, pickup, silk, fortune, allowSwap, visCost));
    }

    /** То же, но цель — готовое состояние блока (в 1.12 — предмет-блок с metadata: сторона, вариант). */
    public static void addSwapper(Level world, BlockPos pos, Predicate<BlockState> source, BlockState targetState, Player player, boolean fx, boolean fancy,
                                  int color) {
        if (world.isClientSide) {
            return;
        }
        swapList.computeIfAbsent(world.dimension(), k -> new ArrayDeque<>())
                .offer(new VirtualSwapper(pos, source, new ItemStack(targetState.getBlock()), targetState, false, 0, player, fx, fancy, color, false, false, 0,
                        DEFAULT_PREDICATE, 0.0F));
    }

    /** Выполнить на сервере через delay тиков. */
    public static void addRunnableServer(Level world, Runnable runnable, int delay) {
        if (!world.isClientSide) {
            serverRunList.computeIfAbsent(world.dimension(), k -> new ArrayDeque<>()).add(new RunnableEntry(runnable, delay));
        }
    }

    static class RunnableEntry {
        final Runnable runnable;
        int delay;

        RunnableEntry(Runnable runnable, int delay) {
            this.runnable = runnable;
            this.delay = delay;
        }
    }

    public static class SwapperPredicate {
        public final Level world;
        public final Player player;
        public final BlockPos pos;

        public SwapperPredicate(Level world, Player player, BlockPos pos) {
            this.world = world;
            this.player = player;
            this.pos = pos;
        }
    }

    record VirtualSwapper(BlockPos pos, Predicate<BlockState> source, ItemStack target, BlockState targetState, boolean consumeTarget, int lifespan, Player player, boolean fx,
                          boolean fancy, int color, boolean pickup, boolean silk, int fortune, Predicate<SwapperPredicate> allowSwap, float visCost) {
    }

    /**
     * Постепенная поломка блока за игрока (addBreaker оригинала): каждый тик прочность уменьшается на strength, трещины видны
     * (fx); при нуле — добыча (с шёлковым касанием / удачей), Вис ауры — плата. Блок изменился или выгружен — отмена.
     */
    public static void addBreaker(Level world, BlockPos pos, BlockState source, Player player, boolean fx, boolean silk, int fortune, float str,
                                  float durabilityCurrent, float durabilityMax, int delay, float vis, Runnable run) {
        if (world.isClientSide) {
            return;
        }
        if (delay > 0) {
            addRunnableServer(world, () -> addBreaker(world, pos, source, player, fx, silk, fortune, str, durabilityCurrent, durabilityMax, 0, vis, run),
                    delay);
        } else {
            breakList.computeIfAbsent(world.dimension(), k -> new java.util.concurrent.ConcurrentLinkedDeque<>())
                    .offer(new BreakData(str, durabilityCurrent, durabilityMax, pos, source, player, fx, silk, fortune, vis));
            if (run != null) {
                run.run();
            }
        }
    }

    private static void tickBlockBreak(ServerLevel world) {
        Deque<BreakData> queue = breakList.get(world.dimension());
        if (queue == null) {
            return;
        }
        Deque<BreakData> queue2 = new ArrayDeque<>();
        BreakData vs;
        while ((vs = queue.poll()) != null) {
            if (!world.isLoaded(vs.pos)) {
                continue;
            }
            BlockState bs = world.getBlockState(vs.pos);
            if (bs == vs.source) {
                if ((!(vs.visCost > 0.0F) || !(thaumcraft.api.aura.AuraHelper.getVis(world, vs.pos) < vs.visCost)) && world.mayInteract(vs.player, vs.pos)
                        && bs.getDestroySpeed(world, vs.pos) >= 0.0F) {
                    if (vs.fx) {
                        world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, (int) ((1.0F - vs.durabilityCurrent / vs.durabilityMax) * 10.0F));
                    }
                    vs.durabilityCurrent -= vs.strength;
                    if (vs.durabilityCurrent <= 0.0F) {
                        thaumcraft.common.lib.utils.BlockUtils.harvestBlock(world, vs.player, vs.pos, true, vs.silk, vs.fortune, false);
                        if (vs.fx) {
                            world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, -1);
                        }
                        if (vs.visCost > 0.0F) {
                            thaumcraft.api.aura.AuraHelper.drainVis(world, vs.pos, vs.visCost, false);
                        }
                    } else {
                        queue2.offer(vs);
                    }
                }
            } else if (vs.fx) {
                world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, -1);
            }
        }
        queue.addAll(queue2);
    }

    public static class BreakData {
        final float strength;
        float durabilityCurrent;
        final float durabilityMax;
        final BlockState source;
        final BlockPos pos;
        final Player player;
        final boolean fx;
        final boolean silk;
        final int fortune;
        final float visCost;

        public BreakData(float strength, float durabilityCurrent, float durabilityMax, BlockPos pos, BlockState source, Player player, boolean fx,
                         boolean silk, int fortune, float vis) {
            this.strength = strength;
            this.durabilityCurrent = durabilityCurrent;
            this.durabilityMax = durabilityMax;
            this.pos = pos;
            this.source = source;
            this.player = player;
            this.fx = fx;
            this.silk = silk;
            this.fortune = fortune;
            this.visCost = vis;
        }
    }
}
