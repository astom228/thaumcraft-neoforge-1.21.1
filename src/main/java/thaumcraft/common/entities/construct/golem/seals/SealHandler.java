package thaumcraft.common.entities.construct.golem.seals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.network.misc.PacketSealToClient;

/**
 * Печати управления (порт SealHandler). Типы — реестр при запуске; поставленные печати — только на сервере, по
 * измерениям (у клиента своя копия, ClientSeals, из PacketSealToClient). Хранятся в данных чанка (ChunkEvents), как
 * в оригинале.
 */
public class SealHandler {
    public static final LinkedHashMap<String, ISeal> types = new LinkedHashMap<>();
    private static final Map<ResourceKey<Level>, Map<SealPos, SealEntity>> sealEntities = new ConcurrentHashMap<>();
    static int count = 0;

    public static void registerSeal(ISeal seal) {
        if (types.containsKey(seal.getKey())) {
            Thaumcraft.log.error("Attempting to register Seal [" + seal.getKey() + "] twice. Ignoring.");
        } else {
            types.put(seal.getKey(), seal);
        }
    }

    public static String[] getRegisteredSeals() {
        return types.keySet().toArray(new String[0]);
    }

    public static ISeal getSeal(String key) {
        return types.get(key);
    }

    /** Новый экземпляр печати этого типа (getClass().newInstance() оригинала). */
    public static ISeal newSeal(String key) {
        ISeal proto = types.get(key);
        if (proto == null) {
            return null;
        }
        try {
            return proto.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    /** Предмет печати (в 1.12 — seals с метой = номер типа + 1); предметы печатей — ItemSealPlacer. */
    public static ItemStack getSealStack(String key) {
        return ItemSealPlacer.getSealStack(key);
    }

    private static Map<SealPos, SealEntity> map(Level world) {
        return sealEntities.computeIfAbsent(world.dimension(), k -> new ConcurrentHashMap<>());
    }

    public static List<SealEntity> getSealsInRange(Level world, BlockPos source, int range) {
        List<SealEntity> out = new ArrayList<>();
        for (SealEntity se : map(world).values()) {
            if (se.getSeal() != null && se.getSealPos() != null && se.sealPos.pos.distSqr(source) <= range * range) {
                out.add(se);
            }
        }
        return out;
    }

    public static List<SealEntity> getSealsInChunk(Level world, ChunkPos chunk) {
        List<SealEntity> out = new ArrayList<>();
        for (SealEntity se : map(world).values()) {
            if (se.getSeal() != null && se.getSealPos() != null && new ChunkPos(se.sealPos.pos).equals(chunk)) {
                out.add(se);
            }
        }
        return out;
    }

    public static ISealEntity getSealEntity(ResourceKey<Level> dim, SealPos pos) {
        if (pos == null) {
            return null;
        }
        Map<SealPos, SealEntity> se = sealEntities.get(dim);
        return se != null ? se.get(pos) : null;
    }

    /** Снять печать; если не quiet — выпадает её предмет. Задачи печати приостанавливаются, клиенты узнают об удалении. */
    public static void removeSealEntity(Level world, SealPos pos, boolean quiet) {
        SealEntity seal = map(world).remove(pos);
        if (!world.isClientSide && seal != null && seal.seal != null) {
            try {
                seal.seal.onRemoval(world, pos.pos, pos.face);
            } catch (Exception e) {
                Thaumcraft.log.warn("Removing invalid seal at " + pos.pos);
            }
            if (!quiet) {
                ItemStack stack = getSealStack(seal.seal.getKey());
                if (!stack.isEmpty()) {
                    world.addFreshEntity(new ItemEntity(world, pos.pos.getX() + 0.5 + pos.face.getStepX() / 1.7F, pos.pos.getY() + 0.5 + pos.face.getStepY() / 1.7F,
                            pos.pos.getZ() + 0.5 + pos.face.getStepZ() / 1.7F, stack));
                }
            }
        }
        for (Task task : TaskHandler.getTasks(world.dimension()).values()) {
            if (task.getSealPos() != null && task.getSealPos().equals(pos)) {
                task.setSuspended(true);
            }
        }
        if (world instanceof ServerLevel sl) {
            sync(sl, new SealEntity(pos, null));
            if (!quiet) {
                markChunkAsDirty(sl, pos.pos);
            }
        }
    }

    public static boolean addSealEntity(Level world, BlockPos pos, Direction face, ISeal seal, Player player) {
        Map<SealPos, SealEntity> se = map(world);
        SealPos sp = new SealPos(pos, face);
        if (se.containsKey(sp)) {
            return false;
        }
        SealEntity sealent = new SealEntity(sp, seal);
        sealent.setOwner(player.getUUID().toString());
        se.put(sp, sealent);
        if (world instanceof ServerLevel sl) {
            sealent.syncToClient(sl);
            markChunkAsDirty(sl, pos);
        }
        return true;
    }

    public static boolean addSealEntity(Level world, SealEntity seal) {
        if (world == null || seal.getSealPos() == null) {
            return false;
        }
        Map<SealPos, SealEntity> se = map(world);
        if (se.containsKey(seal.getSealPos())) {
            return false;
        }
        se.put(seal.getSealPos(), seal);
        if (world instanceof ServerLevel sl) {
            seal.syncToClient(sl);
            markChunkAsDirty(sl, seal.getSealPos().pos);
        }
        return true;
    }

    /** Тик печатей загруженных чанков; раз в секунду снимает те, что больше нельзя ставить на это место. */
    public static void tickSealEntities(ServerLevel world) {
        count++;
        for (SealEntity sealEntity : new ArrayList<>(map(world).values())) {
            if (world.isLoaded(sealEntity.sealPos.pos)) {
                try {
                    boolean tick = true;
                    if (count % 20 == 0 && !sealEntity.seal.canPlaceAt(world, sealEntity.sealPos.pos, sealEntity.sealPos.face)) {
                        removeSealEntity(world, sealEntity.sealPos, false);
                        tick = false;
                    }
                    if (tick) {
                        sealEntity.tickSealEntity(world);
                    }
                } catch (Exception e) {
                    removeSealEntity(world, sealEntity.sealPos, false);
                }
            }
        }
    }

    /** Данные чанка с печатью изменились — переписать копию печатей во вложении чанка (сохранится с чанком). */
    public static void markChunkAsDirty(ServerLevel world, BlockPos bp) {
        if (world.isLoaded(bp)) {
            writeChunk(world, world.getChunkAt(bp));
        }
    }

    /** chunkSave оригинала: печати чанка → вложение SEALS. */
    public static void writeChunk(ServerLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (SealEntity seal : getSealsInChunk(world, chunk.getPos())) {
            list.add(seal.writeNBT(world.registryAccess()));
        }
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.put("seals", list);
        chunk.setData(thaumcraft.common.lib.capabilities.ThaumcraftAttachments.SEALS, tag);
        chunk.setUnsaved(true);
    }

    /** chunkLoad оригинала: вложение SEALS → печати в памяти. */
    public static void readChunk(ServerLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        if (!chunk.hasData(thaumcraft.common.lib.capabilities.ThaumcraftAttachments.SEALS)) {
            return;
        }
        net.minecraft.nbt.ListTag list = chunk.getData(thaumcraft.common.lib.capabilities.ThaumcraftAttachments.SEALS).getList("seals", 10);
        Map<SealPos, SealEntity> m = map(world);
        for (int a = 0; a < list.size(); a++) {
            SealEntity seal = new SealEntity();
            seal.readNBT(list.getCompound(a), world.registryAccess());
            if (seal.getSeal() != null && seal.getSealPos() != null) {
                m.putIfAbsent(seal.getSealPos(), seal);
            }
        }
    }

    /** Разослать состояние печати игрокам, видящим её чанк (sendToDimension оригинала — шире, чем нужно). */
    public static void sync(ServerLevel world, SealEntity seal) {
        PacketSealToClient pkt = new PacketSealToClient(seal.writeNBT(world.registryAccess()));
        PacketDistributor.sendToPlayersTrackingChunk(world, new ChunkPos(seal.getSealPos().pos), pkt);
    }

    public static void sendChunkTo(ServerPlayer player, ChunkPos chunk) {
        for (SealEntity seal : getSealsInChunk(player.serverLevel(), chunk)) {
            PacketDistributor.sendToPlayer(player, new PacketSealToClient(seal.writeNBT(player.registryAccess())));
        }
    }

    /** Чанк выгружается — печати записываются во вложение и убираются из памяти. */
    public static void unloadChunk(ServerLevel world, net.minecraft.world.level.chunk.LevelChunk lc) {
        writeChunk(world, lc);
        ChunkPos chunk = lc.getPos();
        Map<SealPos, SealEntity> m = sealEntities.get(world.dimension());
        if (m != null) {
            m.values().removeIf(se -> se.getSealPos() != null && new ChunkPos(se.sealPos.pos).equals(chunk));
        }
    }

    /** Записать печати всех загруженных чанков в их вложения. */
    public static void saveAll(ServerLevel world) {
        java.util.Set<ChunkPos> chunks = new java.util.HashSet<>();
        for (SealEntity se : map(world).values()) {
            if (se.getSealPos() != null) {
                chunks.add(new ChunkPos(se.sealPos.pos));
            }
        }
        for (ChunkPos cp : chunks) {
            if (world.hasChunk(cp.x, cp.z)) {
                writeChunk(world, world.getChunk(cp.x, cp.z));
            }
        }
    }

    public static void clear(Level world) {
        sealEntities.remove(world.dimension());
    }

    public static List<String> typeKeys() {
        return Collections.unmodifiableList(new ArrayList<>(types.keySet()));
    }
}
