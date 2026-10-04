package thaumcraft.common.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import thaumcraft.Thaumcraft;

/**
 * Генерация данных (gradlew runData → src/generated/resources): таблицы добычи, теги, рецепты.
 * Blockstates и модели не генерируются — их конвертирует tools/assets/convert-assets.js из ресурсов оригинала.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(TCBlockLootTables::new, LootContextParamSets.BLOCK)), lookup));
        TCBlockTagsProvider blockTags = generator.addProvider(event.includeServer(), new TCBlockTagsProvider(output, lookup, helper));
        generator.addProvider(event.includeServer(), new TCItemTagsProvider(output, lookup, blockTags.contentsGetter(), helper));
    }
}
