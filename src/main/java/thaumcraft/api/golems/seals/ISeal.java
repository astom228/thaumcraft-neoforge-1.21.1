package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.tasks.Task;

/** Тип печати управления (порт ISeal): ставит задачи, проверяет голема, хранит свои настройки. */
public interface ISeal {
    String getKey();

    boolean canPlaceAt(Level world, BlockPos pos, Direction side);

    void tickSeal(Level world, ISealEntity seal);

    void onTaskStarted(Level world, IGolemAPI golem, Task task);

    boolean onTaskCompletion(Level world, IGolemAPI golem, Task task);

    void onTaskSuspension(Level world, Task task);

    boolean canGolemPerformTask(IGolemAPI golem, Task task);

    void readCustomNBT(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries);

    void writeCustomNBT(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries);

    ResourceLocation getSealIcon();

    void onRemoval(Level world, BlockPos pos, Direction side);

    /** Меню настройки (на сервере); экран — ClientSeals по типу меню. */
    Object returnContainer(Level world, Player player, BlockPos pos, Direction side, ISealEntity seal);

    EnumGolemTrait[] getRequiredTags();

    EnumGolemTrait[] getForbiddenTags();
}
