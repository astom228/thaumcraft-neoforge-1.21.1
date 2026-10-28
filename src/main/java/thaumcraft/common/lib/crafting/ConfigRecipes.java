package thaumcraft.common.lib.crafting;

import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.blocks.basic.BlockPillar;
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
        // TODO многоблочные — этапы 14 (адская печь), 18 (пресс големов)
        infusionAltar("INFUSION", "infusionaltar", BlocksTC.stoneArcane.get(), BlocksTC.pillarArcane.get(), BlocksTC.pedestalArcane.get());
        infusionAltar("INFUSIONANCIENT", "infusionaltarancient", BlocksTC.stoneAncient.get(), BlocksTC.pillarAncient.get(), BlocksTC.pedestalAncient.get());
        infusionAltar("INFUSIONELDRITCH", "infusionaltareldritch", BlocksTC.stoneEldritchTile.get(), BlocksTC.pillarEldritch.get(),
                BlocksTC.pedestalEldritch.get());
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
     * Алтарь наполнения (infusionAltar*Blueprint оригинала): матрица над камнем, четыре колонны (по две клетки камня, нижняя —
     * колонна с заданной стороной, верхняя убирается) по углам и пьедестал под матрицей.
     */
    private static void infusionAltar(String research, String key, net.minecraft.world.level.block.Block stone, net.minecraft.world.level.block.Block pillar,
                                      net.minecraft.world.level.block.Block pedestal) {
        Part IM = new Part(BlocksTC.infusionMatrix.get(), null);
        Part ST = new Part(stone, "AIR");
        net.minecraft.world.level.block.state.properties.DirectionProperty F = BlockPillar.FACING;
        Part SB1 = new Part(stone, pillar.defaultBlockState().setValue(F, net.minecraft.core.Direction.EAST));
        Part SB2 = new Part(stone, pillar.defaultBlockState().setValue(F, net.minecraft.core.Direction.NORTH));
        Part SB3 = new Part(stone, pillar.defaultBlockState().setValue(F, net.minecraft.core.Direction.SOUTH));
        Part SB4 = new Part(stone, pillar.defaultBlockState().setValue(F, net.minecraft.core.Direction.WEST));
        Part PD = new Part(pedestal.defaultBlockState(), null);
        Part[][][] blueprint = new Part[][][] {
                {{null, null, null}, {null, IM, null}, {null, null, null}},
                {{ST, null, ST}, {null, null, null}, {ST, null, ST}},
                {{SB1, null, SB2}, {null, PD, null}, {SB3, null, SB4}}
        };
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock(research, blueprint));
        ThaumcraftApi.addMultiblockRecipeToCatalog(ResourceLocation.fromNamespaceAndPath("thaumcraft", key),
                new ThaumcraftApi.BluePrint(research, blueprint, new ItemStack(stone, 8), new ItemStack(pedestal), new ItemStack(BlocksTC.infusionMatrix.get())));
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
        ThaumcraftApi.addInfusionCraftingRecipe(tc("focus_2"), new InfusionRecipe("FOCUSADVANCED@1", new ItemStack(ItemsTC.focus2.get()), 3,
                new AspectList().add(Aspect.MAGIC, 25).add(Aspect.ORDER, 50), new ItemStack(ItemsTC.focus1.get()),
                new ItemStack(ItemsTC.quicksilver.get()), Tags.Items.GEMS_DIAMOND, new ItemStack(ItemsTC.quicksilver.get()), new ItemStack(Items.ENDER_PEARL)));
        // TODO: focus_3 — вместе с изначальной жемчужиной (ItemsTC.primordialPearl): MAGIC 25, ORDER 50, VOID 100, нестабильность 5
        ThaumcraftApi.addInfusionCraftingRecipe(tc("JarBrain"), new InfusionRecipe("JARBRAIN", new ItemStack(BlocksTC.jarBrain.get()), 4,
                new AspectList().add(Aspect.MIND, 25).add(Aspect.SENSES, 25).add(Aspect.UNDEAD, 25), new ItemStack(BlocksTC.jarNormal.get()),
                new ItemStack(ItemsTC.brain.get()), new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.SPIDER_EYE)));
        // BootsTraveller: рыба любого вида (Items.FISH, 32767) → тег minecraft:fishes
        ThaumcraftApi.addInfusionCraftingRecipe(tc("BootsTraveller"), new InfusionRecipe("BOOTSTRAVELLER", new ItemStack(ItemsTC.travellerBoots.get()), 1,
                new AspectList().add(Aspect.FLIGHT, 100).add(Aspect.MOTION, 100), new ItemStack(Items.LEATHER_BOOTS),
                ThaumcraftApiHelper.makeCrystal(Aspect.AIR), ThaumcraftApiHelper.makeCrystal(Aspect.AIR), new ItemStack(ItemsTC.fabric.get()),
                new ItemStack(ItemsTC.fabric.get()), new ItemStack(Items.FEATHER), net.minecraft.tags.ItemTags.FISHES));
        // TODO этап 13: VisAmulet, поддельные RunicArmorFake0–2 для книги (центр — безделушка), стихийные инструменты, доспехи и маски «Таумиумной крепости»,
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
        // наполняющие зачарования; в книге — «поддельные» рецепты с примером инструмента
        infusionEnchantment("IEBURROWING", EnumInfusionEnchantment.BURROWING, new AspectList().add(Aspect.SENSES, 80).add(Aspect.EARTH, 150),
                Items.RABBIT_FOOT, Items.WOODEN_PICKAXE);
        infusionEnchantment("IECOLLECTOR", EnumInfusionEnchantment.COLLECTOR, new AspectList().add(Aspect.DESIRE, 80).add(Aspect.WATER, 100),
                Items.LEAD, Items.STONE_AXE);
        infusionEnchantment("IEDESTRUCTIVE", EnumInfusionEnchantment.DESTRUCTIVE, new AspectList().add(Aspect.AVERSION, 200).add(Aspect.ENTROPY, 250),
                net.minecraft.world.level.block.Blocks.TNT, Items.STONE_PICKAXE);
        infusionEnchantment("IEREFINING", EnumInfusionEnchantment.REFINING, new AspectList().add(Aspect.ORDER, 80).add(Aspect.EXCHANGE, 60),
                ItemsTC.salisMundus.get(), Items.IRON_PICKAXE);
        infusionEnchantment("IESOUNDING", EnumInfusionEnchantment.SOUNDING, new AspectList().add(Aspect.SENSES, 40).add(Aspect.FIRE, 60),
                Items.MAP, Items.GOLDEN_PICKAXE);
        infusionEnchantment("IEARCING", EnumInfusionEnchantment.ARCING, new AspectList().add(Aspect.ENERGY, 40).add(Aspect.AIR, 60),
                net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK, Items.WOODEN_SWORD);
        infusionEnchantment("IEESSENCE", EnumInfusionEnchantment.ESSENCE, new AspectList().add(Aspect.BEAST, 40).add(Aspect.FLUX, 60),
                ItemsTC.crystalEssence.get(), Items.STONE_SWORD);
        // руническое усиление: доспехи (безделушки и «поддельные» RunicArmorFake0–2 с кольцом — этап 13)
        ThaumcraftApi.addInfusionCraftingRecipe(tc("RunicArmor"), new InfusionRunicAugmentRecipe());
        // TODO этап 13: MindBiothaumic; этап 14: ArcaneBore, LampGrowth, LampFertility; этапы 13–16: CausalityCollapser (алюментум)
    }

    private static void infusionEnchantment(String key, EnumInfusionEnchantment ench, AspectList aspects, net.minecraft.world.level.ItemLike item,
                                            net.minecraft.world.item.Item example) {
        InfusionEnchantmentRecipe r = new InfusionEnchantmentRecipe(ench, aspects, new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(item));
        ThaumcraftApi.addInfusionCraftingRecipe(tc(key), r);
        ThaumcraftApi.addFakeCraftingRecipe(tc(key + "FAKE"), new InfusionEnchantmentRecipe(r, new ItemStack(example)));
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
        ThaumcraftApi.addCrucibleRecipe(id("focus_1"), new CrucibleRecipe("UNLOCKAUROMANCY", new ItemStack(ItemsTC.focus1.get()),
                ThaumcraftApiHelper.makeCrystal(Aspect.ORDER), new AspectList().merge(Aspect.CRYSTAL, 20).merge(Aspect.MAGIC, 10).merge(Aspect.AURA, 5)));
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
