package thaumcraft.common.world;

import com.mojang.serialization.MapCodec;
import java.util.Calendar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityWisp;

/**
 * Естественное появление существ (postInitEntitySpawns оригинала). Модификатор биомов «thaumcraft:mob_spawns» читает
 * настройки allowSpawn* при загрузке мира: злобные зомби — во всех биомах Верхнего мира с монстрами (вес 10), пехи —
 * в волшебных биомах (10), огненные летучие мыши — в Незере (10, по 1–2) и 31 октября по всему Верхнему миру (5),
 * огоньки — в Незере (5). Биомы Thaumcraft (свои спавны стражей, щупалец, пехов) — этап 17.
 */
public final class MobSpawns {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
            Thaumcraft.MODID);
    /** Один экземпляр кодека: реестр сериализаторов ищет его по ссылке. */
    public static final MapCodec<Modifier> CODEC = MapCodec.unit(Modifier.INSTANCE);

    static {
        SERIALIZERS.register("mob_spawns", () -> CODEC);
    }

    private MobSpawns() {
    }

    public static final class Modifier implements BiomeModifier {
        public static final Modifier INSTANCE = new Modifier();

        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase != Phase.ADD) {
                return;
            }
            var spawns = builder.getMobSpawnSettings();
            boolean hasMonsters = !spawns.getSpawner(MobCategory.MONSTER).isEmpty();
            boolean overworld = biome.is(BiomeTags.IS_OVERWORLD);
            boolean nether = biome.is(Tags.Biomes.IS_NETHER);
            if (ModConfig.CONFIG_WORLD.allowSpawnAngryZombie.get() && overworld && hasMonsters) {
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ConfigEntities.BRAINY_ZOMBIE.get(), 10, 1, 1));
            }
            if (ModConfig.CONFIG_WORLD.allowSpawnPech.get() && biome.is(Tags.Biomes.IS_MAGICAL) && hasMonsters) {
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ConfigEntities.PECH.get(), 10, 1, 1));
            }
            if (ModConfig.CONFIG_WORLD.allowSpawnFireBat.get()) {
                if (nether) {
                    spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ConfigEntities.FIRE_BAT.get(), 10, 1, 2));
                }
                Calendar calendar = Calendar.getInstance();
                if (overworld && calendar.get(Calendar.MONTH) + 1 == 10 && calendar.get(Calendar.DAY_OF_MONTH) == 31) {
                    spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ConfigEntities.FIRE_BAT.get(), 5, 1, 2));
                }
            }
            if (ModConfig.CONFIG_WORLD.allowSpawnWisp.get() && nether) {
                spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ConfigEntities.WISP.get(), 5, 1, 1));
            }
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return CODEC;
        }
    }

    /** Правила размещения (getCanSpawnHere оригиналов). */
    public static void registerPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ConfigEntities.BRAINY_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ConfigEntities.PECH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MobSpawns::checkPech,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ConfigEntities.FIRE_BAT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MobSpawns::checkFireBat,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ConfigEntities.WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MobSpawns::checkWisp,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ConfigEntities.ELDRITCH_GUARDIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityEldritchGuardian::checkGuardianSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Пех: не больше 4 в 16 блоках, волшебный биом, только в Верхнем мире (биомы Thaumcraft — этап 17), правила монстров. */
    private static boolean checkPech(EntityType<EntityPech> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource rand) {
        if (!level.getBiome(pos).is(Tags.Biomes.IS_MAGICAL) || level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }
        return level.getEntitiesOfClass(EntityPech.class, new AABB(pos).inflate(16.0)).size() < 4 && Monster.checkMonsterSpawnRules(type, level, reason, pos, rand);
    }

    /** Огненная мышь: светлее случайного 0–6 — нельзя; дальше — правила монстров. */
    private static <T extends Monster> boolean checkFireBat(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource rand) {
        return level.getMaxLocalRawBrightness(pos) <= rand.nextInt(7) && Monster.checkMonsterSpawnRules(type, level, reason, pos, rand);
    }

    /** Огонёк: не больше 8 в 16 блоках, не на мирной сложности, достаточно темно. */
    private static boolean checkWisp(EntityType<EntityWisp> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource rand) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getEntitiesOfClass(EntityWisp.class, new AABB(pos).inflate(16.0)).size() < 8
                && Monster.isDarkEnoughToSpawn(level, pos, rand);
    }
}
