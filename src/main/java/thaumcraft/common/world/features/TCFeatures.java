package thaumcraft.common.world.features;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

/**
 * Фичи генерации мира (порт ThaumcraftWorldGenerator). Размещение — data/thaumcraft/worldgen (по разу на чанк) и
 * модификаторы биомов data/thaumcraft/neoforge/biome_modifier. Настройки generate* и oreDensity читаются при генерации.
 */
public final class TCFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> ORES = FEATURES.register("ores", () -> new FeatureOres(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> VEGETATION = FEATURES.register("vegetation",
            () -> new FeatureVegetation(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> MAGICAL_FOREST = FEATURES.register("magical_forest",
            () -> new FeatureMagicalForest(NoneFeatureConfiguration.CODEC, false));
    public static final Supplier<Feature<NoneFeatureConfiguration>> MAGICAL_FOREST_POST = FEATURES.register("magical_forest_post",
            () -> new FeatureMagicalForest(NoneFeatureConfiguration.CODEC, true));

    public static final Supplier<Feature<NoneFeatureConfiguration>> SURFACE = FEATURES.register("surface", () -> new FeatureSurface(NoneFeatureConfiguration.CODEC));

    private TCFeatures() {
    }
}
