package thaumcraft.common.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import thaumcraft.Thaumcraft;

/** Необязательный мост к публичному API Botania; разрешение методов выполняется один раз, не в тике. */
public final class BotaniaCompat {
    private record Api(Class<?> type, MethodHandle get, MethodHandle set, Object empty, Object water) { }
    private static final Api API = findApi();
    private static boolean failed;

    private BotaniaCompat() { }

    private static Api findApi() {
        if (!ModList.get().isLoaded("botania")) return null;
        try {
            ClassLoader loader = BotaniaCompat.class.getClassLoader();
            Class<?> type = Class.forName("vazkii.botania.api.block.PetalApothecary", false, loader);
            Class<?> state = Class.forName("vazkii.botania.api.block.PetalApothecary$State", false, loader);
            var lookup = MethodHandles.publicLookup();
            return new Api(type, lookup.unreflect(type.getMethod("getFluid")), lookup.unreflect(type.getMethod("setFluid", state)),
                    state.getField("EMPTY").get(null), state.getField("WATER").get(null));
        } catch (ReflectiveOperationException | LinkageError e) {
            Thaumcraft.log.warn("Botania apothecary API is unavailable; urn integration disabled", e);
            return null;
        }
    }

    public static boolean isApothecary(BlockEntity tile) {
        return !failed && API != null && API.type().isInstance(tile);
    }

    /** Вызывать на сервере при наличии 1000 mB. Botania сама обновляет состояние блока/клиент. */
    public static boolean fillEmpty(BlockEntity tile) {
        if (!isApothecary(tile) || tile.getLevel() == null || tile.getLevel().isClientSide) return false;
        try {
            if (API.get().invoke(tile) != API.empty()) return false;
            API.set().invoke(tile, API.water());
            tile.setChanged();
            return true;
        } catch (Throwable e) {
            if (e instanceof Error error && !(e instanceof LinkageError)) throw error;
            failed = true;
            Thaumcraft.log.warn("Botania apothecary API call failed; urn integration disabled", e);
            return false;
        }
    }
}
