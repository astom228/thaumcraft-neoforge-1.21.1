package thaumcraft.common.world.biomes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.api.aspects.Aspect;

/**
 * Свойства биомов для ауры и генерации (порт BiomeHandler: таблица registerBiomeInfo).
 * В 1.12 биомы описывались типами BiomeDictionary, в 1.21 — тегами c:is_*; соответствие задано в {@link Type}.
 */
public class BiomeHandler {
    /** Типы BiomeDictionary 1.12 и соответствующие им теги биомов 1.21. */
    public enum Type {
        HOT(Tags.Biomes.IS_HOT), COLD(Tags.Biomes.IS_COLD), SPARSE(Tags.Biomes.IS_SPARSE_VEGETATION), DENSE(Tags.Biomes.IS_DENSE_VEGETATION),
        WET(Tags.Biomes.IS_WET), DRY(Tags.Biomes.IS_DRY), SAVANNA(Tags.Biomes.IS_SAVANNA), CONIFEROUS(Tags.Biomes.IS_CONIFEROUS_TREE),
        JUNGLE(Tags.Biomes.IS_JUNGLE), SPOOKY(Tags.Biomes.IS_SPOOKY), DEAD(Tags.Biomes.IS_DEAD), LUSH(Tags.Biomes.IS_LUSH),
        NETHER(Tags.Biomes.IS_NETHER), END(Tags.Biomes.IS_END), MUSHROOM(Tags.Biomes.IS_MUSHROOM), MAGICAL(Tags.Biomes.IS_MAGICAL),
        RARE(Tags.Biomes.IS_RARE), PLATEAU(Tags.Biomes.IS_PLATEAU), MODIFIED(Tags.Biomes.IS_MODIFIED), OCEAN(Tags.Biomes.IS_OCEAN),
        RIVER(Tags.Biomes.IS_RIVER), WATER(Tags.Biomes.IS_AQUATIC), MESA(Tags.Biomes.IS_BADLANDS), FOREST(Tags.Biomes.IS_FOREST),
        PLAINS(Tags.Biomes.IS_PLAINS), MOUNTAIN(Tags.Biomes.IS_MOUNTAIN), HILLS(Tags.Biomes.IS_HILL), SWAMP(Tags.Biomes.IS_SWAMP),
        SANDY(Tags.Biomes.IS_SANDY), SNOWY(Tags.Biomes.IS_SNOWY), WASTELAND(Tags.Biomes.IS_WASTELAND), BEACH(Tags.Biomes.IS_BEACH),
        VOID(Tags.Biomes.IS_VOID);

        final TagKey<Biome> tag;

        Type(TagKey<Biome> tag) {
            this.tag = tag;
        }
    }

    /** auraLevel, аспект, растёт ли великое дерево, шанс великого дерева. */
    public record BiomeInfo(float auraLevel, Aspect tag, boolean greatwood, float greatwoodChance) {
    }

    public static final Map<Type, BiomeInfo> biomeInfo = new EnumMap<>(Type.class);

    public static void registerBiomeInfo(Type type, float auraLevel, Aspect tag, boolean greatwood, float greatwoodchance) {
        biomeInfo.put(type, new BiomeInfo(auraLevel, tag, greatwood, greatwoodchance));
    }

