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
        // Адская печь: кирпич и обсидиан → невидимые заполнители, лава → печь (окно — к решётке), решётка убирается
        Part NB = new Part(Blocks.NETHER_BRICKS, BlocksTC.placeholderNetherbrick.get().defaultBlockState());
        Part OB = new Part(Blocks.OBSIDIAN, BlocksTC.placeholderObsidian.get().defaultBlockState());
        Part IB = new Part(Blocks.IRON_BARS, "AIR");
        Part LA = new Part(net.minecraft.tags.FluidTags.LAVA, BlocksTC.infernalFurnace.get(), true);
        Part[][][] infernalFurnaceBlueprint = new Part[][][] {
                {{NB, OB, NB}, {OB, null, OB}, {NB, OB, NB}}, {{NB, OB, NB}, {OB, LA, OB}, {NB, IB, NB}}, {{NB, OB, NB}, {OB, OB, OB}, {NB, OB, NB}}};
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock("INFERNALFURNACE", infernalFurnaceBlueprint));
        ThaumcraftApi.addMultiblockRecipeToCatalog(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infernalfurnace"),
                new ThaumcraftApi.BluePrint("INFERNALFURNACE", infernalFurnaceBlueprint, new ItemStack(Blocks.NETHER_BRICKS, 12),
                        new ItemStack(Blocks.OBSIDIAN, 12), new ItemStack(Blocks.IRON_BARS), new ItemStack(Items.LAVA_BUCKET)));
        // TODO этап 18: пресс големов
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
        // жемчужина — любая стадия (Ingredient.fromItem); после наполнения возвращается с повреждением +1 (getCraftingRemainingItem)
        ThaumcraftApi.addInfusionCraftingRecipe(tc("focus_3"), new InfusionRecipe("FOCUSGREATER@1", new ItemStack(ItemsTC.focus3.get()), 5,
                new AspectList().add(Aspect.MAGIC, 25).add(Aspect.ORDER, 50).add(Aspect.VOID, 100), new ItemStack(ItemsTC.focus2.get()),
                new ItemStack(ItemsTC.quicksilver.get()), net.minecraft.world.item.crafting.Ingredient.of(ItemsTC.primordialPearl.get()),
                new ItemStack(ItemsTC.quicksilver.get()), new ItemStack(Items.NETHER_STAR)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("JarBrain"), new InfusionRecipe("JARBRAIN", new ItemStack(BlocksTC.jarBrain.get()), 4,
                new AspectList().add(Aspect.MIND, 25).add(Aspect.SENSES, 25).add(Aspect.UNDEAD, 25), new ItemStack(BlocksTC.jarNormal.get()),
                new ItemStack(ItemsTC.brain.get()), new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.SPIDER_EYE)));
        // BootsTraveller: рыба любого вида (Items.FISH, 32767) → тег minecraft:fishes
        ThaumcraftApi.addInfusionCraftingRecipe(tc("BootsTraveller"), new InfusionRecipe("BOOTSTRAVELLER", new ItemStack(ItemsTC.travellerBoots.get()), 1,
                new AspectList().add(Aspect.FLIGHT, 100).add(Aspect.MOTION, 100), new ItemStack(Items.LEATHER_BOOTS),
                ThaumcraftApiHelper.makeCrystal(Aspect.AIR), ThaumcraftApiHelper.makeCrystal(Aspect.AIR), new ItemStack(ItemsTC.fabric.get()),
                new ItemStack(ItemsTC.fabric.get()), new ItemStack(Items.FEATHER), net.minecraft.tags.ItemTags.FISHES));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("VisAmulet"), new InfusionRecipe("VISAMULET", new ItemStack(ItemsTC.amuletVis.get()), 6,
                new AspectList().add(Aspect.AURA, 50).add(Aspect.ENERGY, 100).add(Aspect.VOID, 50), new ItemStack(ItemsTC.amuletMundane.get()),
                new ItemStack(ItemsTC.visResonator.get()), ThaumcraftApiHelper.makeCrystal(Aspect.AIR), ThaumcraftApiHelper.makeCrystal(Aspect.FIRE),
                ThaumcraftApiHelper.makeCrystal(Aspect.WATER), ThaumcraftApiHelper.makeCrystal(Aspect.EARTH), ThaumcraftApiHelper.makeCrystal(Aspect.ORDER)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("VerdantHeart"), new InfusionRecipe("VERDANTCHARMS", new ItemStack(ItemsTC.charmVerdant.get()), 5,
                new AspectList().add(Aspect.LIFE, 60).add(Aspect.ORDER, 30).add(Aspect.PLANT, 60), new ItemStack(ItemsTC.amuletFancy.get()),
                new ItemStack(ItemsTC.nuggetRareEarth.get()), ThaumcraftApiHelper.makeCrystal(Aspect.LIFE), new ItemStack(Items.MILK_BUCKET),
                ThaumcraftApiHelper.makeCrystal(Aspect.PLANT)));
        // варианты: результат — тот же оберег с type 1 / 2 (Object[]{"type", NBTTagByte} оригинала → компонент)
        ThaumcraftApi.addInfusionCraftingRecipe(tc("VerdantHeartLife"), new InfusionRecipe("VERDANTCHARMS",
                new Object[] {thaumcraft.common.lib.TCDataComponents.VERDANT_TYPE.get(), 1}, 5,
                new AspectList().add(Aspect.LIFE, 80).add(Aspect.MAN, 80), new ItemStack(ItemsTC.charmVerdant.get()), new ItemStack(Items.GOLDEN_APPLE),
                ThaumcraftApiHelper.makeCrystal(Aspect.LIFE),
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.STRONG_HEALING),
                ThaumcraftApiHelper.makeCrystal(Aspect.MAN)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("VerdantHeartSustain"), new InfusionRecipe("VERDANTCHARMS",
                new Object[] {thaumcraft.common.lib.TCDataComponents.VERDANT_TYPE.get(), 2}, 5,
                new AspectList().add(Aspect.DESIRE, 80).add(Aspect.AIR, 80), new ItemStack(ItemsTC.charmVerdant.get()), new ItemStack(ItemsTC.tripleMeatTreat.get()),
                ThaumcraftApiHelper.makeCrystal(Aspect.DESIRE),
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.STRONG_REGENERATION),
                ThaumcraftApiHelper.makeCrystal(Aspect.AIR)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("CLOUDRING"), new InfusionRecipe("CLOUDRING", new ItemStack(ItemsTC.ringCloud.get()), 1,
                new AspectList().add(Aspect.AIR, 50), new ItemStack(ItemsTC.ringMundane.get()), ThaumcraftApiHelper.makeCrystal(Aspect.AIR),
                new ItemStack(Items.FEATHER)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("CuriosityBand"), new InfusionRecipe("CURIOSITYBAND", new ItemStack(ItemsTC.bandCuriosity.get()), 5,
                new AspectList().add(Aspect.MIND, 150).add(Aspect.VOID, 50).add(Aspect.TRAP, 100), new ItemStack(ItemsTC.girdleFancy.get()),
                new ItemStack(Items.EMERALD), new ItemStack(Items.WRITABLE_BOOK), new ItemStack(Items.EMERALD), new ItemStack(Items.WRITABLE_BOOK),
                new ItemStack(Items.EMERALD), new ItemStack(Items.WRITABLE_BOOK), new ItemStack(Items.EMERALD), new ItemStack(Items.WRITABLE_BOOK)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("CHARMUNDYING"), new InfusionRecipe("CHARMUNDYING", new ItemStack(ItemsTC.charmUndying.get()), 2,
                new AspectList().add(Aspect.LIFE, 25), new ItemStack(Items.TOTEM_OF_UNDYING), TCTags.Items.PLATES_BRASS));
        // стихийные инструменты: результат сразу с наполняющими зачарованиями; центр — таумиумный инструмент любой прочности
        elemental("ElementalAxe", ItemsTC.elementalAxe.get(), new AspectList().add(Aspect.WATER, 60).add(Aspect.PLANT, 30), ItemsTC.thaumiumAxe.get(),
                Aspect.WATER, Aspect.WATER, EnumInfusionEnchantment.COLLECTOR, 1, EnumInfusionEnchantment.BURROWING, 1);
        elemental("ElementalPick", ItemsTC.elementalPick.get(), new AspectList().add(Aspect.FIRE, 30).add(Aspect.METAL, 30).add(Aspect.SENSES, 30),
                ItemsTC.thaumiumPick.get(), Aspect.FIRE, Aspect.FIRE, EnumInfusionEnchantment.REFINING, 1, EnumInfusionEnchantment.SOUNDING, 2);
        elemental("ElementalSword", ItemsTC.elementalSword.get(), new AspectList().add(Aspect.AIR, 30).add(Aspect.MOTION, 30).add(Aspect.AVERSION, 30),
                ItemsTC.thaumiumSword.get(), Aspect.AIR, Aspect.AIR, EnumInfusionEnchantment.ARCING, 2, null, 0);
        elemental("ElementalShovel", ItemsTC.elementalShovel.get(), new AspectList().add(Aspect.EARTH, 60).add(Aspect.CRAFT, 30),
                ItemsTC.thaumiumShovel.get(), Aspect.EARTH, Aspect.EARTH, EnumInfusionEnchantment.DESTRUCTIVE, 1, null, 0);
        elemental("ElementalHoe", ItemsTC.elementalHoe.get(), new AspectList().add(Aspect.ORDER, 30).add(Aspect.PLANT, 30).add(Aspect.ENTROPY, 30),
                ItemsTC.thaumiumHoe.get(), Aspect.ORDER, Aspect.ENTROPY, null, 0, null, 0);
        // «Таумиумная крепость»: центр — таумиумная броня любой прочности; маски и очки — компонент шлема (Object[] оригинала)
        ThaumcraftApi.addInfusionCraftingRecipe(tc("ThaumiumFortressHelm"), new InfusionRecipe("ARMORFORTRESS", new ItemStack(ItemsTC.fortressHelm.get()), 3,
                new AspectList().add(Aspect.METAL, 50).add(Aspect.PROTECT, 20).add(Aspect.ENERGY, 25), new ItemStack(ItemsTC.thaumiumHelm.get()),
                TCTags.Items.PLATES_THAUMIUM, TCTags.Items.PLATES_THAUMIUM, new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.EMERALD)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("ThaumiumFortressChest"), new InfusionRecipe("ARMORFORTRESS", new ItemStack(ItemsTC.fortressChest.get()), 3,
                new AspectList().add(Aspect.METAL, 50).add(Aspect.PROTECT, 30).add(Aspect.ENERGY, 25), new ItemStack(ItemsTC.thaumiumChest.get()),
                TCTags.Items.PLATES_THAUMIUM, TCTags.Items.PLATES_THAUMIUM, TCTags.Items.PLATES_THAUMIUM, TCTags.Items.PLATES_THAUMIUM,
                new ItemStack(Items.GOLD_INGOT), Tags.Items.LEATHERS));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("ThaumiumFortressLegs"), new InfusionRecipe("ARMORFORTRESS", new ItemStack(ItemsTC.fortressLegs.get()), 3,
                new AspectList().add(Aspect.METAL, 50).add(Aspect.PROTECT, 25).add(Aspect.ENERGY, 25), new ItemStack(ItemsTC.thaumiumLegs.get()),
                TCTags.Items.PLATES_THAUMIUM, TCTags.Items.PLATES_THAUMIUM, TCTags.Items.PLATES_THAUMIUM, new ItemStack(Items.GOLD_INGOT),
                Tags.Items.LEATHERS));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("HelmGoggles"), new InfusionRecipe("FORTRESSMASK",
                new Object[] {thaumcraft.common.lib.TCDataComponents.FORTRESS_GOGGLES.get(), true}, 5,
                new AspectList().add(Aspect.SENSES, 40).add(Aspect.AURA, 20).add(Aspect.PROTECT, 20), new ItemStack(ItemsTC.fortressHelm.get()),
                new ItemStack(Items.SLIME_BALL), new ItemStack(ItemsTC.goggles.get())));
        // Items.DYE 0 / 15 / 1 (чернила / костная мука / красный краситель 1.12) → чернильный мешок / костная мука / красный краситель
        mask("MaskGrinningDevil", 0, new AspectList().add(Aspect.MIND, 80).add(Aspect.LIFE, 80).add(Aspect.PROTECT, 20), Items.INK_SAC,
                new ItemStack(BlocksTC.shimmerleaf.get()), new ItemStack(ItemsTC.brain.get()));
        mask("MaskAngryGhost", 1, new AspectList().add(Aspect.ENTROPY, 80).add(Aspect.DEATH, 80).add(Aspect.PROTECT, 20), Items.BONE_MEAL,
                new ItemStack(Items.POISONOUS_POTATO), new ItemStack(Items.WITHER_SKELETON_SKULL));
        mask("MaskSippingFiend", 2, new AspectList().add(Aspect.UNDEAD, 80).add(Aspect.LIFE, 80).add(Aspect.PROTECT, 20), Items.RED_DYE,
                new ItemStack(Items.GHAST_TEAR), new ItemStack(Items.MILK_BUCKET));
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
        // руническое усиление: доспехи и безделушки; в книге — «поддельные» RunicArmorFake0–2 с простым кольцом (заряд 0–2)
        ThaumcraftApi.addInfusionCraftingRecipe(tc("RunicArmor"), new InfusionRunicAugmentRecipe());
        for (int a = 0; a < 3; a++) {
            ItemStack in = new ItemStack(ItemsTC.ringMundane.get());
            if (a > 0) {
                in.set(thaumcraft.common.lib.TCDataComponents.RUNIC_CHARGE.get(), a);
            }
            ThaumcraftApi.addFakeCraftingRecipe(tc("RunicArmorFake" + a), new InfusionRunicAugmentRecipe(in));
        }
        // Items.DYE 15 — костная мука
        ThaumcraftApi.addInfusionCraftingRecipe(tc("LampGrowth"), new InfusionRecipe("LAMPGROWTH", new ItemStack(BlocksTC.lampGrowth.get()), 4,
                new AspectList().add(Aspect.PLANT, 20).add(Aspect.LIGHT, 15).add(Aspect.LIFE, 15).add(Aspect.TOOL, 15), new ItemStack(BlocksTC.lampArcane.get()),
                new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.BONE_MEAL), ThaumcraftApiHelper.makeCrystal(Aspect.EARTH), new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.BONE_MEAL), ThaumcraftApiHelper.makeCrystal(Aspect.EARTH)));
        ThaumcraftApi.addInfusionCraftingRecipe(tc("LampFertility"), new InfusionRecipe("LAMPFERTILITY", new ItemStack(BlocksTC.lampFertility.get()), 4,
                new AspectList().add(Aspect.BEAST, 20).add(Aspect.LIGHT, 15).add(Aspect.LIFE, 15).add(Aspect.DESIRE, 15), new ItemStack(BlocksTC.lampArcane.get()),
                new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.WHEAT), ThaumcraftApiHelper.makeCrystal(Aspect.FIRE), new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.CARROT), ThaumcraftApiHelper.makeCrystal(Aspect.FIRE)));
        // TODO этап 13: MindBiothaumic; этап 14: ArcaneBore; этапы 13–16: CausalityCollapser (алюментум)
    }

    private static void mask(String key, int mask, AspectList aspects, net.minecraft.world.item.Item dye, ItemStack c3, ItemStack c4) {
        ThaumcraftApi.addInfusionCraftingRecipe(tc(key), new InfusionRecipe("FORTRESSMASK",
                new Object[] {thaumcraft.common.lib.TCDataComponents.FORTRESS_MASK.get(), mask}, 8, aspects, new ItemStack(ItemsTC.fortressHelm.get()),
                new ItemStack(dye), TCTags.Items.PLATES_IRON, Tags.Items.LEATHERS, c3, c4, TCTags.Items.PLATES_IRON));
    }

    /** Бонусы плавки адской печи (конец initializeNormalRecipes оригинала); OreDictionary → теги c:. Вызывается при запуске. */
    public static void initializeSmeltingBonus() {
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_GOLD, new ItemStack(Items.GOLD_NUGGET));
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_IRON, new ItemStack(Items.IRON_NUGGET));
        ThaumcraftApi.addSmeltingBonus(TCTags.Items.ORES_CINNABAR, new ItemStack(ItemsTC.nuggetQuicksilver.get()));
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_COPPER, new ItemStack(ItemsTC.nuggetCopper.get()));
        ThaumcraftApi.addSmeltingBonus(cTag("ores/tin"), new ItemStack(ItemsTC.nuggetTin.get()));
        ThaumcraftApi.addSmeltingBonus(cTag("ores/silver"), new ItemStack(ItemsTC.nuggetSilver.get()));
        ThaumcraftApi.addSmeltingBonus(cTag("ores/lead"), new ItemStack(ItemsTC.nuggetLead.get()));
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_QUARTZ, new ItemStack(ItemsTC.nuggetQuartz.get()));
        ThaumcraftApi.addSmeltingBonus(ItemsTC.clusterIron.get(), new ItemStack(Items.IRON_NUGGET));
        ThaumcraftApi.addSmeltingBonus(ItemsTC.clusterGold.get(), new ItemStack(Items.GOLD_NUGGET));
        ThaumcraftApi.addSmeltingBonus(ItemsTC.clusterCinnabar.get(), new ItemStack(ItemsTC.nuggetQuicksilver.get()));
        ThaumcraftApi.addSmeltingBonus(ItemsTC.clusterCopper.get(), new ItemStack(ItemsTC.nuggetCopper.get()));
        ThaumcraftApi.addSmeltingBonus(ItemsTC.clusterTin.get(), new ItemStack(ItemsTC.nuggetTin.get()));
        ThaumcraftApi.addSmeltingBonus(ItemsTC.clusterSilver.get(), new ItemStack(ItemsTC.nuggetSilver.get()));
        ThaumcraftApi.addSmeltingBonus(ItemsTC.clusterLead.get(), new ItemStack(ItemsTC.nuggetLead.get()));
        ThaumcraftApi.addSmeltingBonus(ItemsTC.clusterQuartz.get(), new ItemStack(ItemsTC.nuggetQuartz.get()));
        ThaumcraftApi.addSmeltingBonus(Items.BEEF, new ItemStack(ItemsTC.chunkBeef.get()));
        ThaumcraftApi.addSmeltingBonus(Items.CHICKEN, new ItemStack(ItemsTC.chunkChicken.get()));
        ThaumcraftApi.addSmeltingBonus(Items.PORKCHOP, new ItemStack(ItemsTC.chunkPork.get()));
        ThaumcraftApi.addSmeltingBonus(net.minecraft.tags.ItemTags.FISHES, new ItemStack(ItemsTC.chunkFish.get()));
        ThaumcraftApi.addSmeltingBonus(Items.RABBIT, new ItemStack(ItemsTC.chunkRabbit.get()));
        ThaumcraftApi.addSmeltingBonus(Items.MUTTON, new ItemStack(ItemsTC.chunkMutton.get()));
        ItemStack re = new ItemStack(ItemsTC.nuggetRareEarth.get());
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_DIAMOND, re, 0.025F);
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_REDSTONE, re, 0.01F);
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_LAPIS, re, 0.01F);
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_EMERALD, re, 0.025F);
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_GOLD, re, 0.02F);
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_IRON, re, 0.01F);
        ThaumcraftApi.addSmeltingBonus(TCTags.Items.ORES_CINNABAR, re, 0.025F);
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_COPPER, re, 0.01F);
        ThaumcraftApi.addSmeltingBonus(cTag("ores/tin"), re, 0.01F);
        ThaumcraftApi.addSmeltingBonus(cTag("ores/silver"), re, 0.02F);
        ThaumcraftApi.addSmeltingBonus(cTag("ores/lead"), re, 0.01F);
        ThaumcraftApi.addSmeltingBonus(Tags.Items.ORES_QUARTZ, re, 0.01F);
        for (net.minecraft.world.item.Item c : new net.minecraft.world.item.Item[] {ItemsTC.clusterIron.get(), ItemsTC.clusterGold.get(),
                ItemsTC.clusterCopper.get(), ItemsTC.clusterTin.get(), ItemsTC.clusterSilver.get(), ItemsTC.clusterLead.get(),
                ItemsTC.clusterCinnabar.get(), ItemsTC.clusterQuartz.get()}) {
            ThaumcraftApi.addSmeltingBonus(c, re, 0.02F);
        }
    }

    private static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> cTag(String path) {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static void elemental(String key, net.minecraft.world.item.Item out, AspectList aspects, net.minecraft.world.item.Item center, Aspect c1, Aspect c2,
                                  EnumInfusionEnchantment e1, int l1, EnumInfusionEnchantment e2, int l2) {
        ItemStack result = new ItemStack(out);
        if (e1 != null) {
            EnumInfusionEnchantment.addInfusionEnchantment(result, e1, l1);
        }
        if (e2 != null) {
            EnumInfusionEnchantment.addInfusionEnchantment(result, e2, l2);
        }
        ThaumcraftApi.addInfusionCraftingRecipe(tc(key), new InfusionRecipe("ELEMENTALTOOLS", result, 1, aspects, new ItemStack(center),
                ThaumcraftApiHelper.makeCrystal(c1), ThaumcraftApiHelper.makeCrystal(c2), new ItemStack(ItemsTC.nuggetRareEarth.get()),
                new ItemStack(BlocksTC.plankGreatwood.get())));
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
        // катализатор — уголь любого вида (Items.COAL, 32767: уголь и древесный уголь)
        ThaumcraftApi.addCrucibleRecipe(id("alumentum"), new CrucibleRecipe("ALUMENTUM", new ItemStack(ItemsTC.alumentum.get()),
                net.minecraft.tags.ItemTags.COALS, new AspectList().merge(Aspect.ENERGY, 10).merge(Aspect.FIRE, 10).merge(Aspect.ENTROPY, 5)));
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
        ThaumcraftApi.addCrucibleRecipe(id("liquiddeath"), new CrucibleRecipe("LIQUIDDEATH", new ItemStack(ItemsTC.bucketDeath.get()),
                new ItemStack(Items.BUCKET), new AspectList().add(Aspect.DEATH, 100).add(Aspect.ALCHEMY, 20).add(Aspect.ENTROPY, 50)));
        // TODO этап 15.6: BottleTaint (из фиала Выброса): FLUX 30, WATER 30
        ThaumcraftApi.addCrucibleRecipe(id("sanesoap"), new CrucibleRecipe("SANESOAP", new ItemStack(ItemsTC.sanitySoap.get()),
                new ItemStack(BlocksTC.fleshBlock.get()), new AspectList().add(Aspect.MIND, 75).add(Aspect.ELDRITCH, 50).add(Aspect.ORDER, 75).add(Aspect.LIFE, 50)));
        ThaumcraftApi.addCrucibleRecipe(id("bathsalts"), new CrucibleRecipe("BATHSALTS", new ItemStack(ItemsTC.bathSalts.get()),
                new ItemStack(ItemsTC.salisMundus.get()), new AspectList().add(Aspect.MIND, 40).add(Aspect.AIR, 40).add(Aspect.ORDER, 40).add(Aspect.LIFE, 40)));
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
