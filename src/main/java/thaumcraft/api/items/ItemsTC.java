package thaumcraft.api.items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.consumables.ItemZombieBrain;
import thaumcraft.common.items.curios.ItemCelestialNotes;
import thaumcraft.common.items.curios.ItemCurio;
import thaumcraft.common.items.curios.ItemEnchantmentPlaceholder;
import thaumcraft.common.items.curios.ItemThaumonomicon;
import thaumcraft.common.items.consumables.ItemLabel;
import thaumcraft.common.items.tools.ItemResonator;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumcraft.common.items.resources.ItemCrystalEssence;
import thaumcraft.common.items.resources.ItemMagicDust;
import thaumcraft.common.items.tools.ItemScribingTools;
import thaumcraft.common.items.tools.ItemThaumometer;

/**
 * Предметы Thaumcraft (в оригинале — статические поля, заполняемые ConfigItems.initItems).
 * Варианты по metadata разделены на отдельные предметы (ID_MAPPING.md); имена полей — по оригиналу + вариант.
 */
public class ItemsTC {
    // --- ресурсы (ItemTCBase) ---
    public static final DeferredItem<Item> amber = simple("amber");
    public static final DeferredItem<Item> quicksilver = simple("quicksilver");
    // ingot: thaumium, void, brass
    public static final DeferredItem<Item> ingotThaumium = simple("thaumium_ingot");
    public static final DeferredItem<Item> ingotVoid = simple("void_metal_ingot");
    public static final DeferredItem<Item> ingotBrass = simple("brass_ingot");
    // nugget: iron, copper, tin, silver, lead, quicksilver, thaumium, void, brass, quartz, rareearth
    public static final DeferredItem<Item> nuggetIron = simple("iron_nugget");
    public static final DeferredItem<Item> nuggetCopper = simple("copper_nugget");
    public static final DeferredItem<Item> nuggetTin = simple("tin_nugget");
    public static final DeferredItem<Item> nuggetSilver = simple("silver_nugget");
    public static final DeferredItem<Item> nuggetLead = simple("lead_nugget");
    public static final DeferredItem<Item> nuggetQuicksilver = simple("quicksilver_drop");
    public static final DeferredItem<Item> nuggetThaumium = simple("thaumium_nugget");
    public static final DeferredItem<Item> nuggetVoid = simple("void_metal_nugget");
    public static final DeferredItem<Item> nuggetBrass = simple("brass_nugget");
    public static final DeferredItem<Item> nuggetQuartz = simple("quartz_sliver");
    public static final DeferredItem<Item> nuggetRareEarth = simple("rare_earths");
    // cluster: iron, gold, copper, tin, silver, lead, cinnabar, quartz
    public static final DeferredItem<Item> clusterIron = simple("iron_cluster");
    public static final DeferredItem<Item> clusterGold = simple("gold_cluster");
    public static final DeferredItem<Item> clusterCopper = simple("copper_cluster");
    public static final DeferredItem<Item> clusterTin = simple("tin_cluster");
    public static final DeferredItem<Item> clusterSilver = simple("silver_cluster");
    public static final DeferredItem<Item> clusterLead = simple("lead_cluster");
    public static final DeferredItem<Item> clusterCinnabar = simple("cinnabar_cluster");
    public static final DeferredItem<Item> clusterQuartz = simple("quartz_cluster");
    public static final DeferredItem<Item> fabric = simple("enchanted_fabric");
    public static final DeferredItem<Item> visResonator = simple("vis_resonator");
    public static final DeferredItem<Item> tallow = simple("magic_tallow");
    public static final DeferredItem<Item> mechanismSimple = simple("simple_arcane_mechanism");
    public static final DeferredItem<Item> mechanismComplex = simple("complex_arcane_mechanism");
    // plate: brass, iron, thaumium, void
    public static final DeferredItem<Item> plateBrass = simple("brass_plate");
    public static final DeferredItem<Item> plateIron = simple("iron_plate");
    public static final DeferredItem<Item> plateThaumium = simple("thaumium_plate");
    public static final DeferredItem<Item> plateVoid = simple("void_metal_plate");
    public static final DeferredItem<Item> filter = simple("essentia_filter");
    public static final DeferredItem<Item> morphicResonator = simple("morphic_resonator");
    public static final DeferredItem<Item> mirroredGlass = simple("mirrored_glass");
    public static final DeferredItem<Item> voidSeed = simple("void_seed");
    // mind: clockwork, biothaumic
    public static final DeferredItem<Item> mindClockwork = simple("clockwork_mind");
    public static final DeferredItem<Item> mindBiothaumic = simple("biothaumic_mind");
    // module: vision, aggression
    public static final DeferredItem<Item> moduleVision = simple("vision_module");
    public static final DeferredItem<Item> moduleAggression = simple("aggression_module");
    // phial: empty, filled (ItemPhial, base 10); label: blank, filled (ItemLabel, base 1)
    public static final DeferredItem<Item> phial = ConfigItems.ITEMS.registerItem("phial", p -> new ItemPhial(false, p));
    public static final DeferredItem<Item> phialFilled = ConfigItems.ITEMS.registerItem("essentia_phial", p -> new ItemPhial(true, p));
    public static final DeferredItem<Item> label = ConfigItems.ITEMS.registerItem("label", p -> new ItemLabel(false, p));
    public static final DeferredItem<Item> labelFilled = ConfigItems.ITEMS.registerItem("marked_label", p -> new ItemLabel(true, p));
    public static final DeferredItem<Item> resonator = ConfigItems.ITEMS.registerItem("essentia_resonator", ItemResonator::new);
    public static final DeferredItem<Item> salisMundus = ConfigItems.ITEMS.registerItem("salis_mundus", ItemMagicDust::new);
    public static final DeferredItem<Item> crystalEssence = ConfigItems.ITEMS.registerItem("crystal_essence", ItemCrystalEssence::new);
    public static final DeferredItem<Item> enchantedPlaceholder = ConfigItems.ITEMS.registerItem("enchanted_placeholder", ItemEnchantmentPlaceholder::new);
    public static final DeferredItem<Item> thaumometer = ConfigItems.ITEMS.registerItem("thaumometer", ItemThaumometer::new);
    public static final DeferredItem<Item> thaumonomicon = ConfigItems.ITEMS.registerItem("thaumonomicon", p -> new ItemThaumonomicon(false, p));
    public static final DeferredItem<Item> thaumonomiconCheat = ConfigItems.ITEMS.registerItem("cheaters_thaumonomicon", p -> new ItemThaumonomicon(true, p));
    public static final DeferredItem<Item> scribingTools = ConfigItems.ITEMS.registerItem("scribing_tools", ItemScribingTools::new);
    // curio: arcane, preserved, ancient, eldritch, knowledge, twisted, rites (meta 0..6)
    public static final DeferredItem<Item> curioArcane = curio("arcane_curio", 0);
    public static final DeferredItem<Item> curioPreserved = curio("preserved_curio", 1);
    public static final DeferredItem<Item> curioAncient = curio("ancient_curio", 2);
    public static final DeferredItem<Item> curioEldritch = curio("eldritch_curio", 3);
    public static final DeferredItem<Item> curioKnowledge = curio("illuminating_curio", 4);
    public static final DeferredItem<Item> curioTwisted = curio("twisted_curio", 5);
    public static final DeferredItem<Item> curioRites = curio("crimson_rites", 6);
    /** Небесные заметки по варианту (sun, stars_1..4, moon_1..8). */
    public static final Map<String, DeferredItem<Item>> celestialNotes = new LinkedHashMap<>();