    /** Типы биома (как BiomeDictionary.getTypes). */
    public static List<Type> getTypes(Holder<Biome> biome) {
        List<Type> out = new ArrayList<>();
        for (Type t : Type.values()) {
            if (biome.is(t.tag)) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * Среднее auraLevel по типам биома. Как в оригинале: если у какого-то типа нет записи (RARE, PLATEAU, MODIFIED, VOID),
     * возникало исключение и возвращалось 0.5; у биома без типов — 0/0 (NaN, база ауры становится 0).
     */
    public static float getBiomeAuraModifier(Holder<Biome> biome) {
        float average = 0.0F;
        int count = 0;
        for (Type type : getTypes(biome)) {
            BiomeInfo info = biomeInfo.get(type);
            if (info == null) {
                return 0.5F;
            }
            average += info.auraLevel();
            count++;
        }
        return average / count;
    }

    public static Aspect getRandomBiomeTag(Holder<Biome> biome, RandomSource random) {
        List<Type> types = getTypes(biome);
        if (types.isEmpty()) {
            return null;
        }
        BiomeInfo info = biomeInfo.get(types.get(random.nextInt(types.size())));
        return info != null ? info.tag() : null;
    }

    public static float getBiomeSupportsGreatwood(Holder<Biome> biome) {
        for (Type type : getTypes(biome)) {
            BiomeInfo info = biomeInfo.get(type);
            if (info == null) {
                return 0.0F;
            }
            if (info.greatwood()) {
                return info.greatwoodChance();
            }
        }
        return 0.0F;
    }

    /** registerBiomes оригинала (часть с таблицей; регистрация биомов Thaumcraft — этап 17). Порядок важен: DRY записан дважды. */
    public static void registerBiomeInfo() {
        registerBiomeInfo(Type.WATER, 0.33F, Aspect.WATER, false, 0.0F);
        registerBiomeInfo(Type.OCEAN, 0.33F, Aspect.WATER, false, 0.0F);
        registerBiomeInfo(Type.RIVER, 0.4F, Aspect.WATER, false, 0.0F);
        registerBiomeInfo(Type.WET, 0.4F, Aspect.WATER, false, 0.0F);
        registerBiomeInfo(Type.LUSH, 0.5F, Aspect.WATER, true, 0.5F);
        registerBiomeInfo(Type.HOT, 0.33F, Aspect.FIRE, false, 0.0F);
        registerBiomeInfo(Type.DRY, 0.25F, Aspect.FIRE, false, 0.0F);
        registerBiomeInfo(Type.NETHER, 0.125F, Aspect.FIRE, false, 0.0F);
        registerBiomeInfo(Type.MESA, 0.33F, Aspect.FIRE, false, 0.0F);
        registerBiomeInfo(Type.SPOOKY, 0.5F, Aspect.FIRE, false, 0.0F);
        registerBiomeInfo(Type.DENSE, 0.4F, Aspect.ORDER, false, 0.0F);
        registerBiomeInfo(Type.SNOWY, 0.25F, Aspect.ORDER, false, 0.0F);
        registerBiomeInfo(Type.COLD, 0.25F, Aspect.ORDER, false, 0.0F);
        registerBiomeInfo(Type.MUSHROOM, 0.75F, Aspect.ORDER, false, 0.0F);
        registerBiomeInfo(Type.MAGICAL, 0.75F, Aspect.ORDER, true, 1.0F);
        registerBiomeInfo(Type.CONIFEROUS, 0.33F, Aspect.EARTH, true, 0.2F);
        registerBiomeInfo(Type.FOREST, 0.5F, Aspect.EARTH, true, 1.0F);
        registerBiomeInfo(Type.SANDY, 0.25F, Aspect.EARTH, false, 0.0F);
        registerBiomeInfo(Type.BEACH, 0.3F, Aspect.EARTH, false, 0.0F);
        registerBiomeInfo(Type.JUNGLE, 0.6F, Aspect.EARTH, false, 0.0F);
        registerBiomeInfo(Type.SAVANNA, 0.25F, Aspect.AIR, true, 0.2F);
        registerBiomeInfo(Type.MOUNTAIN, 0.3F, Aspect.AIR, false, 0.0F);
        registerBiomeInfo(Type.HILLS, 0.33F, Aspect.AIR, false, 0.0F);
        registerBiomeInfo(Type.PLAINS, 0.3F, Aspect.AIR, true, 0.2F);
        registerBiomeInfo(Type.END, 0.125F, Aspect.AIR, false, 0.0F);
        registerBiomeInfo(Type.DRY, 0.125F, Aspect.ENTROPY, false, 0.0F);
        registerBiomeInfo(Type.SPARSE, 0.2F, Aspect.ENTROPY, false, 0.0F);
        registerBiomeInfo(Type.SWAMP, 0.5F, Aspect.ENTROPY, true, 0.2F);
        registerBiomeInfo(Type.WASTELAND, 0.125F, Aspect.ENTROPY, false, 0.0F);
        registerBiomeInfo(Type.DEAD, 0.1F, Aspect.ENTROPY, false, 0.0F);
    }

    static {
        registerBiomeInfo();
    }
}
