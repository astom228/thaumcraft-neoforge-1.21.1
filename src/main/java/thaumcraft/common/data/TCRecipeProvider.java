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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.lib.TCTags;
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
        // TODO этап 4: StoneArcane — 8 камня + кристалл эссенции → 9 магического камня
        square(out, "brickarcane", BlocksTC.stoneArcane.get(), BlocksTC.stoneArcaneBrick.get(), 4);
        slab(out, "slabgreatwood", BlocksTC.plankGreatwood.get(), BlocksTC.slabGreatwood.get());
        slab(out, "slabsilverwood", BlocksTC.plankSilverwood.get(), BlocksTC.slabSilverwood.get());
        slab(out, "slabarcanestone", BlocksTC.stoneArcane.get(), BlocksTC.slabArcaneStone.get());
        slab(out, "slabarcanebrick", BlocksTC.stoneArcaneBrick.get(), BlocksTC.slabArcaneBrick.get());
        slab(out, "slabancient", BlocksTC.stoneAncient.get(), BlocksTC.slabAncient.get());
        slab(out, "slabeldritch", BlocksTC.stoneEldritchTile.get(), BlocksTC.slabEldritch.get());

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, jarBrace.get(), 2)
                .pattern("NSN").pattern("S S").pattern("NSN").define('N', TCTags.Items.NUGGETS_BRASS).define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has", has(TCTags.Items.NUGGETS_BRASS)).save(out, id("brassbrace"));

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
