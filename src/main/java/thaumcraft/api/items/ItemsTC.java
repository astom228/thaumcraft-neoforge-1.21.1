package thaumcraft.api.items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.ThaumcraftMaterials;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.consumables.ItemZombieBrain;
import thaumcraft.common.items.curios.ItemCelestialNotes;
import thaumcraft.common.items.curios.ItemCurio;
import thaumcraft.common.items.curios.ItemEnchantmentPlaceholder;
import thaumcraft.common.items.curios.ItemThaumonomicon;
import thaumcraft.common.items.consumables.ItemLabel;
import thaumcraft.common.items.tools.ItemResonator;
import thaumcraft.common.items.tools.ItemHandMirror;
import thaumcraft.common.items.casters.ItemCaster;
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
    // Перчатка заклинателя (ItemCaster "caster_basic", area 0); этап 11 — только щелчок по блокам (DECISIONS 92)
    public static final DeferredItem<Item> casterBasic = ConfigItems.ITEMS.registerItem("caster_basic", p -> new ItemCaster(0, p));
    // фокусы: предельная сложность 15 / 25 / 50
    public static final DeferredItem<Item> focus1 = ConfigItems.ITEMS.registerItem("focus_1", p -> new thaumcraft.common.items.casters.ItemFocus(15, p));
    public static final DeferredItem<Item> focus2 = ConfigItems.ITEMS.registerItem("focus_2", p -> new thaumcraft.common.items.casters.ItemFocus(25, p));
    public static final DeferredItem<Item> focusPouch = ConfigItems.ITEMS.registerItem("focus_pouch", thaumcraft.common.items.casters.ItemFocusPouch::new);
    public static final DeferredItem<Item> focus3 = ConfigItems.ITEMS.registerItem("focus_3", p -> new thaumcraft.common.items.casters.ItemFocus(50, p));
    public static final DeferredItem<Item> handMirror = ConfigItems.ITEMS.registerItem("hand_mirror", ItemHandMirror::new);
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

    // ItemGoggles: шлем SPECIAL, setMaxDamage(350), RARE; безделушка на голову
    public static final DeferredItem<Item> goggles = ConfigItems.ITEMS.registerItem("goggles",
            p -> new thaumcraft.common.items.armor.ItemGoggles(ThaumcraftMaterials.ARMORMAT_SPECIAL, p),
            new Item.Properties().durability(350).rarity(Rarity.RARE));

    // --- инструменты (ConfigItems.initItems: ThaumcraftMaterials.TOOLMAT_*) ---
    public static final DeferredItem<Item> thaumiumAxe = tool("thaumium_axe", p -> new thaumcraft.common.items.tools.ItemThaumiumAxe(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p));
    public static final DeferredItem<Item> thaumiumSword = tool("thaumium_sword", p -> new thaumcraft.common.items.tools.ItemThaumiumSword(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p));
    public static final DeferredItem<Item> thaumiumShovel = tool("thaumium_shovel", p -> new thaumcraft.common.items.tools.ItemThaumiumShovel(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p));
    public static final DeferredItem<Item> thaumiumPick = tool("thaumium_pickaxe", p -> new thaumcraft.common.items.tools.ItemThaumiumPickaxe(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p));
    public static final DeferredItem<Item> thaumiumHoe = tool("thaumium_hoe", p -> new thaumcraft.common.items.tools.ItemThaumiumHoe(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p));
    public static final DeferredItem<Item> voidAxe = tool("void_metal_axe", p -> new thaumcraft.common.items.tools.ItemVoidAxe(ThaumcraftMaterials.TOOLMAT_VOID, p));
    public static final DeferredItem<Item> voidSword = tool("void_metal_sword", p -> new thaumcraft.common.items.tools.ItemVoidSword(ThaumcraftMaterials.TOOLMAT_VOID, p));
    public static final DeferredItem<Item> voidShovel = tool("void_metal_shovel", p -> new thaumcraft.common.items.tools.ItemVoidShovel(ThaumcraftMaterials.TOOLMAT_VOID, p));
    public static final DeferredItem<Item> voidPick = tool("void_metal_pickaxe", p -> new thaumcraft.common.items.tools.ItemVoidPickaxe(ThaumcraftMaterials.TOOLMAT_VOID, p));
    public static final DeferredItem<Item> voidHoe = tool("void_metal_hoe", p -> new thaumcraft.common.items.tools.ItemVoidHoe(ThaumcraftMaterials.TOOLMAT_VOID, p));

    // стихийные (TOOLMAT_ELEMENTAL, RARE)
    public static final DeferredItem<Item> elementalAxe = rareTool("elemental_axe", p -> new thaumcraft.common.items.tools.ItemElementalAxe(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p));
    public static final DeferredItem<Item> elementalSword = rareTool("elemental_sword", p -> new thaumcraft.common.items.tools.ItemElementalSword(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p));
    public static final DeferredItem<Item> elementalShovel = rareTool("elemental_shovel", p -> new thaumcraft.common.items.tools.ItemElementalShovel(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p));
    public static final DeferredItem<Item> elementalPick = rareTool("elemental_pickaxe", p -> new thaumcraft.common.items.tools.ItemElementalPickaxe(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p));
    public static final DeferredItem<Item> elementalHoe = rareTool("elemental_hoe", p -> new thaumcraft.common.items.tools.ItemElementalHoe(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p));
    // ItemPrimalCrusher (обычная редкость), ItemCrimsonBlade (EPIC)
    public static final DeferredItem<Item> primalCrusher = tool("primal_crusher", thaumcraft.common.items.tools.ItemPrimalCrusher::new);
    public static final DeferredItem<Item> crimsonBlade = ConfigItems.ITEMS.registerItem("crimson_blade", thaumcraft.common.items.tools.ItemCrimsonBlade::new,
            new Item.Properties().rarity(Rarity.EPIC));

    private static DeferredItem<Item> rareTool(String name, java.util.function.Function<Item.Properties, Item> factory) {
        return ConfigItems.ITEMS.registerItem(name, factory, new Item.Properties().rarity(Rarity.RARE));
    }

    // --- броня (ConfigItems.initItems: ThaumcraftMaterials.ARMORMAT_*; прочность — множитель оригинала) ---
    public static final DeferredItem<Item> thaumiumHelm = thaumiumArmor("thaumium_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> thaumiumChest = thaumiumArmor("thaumium_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> thaumiumLegs = thaumiumArmor("thaumium_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> thaumiumBoots = thaumiumArmor("thaumium_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<Item> clothChest = robe("thaumaturge_robe", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> clothLegs = robe("thaumaturge_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> clothBoots = robe("thaumaturge_boots", ArmorItem.Type.BOOTS);
    // ItemBootsTraveller: setMaxDamage(350), редкость RARE
    public static final DeferredItem<Item> travellerBoots = ConfigItems.ITEMS.registerItem("traveller_boots",
            p -> new thaumcraft.common.items.armor.ItemBootsTraveller(ThaumcraftMaterials.ARMORMAT_SPECIAL, p),
            new Item.Properties().durability(350).rarity(Rarity.RARE));
    public static final DeferredItem<Item> voidHelm = voidArmor("void_metal_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> voidChest = voidArmor("void_metal_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> voidLegs = voidArmor("void_metal_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> voidBoots = voidArmor("void_metal_boots", ArmorItem.Type.BOOTS);

    // --- безделушки (ItemBaubles meta 0–6, ItemAmuletVis meta 0–1; слоты Curios — теги) ---
    public static final DeferredItem<Item> amuletMundane = bauble("mundane_amulet", 0);
    public static final DeferredItem<Item> ringMundane = bauble("mundane_ring", 1);
    public static final DeferredItem<Item> girdleMundane = bauble("mundane_belt", 2);
    public static final DeferredItem<Item> ringApprentice = bauble("apprentice_ring", 3);
    public static final DeferredItem<Item> amuletFancy = bauble("fancy_amulet", 4);
    public static final DeferredItem<Item> ringFancy = bauble("fancy_ring", 5);
    public static final DeferredItem<Item> girdleFancy = bauble("fancy_belt", 6);
    public static final DeferredItem<Item> amuletVisFound = ConfigItems.ITEMS.registerItem("vis_stone",
            p -> new thaumcraft.common.items.baubles.ItemAmuletVis(false, p), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> amuletVis = ConfigItems.ITEMS.registerItem("vis_amulet",
            p -> new thaumcraft.common.items.baubles.ItemAmuletVis(true, p), new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    // особые безделушки (RARE, не чинятся)
    public static final DeferredItem<Item> charmUndying = rare("charm_undying", thaumcraft.common.items.baubles.ItemCharmUndying::new);
    public static final DeferredItem<Item> ringCloud = rare("cloud_ring", thaumcraft.common.items.baubles.ItemCloudRing::new);
    public static final DeferredItem<Item> bandCuriosity = rare("curiosity_band", thaumcraft.common.items.baubles.ItemCuriosityBand::new);
    public static final DeferredItem<Item> charmVerdant = rare("verdant_charm", thaumcraft.common.items.baubles.ItemVerdantCharm::new);
    public static final DeferredItem<Item> charmVoidseer = rare("voidseer_charm", thaumcraft.common.items.baubles.ItemVoidseerCharm::new);

    private static DeferredItem<Item> rare(String name, java.util.function.Function<Item.Properties, Item> factory) {
        return ConfigItems.ITEMS.registerItem(name, factory, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    private static DeferredItem<Item> bauble(String name, int meta) {
        return ConfigItems.ITEMS.registerItem(name, p -> new thaumcraft.common.items.baubles.ItemBaubles(meta, p),
                new Item.Properties().stacksTo(1).rarity(meta >= 3 ? Rarity.UNCOMMON : Rarity.COMMON));
    }

    // «Таумиумная крепость» (RARE), пустотная роба (EPIC), броня культа (UNCOMMON, претор — RARE)
    public static final DeferredItem<Item> fortressHelm = fortress("fortress_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> fortressChest = fortress("fortress_cuirass", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> fortressLegs = fortress("fortress_thigh_guards", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> voidRobeHelm = voidRobe("void_thaumaturge_hood", ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> voidRobeChest = voidRobe("void_thaumaturge_robe", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> voidRobeLegs = voidRobe("void_thaumaturge_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> crimsonPlateHelm = cultist("crimson_cult_helmet", thaumcraft.common.items.armor.ItemCultistArmor.Kind.PLATE, ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> crimsonPlateChest = cultist("crimson_cult_chestplate", thaumcraft.common.items.armor.ItemCultistArmor.Kind.PLATE, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> crimsonPlateLegs = cultist("crimson_cult_greaves", thaumcraft.common.items.armor.ItemCultistArmor.Kind.PLATE, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> crimsonBoots = cultist("crimson_cult_boots", thaumcraft.common.items.armor.ItemCultistArmor.Kind.BOOTS, ArmorItem.Type.BOOTS);
    public static final DeferredItem<Item> crimsonRobeHelm = cultist("crimson_cult_hood", thaumcraft.common.items.armor.ItemCultistArmor.Kind.ROBE, ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> crimsonRobeChest = cultist("crimson_cult_robe", thaumcraft.common.items.armor.ItemCultistArmor.Kind.ROBE, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> crimsonRobeLegs = cultist("crimson_cult_leggings", thaumcraft.common.items.armor.ItemCultistArmor.Kind.ROBE, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> crimsonPraetorHelm = cultist("crimson_praetor_helmet", thaumcraft.common.items.armor.ItemCultistArmor.Kind.LEADER, ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> crimsonPraetorChest = cultist("crimson_praetor_chestplate", thaumcraft.common.items.armor.ItemCultistArmor.Kind.LEADER, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> crimsonPraetorLegs = cultist("crimson_praetor_greaves", thaumcraft.common.items.armor.ItemCultistArmor.Kind.LEADER, ArmorItem.Type.LEGGINGS);

    private static DeferredItem<Item> fortress(String name, ArmorItem.Type type) {
        return ConfigItems.ITEMS.registerItem(name, p -> new thaumcraft.common.items.armor.ItemFortressArmor(ThaumcraftMaterials.ARMORMAT_FORTRESS_SPECIAL, type, p),
                new Item.Properties().durability(type.getDurability(ThaumcraftMaterials.DURABILITY_FORTRESS)).rarity(Rarity.RARE));
    }

    private static DeferredItem<Item> voidRobe(String name, ArmorItem.Type type) {
        return ConfigItems.ITEMS.registerItem(name, p -> new thaumcraft.common.items.armor.ItemVoidRobeArmor(ThaumcraftMaterials.ARMORMAT_VOIDROBE, type, p),
                new Item.Properties().durability(type.getDurability(ThaumcraftMaterials.DURABILITY_VOIDROBE)).rarity(Rarity.EPIC));
    }

    /** Латы, роба и ботинки — железо (ArmorMaterial.IRON 1.12, множитель прочности 15), претор — материал FORTRESS (40). */
    private static DeferredItem<Item> cultist(String name, thaumcraft.common.items.armor.ItemCultistArmor.Kind kind, ArmorItem.Type type) {
        boolean leader = kind == thaumcraft.common.items.armor.ItemCultistArmor.Kind.LEADER;
        return ConfigItems.ITEMS.registerItem(name, p -> new thaumcraft.common.items.armor.ItemCultistArmor(kind,
                        leader ? ThaumcraftMaterials.ARMORMAT_FORTRESS : net.minecraft.world.item.ArmorMaterials.IRON, type, p),
                new Item.Properties().durability(type.getDurability(leader ? ThaumcraftMaterials.DURABILITY_FORTRESS : 15))
                        .rarity(leader ? Rarity.RARE : Rarity.UNCOMMON));
    }

    private static DeferredItem<Item> tool(String name, java.util.function.Function<Item.Properties, Item> factory) {
        return ConfigItems.ITEMS.registerItem(name, factory);
    }

    private static DeferredItem<Item> thaumiumArmor(String name, ArmorItem.Type type) {
        return ConfigItems.ITEMS.registerItem(name, p -> new thaumcraft.common.items.armor.ItemThaumiumArmor(ThaumcraftMaterials.ARMORMAT_THAUMIUM, type, p),
                new Item.Properties().durability(type.getDurability(ThaumcraftMaterials.DURABILITY_THAUMIUM)).rarity(Rarity.UNCOMMON));
    }

    private static DeferredItem<Item> robe(String name, ArmorItem.Type type) {
        return ConfigItems.ITEMS.registerItem(name, p -> new thaumcraft.common.items.armor.ItemRobeArmor(ThaumcraftMaterials.ARMORMAT_ROBE, type, p),
                new Item.Properties().durability(type.getDurability(ThaumcraftMaterials.DURABILITY_SPECIAL)).rarity(Rarity.UNCOMMON));
    }

    private static DeferredItem<Item> voidArmor(String name, ArmorItem.Type type) {
        return ConfigItems.ITEMS.registerItem(name, p -> new thaumcraft.common.items.armor.ItemVoidArmor(ThaumcraftMaterials.ARMORMAT_VOID, type, p),
                new Item.Properties().durability(type.getDurability(ThaumcraftMaterials.DURABILITY_VOID)).rarity(Rarity.UNCOMMON));
    }

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
