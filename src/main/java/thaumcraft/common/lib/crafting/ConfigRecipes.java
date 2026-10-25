package thaumcraft.common.lib.crafting;

import thaumcraft.api.crafting.InfusionRecipe;
import net.minecraft.core.registries.Registries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.api.crafting.Part;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.items.ItemsTC;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.lib.TCTags;

/** Типы и сериализаторы рецептов (особые рецепты оригинала, магический верстак; тигель и наполнение — на этапах 9, 11). */
public class ConfigRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Thaumcraft.MODID);

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Thaumcraft.MODID);

    /** Магический верстак (в 1.12 — обычные IRecipe, отличавшиеся по интерфейсу IArcaneRecipe). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<IArcaneRecipe>> ARCANE_CRAFTING =
            TYPES.register("arcane_crafting", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "arcane_crafting")));

    public static final DeferredHolder<RecipeSerializer<?>, ShapedArcaneRecipe.Serializer> ARCANE_SHAPED =
            SERIALIZERS.register("arcane_shaped", ShapedArcaneRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ShapelessArcaneRecipe.Serializer> ARCANE_SHAPELESS =
            SERIALIZERS.register("arcane_shapeless", ShapelessArcaneRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RecipeMagicDust>> SALIS_MUNDUS =
            SERIALIZERS.register("salis_mundus", () -> new SimpleCraftingRecipeSerializer<>(RecipeMagicDust::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RecipeTripleMeatTreat>> TRIPLE_MEAT_TREAT =
            SERIALIZERS.register("triple_meat_treat", () -> new SimpleCraftingRecipeSerializer<>(RecipeTripleMeatTreat::new));

    /**
     * initializeCompoundRecipes оригинала (часть): превращения солью мира. Тигель (котёл → тигель) и многоблочные
     * структуры добавляются на этапах своих систем. OreDictionary «bookshelf» / «workbench» → теги c:bookshelves /
     * c:player_workstations/crafting_tables.
     */
    public static void initializeCompoundRecipes() {
        IDustTrigger.registerDustTrigger(new DustTriggerSimple("!gotdream", Blocks.BOOKSHELF, new ItemStack(ItemsTC.thaumonomicon.get())));
        IDustTrigger.registerDustTrigger(new DustTriggerOre("!gotdream", Tags.Blocks.BOOKSHELVES, new ItemStack(ItemsTC.thaumonomicon.get())));
        IDustTrigger.registerDustTrigger(new DustTriggerSimple("FIRSTSTEPS@1", Blocks.CRAFTING_TABLE, new ItemStack(BlocksTC.arcaneWorkbench.get())));
        IDustTrigger.registerDustTrigger(new DustTriggerOre("FIRSTSTEPS@1", Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES,
                new ItemStack(BlocksTC.arcaneWorkbench.get())));
        // в 1.12 котёл с водой — тот же блок (уровень — свойство); в 1.21 у наполненных котлов свои блоки — тег minecraft:cauldrons
        IDustTrigger.registerDustTrigger(new DustTriggerOre("UNLOCKALCHEMY@1", BlockTags.CAULDRONS, new ItemStack(BlocksTC.crucible.get())));
        // TODO многоблочные — этапы 11 (алтарь наполнения), 14 (адская печь), 18 (пресс големов)
        Part TH1 = new Part(BlocksTC.metalAlchemical.get().defaultBlockState(), BlocksTC.thaumatoriumTop.get()).setApplyPlayerFacing(true);
        Part TH2 = new Part(BlocksTC.metalAlchemical.get().defaultBlockState(), BlocksTC.thaumatorium.get()).setApplyPlayerFacing(true);
        Part TH3 = new Part(BlocksTC.crucible.get(), null);
        Part[][][] thaumotoriumBlueprint = new Part[][][] {{{TH1}}, {{TH2}}, {{TH3}}};
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock("THAUMATORIUM", thaumotoriumBlueprint));
        ThaumcraftApi.addMultiblockRecipeToCatalog(ResourceLocation.fromNamespaceAndPath("thaumcraft", "thaumatorium"),
                new ThaumcraftApi.BluePrint("THAUMATORIUM", thaumotoriumBlueprint, new ItemStack(BlocksTC.metalAlchemical.get(), 2),
                        new ItemStack(BlocksTC.crucible.get())));
    }

    /**
     * initializeInfusionRecipes оригинала: рецепты наполнения (каталог, на обеих сторонах). Перенесены те, чьи предметы уже есть;
     * остальные — с этапами своих предметов (TODO ниже).
     */
    public static void initializeInfusionRecipes() {
        // TODO этап 18: печати големов (SealHarvest, SealButcher, SealBreak)
        cluster("CrystalClusterAir", BlocksTC.crystalAir.get(), Aspect.AIR, 0);
        cluster("CrystalClusterFire", BlocksTC.crystalFire.get(), Aspect.FIRE, 0);
        cluster("CrystalClusterWater", BlocksTC.crystalWater.get(), Aspect.WATER, 0);
        cluster("CrystalClusterEarth", BlocksTC.crystalEarth.get(), Aspect.EARTH, 0);
        cluster("CrystalClusterOrder", BlocksTC.crystalOrder.get(), Aspect.ORDER, 0);
        cluster("CrystalClusterEntropy", BlocksTC.crystalEntropy.get(), Aspect.ENTROPY, 0);
        cluster("CrystalClusterFlux", BlocksTC.crystalTaint.get(), Aspect.FLUX, 4);
        // TODO этап 12: focus_2, focus_3
        ThaumcraftApi.addInfusionCraftingRecipe(tc("JarBrain"), new InfusionRecipe("JARBRAIN", new ItemStack(BlocksTC.jarBrain.get()), 4,
                new AspectList().add(Aspect.MIND, 25).add(Aspect.SENSES, 25).add(Aspect.UNDEAD, 25), new ItemStack(BlocksTC.jarNormal.get()),
                new ItemStack(ItemsTC.brain.get()), new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.SPIDER_EYE)));
        // TODO этап 13: VisAmulet, RunicArmor (+ поддельные), стихийные инструменты, BootsTraveller, доспехи и маски «Таумиумной крепости»,
        //  VerdantHeart (3), CLOUDRING, CuriosityBand, CHARMUNDYING
        ThaumcraftApi.addInfusionCraftingRecipe(tc("Mirror"), new InfusionRecipe("MIRROR", new ItemStack(BlocksTC.mirror.get()), 1,
                new AspectList().add(Aspect.MOTION, 25).add(Aspect.DARKNESS, 25).add(Aspect.EXCHANGE, 25), new ItemStack(ItemsTC.mirroredGlass.get()),
                Tags.Items.INGOTS_GOLD, Tags.Items.INGOTS_GOLD, Tags.Items.INGOTS_GOLD, new ItemStack(Items.ENDER_PEARL)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("MirrorHand"), new InfusionRecipe("MIRRORHAND", new ItemStack(ItemsTC.handMirror.get()), 5,
                new AspectList().add(Aspect.TOOL, 50).add(Aspect.MOTION, 50), new ItemStack(BlocksTC.mirror.get()), Tags.Items.RODS_WOODEN,
                new ItemStack(Items.COMPASS), new ItemStack(Items.MAP)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("MirrorEssentia"), new InfusionRecipe("MIRRORESSENTIA", new ItemStack(BlocksTC.mirrorEssentia.get()), 2,
                new AspectList().add(Aspect.MOTION, 25).add(Aspect.WATER, 25).add(Aspect.EXCHANGE, 25), new ItemStack(ItemsTC.mirroredGlass.get()),
                Tags.Items.INGOTS_IRON, Tags.Items.INGOTS_IRON, Tags.Items.INGOTS_IRON, new ItemStack(Items.ENDER_PEARL)));
        // TODO этап 11.6: наполняющие зачарования (IEBURROWING … IEESSENCE + поддельные)
        // TODO этап 13: MindBiothaumic; этап 14: ArcaneBore, LampGrowth, LampFertility; этапы 13–16: CausalityCollapser (алюментум)
    }

    /** Кластер вис-кристаллов из кристалла (CrystalCluster* оригинала). */
    private static void cluster(String name, net.minecraft.world.level.block.Block out, Aspect a, int inst) {
        ThaumcraftApi.addInfusionCraftingRecipe(tc(name), new InfusionRecipe("CRYSTALFARMER", new ItemStack(out), inst,
                new AspectList().add(a, 10).add(Aspect.CRYSTAL, 10).add(Aspect.TRAP, 5), ThaumcraftApiHelper.makeCrystal(a),
                new ItemStack(Items.WHEAT_SEEDS), new ItemStack(ItemsTC.salisMundus.get())));
    }

    /** Ключ рецепта: как в оригинале, но в нижнем регистре (так их ищут исследования). */
    private static ResourceLocation tc(String name) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name.toLowerCase(java.util.Locale.ROOT));
    }

    /** Из initializeNormalRecipes оригинала: «поддельный» рецепт соли мира для книги (кристаллы — любые, сменяются). */
    public static void initializeFakeRecipes() {
        List<ItemStack> crystals = new ArrayList<>();
        for (Aspect a : Aspect.aspects.values()) {
            crystals.add(ThaumcraftApiHelper.makeCrystal(a));
        }
        Ingredient anyCrystal = Ingredient.of(crystals.stream());
        ThaumcraftApi.addFakeCraftingRecipe(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "salismundusfake"),
                new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(ItemsTC.salisMundus.get()), NonNullList.of(Ingredient.EMPTY,
                        Ingredient.of(Items.FLINT), Ingredient.of(Items.BOWL), Ingredient.of(Items.REDSTONE), anyCrystal, anyCrystal, anyCrystal)));
    }

    /**
     * initializeAlchemyRecipes оригинала: рецепты тигля (каталог, на обеих сторонах). Перенесены те, чьи предметы уже есть;
     * остальные — с этапами своих предметов (TODO ниже). Окраска нитора — обычные рецепты (TCRecipeProvider).
     * OreDictionary → теги (DECISIONS.md п. 27).
     */
    public static void initializeAlchemyRecipes() {
        ResourceLocation visCrystalGroup = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "viscrystalgroup");
        for (Aspect aspect : Aspect.aspects.values()) {
            ThaumcraftApi.addCrucibleRecipe(id("vis_crystal_" + aspect.getTag()),
                    new CrucibleRecipe("BASEALCHEMY", ThaumcraftApiHelper.makeCrystal(aspect), TCTags.Items.NUGGETS_QUARTZ, new AspectList().add(aspect, 2))
                            .setGroup(visCrystalGroup));
        }
        ThaumcraftApi.addCrucibleRecipe(id("nitor"), new CrucibleRecipe("UNLOCKALCHEMY@3", new ItemStack(BlocksTC.nitor.get(DyeColor.YELLOW).get()),
                Tags.Items.DUSTS_GLOWSTONE, new AspectList().merge(Aspect.ENERGY, 10).merge(Aspect.FIRE, 10).merge(Aspect.LIGHT, 10)));
        // TODO alumentum (ItemsTC.alumentum, катализатор — уголь и древесный уголь): ENERGY 10, FIRE 10, ENTROPY 5 — со своим предметом
        ThaumcraftApi.addCrucibleRecipe(id("brassingot"), new CrucibleRecipe("METALLURGY@1", new ItemStack(ItemsTC.ingotBrass.get()),
                Tags.Items.INGOTS_IRON, new AspectList().merge(Aspect.TOOL, 5)));
        ThaumcraftApi.addCrucibleRecipe(id("thaumiumingot"), new CrucibleRecipe("METALLURGY@2", new ItemStack(ItemsTC.ingotThaumium.get()),
                Tags.Items.INGOTS_IRON, new AspectList().merge(Aspect.MAGIC, 5).merge(Aspect.EARTH, 5)));
        ThaumcraftApi.addCrucibleRecipe(id("voidingot"), new CrucibleRecipe("METALLURGY@4", new ItemStack(ItemsTC.ingotVoid.get()),
                new ItemStack(ItemsTC.voidSeed.get()), new AspectList().merge(Aspect.METAL, 10).merge(Aspect.FLUX, 5)));
        ThaumcraftApi.addCrucibleRecipe(id("voidseed"), new CrucibleRecipe("METALLURGY@4", new ItemStack(ItemsTC.voidSeed.get()),
                new ItemStack(Items.WHEAT_SEEDS), new AspectList().merge(Aspect.DARKNESS, 5).merge(Aspect.VOID, 5).merge(Aspect.ELDRITCH, 5).merge(Aspect.MAGIC, 5)));
        ThaumcraftApi.addCrucibleRecipe(id("hedge_tallow"), new CrucibleRecipe("HEDGEALCHEMY@1", new ItemStack(ItemsTC.tallow.get()),
                new ItemStack(Items.ROTTEN_FLESH), new AspectList().merge(Aspect.FIRE, 1)));
        ThaumcraftApi.addCrucibleRecipe(id("hedge_leather"), new CrucibleRecipe("HEDGEALCHEMY@1", new ItemStack(Items.LEATHER),
                new ItemStack(Items.ROTTEN_FLESH), new AspectList().merge(Aspect.AIR, 3).merge(Aspect.BEAST, 3)));
        // TODO этап 12: focus_1 (ItemsTC.focus1 из кристалла порядка): CRYSTAL 20, MAGIC 10, AURA 5
        purification("iron", ItemsTC.clusterIron.get(), Tags.Items.ORES_IRON);
        purification("gold", ItemsTC.clusterGold.get(), Tags.Items.ORES_GOLD);
        purification("cinnabar", ItemsTC.clusterCinnabar.get(), TCTags.Items.ORES_CINNABAR);
        purification("copper", ItemsTC.clusterCopper.get(), Tags.Items.ORES_COPPER);
        purification("tin", ItemsTC.clusterTin.get(), oreTag("tin"));
        purification("silver", ItemsTC.clusterSilver.get(), oreTag("silver"));
        purification("lead", ItemsTC.clusterLead.get(), oreTag("lead"));
        // TODO LiquidDeath (ведро жидкой смерти из ведра): DEATH 100, ALCHEMY 20, ENTROPY 50 — с жидкостью (этап 15)
        // TODO этап 15: BottleTaint (из фиала Выброса): FLUX 30, WATER 30; SaneSoap (из блока плоти): MIND 75, ELDRITCH 50, ORDER 75, LIFE 50
        // TODO этап 14: BathSalts (из соли мира): MIND 40, AIR 40, ORDER 40, LIFE 40
        // TODO этап 18: печати големов (SealCollect…SealUse) — 12 рецептов
        ThaumcraftApi.addCrucibleRecipe(id("everfullurn"), new CrucibleRecipe("EVERFULLURN", new ItemStack(BlocksTC.everfullUrn.get()),
                new ItemStack(Items.FLOWER_POT), new AspectList().add(Aspect.WATER, 30).add(Aspect.CRAFT, 10).add(Aspect.EARTH, 10)));
    }

    private static void purification(String metal, ItemLike cluster, TagKey<Item> ore) {
        ThaumcraftApi.addCrucibleRecipe(id("metal_purification_" + metal), new CrucibleRecipe("METALPURIFICATION", new ItemStack(cluster), ore,
                new AspectList().merge(Aspect.METAL, 5).merge(Aspect.ORDER, 5)));
    }

    private static TagKey<Item> oreTag(String metal) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/" + metal));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }
}
