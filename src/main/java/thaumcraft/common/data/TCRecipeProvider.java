package thaumcraft.common.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.lib.TCTags;
import thaumcraft.common.lib.crafting.RecipeMagicDust;
import thaumcraft.common.lib.crafting.RecipeTripleMeatTreat;

import static thaumcraft.api.items.ItemsTC.*;

/**
 * Рецепты верстака и печи (порт ConfigRecipes.initializeNormalRecipes / initializeSmelting / ModConfig.modCompatibility).
 * Имена рецептов — как в оригинале (в нижнем регистре), группы — как в оригинале. Добавляются по мере переноса предметов.
 */
public class TCRecipeProvider extends RecipeProvider {
    private static final String BRASS = "thaumcraft:brass_stuff";
    private static final String THAUMIUM = "thaumcraft:thaumium_stuff";
    private static final String VOID = "thaumcraft:void_stuff";
    private static final String CANDLES = "thaumcraft:tallowcandles";
    private static final String INKWELL = "thaumcraft:inkwell";
    private static final String BAUBLES = "thaumcraft:baubles_stuff";

    public TCRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        // --- слитки ↔ самородки ---
        toNuggets(out, "thaumiumtonuggets", ingotThaumium.get(), nuggetThaumium.get(), null);
        toNuggets(out, "voidtonuggets", ingotVoid.get(), nuggetVoid.get(), null);
        toNuggets(out, "brasstonuggets", ingotBrass.get(), nuggetBrass.get(), null);
        toNuggets(out, "quartztonuggets", Items.QUARTZ, nuggetQuartz.get(), null);
        toNuggets(out, "quicksilvertonuggets", quicksilver.get(), nuggetQuicksilver.get(), null);
        fromNuggets(out, "nuggetstothaumium", TCTags.Items.NUGGETS_THAUMIUM, ingotThaumium.get(), null);
        fromNuggets(out, "nuggetstovoid", TCTags.Items.NUGGETS_VOID, ingotVoid.get(), null);
        fromNuggets(out, "nuggetstobrass", TCTags.Items.NUGGETS_BRASS, ingotBrass.get(), null);
        fromNuggets(out, "nuggetstoquicksilver", TCTags.Items.NUGGETS_QUICKSILVER, quicksilver.get(), null);

