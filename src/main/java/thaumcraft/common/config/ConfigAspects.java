package thaumcraft.common.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApi.EntityTagsNBT;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.lib.TCTags;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.utils.LegacyItems;

/**
 * Аспекты предметов и существ (порт ConfigAspects; тело перенесено из оригинала с сохранением порядка и чисел).
 * Ссылки 1.12 переведены помощниками:
 * <ul>
 * <li>L("id 1.12", meta) — предмет 1.12 через DataFixer (32767 — все варианты), см. {@link LegacyItems};</li>
 * <li>T("новый id") — предмет Thaumcraft по ID_MAPPING.md; ещё не перенесённые предметы пропускаются;</li>
 * <li>O("имяOreDictionary") — тег 1.21 для категорий или конкретные предметы, которые Forge 1.12 записывал под этим именем;</li>
 * <li>entity("Имя1.12", …) — существо по имени EntityList 1.12, переведённому в id 1.21.</li>
 * </ul>
 * Таблица строится после загрузки тегов и рецептов (AspectRegistryEvents); порядок важен — aspectsOf(...) читает уже
 * зарегистрированное, как и в оригинале.
 */
public class ConfigAspects {
    /** Баннеры Thaumcraft (BlocksTC.banners) — переносятся на этапе 14. */
    private static final List<String> BANNERS = List.of("white_banner", "orange_banner", "magenta_banner", "light_blue_banner",
            "yellow_banner", "lime_banner", "pink_banner", "gray_banner", "light_gray_banner", "cyan_banner", "purple_banner",
            "blue_banner", "brown_banner", "green_banner", "red_banner", "black_banner");

    /** Аспекты существ: регистрируются один раз (не зависят от данных мира). */
    public static void initEntities() {
        registerEntityAspects();
    }

    /** Аспекты предметов: перестраиваются при каждой загрузке тегов/рецептов. */
    public static void initItems() {
        registerItemAspects();
    }

    // --- помощники перевода ссылок 1.12 ------------------------------------------------------------------------------

    private static List<ItemStack> L(String legacyId, int meta) {
        return LegacyItems.stacks(legacyId, meta);
    }

