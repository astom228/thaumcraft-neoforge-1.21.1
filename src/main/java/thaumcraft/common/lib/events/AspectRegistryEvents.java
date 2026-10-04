package thaumcraft.common.lib.events;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.common.config.ConfigAspects;

/**
 * Построение таблицы аспектов предметов (в оригинале — ConfigAspects.postInit, один раз при запуске).
 * В 1.21 теги и рецепты — данные, поэтому таблица строится после их загрузки: на сервере — при загрузке/перезагрузке данных,
 * на клиенте — после получения рецептов от сервера (ClientAspectEvents). Сгенерированные из рецептов аспекты кешируются
 * в той же таблице и сбрасываются при перестроении.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class AspectRegistryEvents {
    private static RecipeManager pendingRecipes;
    private static RegistryAccess pendingRegistries;

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        pendingRecipes = event.getServerResources().getRecipeManager();
        pendingRegistries = event.getRegistryAccess();
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD && pendingRecipes != null) {
            build(LogicalSide.SERVER, pendingRecipes, pendingRegistries);
        }
    }

    public static void build(LogicalSide side, RecipeManager recipes, RegistryAccess registries) {
        CommonInternals.SideState state = CommonInternals.state(side);
        long start = System.currentTimeMillis();
        CommonInternals.withSide(side, () -> {
            state.recipes = recipes;
            state.registries = registries;
            state.objectTags.clear();
            try {
                ConfigAspects.initItems();
            } catch (Exception e) {
                Thaumcraft.log.error("Failed to build aspect table", e);
            }
        });
        Thaumcraft.log.info("Aspect table ({}) built: {} entries in {} ms", side, state.objectTags.size(), System.currentTimeMillis() - start);
        String dump = System.getenv("THAUMCRAFT_DUMP_ASPECTS");
        if (dump != null && side == LogicalSide.SERVER) {
            dumpAspects(dump);
        }
    }

    /** Инструмент разработчика: аспекты всех предметов в файл (переменная окружения THAUMCRAFT_DUMP_ASPECTS = путь). */
    private static void dumpAspects(String path) {
        CommonInternals.withSide(LogicalSide.SERVER, () -> {
            StringBuilder sb = new StringBuilder();
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                var stack = new net.minecraft.world.item.ItemStack(item);
                var al = thaumcraft.common.lib.crafting.ThaumcraftCraftingManager.getObjectTags(stack);
                sb.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)).append(" =");
                if (al != null) {
                    for (var a : al.getAspects()) {
                        sb.append(' ').append(a.getTag()).append(':').append(al.getAmount(a));
                    }
                }
                sb.append('\n');
            }
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(path), sb.toString());
                Thaumcraft.log.info("Aspect dump written to {}", path);
            } catch (java.io.IOException e) {
                Thaumcraft.log.error("Aspect dump failed", e);
            }
        });
    }
}
