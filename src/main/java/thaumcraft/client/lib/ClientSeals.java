package thaumcraft.client.lib;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.entities.construct.golem.seals.SealEntity;

/**
 * Печати, известные клиенту (в оригинале — общий с сервером SealHandler.sealEntities): только для отрисовки и окон
 * настройки. Сбрасываются при смене мира.
 */
public final class ClientSeals {
    private static final Map<SealPos, SealEntity> SEALS = new ConcurrentHashMap<>();

    private ClientSeals() {
    }

    public static void handle(CompoundTag data) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        SealEntity seal = new SealEntity();
        seal.readNBT(data, level.registryAccess());
        if (seal.getSealPos() == null) {
            return;
        }
        if ("REMOVE".equals(data.getString("type")) || seal.getSeal() == null) {
            SEALS.remove(seal.getSealPos());
        } else {
            SEALS.put(seal.getSealPos(), seal);
        }
    }

    public static SealEntity get(SealPos pos) {
        return SEALS.get(pos);
    }

    public static Collection<SealEntity> all() {
        return SEALS.values();
    }

    public static void clear() {
        SEALS.clear();
    }
}