        // --- блоки металлов ---
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlocksTC.metalBlockThaumium.get()).group(THAUMIUM)
                .pattern("###").pattern("###").pattern("###").define('#', ingotThaumium.get())
                .unlockedBy("has", has(ingotThaumium.get())).save(out, id("thaumiumingotstoblock"));
        single(out, "thaumiumblocktoingots", THAUMIUM, BlocksTC.metalBlockThaumium.get(), ingotThaumium.get(), 9);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlocksTC.metalBlockVoid.get()).group(VOID)
                .pattern("###").pattern("###").pattern("###").define('#', ingotVoid.get())
                .unlockedBy("has", has(ingotVoid.get())).save(out, id("voidingotstoblock"));
        single(out, "voidblocktoingots", VOID, BlocksTC.metalBlockVoid.get(), ingotVoid.get(), 9);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlocksTC.metalBlockBrass.get()).group(BRASS)
                .pattern("###").pattern("###").pattern("###").define('#', ingotBrass.get())
                .unlockedBy("has", has(ingotBrass.get())).save(out, id("brassingotstoblock"));
        single(out, "brassblocktoingots", BRASS, BlocksTC.metalBlockBrass.get(), ingotBrass.get(), 9);

        // --- плоть, янтарь ---
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlocksTC.fleshBlock.get())
                .pattern("###").pattern("###").pattern("###").define('#', Items.ROTTEN_FLESH)
                .unlockedBy("has", has(Items.ROTTEN_FLESH)).save(out, id("fleshtoblock"));
        single(out, "blocktoflesh", null, BlocksTC.fleshBlock.get(), Items.ROTTEN_FLESH, 9);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlocksTC.amberBlock.get())
                .pattern("##").pattern("##").define('#', TCTags.Items.GEMS_AMBER)
                .unlockedBy("has", has(TCTags.Items.GEMS_AMBER)).save(out, id("ambertoblock"));
        square(out, "amberblocktobrick", BlocksTC.amberBlock.get(), BlocksTC.amberBrick.get(), 4);
        square(out, "amberbricktoblock", BlocksTC.amberBrick.get(), BlocksTC.amberBlock.get(), 4);
        single(out, "amberblocktoamber", null, BlocksTC.amberBlock.get(), amber.get(), 4);

        // --- пластины ---
        plate(out, "ironplate", null, Tags.Items.INGOTS_IRON, plateIron.get());
        plate(out, "brassplate", BRASS, TCTags.Items.INGOTS_BRASS, plateBrass.get());
        plate(out, "thaumiumplate", THAUMIUM, TCTags.Items.INGOTS_THAUMIUM, plateThaumium.get());
        plate(out, "voidplate", VOID, TCTags.Items.INGOTS_VOID, plateVoid.get());

        // --- таумиумные и пустотные инструменты и броня (oreDictRecipe оригинала; "stickWood" → c:rods/wooden) ---
        gear(out, "thaumium", THAUMIUM, TCTags.Items.INGOTS_THAUMIUM, thaumiumHelm.get(), thaumiumChest.get(), thaumiumLegs.get(), thaumiumBoots.get(),
                thaumiumShovel.get(), thaumiumPick.get(), thaumiumAxe.get(), thaumiumHoe.get(), thaumiumSword.get());
        gear(out, "void", VOID, TCTags.Items.INGOTS_VOID, voidHelm.get(), voidChest.get(), voidLegs.get(), voidBoots.get(),
                voidShovel.get(), voidPick.get(), voidAxe.get(), voidHoe.get(), voidSword.get());

        // --- безделушки (группа baubles_stuff; имена рецептов оригинала, в т.ч. опечатка "babule") ---
        bauble(out, "babuleamulet", amuletMundane.get(), new String[] {" S ", "S S", " I "}, 'S', Tags.Items.STRINGS, 'I', TCTags.Items.INGOTS_BRASS);
        bauble(out, "babulering", ringMundane.get(), new String[] {"NNN", "N N", "NNN"}, 'N', TCTags.Items.NUGGETS_BRASS, null, null);
        bauble(out, "babulegirdle", girdleMundane.get(), new String[] {" L ", "L L", " I "}, 'L', Tags.Items.LEATHERS, 'I', TCTags.Items.INGOTS_BRASS);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, amuletFancy.get()).group(BAUBLES).pattern(" S ").pattern("SGS").pattern(" I ")
                .define('S', Tags.Items.STRINGS).define('G', Tags.Items.GEMS_DIAMOND).define('I', Tags.Items.INGOTS_GOLD)
                .unlockedBy("has", has(Tags.Items.GEMS_DIAMOND)).save(out, id("babuleamuletfancy"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ringFancy.get()).group(BAUBLES).pattern("NGN").pattern("N N").pattern("NNN")
                .define('G', Tags.Items.GEMS_DIAMOND).define('N', Tags.Items.NUGGETS_GOLD)
                .unlockedBy("has", has(Tags.Items.GEMS_DIAMOND)).save(out, id("babuleringfancy"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, girdleFancy.get()).group(BAUBLES).pattern(" L ").pattern("LGL").pattern(" I ")
                .define('L', Tags.Items.LEATHERS).define('G', Tags.Items.GEMS_DIAMOND).define('I', Tags.Items.INGOTS_GOLD)
                .unlockedBy("has", has(Tags.Items.GEMS_DIAMOND)).save(out, id("babulegirdlefancy"));

        // --- растения ---
        single(out, "shimmerleaftoquicksilver", null, BlocksTC.shimmerleaf.get(), quicksilver.get(), 1);
        single(out, "cinderpearltoblazepowder", null, BlocksTC.cinderpearl.get(), Items.BLAZE_POWDER, 1);

        // --- дерево и камень ---
        single(out, "plankgreatwood", null, BlocksTC.logGreatwood.get(), BlocksTC.plankGreatwood.get(), 4);
        single(out, "planksilverwood", null, BlocksTC.logSilverwood.get(), BlocksTC.plankSilverwood.get(), 4);
        stairs(out, "stairsgreatwood", BlocksTC.plankGreatwood.get(), BlocksTC.stairsGreatwood.get());
        stairs(out, "stairssilverwood", BlocksTC.plankSilverwood.get(), BlocksTC.stairsSilverwood.get());
        stairs(out, "stairsarcane", BlocksTC.stoneArcane.get(), BlocksTC.stairsArcane.get());
        stairs(out, "stairsarcanebrick", BlocksTC.stoneArcaneBrick.get(), BlocksTC.stairsArcaneBrick.get());
        stairs(out, "stairsancient", BlocksTC.stoneAncient.get(), BlocksTC.stairsAncient.get());
        // StoneArcane: 8 камня (OreDictionary "stone" = minecraft:stone) + вис-кристалл (любой аспект) → 9
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlocksTC.stoneArcane.get(), 9)
                .pattern("KKK").pattern("KCK").pattern("KKK").define('K', Items.STONE).define('C', crystalEssence.get())
                .unlockedBy("has", has(crystalEssence.get())).save(out, id("stonearcane"));
        square(out, "brickarcane", BlocksTC.stoneArcane.get(), BlocksTC.stoneArcaneBrick.get(), 4);
        slab(out, "slabgreatwood", BlocksTC.plankGreatwood.get(), BlocksTC.slabGreatwood.get());
        slab(out, "slabsilverwood", BlocksTC.plankSilverwood.get(), BlocksTC.slabSilverwood.get());
        slab(out, "slabarcanestone", BlocksTC.stoneArcane.get(), BlocksTC.slabArcaneStone.get());
        slab(out, "slabarcanebrick", BlocksTC.stoneArcaneBrick.get(), BlocksTC.slabArcaneBrick.get());
        slab(out, "slabancient", BlocksTC.stoneAncient.get(), BlocksTC.slabAncient.get());
        slab(out, "slabeldritch", BlocksTC.stoneEldritchTile.get(), BlocksTC.slabEldritch.get());

        // --- столы и письменные принадлежности ---
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, BlocksTC.tableWood.get())
                .pattern("SSS").pattern("W W").define('S', ItemTags.WOODEN_SLABS).define('W', ItemTags.PLANKS)
                .unlockedBy("has", has(ItemTags.PLANKS)).save(out, id("tablewood"));
        // Blocks.STONE_SLAB (meta 0) 1.12 = гладкая каменная плита 1.21; "stone" = minecraft:stone
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, BlocksTC.tableStone.get())
                .pattern("SSS").pattern("W W").define('S', Items.SMOOTH_STONE_SLAB).define('W', Items.STONE)
                .unlockedBy("has", has(Items.STONE)).save(out, id("tablestone"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, scribingTools.get()).group(INKWELL)
                .requires(phial.get()).requires(Items.FEATHER).requires(Tags.Items.DYES_BLACK)
                .unlockedBy("has", has(Items.FEATHER)).save(out, id("scribingtoolscraft1"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, scribingTools.get()).group(INKWELL)
                .requires(Items.GLASS_BOTTLE).requires(Items.FEATHER).requires(Tags.Items.DYES_BLACK)
                .unlockedBy("has", has(Items.FEATHER)).save(out, id("scribingtoolscraft2"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, scribingTools.get()).group(INKWELL)
                .requires(scribingTools.get()).requires(Tags.Items.DYES_BLACK)
                .unlockedBy("has", has(scribingTools.get())).save(out, id("scribingtoolsrefill"));

        // --- свечи ---
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, BlocksTC.candles.get(DyeColor.WHITE).get(), 3).group(CANDLES)
                .pattern(" S ").pattern(" T ").pattern(" T ").define('S', Tags.Items.STRINGS).define('T', tallow.get())
                .unlockedBy("has", has(tallow.get())).save(out, id("tallowcandle"));
        Ingredient anyCandle = Ingredient.of(BlocksTC.candles.values().stream().map(b -> (ItemLike) b.get()).toArray(ItemLike[]::new));
        for (DyeColor dye : DyeColor.values()) {
            // В оригинале имя рецепта — "TallowCandle" + ключ перевода красителя (lightBlue, silver …)
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, BlocksTC.candles.get(dye).get()).group(CANDLES)
                    .requires(dye.getTag()).requires(anyCandle)
                    .unlockedBy("has", has(tallow.get())).save(out, id("tallowcandle" + legacyDyeName(dye)));
        }

        // --- окраска нитора (initializeAlchemyRecipes оригинала): краситель + любой нитор ---
        for (DyeColor dye : DyeColor.values()) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, BlocksTC.nitor.get(dye).get()).group("thaumcraft:nitorgroup")
                    .requires(dye.getTag()).requires(TCTags.Items.NITOR)
                    .unlockedBy("has", has(TCTags.Items.NITOR)).save(out, id("nitordye" + legacyDyeName(dye)));
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, jarBrace.get(), 2)
                .pattern("NSN").pattern("S S").pattern("NSN").define('N', TCTags.Items.NUGGETS_BRASS).define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has", has(TCTags.Items.NUGGETS_BRASS)).save(out, id("brassbrace"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, golemBell.get())
                .pattern(" QQ").pattern(" QQ").pattern("S  ").define('Q', Tags.Items.GEMS_QUARTZ).define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has", has(Tags.Items.GEMS_QUARTZ)).save(out, id("golembell"));

        // фиалы: "blockGlass" — любое стекло
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, phial.get(), 8)
                .pattern(" C ").pattern("G G").pattern(" G ").define('G', Tags.Items.GLASS_BLOCKS).define('C', Items.CLAY_BALL)
                .unlockedBy("has", has(Items.CLAY_BALL)).save(out, id("phial"));

        // --- ярлыки: чистый, с аспектом (чистый + фиал этого аспекта), снятие пометки ---
        String labels = "thaumcraft:jarlabels";
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, label.get(), 4).group(labels)
                .requires(Tags.Items.DYES_BLACK).requires(Tags.Items.SLIME_BALLS).requires(Items.PAPER, 4)
                .unlockedBy("has", has(Items.PAPER)).save(out, id("jarlabel"));
        for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.aspects.values()) {
            net.minecraft.world.item.ItemStack marked = new net.minecraft.world.item.ItemStack(labelFilled.get());
            marked.set(thaumcraft.common.lib.TCDataComponents.ASPECTS.get(), new thaumcraft.api.aspects.AspectList().add(aspect, 1));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, marked).group(labels)
                    .requires(label.get())
                    .requires(net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false, thaumcraft.common.lib.TCDataComponents.ASPECTS,
                            new thaumcraft.api.aspects.AspectList().add(aspect, 10), phialFilled.get()))
                    .unlockedBy("has", has(label.get())).save(out, id("label_" + aspect.getTag()));
        }
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, label.get()).group(labels).requires(labelFilled.get())
                .unlockedBy("has", has(labelFilled.get())).save(out, id("jarlabelnull"));

        // --- печь (initializeSmelting) ---
        smelt(out, "smelt_cinnabar_ore", BlocksTC.oreCinnabar.get(), quicksilver.get(), 1, 1.0F);
        smelt(out, "smelt_amber_ore", BlocksTC.oreAmber.get(), amber.get(), 1, 1.0F);
        smelt(out, "smelt_quartz_ore", BlocksTC.oreQuartz.get(), Items.QUARTZ, 1, 1.0F);
        smelt(out, "smelt_greatwood_log", BlocksTC.logGreatwood.get(), Items.CHARCOAL, 1, 0.5F);
        smelt(out, "smelt_silverwood_log", BlocksTC.logSilverwood.get(), Items.CHARCOAL, 1, 0.5F);
        smelt(out, "smelt_iron_cluster", clusterIron.get(), Items.IRON_INGOT, 2, 1.0F);
        smelt(out, "smelt_gold_cluster", clusterGold.get(), Items.GOLD_INGOT, 2, 1.0F);
        smelt(out, "smelt_cinnabar_cluster", clusterCinnabar.get(), quicksilver.get(), 2, 1.0F);
        smelt(out, "smelt_quartz_cluster", clusterQuartz.get(), Items.QUARTZ, 2, 1.0F);

        // --- ModConfig.modCompatibility: медь в 1.21 ванильная, поэтому ветка «есть ingotCopper» срабатывает всегда ---
        toNuggets(out, "coppertonuggets", Items.COPPER_INGOT, nuggetCopper.get(), null);
        fromNuggets(out, "coppernuggetstoingot", TCTags.Items.NUGGETS_COPPER, Items.COPPER_INGOT, null);
        smelt(out, "smelt_copper_cluster", clusterCopper.get(), Items.COPPER_INGOT, 2, 1.0F);
        // Олово/серебро/свинец: только слиток → самородки, если такие слитки есть (другие моды).
        // Самородки → слиток и плавка кластеров требуют конкретного предмета-результата (в оригинале — первый слиток из OreDictionary),
        // в рецептах 1.21 результат-тег невозможен — см. DEVIATIONS.md.
        tagToNuggets(out, "tintonuggets", tag("ingots/tin"), nuggetTin.get());
        tagToNuggets(out, "silvertonuggets", tag("ingots/silver"), nuggetSilver.get());
        tagToNuggets(out, "leadtonuggets", tag("ingots/lead"), nuggetLead.get());

        // Особый рецепт: сахар + 3 разных кусочка мяса (исправление ошибки оригинала, DEVIATIONS №10)
        SpecialRecipeBuilder.special(RecipeTripleMeatTreat::new).save(out, id("triplemeattreat"));
        SpecialRecipeBuilder.special(RecipeMagicDust::new).save(out, id("salismundus"));
    }

    private static String legacyDyeName(DyeColor dye) {
        return switch (dye) {
            case LIGHT_BLUE -> "lightblue";
            case LIGHT_GRAY -> "silver";
            default -> dye.getSerializedName();
        };
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name);
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    /** Шлем, нагрудник, поножи, ботинки, лопата, кирка, топор, мотыга, меч — узоры и имена рецептов оригинала. */
    private void gear(RecipeOutput out, String p, String group, TagKey<Item> ingot, ItemLike helm, ItemLike chest, ItemLike legs, ItemLike boots,
                      ItemLike shovel, ItemLike pick, ItemLike axe, ItemLike hoe, ItemLike sword) {
        armorPiece(out, p + "helm", group, ingot, helm, "III", "I I");
        armorPiece(out, p + "chest", group, ingot, chest, "I I", "III", "III");
        armorPiece(out, p + "legs", group, ingot, legs, "III", "I I", "I I");
        armorPiece(out, p + "boots", group, ingot, boots, "I I", "I I");
        toolPiece(out, p + "shovel", group, ingot, shovel, "I", "S", "S");
        toolPiece(out, p + "pick", group, ingot, pick, "III", " S ", " S ");
        toolPiece(out, p + "axe", group, ingot, axe, "II", "SI", "S ");
        toolPiece(out, p + "hoe", group, ingot, hoe, "II", "S ", "S ");
        toolPiece(out, p + "sword", group, ingot, sword, "I", "I", "S");
    }

    private void armorPiece(RecipeOutput out, String name, String group, TagKey<Item> ingot, ItemLike result, String... rows) {
        ShapedRecipeBuilder b = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result).group(group).define('I', ingot);
        for (String r : rows) {
            b.pattern(r);
        }
        b.unlockedBy("has", has(ingot)).save(out, id(name));
    }

    private void toolPiece(RecipeOutput out, String name, String group, TagKey<Item> ingot, ItemLike result, String... rows) {
        ShapedRecipeBuilder b = ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result).group(group).define('I', ingot)
                .define('S', Tags.Items.RODS_WOODEN);
        for (String r : rows) {
            b.pattern(r);
        }
        b.unlockedBy("has", has(ingot)).save(out, id(name));
    }

    private void bauble(RecipeOutput out, String name, ItemLike result, String[] rows, char k1, TagKey<Item> t1, Character k2, TagKey<Item> t2) {
        ShapedRecipeBuilder b = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result).group(BAUBLES).define(k1, t1);
        if (k2 != null) {
            b.define(k2, t2);
        }
        for (String r : rows) {
            b.pattern(r);
        }
        b.unlockedBy("has", has(t1)).save(out, id(name));
    }

    private void toNuggets(RecipeOutput out, String name, ItemLike ingot, ItemLike nugget, String group) {
        single(out, name, group, ingot, nugget, 9);
    }

    private void fromNuggets(RecipeOutput out, String name, TagKey<Item> nuggets, ItemLike ingot, String group) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ingot).group(group)
                .pattern("###").pattern("###").pattern("###").define('#', nuggets)
                .unlockedBy("has", has(nuggets)).save(out, id(name));
    }

    private void tagToNuggets(RecipeOutput out, String name, TagKey<Item> ingots, ItemLike nugget) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, nugget, 9).pattern("#").define('#', ingots)
                .unlockedBy("has", has(ingots))
                .save(out.withConditions(new NotCondition(new TagEmptyCondition(ingots))), id(name));
    }

    /** Рецепт «один предмет в сетке» (в оригинале — shaped "#"). */
    private void single(RecipeOutput out, String name, String group, ItemLike in, ItemLike result, int count) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, count).group(group).pattern("#").define('#', in)
                .unlockedBy("has", has(in)).save(out, id(name));
    }

    private void square(RecipeOutput out, String name, ItemLike in, ItemLike result, int count) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, count).pattern("##").pattern("##").define('#', in)
                .unlockedBy("has", has(in)).save(out, id(name));
    }

    private void plate(RecipeOutput out, String name, String group, TagKey<Item> ingots, ItemLike plate) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, plate, 3).group(group).pattern("BBB").define('B', ingots)
                .unlockedBy("has", has(ingots)).save(out, id(name));
    }

    private void stairs(RecipeOutput out, String name, ItemLike in, ItemLike result) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, 4).pattern("K  ").pattern("KK ").pattern("KKK").define('K', in)
                .unlockedBy("has", has(in)).save(out, id(name));
    }

    private void slab(RecipeOutput out, String name, ItemLike in, ItemLike result) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, 6).pattern("KKK").define('K', in)
                .unlockedBy("has", has(in)).save(out, id(name));
    }

    private void smelt(RecipeOutput out, String name, ItemLike in, ItemLike result, int count, float xp) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(in), RecipeCategory.MISC, new net.minecraft.world.item.ItemStack(result, count), xp, 200)
                .unlockedBy("has", has(in)).save(out, id(name));
    }
}
