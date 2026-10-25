package thaumcraft.common.lib.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;

/**
 * Источники эссенции вокруг устройства (порт EssentiaHandler): список IAspectSource в области поиска, от ближних к дальним,
 * кешируется; если ничего не нашлось или источники иссякли — повторный поиск не раньше чем через 10 с. Только сервер:
 * кеш по миру и позиции, очищается при остановке сервера. Незагруженные чанки при поиске пропускаются (раздел 7).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class EssentiaHandler {
    static final int DELAY = 10000;
    private static final Map<GlobalPos, List<BlockPos>> sources = new ConcurrentHashMap<>();
    private static final Map<GlobalPos, Long> sourcesDelay = new ConcurrentHashMap<>();
    private static BlockEntity lat = null;
    private static BlockEntity las = null;
    private static Aspect lasp = null;
    private static int lext = 0;

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        sources.clear();
        sourcesDelay.clear();
        lat = las = null;
        lasp = null;
    }

    private static GlobalPos loc(BlockEntity tile) {
        return GlobalPos.of(tile.getLevel().dimension(), tile.getBlockPos());
    }

    /** Зеркало эссенции (этап 10.5) — ignoreMirror оригинала. */
    private static boolean isMirror(BlockEntity te) {
        // TODO этап 10.5: te instanceof TileMirrorEssentia
        return false;
    }

    private static void sendFx(Level world, BlockPos at, BlockPos from, BlockPos to, Aspect aspect, int ext) {
        if (world instanceof ServerLevel sl) {
            PacketDistributor.sendToPlayersNear(sl, null, at.getX(), at.getY(), at.getZ(), 32.0,
                    new PacketFXEssentiaSource(from, (byte) (from.getX() - to.getX()), (byte) (from.getY() - to.getY()), (byte) (from.getZ() - to.getZ()),
                            aspect.getColor(), ext));
        }
    }

    private static IAspectSource sourceAt(Level world, BlockPos pos) {
        if (!world.isLoaded(pos)) {
            return null;
        }
        return world.getBlockEntity(pos) instanceof IAspectSource as ? as : null;
    }

    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, int ext) {
        return drainEssentia(tile, aspect, direction, range, false, ext);
    }

    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror, int ext) {
        GlobalPos tileLoc = loc(tile);
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            return sources.containsKey(tileLoc) && drainEssentia(tile, aspect, direction, range, ignoreMirror, ext);
        }
        Level world = tile.getLevel();
        for (BlockPos source : sources.get(tileLoc)) {
            IAspectSource as = sourceAt(world, source);
            if (as == null) {
                break;
            }
            if (!as.isBlocked() && (!ignoreMirror || !isMirror((BlockEntity) as)) && as.takeFromContainer(aspect, 1)) {
                sendFx(world, tile.getBlockPos(), tile.getBlockPos(), source, aspect, ext);
                return true;
            }
        }
        sources.remove(tileLoc);
        sourcesDelay.put(tileLoc, System.currentTimeMillis() + DELAY);
        return false;
    }

    public static boolean drainEssentiaWithConfirmation(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror, int ext) {
        GlobalPos tileLoc = loc(tile);
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            return sources.containsKey(tileLoc) && drainEssentiaWithConfirmation(tile, aspect, direction, range, ignoreMirror, ext);
        }
        Level world = tile.getLevel();
        for (BlockPos source : sources.get(tileLoc)) {
            IAspectSource as = sourceAt(world, source);
            if (as == null) {
                break;
            }
            if (!as.isBlocked() && (!ignoreMirror || !isMirror((BlockEntity) as)) && as.doesContainerContainAmount(aspect, 1)) {
                las = (BlockEntity) as;
                lasp = aspect;
                lat = tile;
                lext = ext;
                return true;
            }
        }
        sources.remove(tileLoc);
        sourcesDelay.put(tileLoc, System.currentTimeMillis() + DELAY);
        return false;
    }

    public static void confirmDrain() {
        if (las != null && lasp != null && lat != null) {
            IAspectSource as = (IAspectSource) las;
            if (as.takeFromContainer(lasp, 1)) {
                sendFx(lat.getLevel(), lat.getBlockPos(), lat.getBlockPos(), las.getBlockPos(), lasp, lext);
            }
        }
        las = null;
        lasp = null;
        lat = null;
    }

    public static boolean addEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror, int ext) {
        GlobalPos tileLoc = loc(tile);
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            return sources.containsKey(tileLoc) && addEssentia(tile, aspect, direction, range, ignoreMirror, ext);
        }
        Level world = tile.getLevel();
        List<BlockPos> es = sources.get(tileLoc);
        List<BlockPos> empties = new ArrayList<>();
        for (BlockPos source : es) {
            IAspectSource as = sourceAt(world, source);
            if (as == null) {
                break;
            }
            if (!as.isBlocked() && (!ignoreMirror || !isMirror((BlockEntity) as))) {
                if (!as.doesContainerAccept(aspect) || as.getAspects() != null && as.getAspects().visSize() != 0) {
                    if (as.doesContainerAccept(aspect) && as.addToContainer(aspect, 1) <= 0) {
                        sendFx(world, tile.getBlockPos(), source, tile.getBlockPos(), aspect, ext);
                        return true;
                    }
                } else {
                    empties.add(source);
                }
            }
        }
        for (BlockPos source : empties) {
            IAspectSource as = sourceAt(world, source);
            if (as == null) {
                break;
            }
            if (as.doesContainerAccept(aspect) && as.addToContainer(aspect, 1) <= 0) {
                sendFx(world, tile.getBlockPos(), source, tile.getBlockPos(), aspect, ext);
                return true;
            }
        }
        sources.remove(tileLoc);
        sourcesDelay.put(tileLoc, System.currentTimeMillis() + DELAY);
        return false;
    }

    public static boolean findEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror) {
        GlobalPos tileLoc = loc(tile);
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            return sources.containsKey(tileLoc) && findEssentia(tile, aspect, direction, range, ignoreMirror);
        }
        Level world = tile.getLevel();
        for (BlockPos source : sources.get(tileLoc)) {
            IAspectSource as = sourceAt(world, source);
            if (as == null) {
                break;
            }
            if (!as.isBlocked() && (!ignoreMirror || !isMirror((BlockEntity) as)) && as.doesContainerContainAmount(aspect, 1)) {
                return true;
            }
        }
        return false;
    }

    /** Как в оригинале, при пустом кеше вызывает поиск эссенции (findEssentia), а не проверку приёма. */
    public static boolean canAcceptEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror) {
        GlobalPos tileLoc = loc(tile);
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            return sources.containsKey(tileLoc) && findEssentia(tile, aspect, direction, range, ignoreMirror);
        }
        Level world = tile.getLevel();
        for (BlockPos source : sources.get(tileLoc)) {
            IAspectSource as = sourceAt(world, source);
            if (as == null) {
                break;
            }
            if ((!ignoreMirror || !isMirror((BlockEntity) as)) && !as.isBlocked() && as.doesContainerAccept(aspect)) {
                return true;
            }
        }
        return false;
    }

    private static void getSources(Level world, GlobalPos tileLoc, Direction direction, int range) {
        Long d = sourcesDelay.get(tileLoc);
        if (d != null) {
            if (d > System.currentTimeMillis()) {
                return;
            }
            sourcesDelay.remove(tileLoc);
        }
        BlockPos origin = tileLoc.pos();
        List<BlockPos> sourceList = new ArrayList<>();
        int start = 0;
        if (direction == null) {
            start = -range;
            direction = Direction.UP;
        }
        for (int aa = -range; aa <= range; aa++) {
            for (int bb = -range; bb <= range; bb++) {
                for (int cc = start; cc < range; cc++) {
                    if (aa != 0 || bb != 0 || cc != 0) {
                        int xx = origin.getX();
                        int yy = origin.getY();
                        int zz = origin.getZ();
                        if (direction.getStepY() != 0) {
                            xx += aa;
                            yy += cc * direction.getStepY();
                            zz += bb;
                        } else if (direction.getStepX() == 0) {
                            xx += aa;
                            yy += bb;
                            zz += cc * direction.getStepZ();
                        } else {
                            xx += cc * direction.getStepX();
                            yy += aa;
                            zz += bb;
                        }
                        BlockPos p = new BlockPos(xx, yy, zz);
                        // TODO этап 10.5: зеркало эссенции не берёт эссенцию из связанного с ним зеркала
                        if (sourceAt(world, p) != null) {
                            sourceList.add(p);
                        }
                    }
                }
            }
        }
        if (!sourceList.isEmpty()) {
            List<BlockPos> sourceList2 = new ArrayList<>();
            outer:
            for (BlockPos wc : sourceList) {
                double dist = wc.distSqr(origin);
                for (int a = 0; a < sourceList2.size(); a++) {
                    if (dist < sourceList2.get(a).distSqr(origin)) {
                        sourceList2.add(a, wc);
                        continue outer;
                    }
                }
                sourceList2.add(wc);
            }
            sources.put(tileLoc, sourceList2);
        } else {
            sourcesDelay.put(tileLoc, System.currentTimeMillis() + DELAY);
        }
    }

    public static void refreshSources(BlockEntity tile) {
        sources.remove(loc(tile));
    }
}