    private static List<ItemStack> T(String... ids) {
        List<ItemStack> out = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, id);
            if (BuiltInRegistries.ITEM.containsKey(rl)) {
                out.add(new ItemStack(BuiltInRegistries.ITEM.get(rl)));
            }
        }
        return out;
    }

    private static TagKey<Item> c(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static List<ItemStack> tag(TagKey<Item> tag) {
        return ThaumcraftApiHelper.getOresWithWildCards(tag);
    }

    private static List<ItemStack> items(Item... items) {
        List<ItemStack> out = new ArrayList<>();
        for (Item i : items) {
            out.add(new ItemStack(i));
        }
        return out;
    }

    /**
     * Имя OreDictionary 1.12 → предметы 1.21. Категории (руды, слитки, брёвна…) — теги: в OreDictionary попадали предметы
     * всех модов, в теги 1.21 тоже. Имена конкретных блоков — те предметы, что Forge 1.12 под ними регистрировал.
     */
    private static List<ItemStack> O(String ore) {
        return switch (ore) {
            case "oreLapis" -> tag(Tags.Items.ORES_LAPIS);
            case "oreDiamond" -> tag(Tags.Items.ORES_DIAMOND);
            case "gemDiamond" -> tag(Tags.Items.GEMS_DIAMOND);
            case "oreRedstone" -> tag(Tags.Items.ORES_REDSTONE);
            case "oreEmerald" -> tag(Tags.Items.ORES_EMERALD);
            case "gemEmerald" -> tag(Tags.Items.GEMS_EMERALD);
            case "oreQuartz" -> tag(Tags.Items.ORES_QUARTZ);
            case "gemQuartz" -> tag(Tags.Items.GEMS_QUARTZ);
            case "oreIron" -> tag(Tags.Items.ORES_IRON);
            case "ingotIron" -> tag(Tags.Items.INGOTS_IRON);
            case "oreGold" -> tag(Tags.Items.ORES_GOLD);
            case "ingotGold" -> tag(Tags.Items.INGOTS_GOLD);
            case "dustRedstone" -> tag(Tags.Items.DUSTS_REDSTONE);
            case "dustGlowstone" -> tag(Tags.Items.DUSTS_GLOWSTONE);
            case "glowstone" -> items(Items.GLOWSTONE);
            case "ingotCopper" -> tag(Tags.Items.INGOTS_COPPER);
            case "oreCopper" -> tag(Tags.Items.ORES_COPPER);
            case "stone" -> items(Items.STONE);
            case "stoneGranite" -> items(Items.GRANITE);
            case "stoneDiorite" -> items(Items.DIORITE);
            case "stoneAndesite" -> items(Items.ANDESITE);
            case "cobblestone" -> items(Items.COBBLESTONE);
            case "dirt" -> L("dirt", LegacyItems.WILDCARD);
            case "sand" -> L("sand", LegacyItems.WILDCARD);
            case "grass" -> items(Items.GRASS_BLOCK);
            case "endstone" -> items(Items.END_STONE);
            case "gravel" -> items(Items.GRAVEL);
            case "netherrack" -> items(Items.NETHERRACK);
            case "ingotBrickNether" -> items(Items.NETHER_BRICK);
            case "blockGlass" -> {
                List<ItemStack> g = new ArrayList<>(items(Items.GLASS));
                g.addAll(L("stained_glass", LegacyItems.WILDCARD));
                yield g;
            }
            case "obsidian" -> items(Items.OBSIDIAN);
            case "logWood" -> tag(ItemTags.LOGS);
            case "treeSapling" -> tag(ItemTags.SAPLINGS);
            case "treeLeaves" -> tag(ItemTags.LEAVES);
            case "vine" -> items(Items.VINE);
            case "cropNetherWart" -> tag(Tags.Items.CROPS_NETHER_WART);
            case "blockCactus" -> items(Items.CACTUS);
            case "sugarcane" -> items(Items.SUGAR_CANE);
            case "cropWheat" -> tag(Tags.Items.CROPS_WHEAT);
            case "cropCarrot" -> tag(Tags.Items.CROPS_CARROT);
            case "cropPotato" -> tag(Tags.Items.CROPS_POTATO);
            case "string" -> tag(Tags.Items.STRINGS);
            case "slimeball" -> tag(Tags.Items.SLIME_BALLS);
            case "leather" -> tag(Tags.Items.LEATHERS);
            case "feather" -> tag(Tags.Items.FEATHERS);
            case "bone" -> tag(Tags.Items.BONES);
            case "egg" -> tag(Tags.Items.EGGS);
            case "gunpowder" -> tag(Tags.Items.GUNPOWDERS);
            case "enderpearl" -> tag(Tags.Items.ENDER_PEARLS);
            case "netherStar" -> tag(Tags.Items.NETHER_STARS);
            case "clusterIron" -> tag(TCTags.Items.CLUSTERS_IRON);
            case "clusterGold" -> tag(TCTags.Items.CLUSTERS_GOLD);
            case "clusterCopper" -> tag(TCTags.Items.CLUSTERS_COPPER);
            case "clusterTin" -> tag(TCTags.Items.CLUSTERS_TIN);
            case "clusterSilver" -> tag(TCTags.Items.CLUSTERS_SILVER);
            case "clusterLead" -> tag(TCTags.Items.CLUSTERS_LEAD);
            case "clusterCinnabar" -> tag(TCTags.Items.CLUSTERS_CINNABAR);
            case "clusterQuartz" -> tag(TCTags.Items.CLUSTERS_QUARTZ);
            case "oreCinnabar" -> tag(TCTags.Items.ORES_CINNABAR);
            case "oreAmber" -> tag(TCTags.Items.ORES_AMBER);
            case "quicksilver" -> tag(TCTags.Items.QUICKSILVER);
            case "gemAmber" -> tag(TCTags.Items.GEMS_AMBER);
            case "ingotBrass" -> tag(TCTags.Items.INGOTS_BRASS);
            // Имена материалов других модов (в оригинале — если такие моды стоят): теги c:<вид>/<материал>
            default -> tag(c(oreToTagPath(ore)));
        };
    }

    /** dustIron → dusts/iron, ingotTin → ingots/tin, gemRuby → gems/ruby, itemRubber → items/rubber … */
    private static String oreToTagPath(String ore) {
        int i = 0;
        while (i < ore.length() && Character.isLowerCase(ore.charAt(i))) {
            i++;
        }
        String kind = ore.substring(0, i);
        String material = ore.substring(i).replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        return kind + "s/" + material;
    }

    private static AspectList aspectsOf(List<ItemStack> stacks) {
        return stacks.isEmpty() ? new AspectList() : new AspectList(stacks.get(0));
    }

    private static void obj(List<ItemStack> stacks, AspectList aspects) {
        for (ItemStack s : stacks) {
            ThaumcraftApi.registerObjectTag(s, aspects.copy());
        }
    }

    private static void obj(ItemStack stack, AspectList aspects) {
        ThaumcraftApi.registerObjectTag(stack, aspects);
    }

    private static void complex(List<ItemStack> stacks, AspectList aspects) {
        for (ItemStack s : stacks) {
            ThaumcraftApi.registerComplexObjectTag(s, aspects.copy());
        }
    }

    private static void complex(ItemStack stack, AspectList aspects) {
        ThaumcraftApi.registerComplexObjectTag(stack, aspects);
    }

    /** Имена EntityList 1.12 → id 1.21 (Ozelot в 1.12 — и оцелот, и кошка; «Guardian» с Elder=true — древний страж). */
    private static final Map<String, String[]> ENTITY_NAMES = Map.ofEntries(
            Map.entry("Zombie", new String[]{"zombie"}), Map.entry("Husk", new String[]{"husk"}), Map.entry("Giant", new String[]{"giant"}),
            Map.entry("Skeleton", new String[]{"skeleton"}), Map.entry("WitherSkeleton", new String[]{"wither_skeleton"}),
            Map.entry("Creeper", new String[]{"creeper"}), Map.entry("Horse", new String[]{"horse"}), Map.entry("Donkey", new String[]{"donkey"}),
            Map.entry("Mule", new String[]{"mule"}), Map.entry("SkeletonHorse", new String[]{"skeleton_horse"}),
            Map.entry("ZombieHorse", new String[]{"zombie_horse"}), Map.entry("Pig", new String[]{"pig"}), Map.entry("XPOrb", new String[]{"experience_orb"}),
            Map.entry("Sheep", new String[]{"sheep"}), Map.entry("Cow", new String[]{"cow"}), Map.entry("MushroomCow", new String[]{"mooshroom"}),
            Map.entry("SnowMan", new String[]{"snow_golem"}), Map.entry("Ozelot", new String[]{"ocelot", "cat"}), Map.entry("Chicken", new String[]{"chicken"}),
            Map.entry("Squid", new String[]{"squid"}), Map.entry("Wolf", new String[]{"wolf"}), Map.entry("Bat", new String[]{"bat"}),
            Map.entry("Spider", new String[]{"spider"}), Map.entry("Slime", new String[]{"slime"}), Map.entry("Ghast", new String[]{"ghast"}),
            Map.entry("PigZombie", new String[]{"zombified_piglin"}), Map.entry("Enderman", new String[]{"enderman"}),
            Map.entry("CaveSpider", new String[]{"cave_spider"}), Map.entry("Silverfish", new String[]{"silverfish"}), Map.entry("Blaze", new String[]{"blaze"}),
            Map.entry("LavaSlime", new String[]{"magma_cube"}), Map.entry("EnderDragon", new String[]{"ender_dragon"}),
            Map.entry("WitherBoss", new String[]{"wither"}), Map.entry("Witch", new String[]{"witch"}), Map.entry("Villager", new String[]{"villager"}),
            Map.entry("VillagerGolem", new String[]{"iron_golem"}), Map.entry("EnderCrystal", new String[]{"end_crystal"}),
            Map.entry("ItemFrame", new String[]{"item_frame"}), Map.entry("Painting", new String[]{"painting"}), Map.entry("Guardian", new String[]{"guardian"}),
            Map.entry("Rabbit", new String[]{"rabbit"}), Map.entry("Endermite", new String[]{"endermite"}), Map.entry("PolarBear", new String[]{"polar_bear"}),
            Map.entry("Shulker", new String[]{"shulker"}), Map.entry("EvocationIllager", new String[]{"evoker"}),
            Map.entry("VindicationIllager", new String[]{"vindicator"}), Map.entry("IllusionIllager", new String[]{"illusioner"}),
            Map.entry("Llama", new String[]{"llama"}), Map.entry("Parrot", new String[]{"parrot"}), Map.entry("Stray", new String[]{"stray"}),
            Map.entry("Vex", new String[]{"vex"}));

    private static void entity(String name, AspectList aspects, EntityTagsNBT... nbt) {
        // Древний страж: в 1.12 — Guardian с NBT Elder=true, в 1.21 — отдельное существо
        if (name.equals("Guardian") && nbt.length == 1 && nbt[0].name.equals("Elder")) {
            ThaumcraftApi.registerEntityTag("minecraft:elder_guardian", aspects);
            return;
        }
        String[] ids = ENTITY_NAMES.get(name);
        if (ids == null) {
            // существа Thaumcraft: «Thaumcraft.X» → thaumcraft:x (в т.ч. Thaumcraft.Golem → thaumcraft:golem)
            if (name.startsWith("Thaumcraft.")) {
                String n = name.substring("Thaumcraft.".length());
                String id = switch (n) {
                    case "TaintSeed" -> "taint_seed";
                    case "TaintSeedPrime" -> "taint_seed_prime";
                    case "FluxRift" -> "flux_rift";
                    default -> n.toLowerCase(java.util.Locale.ROOT);
                };
                ThaumcraftApi.registerEntityTag("thaumcraft:" + id, aspects, nbt);
                return;
            }
            ThaumcraftApi.registerEntityTag(name, aspects, nbt);
            return;
        }
        for (String id : ids) {
            ThaumcraftApi.registerEntityTag("minecraft:" + id, aspects, nbt);
        }
    }

    // --- тело оригинала ----------------------------------------------------------------------------------------------

    static void registerEntityAspects() {
        entity("Zombie", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 10).add(Aspect.EARTH, 5));
        entity("Husk", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 10).add(Aspect.FIRE, 5));
        entity("Giant", new AspectList().add(Aspect.UNDEAD, 25).add(Aspect.MAN, 15).add(Aspect.EARTH, 10));
        entity("Skeleton", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 5).add(Aspect.EARTH, 5));
        entity("WitherSkeleton", new AspectList().add(Aspect.UNDEAD, 25).add(Aspect.MAN, 5).add(Aspect.ENTROPY, 10));
        entity("Creeper", new AspectList().add(Aspect.PLANT, 15).add(Aspect.FIRE, 15));
        entity(
            "Creeper", new AspectList().add(Aspect.PLANT, 15).add(Aspect.FIRE, 15).add(Aspect.ENERGY, 15), new EntityTagsNBT("powered", (byte)1)
        );
        entity("Horse", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        entity("Donkey", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        entity("Mule", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        entity("SkeletonHorse", new AspectList().add(Aspect.BEAST, 5).add(Aspect.UNDEAD, 10).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        entity("ZombieHorse", new AspectList().add(Aspect.BEAST, 10).add(Aspect.UNDEAD, 5).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        entity("Pig", new AspectList().add(Aspect.BEAST, 10).add(Aspect.EARTH, 10).add(Aspect.DESIRE, 5));
        entity("XPOrb", new AspectList().add(Aspect.MIND, 10));
        entity("Sheep", new AspectList().add(Aspect.BEAST, 10).add(Aspect.EARTH, 10));
        entity("Cow", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 15));
        entity("MushroomCow", new AspectList().add(Aspect.BEAST, 15).add(Aspect.PLANT, 15).add(Aspect.EARTH, 15));
        entity("SnowMan", new AspectList().add(Aspect.COLD, 10).add(Aspect.MAN, 5).add(Aspect.MECHANISM, 5).add(Aspect.MAGIC, 5));
        entity("Ozelot", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ENTROPY, 10));
        entity("Chicken", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.AIR, 5));
        entity("Squid", new AspectList().add(Aspect.BEAST, 5).add(Aspect.WATER, 10));
        entity("Wolf", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 10).add(Aspect.AVERSION, 5));
        entity("Bat", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.DARKNESS, 5));
        entity("Spider", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ENTROPY, 10).add(Aspect.TRAP, 10));
        entity("Slime", new AspectList().add(Aspect.LIFE, 10).add(Aspect.WATER, 10).add(Aspect.ALCHEMY, 5));
        entity("Ghast", new AspectList().add(Aspect.UNDEAD, 15).add(Aspect.FIRE, 15));
        entity("PigZombie", new AspectList().add(Aspect.UNDEAD, 15).add(Aspect.FIRE, 15).add(Aspect.BEAST, 10));
        entity("Enderman", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MOTION, 15).add(Aspect.DESIRE, 5));
        entity("CaveSpider", new AspectList().add(Aspect.BEAST, 5).add(Aspect.DEATH, 10).add(Aspect.TRAP, 10));
        entity("Silverfish", new AspectList().add(Aspect.BEAST, 5).add(Aspect.EARTH, 10));
        entity("Blaze", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.FIRE, 15).add(Aspect.FLIGHT, 5));
        entity("LavaSlime", new AspectList().add(Aspect.WATER, 5).add(Aspect.FIRE, 10).add(Aspect.ALCHEMY, 5));
        entity(
            "EnderDragon", new AspectList().add(Aspect.ELDRITCH, 50).add(Aspect.BEAST, 30).add(Aspect.ENTROPY, 50).add(Aspect.FLIGHT, 10)
        );
        entity("WitherBoss", new AspectList().add(Aspect.UNDEAD, 50).add(Aspect.ENTROPY, 25).add(Aspect.FIRE, 25));
        entity("Witch", new AspectList().add(Aspect.MAN, 15).add(Aspect.MAGIC, 5).add(Aspect.ALCHEMY, 10));
        entity("Villager", new AspectList().add(Aspect.MAN, 15));
        entity("VillagerGolem", new AspectList().add(Aspect.METAL, 15).add(Aspect.MAN, 5).add(Aspect.MECHANISM, 5).add(Aspect.MAGIC, 5));
        entity("EnderCrystal", new AspectList().add(Aspect.ELDRITCH, 15).add(Aspect.AURA, 15).add(Aspect.LIFE, 15));
        entity("ItemFrame", new AspectList().add(Aspect.SENSES, 5).add(Aspect.CRAFT, 5));
        entity("Painting", new AspectList().add(Aspect.SENSES, 10).add(Aspect.CRAFT, 5));
        entity("Guardian", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ELDRITCH, 10).add(Aspect.WATER, 10));
        entity(
            "Guardian", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ELDRITCH, 15).add(Aspect.WATER, 15), new EntityTagsNBT("Elder", true)
        );
        entity("Rabbit", new AspectList().add(Aspect.BEAST, 5).add(Aspect.EARTH, 5).add(Aspect.MOTION, 5));
        entity("Endermite", new AspectList().add(Aspect.BEAST, 5).add(Aspect.ELDRITCH, 5).add(Aspect.MOTION, 5));
        entity("PolarBear", new AspectList().add(Aspect.BEAST, 15).add(Aspect.COLD, 10));
        entity("Shulker", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.TRAP, 5).add(Aspect.FLIGHT, 5).add(Aspect.PROTECT, 5));
        entity("EvocationIllager", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.MAGIC, 5).add(Aspect.MAN, 10));
        entity("VindicationIllager", new AspectList().add(Aspect.AVERSION, 5).add(Aspect.MAGIC, 5).add(Aspect.MAN, 10));
        entity("IllusionIllager", new AspectList().add(Aspect.SENSES, 5).add(Aspect.MAGIC, 5).add(Aspect.MAN, 10));
        entity("Llama", new AspectList().add(Aspect.BEAST, 15).add(Aspect.WATER, 5));
        entity("Parrot", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.SENSES, 5));
        entity("Stray", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 5).add(Aspect.TRAP, 5));
        entity("Vex", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.FLIGHT, 5).add(Aspect.MAGIC, 5).add(Aspect.MAN, 5));
        entity("Stray", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 5).add(Aspect.TRAP, 5));
        entity("Thaumcraft.FluxRift", new AspectList().add(Aspect.FLUX, 20).add(Aspect.ELDRITCH, 20).add(Aspect.AURA, 20));
        entity("Thaumcraft.Firebat", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.FIRE, 10));
        entity(
            "Thaumcraft.Pech",
            new AspectList().add(Aspect.MAN, 10).add(Aspect.AURA, 5).add(Aspect.EXCHANGE, 10).add(Aspect.DESIRE, 5),
            new EntityTagsNBT("PechType", (byte)0)
        );
        entity(
            "Thaumcraft.Pech",
            new AspectList().add(Aspect.MAN, 10).add(Aspect.AURA, 5).add(Aspect.EXCHANGE, 10).add(Aspect.AVERSION, 5),
            new EntityTagsNBT("PechType", (byte)1)
        );
        entity(
            "Thaumcraft.Pech",
            new AspectList().add(Aspect.MAN, 10).add(Aspect.AURA, 5).add(Aspect.EXCHANGE, 10).add(Aspect.MAGIC, 5),
            new EntityTagsNBT("PechType", (byte)2)
        );
        entity(
            "Thaumcraft.ThaumSlime", new AspectList().add(Aspect.LIFE, 5).add(Aspect.WATER, 5).add(Aspect.FLUX, 5).add(Aspect.ALCHEMY, 5)
        );
        entity(
            "Thaumcraft.BrainyZombie", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 10).add(Aspect.MIND, 5).add(Aspect.AVERSION, 5)
        );
        entity(
            "Thaumcraft.GiantBrainyZombie", new AspectList().add(Aspect.UNDEAD, 25).add(Aspect.MAN, 15).add(Aspect.MIND, 5).add(Aspect.AVERSION, 10)
        );
        entity("Thaumcraft.Taintacle", new AspectList().add(Aspect.FLUX, 15).add(Aspect.BEAST, 10));
        entity("Thaumcraft.TaintSeed", new AspectList().add(Aspect.FLUX, 20).add(Aspect.AURA, 10).add(Aspect.PLANT, 5));
        entity("Thaumcraft.TaintSeedPrime", new AspectList().add(Aspect.FLUX, 25).add(Aspect.AURA, 15).add(Aspect.PLANT, 5));
        entity("Thaumcraft.TaintacleTiny", new AspectList().add(Aspect.FLUX, 5).add(Aspect.BEAST, 5));
        entity("Thaumcraft.TaintSwarm", new AspectList().add(Aspect.FLUX, 15).add(Aspect.AIR, 5));
        entity("Thaumcraft.MindSpider", new AspectList().add(Aspect.FLUX, 5).add(Aspect.FIRE, 5));
        entity("Thaumcraft.EldritchGuardian", new AspectList().add(Aspect.ELDRITCH, 20).add(Aspect.DEATH, 20).add(Aspect.UNDEAD, 20));
        entity("Thaumcraft.CultistKnight", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.MAN, 15).add(Aspect.AVERSION, 5));
        entity("Thaumcraft.CultistCleric", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.MAN, 15).add(Aspect.AVERSION, 5));
        entity("Thaumcraft.EldritchCrab", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.BEAST, 10).add(Aspect.TRAP, 10));
        entity("Thaumcraft.InhabitedZombie", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.UNDEAD, 10).add(Aspect.MAN, 5));
        entity("Thaumcraft.TaintSeed", new AspectList().add(Aspect.PLANT, 20).add(Aspect.BEAST, 20).add(Aspect.FLUX, 20));
        entity("Thaumcraft.TaintSeedPrime", new AspectList().add(Aspect.PLANT, 30).add(Aspect.BEAST, 30).add(Aspect.FLUX, 30));
        entity("Thaumcraft.EldritchWarden", new AspectList().add(Aspect.ELDRITCH, 40).add(Aspect.DEATH, 40).add(Aspect.UNDEAD, 40));
        entity("Thaumcraft.EldritchGolem", new AspectList().add(Aspect.ELDRITCH, 40).add(Aspect.ENERGY, 40).add(Aspect.MECHANISM, 40));
        entity("Thaumcraft.CultistLeader", new AspectList().add(Aspect.ELDRITCH, 40).add(Aspect.AVERSION, 40).add(Aspect.MAN, 40));
        entity("Thaumcraft.TaintacleGiant", new AspectList().add(Aspect.ELDRITCH, 40).add(Aspect.BEAST, 40).add(Aspect.FLUX, 40));

        for (Aspect tag : Aspect.aspects.values()) {
            entity(
                "Thaumcraft.Wisp", new AspectList().add(tag, 5).add(Aspect.AURA, 5).add(Aspect.FLIGHT, 5), new EntityTagsNBT("Type", tag.getTag())
            );
        }

        entity("Thaumcraft.Golem", new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.MAN, 10).add(Aspect.MOTION, 10));
    }

    static void registerItemAspects() {
        obj(O("oreLapis"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.SENSES, 15));
        obj(O("oreDiamond"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.DESIRE, 15).add(Aspect.CRYSTAL, 15));
        obj(O("gemDiamond"), new AspectList().add(Aspect.CRYSTAL, 15).add(Aspect.DESIRE, 15));
        obj(O("oreRedstone"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENERGY, 15));
        obj(L("lit_redstone_ore", 0), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENERGY, 15));
        obj(O("oreEmerald"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.DESIRE, 10).add(Aspect.CRYSTAL, 15));
        obj(O("gemEmerald"), new AspectList().add(Aspect.CRYSTAL, 15).add(Aspect.DESIRE, 10));
        obj(O("oreQuartz"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.CRYSTAL, 10));
        obj(O("gemQuartz"), new AspectList().add(Aspect.CRYSTAL, 5));
        obj(O("oreIron"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.METAL, 15));
        obj(O("dustIron"), new AspectList().add(Aspect.METAL, 15).add(Aspect.ENTROPY, 1));
        obj(O("ingotIron"), new AspectList().add(Aspect.METAL, 15));
        obj(O("oreGold"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.METAL, 10).add(Aspect.DESIRE, 10));
        obj(O("dustGold"), new AspectList().add(Aspect.METAL, 10).add(Aspect.DESIRE, 10).add(Aspect.ENTROPY, 1));
        obj(O("ingotGold"), new AspectList().add(Aspect.METAL, 10).add(Aspect.DESIRE, 10));
        obj(L("coal_ore", 0), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENERGY, 15).add(Aspect.FIRE, 15));
        obj(L("coal", 32767), new AspectList().add(Aspect.ENERGY, 10).add(Aspect.FIRE, 10));
        obj(O("dustRedstone"), new AspectList().add(Aspect.ENERGY, 10));
        obj(O("dustGlowstone"), new AspectList().add(Aspect.SENSES, 5).add(Aspect.LIGHT, 10));
        obj(O("glowstone"), aspectsOf(L("glowstone", 0)));
        obj(O("ingotCopper"), new AspectList().add(Aspect.METAL, 10).add(Aspect.EXCHANGE, 5));
        obj(O("dustCopper"), new AspectList().add(Aspect.METAL, 10).add(Aspect.ENTROPY, 1).add(Aspect.EXCHANGE, 5));
        obj(O("oreCopper"), new AspectList().add(Aspect.METAL, 10).add(Aspect.EARTH, 5).add(Aspect.EXCHANGE, 5));
        obj(O("clusterCopper"), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.EXCHANGE, 10)
        );
        obj(O("ingotTin"), new AspectList().add(Aspect.METAL, 10).add(Aspect.CRYSTAL, 5));
        obj(O("dustTin"), new AspectList().add(Aspect.METAL, 10).add(Aspect.ENTROPY, 1).add(Aspect.CRYSTAL, 5));
        obj(O("oreTin"), new AspectList().add(Aspect.METAL, 10).add(Aspect.EARTH, 5).add(Aspect.CRYSTAL, 5));
        obj(O("clusterTin"), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.CRYSTAL, 10));
        obj(O("ingotSilver"), new AspectList().add(Aspect.METAL, 10).add(Aspect.DESIRE, 5));
        obj(O("dustSilver"), new AspectList().add(Aspect.METAL, 10).add(Aspect.ENTROPY, 1).add(Aspect.DESIRE, 5));
        obj(O("oreSilver"), new AspectList().add(Aspect.METAL, 10).add(Aspect.EARTH, 5).add(Aspect.DESIRE, 5));
        obj(O("clusterSilver"), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.DESIRE, 10));
        obj(O("ingotLead"), new AspectList().add(Aspect.METAL, 10).add(Aspect.ORDER, 5));
        obj(O("dustLead"), new AspectList().add(Aspect.METAL, 10).add(Aspect.ENTROPY, 1).add(Aspect.ORDER, 5));
        obj(O("oreLead"), new AspectList().add(Aspect.METAL, 10).add(Aspect.EARTH, 5).add(Aspect.ORDER, 5));
        obj(O("clusterLead"), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.ORDER, 10));
        obj(O("ingotBrass"), new AspectList().add(Aspect.METAL, 10).add(Aspect.TOOL, 5));
        obj(O("dustBrass"), new AspectList().add(Aspect.METAL, 10).add(Aspect.ENTROPY, 1).add(Aspect.TOOL, 5));
        obj(O("ingotBronze"), new AspectList().add(Aspect.METAL, 10).add(Aspect.TOOL, 5));
        obj(O("dustBronze"), new AspectList().add(Aspect.METAL, 10).add(Aspect.ENTROPY, 1).add(Aspect.TOOL, 5));
        obj(O("oreUranium"), new AspectList().add(Aspect.METAL, 10).add(Aspect.DEATH, 5).add(Aspect.ENERGY, 10));
        obj(O("itemDropUranium"), new AspectList().add(Aspect.METAL, 10).add(Aspect.DEATH, 5).add(Aspect.ENERGY, 10));
        obj(O("ingotUranium"), new AspectList().add(Aspect.METAL, 10).add(Aspect.DEATH, 5).add(Aspect.ENERGY, 10));
        obj(O("gemRuby"), new AspectList().add(Aspect.CRYSTAL, 10).add(Aspect.DESIRE, 10));
        obj(O("gemGreenSapphire"), new AspectList().add(Aspect.CRYSTAL, 10).add(Aspect.DESIRE, 10));
        obj(O("gemSapphire"), new AspectList().add(Aspect.CRYSTAL, 10).add(Aspect.DESIRE, 10));
        obj(O("ingotSteel"), new AspectList().add(Aspect.METAL, 15).add(Aspect.ORDER, 5));
        obj(O("itemRubber"), new AspectList().add(Aspect.MOTION, 5).add(Aspect.TOOL, 5));
        obj(O("stone"), new AspectList().add(Aspect.EARTH, 5));
        obj(O("stoneGranite"), new AspectList().add(Aspect.EARTH, 5));
        obj(O("stoneDiorite"), new AspectList().add(Aspect.EARTH, 5));
        obj(O("stoneAndesite"), new AspectList().add(Aspect.EARTH, 5));
        obj(O("cobblestone"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 1));
        obj(
            L("bedrock", 0), new AspectList().add(Aspect.VOID, 25).add(Aspect.ENTROPY, 25).add(Aspect.EARTH, 25).add(Aspect.DARKNESS, 25)
        );
        obj(O("dirt"), new AspectList().add(Aspect.EARTH, 5));
        obj(L("dirt", 2), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 1));
        obj(
            L("farmland", 32767), new AspectList().add(Aspect.EARTH, 5).add(Aspect.WATER, 2).add(Aspect.ORDER, 2)
        );
        obj(O("sand"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 5));
        obj(O("grass"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 2));
        obj(L("grass_path", 0), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 2).add(Aspect.ORDER, 2));
        obj(O("endstone"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.DARKNESS, 5));
        obj(O("gravel"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 2));
        obj(L("mycelium", 0), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 1).add(Aspect.FLUX, 1));
        obj(L("clay_ball", 32767), new AspectList().add(Aspect.WATER, 5).add(Aspect.EARTH, 5));
        obj(
            L("hardened_clay", 32767), aspectsOf(L("clay", 0)).add(Aspect.FIRE, 1)
        );
        obj(
            L("stained_hardened_clay", 32767), aspectsOf(L("clay", 0)).add(Aspect.FIRE, 1).add(Aspect.SENSES, 1)
        );
        obj(L("brick", 32767), aspectsOf(L("clay_ball", 0)).add(Aspect.FIRE, 1));
        obj(O("netherrack"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.FIRE, 2));
        obj(O("ingotBrickNether"), aspectsOf(L("netherrack", 0)).add(Aspect.FIRE, 1).add(Aspect.ORDER, 1));
        obj(
            L("soul_sand", 32767), new AspectList().add(Aspect.EARTH, 3).add(Aspect.TRAP, 1).add(Aspect.SOUL, 3)
        );
        obj(O("blockGlass"), new AspectList().add(Aspect.CRYSTAL, 5));
        obj(
            L("mossy_cobblestone", 32767), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 3).add(Aspect.ENTROPY, 1)
        );
        obj(O("obsidian"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.FIRE, 5).add(Aspect.DARKNESS, 5));
        obj(L("stonebrick", 1), aspectsOf(L("stonebrick", 0)).add(Aspect.PLANT, 1));
        obj(L("stonebrick", 2), aspectsOf(L("stonebrick", 0)).add(Aspect.ENTROPY, 1));
        obj(L("stonebrick", 3), aspectsOf(L("stonebrick", 0)).add(Aspect.ORDER, 1));
        obj(L("sandstone", 1), aspectsOf(L("sandstone", 0)).add(Aspect.ORDER, 1));
        obj(L("sandstone", 2), aspectsOf(L("sandstone", 0)).add(Aspect.ORDER, 1));
        complex(
            L("concrete", 32767), aspectsOf(L("concrete_powder", 0)).add(Aspect.WATER, 1).add(Aspect.ORDER, 1)
        );
        obj(
            L("black_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("blue_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("brown_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("cyan_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("gray_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("green_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("light_blue_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("lime_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("magenta_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("orange_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("pink_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("purple_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("red_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("silver_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("white_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(
            L("yellow_glazed_terracotta", 0), aspectsOf(L("stained_hardened_clay", 0)).add(Aspect.SENSES, 1).add(Aspect.FIRE, 1)
        );
        obj(O("logWood"), new AspectList().add(Aspect.PLANT, 20));
        obj(O("treeSapling"), new AspectList().add(Aspect.PLANT, 15).add(Aspect.LIFE, 5));
        obj(O("treeLeaves"), new AspectList().add(Aspect.PLANT, 5));
        obj(L("tallgrass", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1));
        obj(
            L("double_plant", 0), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1).add(Aspect.SENSES, 5).add(Aspect.LIFE, 1)
        );
        obj(
            L("double_plant", 1), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1).add(Aspect.SENSES, 5).add(Aspect.LIFE, 1)
        );
        obj(L("double_plant", 2), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1));
        obj(L("double_plant", 3), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1));
        obj(
            L("double_plant", 4), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1).add(Aspect.SENSES, 5).add(Aspect.LIFE, 1)
        );
        obj(
            L("double_plant", 5), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1).add(Aspect.SENSES, 5).add(Aspect.LIFE, 1)
        );
        obj(L("waterlily", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.WATER, 1));
        obj(L("deadbush", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.ENTROPY, 1));
        obj(O("vine"), new AspectList().add(Aspect.PLANT, 5));
        obj(L("wheat_seeds", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1));
        obj(L("melon_seeds", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1));
        obj(L("pumpkin_seeds", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1));
        obj(L("beetroot_seeds", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1));
        obj(O("cropNetherWart"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.FIRE, 1).add(Aspect.ALCHEMY, 5));
        obj(
            L("red_flower", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1).add(Aspect.SENSES, 5)
        );
        obj(
            L("yellow_flower", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1).add(Aspect.SENSES, 5)
        );
        obj(O("blockCactus"), new AspectList().add(Aspect.PLANT, 10).add(Aspect.WATER, 5).add(Aspect.AVERSION, 1));
        obj(L("brown_mushroom", 0), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DARKNESS, 5).add(Aspect.EARTH, 5));
        obj(L("red_mushroom", 0), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DARKNESS, 5).add(Aspect.FIRE, 5));
        obj(
            L("brown_mushroom_block", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DARKNESS, 5).add(Aspect.EARTH, 5)
        );
        obj(
            L("red_mushroom_block", 32767), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DARKNESS, 5).add(Aspect.FIRE, 5)
        );
        obj(O("sugarcane"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.WATER, 3).add(Aspect.AIR, 2));
        obj(O("cropWheat"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5));
        obj(L("apple", 0), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5));
        obj(O("cropCarrot"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.SENSES, 5));
        obj(O("cropPotato"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5));
        obj(L("beetroot", 0), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.DESIRE, 1));
        obj(L("baked_potato", 0), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5));
        obj(L("poisonous_potato", 0), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DEATH, 5));
        obj(L("pumpkin", 0), new AspectList().add(Aspect.PLANT, 10));
        complex(L("melon_block", 32767), new AspectList().add(Aspect.PLANT, 10).remove(Aspect.LIFE, 5));
        obj(L("melon", 0), new AspectList().add(Aspect.PLANT, 1));
        obj(L("sponge", 0), new AspectList().add(Aspect.EARTH, 5).add(Aspect.TRAP, 5).add(Aspect.VOID, 5));
        obj(
            L("sponge", 1), new AspectList().add(Aspect.EARTH, 5).add(Aspect.TRAP, 5).add(Aspect.WATER, 5)
        );
        complex(L("sugar", 0), new AspectList().add(Aspect.DESIRE, 1).add(Aspect.ENERGY, 1));
        complex(L("cake", 0), new AspectList().add(Aspect.DESIRE, 1).add(Aspect.LIFE, 2));
        complex(L("pumpkin_pie", 0), new AspectList().add(Aspect.DESIRE, 1).add(Aspect.LIFE, 2));
        obj(L("wool", 0), new AspectList().add(Aspect.BEAST, 15).add(Aspect.CRAFT, 5));
        obj(L("experience_bottle", 0), new AspectList().add(Aspect.MIND, 20));
        obj(L("name_tag", 32767), new AspectList().add(Aspect.MIND, 10).add(Aspect.BEAST, 10));
        obj(
            L("iron_horse_armor", 32767), new AspectList().add(Aspect.METAL, 15).add(Aspect.PROTECT, 10).add(Aspect.BEAST, 5)
        );
        obj(
            L("golden_horse_armor", 32767), new AspectList().add(Aspect.METAL, 10).add(Aspect.PROTECT, 15).add(Aspect.BEAST, 5)
        );
        obj(
            L("diamond_horse_armor", 32767), new AspectList().add(Aspect.CRYSTAL, 15).add(Aspect.PROTECT, 20).add(Aspect.BEAST, 5)
        );
        obj(L("fire", 32767), new AspectList().add(Aspect.FIRE, 20));
        obj(L("magma", 32767), new AspectList().add(Aspect.FIRE, 10).add(Aspect.EARTH, 5));
        obj(
            L("chorus_flower", 32767), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.SENSES, 5).add(Aspect.PLANT, 5)
        );
        obj(L("chorus_plant", 32767), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.PLANT, 5));
        obj(L("chorus_fruit", 0), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.SENSES, 5).add(Aspect.PLANT, 5));
        obj(
            L("chorus_fruit_popped", 0), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.SENSES, 5).add(Aspect.PLANT, 4).add(Aspect.FIRE, 1)
        );
        obj(L("ice", 32767), new AspectList().add(Aspect.COLD, 20));
        obj(L("packed_ice", 32767), new AspectList().add(Aspect.COLD, 15).add(Aspect.ORDER, 5));
        obj(L("snowball", 32767), new AspectList().add(Aspect.COLD, 1));
        complex(L("cookie", 32767), new AspectList().add(Aspect.DESIRE, 1));
        obj(L("potion", 0), new AspectList().add(Aspect.WATER, 5).add(Aspect.CRYSTAL, 5));
        complex(L("torch", 32767), new AspectList().add(Aspect.LIGHT, 5));
        obj(L("web", 32767), new AspectList().add(Aspect.TRAP, 5).add(Aspect.BEAST, 1));
        obj(L("flint", 32767), new AspectList().add(Aspect.EARTH, 5).add(Aspect.TOOL, 5));
        obj(O("string"), new AspectList().add(Aspect.BEAST, 5).add(Aspect.CRAFT, 1));
        obj(O("slimeball"), new AspectList().add(Aspect.WATER, 5).add(Aspect.LIFE, 5).add(Aspect.ALCHEMY, 1));
        obj(O("leather"), new AspectList().add(Aspect.BEAST, 5).add(Aspect.PROTECT, 5));
        obj(
            L("rotten_flesh", 32767), new AspectList().add(Aspect.MAN, 5).add(Aspect.LIFE, 5).add(Aspect.ENTROPY, 5)
        );
        obj(O("feather"), new AspectList().add(Aspect.FLIGHT, 5).add(Aspect.AIR, 5));
        obj(O("bone"), new AspectList().add(Aspect.DEATH, 5).add(Aspect.LIFE, 5));
        obj(O("egg"), new AspectList().add(Aspect.LIFE, 5).add(Aspect.BEAST, 5));
        obj(
            L("spider_eye", 32767), new AspectList().add(Aspect.SENSES, 5).add(Aspect.BEAST, 5).add(Aspect.DEATH, 5)
        );
        obj(O("gunpowder"), new AspectList().add(Aspect.FIRE, 10).add(Aspect.ENTROPY, 10).add(Aspect.ALCHEMY, 5));
        obj(
            L("fish", 32767), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.WATER, 5)
        );
        obj(
            L("cooked_fish", 32767), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5)
        );
        obj(
            L("chicken", 32767), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.AIR, 5)
        );
        obj(
            L("cooked_chicken", 32767), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5)
        );
        obj(
            L("porkchop", 32767), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5)
        );
        obj(
            L("cooked_porkchop", 32767), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5)
        );
        obj(
            L("beef", 32767), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5)
        );
        obj(
            L("cooked_beef", 32767), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5)
        );
        obj(
            L("mutton", 32767), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5)
        );
        obj(
            L("cooked_mutton", 32767), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5)
        );
        obj(
            L("rabbit", 32767), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5)
        );
        obj(
            L("cooked_rabbit", 32767), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5)
        );
        obj(L("rabbit_hide", 32767), new AspectList().add(Aspect.BEAST, 5).add(Aspect.PROTECT, 2));
        obj(
            L("rabbit_foot", 32767),
            new AspectList().add(Aspect.BEAST, 5).add(Aspect.PROTECT, 5).add(Aspect.MOTION, 10).add(Aspect.ALCHEMY, 5)
        );
        obj(L("blaze_rod", 32767), new AspectList().add(Aspect.FIRE, 15).add(Aspect.ENERGY, 5));
        obj(
            L("saddle", 32767), new AspectList().add(Aspect.BEAST, 10).add(Aspect.MOTION, 10).add(Aspect.ORDER, 5)
        );
        obj(O("enderpearl"), new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MOTION, 15));
        obj(
            L("ghast_tear", 32767), new AspectList().add(Aspect.UNDEAD, 5).add(Aspect.SOUL, 10).add(Aspect.ALCHEMY, 10)
        );
        obj(
            L("skull", 0), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.UNDEAD, 10)
        );
        obj(
            L("skull", 1), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.UNDEAD, 10)
        );
        obj(
            L("skull", 2), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.MAN, 10)
        );
        obj(
            L("skull", 3), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.MAN, 10)
        );
        obj(
            L("skull", 4), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.ENTROPY, 5).add(Aspect.FIRE, 5)
        );
        obj(
            L("skull", 5), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.FIRE, 10).add(Aspect.DARKNESS, 10)
        );
        obj(
            L("dragon_breath", 0), new AspectList().add(Aspect.DARKNESS, 10).add(Aspect.ENTROPY, 10).add(Aspect.FIRE, 10).add(Aspect.ALCHEMY, 10)
        );
        complex(L("cauldron", 0), new AspectList().add(Aspect.ALCHEMY, 15));
        complex(L("fermented_spider_eye", 0), new AspectList().add(Aspect.ALCHEMY, 5));
        complex(L("blaze_powder", 0), new AspectList().add(Aspect.ALCHEMY, 5));
        complex(L("speckled_melon", 0), new AspectList().add(Aspect.ALCHEMY, 5));
        complex(L("magma_cream", 0), new AspectList().add(Aspect.ALCHEMY, 5));
        obj(
            L("totem_of_undying", 0), new AspectList().add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 10).add(Aspect.LIFE, 25).add(Aspect.UNDEAD, 10)
        );
        obj(
            L("shulker_shell", 0), new AspectList().add(Aspect.PROTECT, 10).add(Aspect.ELDRITCH, 5).add(Aspect.BEAST, 5).add(Aspect.VOID, 5)
        );
        obj(L("black_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("blue_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("green_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("red_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("yellow_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("silver_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("white_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("orange_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("pink_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("gray_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("cyan_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("light_blue_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("lime_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("magenta_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("brown_shulker_box", 0), aspectsOf(L("purple_shulker_box", 0)));
        obj(L("record_11", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.DESIRE, 15));
        obj(
            L("record_13", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.WATER, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_cat", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.BEAST, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_chirp", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.EARTH, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_far", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.ELDRITCH, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_mall", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.MAN, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_mellohi", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.CRAFT, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_stal", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.DARKNESS, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_strad", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.ENERGY, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_ward", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.LIFE, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_blocks", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.TOOL, 5).add(Aspect.DESIRE, 10)
        );
        obj(
            L("record_wait", 0), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.TRAP, 5).add(Aspect.DESIRE, 10)
        );
        obj(O("netherStar"), new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MAGIC, 20).add(Aspect.ORDER, 20).add(Aspect.AURA, 10));
        obj(L("chainmail_helmet", 32767), new AspectList().add(Aspect.METAL, 42));
        obj(L("chainmail_chestplate", 32767), new AspectList().add(Aspect.METAL, 67));
        obj(L("chainmail_leggings", 32767), new AspectList().add(Aspect.METAL, 58));
        obj(L("chainmail_boots", 32767), new AspectList().add(Aspect.METAL, 33));
        complex(L("paper", 0), new AspectList().add(Aspect.MIND, 2));
        complex(L("enchanted_book", 0), aspectsOf(L("book", 0)));
        complex(L("bookshelf", 0), new AspectList().add(Aspect.MIND, 8));
        obj(
            L("dragon_egg", 0),
            new AspectList().add(Aspect.ELDRITCH, 15).add(Aspect.BEAST, 15).add(Aspect.DARKNESS, 15).add(Aspect.MOTION, 15).add(Aspect.MAGIC, 5)
        );
        obj(
            L("portal", 32767), new AspectList().add(Aspect.FIRE, 10).add(Aspect.MOTION, 20).add(Aspect.MAGIC, 10)
        );
        obj(
            L("end_portal", 32767), new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MOTION, 20).add(Aspect.MAGIC, 10)
        );
        obj(
            L("end_portal_frame", 32767),
            new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.ENERGY, 10).add(Aspect.MOTION, 10).add(Aspect.MAGIC, 5)
        );
        obj(
            L("mob_spawner", 32767),
            new AspectList().add(Aspect.BEAST, 20).add(Aspect.MOTION, 20).add(Aspect.UNDEAD, 20).add(Aspect.MAGIC, 20)
        );
        obj(L("prismarine_shard", 0), new AspectList().add(Aspect.WATER, 5).add(Aspect.EARTH, 5));
        obj(L("prismarine_crystals", 0), new AspectList().add(Aspect.WATER, 5).add(Aspect.CRYSTAL, 5).add(Aspect.LIGHT, 5));
        obj(L("elytra", 0), new AspectList().add(Aspect.FLIGHT, 20).add(Aspect.MOTION, 15));
        obj(L("end_rod", 32767), new AspectList().add(Aspect.FIRE, 1).add(Aspect.LIGHT, 5));
        complex(L("chest", 32767), new AspectList().add(Aspect.VOID, 15));
        complex(L("trapped_chest", 32767), new AspectList().add(Aspect.TRAP, 10));
        complex(L("ender_eye", 0), new AspectList().add(Aspect.SENSES, 10).add(Aspect.MAGIC, 5));
        complex(L("arrow", 0), new AspectList().add(Aspect.AVERSION, 5));
        complex(L("glass_bottle", 0), new AspectList().add(Aspect.VOID, 5));
        complex(L("golden_apple", 0), new AspectList().add(Aspect.MAGIC, 5).add(Aspect.LIFE, 10));
        complex(
            L("golden_apple", 1), new AspectList().add(Aspect.MAGIC, 5).add(Aspect.LIFE, 15).add(Aspect.PROTECT, 15)
        );
        complex(L("bowl", 0), new AspectList().add(Aspect.VOID, 5));
        complex(L("mushroom_stew", 0), new AspectList().add(Aspect.LIFE, 5));
        complex(L("minecart", 0), new AspectList().add(Aspect.MOTION, 15));
        complex(L("iron_door", 0), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("acacia_door", 0), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("dark_oak_door", 0), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("jungle_door", 0), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("wooden_door", 0), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("spruce_door", 0), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("birch_door", 0), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("boat", 0), new AspectList().add(Aspect.WATER, 10).add(Aspect.MOTION, 15));
        complex(L("acacia_boat", 0), new AspectList().add(Aspect.WATER, 10).add(Aspect.MOTION, 15));
        complex(L("birch_boat", 0), new AspectList().add(Aspect.WATER, 10).add(Aspect.MOTION, 15));
        complex(L("dark_oak_boat", 0), new AspectList().add(Aspect.WATER, 10).add(Aspect.MOTION, 15));
        complex(L("jungle_boat", 0), new AspectList().add(Aspect.WATER, 10).add(Aspect.MOTION, 15));
        complex(L("spruce_boat", 0), new AspectList().add(Aspect.WATER, 10).add(Aspect.MOTION, 15));
        complex(L("flint_and_steel", 32767), new AspectList().add(Aspect.FIRE, 10).add(Aspect.TOOL, 5));
        complex(L("fishing_rod", 32767), new AspectList().add(Aspect.WATER, 10).add(Aspect.TOOL, 5));
        complex(L("shield", 32767), new AspectList().add(Aspect.PROTECT, 20));

        for (int a = 0; a < 16; a++) {
            ItemStack sis = new ItemStack(Items.SHIELD);
            sis.set(DataComponents.BASE_COLOR, DyeColor.byId(15 - a));
            complex(sis, new AspectList().merge(Aspect.PROTECT, 20));
        }

        complex(L("spectral_arrow", 32767), new AspectList().add(Aspect.SENSES, 10).add(Aspect.MAGIC, 5));
        complex(L("bucket", 0), new AspectList().add(Aspect.VOID, 5));
        obj(L("water_bucket", 0), aspectsOf(L("bucket", 0)).add(Aspect.WATER, 20));
        obj(
            L("lava_bucket", 0), aspectsOf(L("bucket", 0)).add(Aspect.FIRE, 15).add(Aspect.EARTH, 5)
        );
        obj(
            L("milk_bucket", 0),
            aspectsOf(L("bucket", 0)).add(Aspect.LIFE, 10).add(Aspect.BEAST, 5).add(Aspect.WATER, 5)
        );
        complex(L("brewing_stand", 0), new AspectList().add(Aspect.CRAFT, 15).add(Aspect.ALCHEMY, 25));
        complex(L("stone_button", 0), new AspectList().add(Aspect.MECHANISM, 5));
        complex(L("rail", 32767), new AspectList().add(Aspect.MOTION, 10));
        complex(L("detector_rail", 32767), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.SENSES, 1));
        complex(L("golden_rail", 32767), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.ENERGY, 1));
        complex(L("activator_rail", 32767), new AspectList().add(Aspect.MECHANISM, 5));
        complex(L("acacia_fence_gate", 32767), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("dark_oak_fence_gate", 32767), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("jungle_fence_gate", 32767), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("dark_oak_fence_gate", 32767), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("spruce_fence_gate", 32767), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("birch_fence_gate", 32767), new AspectList().add(Aspect.TRAP, 5).add(Aspect.MECHANISM, 5));
        complex(L("wooden_pressure_plate", 32767), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.SENSES, 5));
        complex(L("stone_pressure_plate", 32767), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.SENSES, 5));
        complex(L("light_weighted_pressure_plate", 32767), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.SENSES, 5));
        complex(L("heavy_weighted_pressure_plate", 32767), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.SENSES, 5));
        complex(L("lever", 32767), new AspectList().add(Aspect.MECHANISM, 5));
        complex(L("piston", 32767), new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.MOTION, 10));
        complex(L("sticky_piston", 32767), new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.MOTION, 10));
        complex(
            L("jukebox", 0), new AspectList().add(Aspect.SENSES, 20).add(Aspect.MECHANISM, 10).add(Aspect.AIR, 15)
        );
        complex(
            L("noteblock", 0), new AspectList().add(Aspect.SENSES, 20).add(Aspect.MECHANISM, 10).add(Aspect.AIR, 15)
        );
        complex(L("trapdoor", 32767), new AspectList().add(Aspect.MOTION, 5));
        complex(L("furnace", 32767), new AspectList().add(Aspect.FIRE, 10));
        complex(L("enchanting_table", 0), new AspectList().add(Aspect.MAGIC, 25).add(Aspect.CRAFT, 15));
        complex(L("crafting_table", 0), new AspectList().add(Aspect.CRAFT, 20));
        complex(L("clock", 0), new AspectList().add(Aspect.MECHANISM, 10));
        complex(
            L("beacon", 0), new AspectList().add(Aspect.AURA, 10).add(Aspect.MAGIC, 10).add(Aspect.EXCHANGE, 10)
        );
        complex(L("wooden_button", 32767), new AspectList().add(Aspect.MECHANISM, 5));
        complex(L("carrot_on_a_stick", 32767), new AspectList().add(Aspect.MOTION, 5).add(Aspect.DESIRE, 10));
        complex(L("flower_pot", 0), new AspectList().add(Aspect.VOID, 5).add(Aspect.PLANT, 5));
        complex(L("golden_carrot", 0), new AspectList().add(Aspect.SENSES, 10).add(Aspect.ALCHEMY, 5));
        complex(
            L("ender_chest", 32767), new AspectList().merge(Aspect.EXCHANGE, 10).merge(Aspect.MOTION, 10).merge(Aspect.VOID, 20)
        );
        complex(
            L("comparator", 32767), new AspectList().merge(Aspect.MECHANISM, 15).merge(Aspect.ORDER, 5).merge(Aspect.SENSES, 5)
        );
        complex(
            L("repeater", 32767), new AspectList().merge(Aspect.MECHANISM, 15).merge(Aspect.ENERGY, 10)
        );
        complex(
            L("hopper", 32767), new AspectList().merge(Aspect.MECHANISM, 5).merge(Aspect.EXCHANGE, 10).merge(Aspect.VOID, 5)
        );
        complex(
            L("dropper", 32767), new AspectList().merge(Aspect.MECHANISM, 5).merge(Aspect.EXCHANGE, 10).merge(Aspect.VOID, 5)
        );
        complex(
            L("dispenser", 32767), new AspectList().merge(Aspect.MECHANISM, 5).merge(Aspect.EXCHANGE, 10).merge(Aspect.VOID, 5)
        );
        obj(
            L("tripwire_hook", 32767), new AspectList().add(Aspect.SENSES, 5).add(Aspect.MECHANISM, 5).add(Aspect.TRAP, 5)
        );
        complex(
            L("tripwire", 32767), new AspectList().merge(Aspect.SENSES, 5).merge(Aspect.MECHANISM, 5).merge(Aspect.TRAP, 5)
        );
        complex(
            L("daylight_detector", 32767), new AspectList().merge(Aspect.SENSES, 10).merge(Aspect.LIGHT, 10).merge(Aspect.MECHANISM, 5)
        );
        // "gear*" — все имена OreDictionary, начинающиеся с gear (шестерни других модов) → тег c:gears
        complex(tag(c("gears")), new AspectList().add(Aspect.MECHANISM, 5));

        for (ItemStack stack : potionStacks(Items.POTION)) {
            obj(stack, getPotionAspects(stack).add(Aspect.WATER, 5));
        }
        for (ItemStack stack2 : potionStacks(Items.TIPPED_ARROW)) {
            obj(stack2, getPotionAspects(stack2).add(Aspect.AVERSION, 5));
        }
        for (ItemStack stack3 : potionStacks(Items.SPLASH_POTION)) {
            obj(stack3, getPotionAspects(stack3).add(Aspect.ENERGY, 5));
        }
        for (ItemStack stack4 : potionStacks(Items.LINGERING_POTION)) {
            obj(stack4, getPotionAspects(stack4).add(Aspect.TRAP, 5));
        }

        obj(L("dye", 0), new AspectList().add(Aspect.WATER, 2).add(Aspect.BEAST, 2));
        obj(
            L("dye", 2), aspectsOf(L("cactus", 0)).add(Aspect.WATER, 1).add(Aspect.FIRE, 1)
        );
        obj(L("dye", 3), new AspectList().add(Aspect.DESIRE, 2).add(Aspect.ENERGY, 2));
        obj(L("dye", 4), new AspectList().add(Aspect.EARTH, 2).add(Aspect.DESIRE, 2));
        obj(
            L("dye", 15), new AspectList().add(Aspect.LIFE, 2).add(Aspect.DEATH, 1).add(Aspect.PLANT, 1)
        );
        obj(T("phial"), new AspectList().add(Aspect.VOID, 3));
        obj(T("essentia_phial"), new AspectList());
        obj(T("grass_ambient"), aspectsOf(L("grass", 0)).add(Aspect.LIGHT, 5));
        complex(T("wooden_table"), new AspectList().add(Aspect.TOOL, 1));
        complex(T("stone_table"), new AspectList().add(Aspect.TOOL, 1));
        obj(
            T("arcane_workbench"), aspectsOf(L("crafting_table", 0)).add(Aspect.MAGIC, 5).add(Aspect.AURA, 5)
        );
        obj(T("triple_meat_treat"), new AspectList().add(Aspect.LIFE, 10).add(Aspect.DESIRE, 10));
        obj(
            T("purifying_fluid_bucket"), new AspectList().add(Aspect.MIND, 15).add(Aspect.ORDER, 15)
        );
        obj(O("clusterIron"), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5));
        obj(O("clusterGold"), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.DESIRE, 10));
        obj(O("clusterCinnabar"), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.ALCHEMY, 5).add(Aspect.DEATH, 5)
        );
        obj(O("clusterQuartz"), new AspectList().add(Aspect.ORDER, 5).add(Aspect.CRYSTAL, 10));
        obj(O("oreCinnabar"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.METAL, 10).add(Aspect.ALCHEMY, 5).add(Aspect.DEATH, 5));
        obj(O("oreAmber"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.TRAP, 10).add(Aspect.CRYSTAL, 10));
        obj(O("quicksilver"), new AspectList().add(Aspect.METAL, 10).add(Aspect.DEATH, 5).add(Aspect.ALCHEMY, 5));
        obj(O("gemAmber"), new AspectList().add(Aspect.TRAP, 10).add(Aspect.CRYSTAL, 10));
        obj(T("rare_earths"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ORDER, 5).add(Aspect.METAL, 5));
        obj(T("aer_crystal"), new AspectList().add(Aspect.AIR, 15).add(Aspect.CRYSTAL, 10));
        obj(T("ignis_crystal"), new AspectList().add(Aspect.FIRE, 15).add(Aspect.CRYSTAL, 10));
        obj(T("aqua_crystal"), new AspectList().add(Aspect.WATER, 15).add(Aspect.CRYSTAL, 10));
        obj(T("terra_crystal"), new AspectList().add(Aspect.EARTH, 15).add(Aspect.CRYSTAL, 10));
        obj(T("ordo_crystal"), new AspectList().add(Aspect.ORDER, 15).add(Aspect.CRYSTAL, 10));
        obj(T("perditio_crystal"), new AspectList().add(Aspect.ENTROPY, 15).add(Aspect.CRYSTAL, 10));
        obj(T("vitium_crystal"), new AspectList().add(Aspect.FLUX, 15).add(Aspect.CRYSTAL, 10));
        obj(T("taint_fibre"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.FLUX, 10));
        obj(T("taint_crust"), new AspectList().add(Aspect.LIFE, 5).add(Aspect.FLUX, 5));
        obj(T("taint_soil"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.FLUX, 5));
        obj(T("taint_geyser"), new AspectList().add(Aspect.AURA, 5).add(Aspect.WATER, 5).add(Aspect.FLUX, 10));
        obj(T("taint_rock"), new AspectList().add(Aspect.EARTH, 10).add(Aspect.FLUX, 5));
        obj(
            T("taint_feature"), new AspectList().add(Aspect.AURA, 5).add(Aspect.BEAST, 5).add(Aspect.FLUX, 10)
        );
        obj(T("taintwood_log"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.FLUX, 5));
        obj(T("greatwood_log"), new AspectList().add(Aspect.PLANT, 20).add(Aspect.LIFE, 5));
        obj(T("silverwood_log"), new AspectList().add(Aspect.PLANT, 20).add(Aspect.AURA, 5));
        obj(T("greatwood_leaves"), new AspectList().add(Aspect.PLANT, 5));
        obj(T("silverwood_leaves"), new AspectList().add(Aspect.PLANT, 5));
        obj(T("greatwood_sapling"), new AspectList().add(Aspect.PLANT, 15).add(Aspect.LIFE, 5));
        obj(T("silverwood_sapling"), new AspectList().add(Aspect.PLANT, 15).add(Aspect.AURA, 5));
        obj(T("shimmerleaf"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AURA, 10).add(Aspect.ENERGY, 5));
        obj(T("cinderpearl"), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AURA, 5).add(Aspect.FIRE, 10));
        obj(
            T("vishroom"), new AspectList().add(Aspect.PLANT, 2).add(Aspect.DEATH, 1).add(Aspect.MAGIC, 1).add(Aspect.ENTROPY, 1)
        );
        obj(T("ancient_stone"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 5));
        obj(T("ancient_stone_tile"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 5));
        obj(T("ancient_rock"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 5));
        obj(T("eldritch_stone"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 5));
        obj(
            T("ancient_barrier"), new AspectList().add(Aspect.METAL, 5).add(Aspect.ELDRITCH, 5).add(Aspect.TRAP, 5)
        );
        obj(
            T("glyphed_stone"), new AspectList().add(Aspect.METAL, 5).add(Aspect.ELDRITCH, 5).add(Aspect.MIND, 5)
        );
        obj(T("porous_stone"), new AspectList().add(Aspect.EARTH, 5).add(Aspect.VOID, 5));
        obj(T("research_table"), aspectsOf(T("wooden_table")).add(Aspect.MIND, 5));
        obj(T("zombie_brain"), new AspectList().add(Aspect.LIFE, 5).add(Aspect.MIND, 20).add(Aspect.UNDEAD, 10));
        obj(T("common_loot_bag"), new AspectList().add(Aspect.DESIRE, 10));
        obj(T("uncommon_loot_bag"), new AspectList().add(Aspect.DESIRE, 20));
        obj(T("rare_loot_bag"), new AspectList().add(Aspect.DESIRE, 30));
        obj(T("common_urn"), new AspectList().add(Aspect.DESIRE, 10).add(Aspect.EARTH, 2));
        obj(T("uncommon_urn"), new AspectList().add(Aspect.DESIRE, 20).add(Aspect.EARTH, 2));
        obj(T("rare_urn"), new AspectList().add(Aspect.DESIRE, 30).add(Aspect.EARTH, 2));
        obj(T("common_crate"), new AspectList().add(Aspect.DESIRE, 10).add(Aspect.PLANT, 2));
        obj(T("uncommon_crate"), new AspectList().add(Aspect.DESIRE, 20).add(Aspect.PLANT, 2));
        obj(T("rare_crate"), new AspectList().add(Aspect.DESIRE, 30).add(Aspect.PLANT, 2));
        obj(T("beef_chunk", "chicken_chunk", "pork_chunk", "fish_chunk", "rabbit_chunk", "mutton_chunk"), new AspectList().add(Aspect.LIFE, 5).add(Aspect.ENTROPY, 1));
        obj(T("salis_mundus"), new AspectList().add(Aspect.MAGIC, 5).add(Aspect.ENERGY, 5));
        obj(
            T("crucible"), aspectsOf(L("cauldron", 32767)).add(Aspect.CRAFT, 20).add(Aspect.ALCHEMY, 20)
        );

        for (var ca : BlocksTC.candles.values()) {
            complex(new ItemStack(ca.get()), new AspectList().add(Aspect.LIGHT, 5));
        }

        obj(
            T("thaumonomicon", "cheaters_thaumonomicon"), aspectsOf(L("bookshelf", 0)).merge(Aspect.MAGIC, 10)
        );
        complex(T("arcane_pedestal"), new AspectList().add(Aspect.MAGIC, 3).add(Aspect.AIR, 3));
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        complex(T("thaumometer"), new AspectList().add(Aspect.SENSES, 10).add(Aspect.AURA, 10));
        complex(T("goggles"), new AspectList().merge(Aspect.SENSES, 10).merge(Aspect.AURA, 10));
        complex(T("arcane_ear"), new AspectList().add(Aspect.SENSES, 20));
        obj(T("vis_stone"), new AspectList().add(Aspect.AURA, 20).add(Aspect.METAL, 5).add(Aspect.MAGIC, 5));
        obj(T("apprentice_ring"), new AspectList().add(Aspect.AURA, 5).add(Aspect.METAL, 5).add(Aspect.MAGIC, 20));
        obj(
            T("crimson_cult_chestplate"), aspectsOf(L("iron_chestplate", 0)).add(Aspect.ELDRITCH, 5)
        );
        obj(
            T("crimson_praetor_chestplate"), aspectsOf(L("iron_chestplate", 0)).add(Aspect.ELDRITCH, 10)
        );
        obj(
            T("crimson_cult_robe"), aspectsOf(L("leather_chestplate", 0)).add(Aspect.MAGIC, 5).add(Aspect.ELDRITCH, 5)
        );
        obj(
            T("crimson_cult_greaves"), aspectsOf(L("iron_leggings", 0)).add(Aspect.ELDRITCH, 5)
        );
        obj(
            T("crimson_praetor_greaves"), aspectsOf(L("iron_leggings", 0)).add(Aspect.ELDRITCH, 10)
        );
        obj(
            T("crimson_cult_leggings"), aspectsOf(L("leather_leggings", 0)).add(Aspect.MAGIC, 5).add(Aspect.ELDRITCH, 5)
        );
        obj(
            T("crimson_cult_helmet"), aspectsOf(L("iron_helmet", 0)).add(Aspect.ELDRITCH, 5)
        );
        obj(
            T("crimson_praetor_helmet"), aspectsOf(L("iron_helmet", 0)).add(Aspect.ELDRITCH, 10)
        );
        obj(
            T("crimson_cult_hood"), aspectsOf(L("leather_helmet", 0)).add(Aspect.MAGIC, 5).add(Aspect.ELDRITCH, 5)
        );
        obj(
            T("crimson_cult_boots"), aspectsOf(L("iron_boots", 0)).add(Aspect.ELDRITCH, 5)
        );
        obj(
            T("crimson_blade"), aspectsOf(L("iron_sword", 0)).add(Aspect.ELDRITCH, 10).add(Aspect.DEATH, 10)
        );

        for (String ca : BANNERS) {
            complex(T(ca), new AspectList().add(Aspect.ELDRITCH, 5));
        }
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        obj(T("arcane_curio"), new AspectList().add(Aspect.MIND, 15).add(Aspect.MAGIC, 15));
        obj(T("preserved_curio"), new AspectList().add(Aspect.MIND, 15).add(Aspect.BEAST, 15));
        obj(T("ancient_curio"), new AspectList().add(Aspect.MIND, 15).add(Aspect.DEATH, 15));
        obj(T("eldritch_curio"), new AspectList().add(Aspect.MIND, 15).add(Aspect.ELDRITCH, 15));
        obj(T("illuminating_curio"), new AspectList().add(Aspect.MIND, 30));
        obj(T("twisted_curio"), new AspectList().add(Aspect.MIND, 15).add(Aspect.FLUX, 15));
        obj(
            T("crimson_rites"), new AspectList().add(Aspect.MIND, 15).add(Aspect.ELDRITCH, 5).add(Aspect.SOUL, 5).add(Aspect.MAGIC, 5)
        );
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        obj(
            T("celestial_notes_sun", "celestial_notes_stars_1", "celestial_notes_stars_2", "celestial_notes_stars_3", "celestial_notes_stars_4", "celestial_notes_moon_1", "celestial_notes_moon_2", "celestial_notes_moon_3", "celestial_notes_moon_4", "celestial_notes_moon_5", "celestial_notes_moon_6", "celestial_notes_moon_7", "celestial_notes_moon_8"), new AspectList().add(Aspect.MIND, 5).add(Aspect.DARKNESS, 5).add(Aspect.LIGHT, 5)
        );
        obj(
            T("primordial_pearl"),
            new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 10).add(Aspect.WATER, 10).add(Aspect.EARTH, 10).add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 10)
        );
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
        // (запись для предмета, которого в BETA17 нет, или под недостижимым ключом — в оригинале не действовала)
    }

    // --- зелья -------------------------------------------------------------------------------------------------------

    /** Стек предмета для каждого зелья реестра, плюс предмет без содержимого (в 1.12 — тип «empty»). */
    private static List<ItemStack> potionStacks(Item item) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(item));
        for (Holder<Potion> potion : BuiltInRegistries.POTION.holders().toList()) {
            out.add(PotionContents.createItemStack(item, potion));
        }
        return out;
    }

    private static PotionBrewing brewing;

    /** synchronized: таблица аспектов строится и на сервере, и на клиенте (в одиночной игре — в одном процессе). */
    private static synchronized PotionBrewing brewing() {
        if (brewing == null) {
            brewing = PotionBrewing.bootstrap(FeatureFlags.DEFAULT_FLAGS);
        }
        return brewing;
    }

    /** PotionHelper.POTION_TYPE_CONVERSIONS 1.12 → смеси PotionBrewing 1.21 (первая подходящая, как в оригинале). */
    private static void getPotionReagentsRecursive(Holder<Potion> potion, Set<ItemStack> hashSet) {
        for (PotionBrewing.Mix<Potion> mix : brewing().potionMixes) {
            if (mix.to().is(potion)) {
                ItemStack[] reagents = mix.ingredient().getItems();
                if (reagents.length > 0) {
                    hashSet.add(reagents[0]);
                }
                if (!mix.from().is(Potions.WATER)) {
                    getPotionReagentsRecursive(mix.from(), hashSet);
                }
                break;
            }
        }
    }

    public static AspectList getPotionAspects(ItemStack itemstack) {
        AspectList tmp = new AspectList();
        boolean didit = false;
        PotionContents contents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.potion().isEmpty() || !contents.is(Potions.WATER)) {
            Set<ItemStack> hs = new LinkedHashSet<>();
            contents.potion().ifPresent(p -> getPotionReagentsRecursive(p, hs));
            for (ItemStack his : hs) {
                AspectList tmp2 = ThaumcraftCraftingManager.getObjectTags(his);
                if (tmp2 != null) {
                    for (Aspect a : tmp2.aspects.keySet()) {
                        tmp.add(a, tmp2.getAmount(a));
                    }
                }
                didit = true;
                tmp.add(Aspect.ALCHEMY, 3);
            }
            for (Aspect a : tmp.aspects.keySet().toArray(new Aspect[0])) {
                int amt = tmp.getAmount(a);
                tmp.remove(a, (int) (amt * 0.66));
            }
            if (!didit) {
                tmp.add(Aspect.MAGIC, 5);
                tmp.add(Aspect.ALCHEMY, 5);
            }
        } else {
            tmp.add(Aspect.WATER, 5);
        }
        return tmp;
    }
}
