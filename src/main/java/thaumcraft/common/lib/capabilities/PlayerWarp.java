package thaumcraft.common.lib.capabilities;

import javax.annotation.Nonnull;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;

/** Реализация Искажения игрока (в оригинале — PlayerWarp.DefaultImpl). Формат NBT: "warp" (int[3]), "counter". */
public class PlayerWarp implements IPlayerWarp {
    private int[] warp = new int[EnumWarpType.values().length];
    private int counter;

    @Override
    public void clear() {
        this.warp = new int[EnumWarpType.values().length];
        this.counter = 0;
    }

    @Override
    public int get(@Nonnull EnumWarpType type) {
        return this.warp[type.ordinal()];
    }

    @Override
    public void set(@Nonnull EnumWarpType type, int amount) {
        this.warp[type.ordinal()] = Mth.clamp(amount, 0, 500);
    }

    @Override
    public int add(@Nonnull EnumWarpType type, int amount) {
        this.warp[type.ordinal()] = Mth.clamp(this.warp[type.ordinal()] + amount, 0, 500);
        return this.warp[type.ordinal()];
    }

    @Override
    public int reduce(@Nonnull EnumWarpType type, int amount) {
        this.warp[type.ordinal()] = Mth.clamp(this.warp[type.ordinal()] - amount, 0, 500);
        return this.warp[type.ordinal()];
    }

    @Override
    public void sync(@Nonnull ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new PacketSyncWarp(this.serializeNBT(player.registryAccess())));
    }

    @Override
    public int getCounter() {
        return this.counter;
    }

    @Override
    public void setCounter(int amount) {
        this.counter = amount;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag properties = new CompoundTag();
        properties.putIntArray("warp", this.warp);
        properties.putInt("counter", this.getCounter());
        return properties;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag properties) {
        if (properties != null) {
            this.clear();
            int[] ba = properties.getIntArray("warp");
            int l = Math.min(EnumWarpType.values().length, ba.length);
            System.arraycopy(ba, 0, this.warp, 0, l);
            this.setCounter(properties.getInt("counter"));
        }
    }
}
