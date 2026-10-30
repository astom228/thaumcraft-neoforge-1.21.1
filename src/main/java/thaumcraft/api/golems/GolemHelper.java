package thaumcraft.api.golems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;

/**
 * Помощник API големов (порт GolemHelper). Заявки на доставку — по измерениям, только на сервере (в оригинале ключ —
 * номер измерения); очищаются при остановке сервера.
 */
public class GolemHelper {
    public static final Map<ResourceKey<Level>, List<ProvisionRequest>> provisionRequests = new ConcurrentHashMap<>();

    public static void registerSeal(ISeal seal) {
        ThaumcraftApi.internalMethods.registerSeal(seal);
    }

    public static ISeal getSeal(String key) {
        return ThaumcraftApi.internalMethods.getSeal(key);
    }

    public static ItemStack getSealStack(String key) {
        return ThaumcraftApi.internalMethods.getSealStack(key);
    }

    public static ISealEntity getSealEntity(ResourceKey<Level> dim, SealPos pos) {
        return ThaumcraftApi.internalMethods.getSealEntity(dim, pos);
    }

    public static void addGolemTask(ResourceKey<Level> dim, Task task) {
        ThaumcraftApi.internalMethods.addGolemTask(dim, task);
    }

    private static void add(Level world, ProvisionRequest pr) {
        if (world.isClientSide) {
            return;
        }
        List<ProvisionRequest> list = provisionRequests.computeIfAbsent(world.dimension(), k -> new ArrayList<>());
        if (!list.contains(pr)) {
            list.add(pr);
        }
    }

    public static void requestProvisioning(Level world, ISealEntity seal, ItemStack stack) {
        add(world, new ProvisionRequest(seal, stack.copy()));
    }

    public static void requestProvisioning(Level world, BlockPos pos, Direction side, ItemStack stack) {
        add(world, new ProvisionRequest(pos, side, stack.copy()));
    }

    public static void requestProvisioning(Level world, Entity entity, ItemStack stack) {
        add(world, new ProvisionRequest(entity, stack.copy()));
    }

    /** count-я позиция в зоне печати (обход по слоям, как в оригинале). */
    public static BlockPos getPosInArea(ISealEntity seal, int count) {
        Direction f = seal.getSealPos().face;
        BlockPos a = seal.getArea();
        int xx = 1 + (a.getX() - 1) * (f.getStepX() == 0 ? 2 : 1);
        int yy = 1 + (a.getY() - 1) * (f.getStepY() == 0 ? 2 : 1);
        int zz = 1 + (a.getZ() - 1) * (f.getStepZ() == 0 ? 2 : 1);
        int qx = f.getStepX() != 0 ? f.getStepX() : 1;
        int qy = f.getStepY() != 0 ? f.getStepY() : 1;
        int qz = f.getStepZ() != 0 ? f.getStepZ() : 1;
        int y = qy * (count / zz / xx) % yy + f.getStepY();
        int x = qx * (count / zz) % xx + f.getStepX();
        int z = qz * count % zz + f.getStepZ();
        return seal.getSealPos().pos.offset(x - (f.getStepX() == 0 ? xx / 2 : 0), y - (f.getStepY() == 0 ? yy / 2 : 0), z - (f.getStepZ() == 0 ? zz / 2 : 0));
    }

    /** Рамка зоны печати: вглубь от грани — на размер зоны, вдоль грани — в обе стороны. */
    public static AABB getBoundsForArea(ISealEntity seal) {
        Direction f = seal.getSealPos().face;
        BlockPos p = seal.getSealPos().pos;
        BlockPos a = seal.getArea();
        return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1)
                .move(f.getStepX(), f.getStepY(), f.getStepZ())
                .expandTowards(f.getStepX() != 0 ? (a.getX() - 1) * f.getStepX() : 0.0, f.getStepY() != 0 ? (a.getY() - 1) * f.getStepY() : 0.0,
                        f.getStepZ() != 0 ? (a.getZ() - 1) * f.getStepZ() : 0.0)
                .inflate(f.getStepX() == 0 ? a.getX() - 1 : 0.0, f.getStepY() == 0 ? a.getY() - 1 : 0.0, f.getStepZ() == 0 ? a.getZ() - 1 : 0.0);
    }
}
