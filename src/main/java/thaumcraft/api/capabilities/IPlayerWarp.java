package thaumcraft.api.capabilities;

import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Искажение игрока: постоянное, обычное и временное (0..500) и счётчик. Порт IPlayerWarp оригинала. */
public interface IPlayerWarp extends INBTSerializable<CompoundTag> {
    void clear();

    int get(@Nonnull EnumWarpType type);

    void set(@Nonnull EnumWarpType type, int amount);

    int add(@Nonnull EnumWarpType type, int amount);

    int reduce(@Nonnull EnumWarpType type, int amount);

    void sync(ServerPlayer player);

    int getCounter();

    void setCounter(int amount);

    enum EnumWarpType {
        PERMANENT,
        NORMAL,
        TEMPORARY
    }
}
