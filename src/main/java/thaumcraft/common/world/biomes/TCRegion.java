package thaumcraft.common.world.biomes;

import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ModConfig;

/**
 * Магический лес в Верхнем мире (BiomeManager.addBiome WARM/COOL с весом biomeMagicalForestWeight). В 1.21 биомы
 * раскладываются по климату, поэтому — регион TerraBlender с весом из настроек, где лес и березняк заменены Магическим
 * лесом (умеренный климат, как WARM/COOL оригинала). Зловещие земли и Внешние земли не размещаются (DECISIONS 11).
 */
public class TCRegion extends Region {
    public TCRegion(int weight) {
        super(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "overworld"), RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.FOREST, BiomeHandler.MAGICAL_FOREST);
            builder.replaceBiome(Biomes.BIRCH_FOREST, BiomeHandler.MAGICAL_FOREST);
        });
    }

    /** generateMagicForest и вес — настройки, требующие перезапуска игры. */
    public static void register() {
        int weight = ModConfig.CONFIG_WORLD.biomeMagicalForestWeight.get();
        if (ModConfig.CONFIG_WORLD.generateMagicForest.get() && weight > 0) {
            Regions.register(new TCRegion(weight));
        }
    }
}