    static {
        for (String v : ItemCelestialNotes.VARIANTS) {
            celestialNotes.put(v, ConfigItems.ITEMS.registerItem("celestial_notes_" + v, p -> new ItemCelestialNotes(v, p)));
        }
    }

    // --- еда ---
    // ItemChunksEdible: ItemFood(1, 0.3F, false), время поедания 10 тиков
    public static final DeferredItem<Item> chunkBeef = chunk("beef_chunk");
    public static final DeferredItem<Item> chunkChicken = chunk("chicken_chunk");
    public static final DeferredItem<Item> chunkPork = chunk("pork_chunk");
    public static final DeferredItem<Item> chunkFish = chunk("fish_chunk");
    public static final DeferredItem<Item> chunkRabbit = chunk("rabbit_chunk");
    public static final DeferredItem<Item> chunkMutton = chunk("mutton_chunk");
    // ItemTripleMeatTreat: ItemFood(6, 0.8F, true), всегда съедобно, регенерация 100 тиков с шансом 0.66
    public static final DeferredItem<Item> tripleMeatTreat = ConfigItems.ITEMS.registerSimpleItem("triple_meat_treat",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 0.66F).build()));
    // ItemZombieBrain: ItemFood(4, 0.2F, true), голод 30 тиков с шансом 0.8
    public static final DeferredItem<Item> brain = ConfigItems.ITEMS.registerItem("zombie_brain", ItemZombieBrain::new,
            new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.2F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 30, 0), 0.8F).build()));

    public static final DeferredItem<Item> jarBrace = simple("jar_brace");
    public static final DeferredItem<Item> grappleGunTip = simple("grappler_head");
    public static final DeferredItem<Item> grappleGunSpool = simple("grappler_spool");

    // TODO этап 13: заменить на ItemGoggles (броня + очки откровения). Пока нужен как иконка вкладки.
    public static final DeferredItem<Item> goggles = ConfigItems.ITEMS.registerSimpleItem("goggles", new Item.Properties().stacksTo(1));

    private static DeferredItem<Item> curio(String name, int type) {
        return ConfigItems.ITEMS.registerItem(name, p -> new ItemCurio(type, p));
    }

    private static DeferredItem<Item> simple(String name) {
        return ConfigItems.ITEMS.registerSimpleItem(name);
    }

    private static DeferredItem<Item> chunk(String name) {
        // FoodProperties 1.21: насыщение = сытость * множитель * 2 (как в 1.12); время 10 тиков = 0.5 с
        return ConfigItems.ITEMS.registerSimpleItem(name,
                new Item.Properties().food(new FoodProperties(1, 1 * 0.3F * 2.0F, false, 0.5F, Optional.empty(), List.of())));
    }

    /** Загружает класс, чтобы все предметы попали в регистр до события регистрации. */
    public static void init() {
    }
}
