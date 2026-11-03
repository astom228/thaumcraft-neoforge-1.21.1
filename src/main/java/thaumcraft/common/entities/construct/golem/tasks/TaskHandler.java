package thaumcraft.common.entities.construct.golem.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.seals.SealHandler;

/**
 * Задачи големов (порт TaskHandler), только сервер, по измерениям; не больше 1000 в измерении. Големы выбирают
 * ближайшие с учётом приоритета (−256 к квадрату расстояния за единицу приоритета).
 */
public class TaskHandler {
    static final int TASK_LIMIT = 1000;
    private static final Map<ResourceKey<Level>, Map<Integer, Task>> tasks = new ConcurrentHashMap<>();

    public static void addTask(ResourceKey<Level> dim, Task ticket) {
        Map<Integer, Task> dc = getTasks(dim);
        if (dc.size() >= TASK_LIMIT && !dc.containsKey(ticket.getId())) {
            Iterator<Task> i = dc.values().iterator();
            if (i.hasNext()) {
                i.next();
                i.remove();
            }
        }
        dc.put(ticket.getId(), ticket);
    }

    public static Task getTask(ResourceKey<Level> dim, int id) {
        return getTasks(dim).get(id);
    }

    public static Map<Integer, Task> getTasks(ResourceKey<Level> dim) {
        return tasks.computeIfAbsent(dim, k -> new ConcurrentHashMap<>());
    }

    private static List<Task> sorted(ResourceKey<Level> dim, UUID uuid, Entity golem, byte type) {
        List<Task> out = new ArrayList<>();
        for (Task ticket : getTasks(dim).values()) {
            if (ticket.isReserved() || ticket.getType() != type || uuid != null && ticket.getGolemUUID() != null && !uuid.equals(ticket.getGolemUUID())) {
                continue;
            }
            if (type == 1 && (ticket.getEntity() == null || !ticket.getEntity().isAlive())) {
                ticket.setSuspended(true);
                continue;
            }
            out.add(ticket);
        }
        out.sort(java.util.Comparator.comparingDouble(ticket -> ticket.getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ()) - ticket.getPriority() * 256));
        return out;
    }

    public static List<Task> getBlockTasksSorted(ResourceKey<Level> dim, UUID uuid, Entity golem) {
        return sorted(dim, uuid, golem, (byte) 0);
    }

    public static List<Task> getEntityTasksSorted(ResourceKey<Level> dim, UUID uuid, Entity golem) {
        return sorted(dim, uuid, golem, (byte) 1);
    }

    public static <T extends LivingEntity & IGolemAPI> void completeTask(Task task, T golem) {
        if (!task.isCompleted() && !task.isSuspended()) {
            ISealEntity se = SealHandler.getSealEntity(golem.level().dimension(), task.getSealPos());
            if (se != null) {
                task.setCompletion(se.getSeal().onTaskCompletion(golem.level(), golem, task));
            } else {
                task.setCompletion(true);
            }
        }
    }

    /** Раз в секунду: убывает срок жизни задач; приостановленные и истёкшие убираются (печать узнаёт об этом). */
    public static void clearSuspendedOrExpiredTasks(Level world) {
        Map<Integer, Task> tickets = getTasks(world.dimension());
        Map<Integer, Task> temp = new ConcurrentHashMap<>();
        for (Task ticket : tickets.values()) {
            if (!ticket.isSuspended() && ticket.getLifespan() > 0L) {
                ticket.setLifespan((short) (ticket.getLifespan() - 1L));
                temp.put(ticket.getId(), ticket);
            } else {
                ISealEntity sEnt = SealHandler.getSealEntity(world.dimension(), ticket.getSealPos());
                if (sEnt != null) {
                    sEnt.getSeal().onTaskSuspension(world, ticket);
                }
            }
        }
        tasks.put(world.dimension(), temp);
    }

    public static void clear(Level world) {
        tasks.remove(world.dimension());
    }
}
