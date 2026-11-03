package thaumcraft.api.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.thread.EffectiveSide;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IThaumcraftRecipe;

/**
 * Внутренние таблицы API (порт CommonInternals).
 * objectTags: в оригинале ключ — hashCode строки NBT стека (id + metadata + NBT). В порте ключ — предмет + компоненты стека
 * без повреждения (metadata-«повреждение» в 1.12 обходилось подстановкой 32767). Таблицы отдельные для логического сервера
 * и клиента (в 1.12 рецепты и OreDictionary были статичны; в 1.21 теги и рецепты загружаются отдельно на каждой стороне).
 */
public class CommonInternals {
    /** Искажение за создание предмета (ThaumcraftApi.addWarpToItem); в оригинале ключ — предмет + metadata. */
    public static final Map<Item, Integer> warpMap = new ConcurrentHashMap<>();
    /** Файлы JSON исследований (ThaumcraftApi.registerResearchLocation); порядок регистрации сохраняется. */
    public static final Map<String, ResourceLocation> jsonLocs = Collections.synchronizedMap(new LinkedHashMap<>());
    public static final ArrayList<ThaumcraftApi.EntityTags> scanEntities = new ArrayList<>();
    /** Бонусы плавки адской печи (ThaumcraftApi.addSmeltingBonus); заполняются при запуске. */
    public static final java.util.List<ThaumcraftApi.SmeltBonus> smeltingBonus = new java.util.concurrent.CopyOnWriteArrayList<>();
    /**
     * Каталог рецептов Thaumcraft (тигель, наполнение, многоблочные схемы); заполняется при загрузке кодом на обеих сторонах.
     * В оригинале HashMap — в порте порядок регистрации сохраняется (выбор среди равных рецептов не зависит от хешей).
     */
    public static final Map<ResourceLocation, IThaumcraftRecipe> craftingRecipeCatalog = Collections.synchronizedMap(new LinkedHashMap<>());

    public static IThaumcraftRecipe getCatalogRecipe(ResourceLocation key) {
        return craftingRecipeCatalog.get(key);
    }

    /** Рецепты только для показа в книге (ThaumcraftApi.addFakeCraftingRecipe); заполняется при загрузке. */
    public static final Map<ResourceLocation, Object> craftingRecipeCatalogFake = new ConcurrentHashMap<>();

    public static Object getCatalogRecipeFake(ResourceLocation key) {
        return craftingRecipeCatalogFake.get(key);
    }

    /** Ключ таблицы аспектов: предмет + компоненты (без DAMAGE). */
    public record ItemKey(Item item, DataComponentPatch components) {
        @Override
        public String toString() {
            return BuiltInRegistries.ITEM.getKey(this.item) + (this.components.isEmpty() ? "" : this.components.toString());
        }
    }

    /** Состояние одной логической стороны: таблица аспектов и данные для генерации из рецептов. */
    public static final class SideState {
        public final Map<ItemKey, AspectList> objectTags = new ConcurrentHashMap<>();
        /** Вычисляемые цены postAspects; отдельный снимок для сервера и клиента, пересобирается вместе с аспектами. */
        public volatile Map<String, AspectList> derivedCrucibleCosts = Map.of();
        public volatile RecipeManager recipes;
        public volatile RegistryAccess registries;
    }

    private static final SideState SERVER = new SideState();
    private static final SideState CLIENT = new SideState();

    /** Явно заданная сторона (на время построения таблицы: начальная загрузка сервера в одиночной игре идёт в потоке клиента). */
    private static final ThreadLocal<SideState> OVERRIDE = new ThreadLocal<>();

    public static SideState state() {
        SideState o = OVERRIDE.get();
        if (o != null) {
            return o;
        }
        return EffectiveSide.get() == LogicalSide.CLIENT ? CLIENT : SERVER;
    }

    /** Выполнить действие от имени указанной стороны. */
    public static void withSide(LogicalSide side, Runnable action) {
        SideState prev = OVERRIDE.get();
        OVERRIDE.set(state(side));
        try {
            action.run();
        } finally {
            if (prev == null) {
                OVERRIDE.remove();
            } else {
                OVERRIDE.set(prev);
            }
        }
    }

    public static SideState state(LogicalSide side) {
        return side == LogicalSide.CLIENT ? CLIENT : SERVER;
    }

    public static Map<ItemKey, AspectList> objectTags() {
        return state().objectTags;
    }

    /** Завершение сеанса: освободить рецепты/реестры мира, не затрагивая другую логическую сторону. */
    public static void clearSide(LogicalSide side) {
        SideState state = state(side);
        state.objectTags.clear();
        state.derivedCrucibleCosts = Map.of();
        state.recipes = null;
        state.registries = null;
    }

    /** generateUniqueItemstackId: предмет + компоненты стека (без повреждения). */
    public static ItemKey generateUniqueItemstackId(ItemStack stack) {
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.get(DataComponents.DAMAGE) != null) {
            patch = patch.forget(t -> t == DataComponents.DAMAGE);
        }
        return new ItemKey(stack.getItem(), patch);
    }

    /** generateUniqueItemstackIdStripped: только предмет (в оригинале — без NBT). */
    public static ItemKey generateUniqueItemstackIdStripped(ItemStack stack) {
        return new ItemKey(stack.getItem(), DataComponentPatch.EMPTY);
    }
}
