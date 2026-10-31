package thaumcraft.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.Part;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.common.lib.InternalMethodHandler;

/** Порт ThaumcraftApi (содержимое добавляется по этапам). */
public class ThaumcraftApi {
    /** В оригинале задаётся в CommonProxy.preInit. */
    public static IInternalMethodHandler internalMethods = new InternalMethodHandler();

    // --- Исследования ------------------------------------------------------------------------------------------------

    /**
     * Файл JSON исследований для загрузки: assets/&lt;namespace&gt;/&lt;path&gt;(.json) из ресурсов мода.
     * Регистрировать при FMLCommonSetupEvent (enqueueWork); загрузка — после завершения запуска всех модов.
     */
    public static void registerResearchLocation(ResourceLocation loc) {
        CommonInternals.jsonLocs.putIfAbsent(loc.toString(), loc);
    }

    // --- Рецепты -------------------------------------------------------------------------------------------------------

    /**
     * Рецепт только для показа на страницах Таумономикона (в игре не работает): например, соль мира с «любыми» кристаллами.
     * Регистрировать при FMLCommonSetupEvent.
     */
    public static void addFakeCraftingRecipe(ResourceLocation registry, Object recipe) {
        CommonInternals.craftingRecipeCatalogFake.put(registry, recipe);
    }

    public static Map<ResourceLocation, IThaumcraftRecipe> getCraftingRecipes() {
        return CommonInternals.craftingRecipeCatalog;
    }

    public static Map<ResourceLocation, Object> getCraftingRecipesFake() {
        return CommonInternals.craftingRecipeCatalogFake;
    }

    /** Рецепт тигля. Регистрировать при FMLCommonSetupEvent (на обеих сторонах). */
    public static void addCrucibleRecipe(ResourceLocation registry, CrucibleRecipe recipe) {
        getCraftingRecipes().put(registry, recipe);
    }

    /** Рецепт наполнения. Регистрировать при FMLCommonSetupEvent (на обеих сторонах). */
    public static void addInfusionCraftingRecipe(ResourceLocation registry, InfusionRecipe recipe) {
        getCraftingRecipes().put(registry, recipe);
    }

    /** Первый рецепт наполнения с таким результатом-предметом (isItemEqual оригинала — без учёта компонентов). */
    public static InfusionRecipe getInfusionRecipe(ItemStack res) {
        synchronized (CommonInternals.craftingRecipeCatalog) {
            for (Object r : getCraftingRecipes().values()) {
                if (r instanceof InfusionRecipe ir && ir.getRecipeOutput() instanceof ItemStack out && ItemStack.isSameItem(out, res)) {
                    return ir;
                }
            }
        }
        return null;
    }

    /** Схема многоблочной конструкции для книги (страница «Конструкция»). Регистрировать при FMLCommonSetupEvent. */
    public static void addMultiblockRecipeToCatalog(ResourceLocation registry, BluePrint recipe) {
        getCraftingRecipes().put(registry, recipe);
    }

    /** Первый рецепт тигля с таким результатом (предмет без учёта компонентов — isItemEqual оригинала). */
    public static CrucibleRecipe getCrucibleRecipe(ItemStack stack) {
        synchronized (CommonInternals.craftingRecipeCatalog) {
            for (Object r : getCraftingRecipes().values()) {
                if (r instanceof CrucibleRecipe cr && ItemStack.isSameItem(cr.getRecipeOutput(), stack)) {
                    return cr;
                }
            }
        }
        return null;
    }

    public static CrucibleRecipe getCrucibleRecipeFromHash(int hash) {
        synchronized (CommonInternals.craftingRecipeCatalog) {
            for (Object recipe : getCraftingRecipes().values()) {
                if (recipe instanceof CrucibleRecipe cr && cr.hash == hash) {
                    return cr;
                }
            }
        }
        return null;
    }

    // --- Искажение -----------------------------------------------------------------------------------------------------

    /** Искажение, которое получает игрок, создав предмет (по верстаку и другим способам, вызывающим событие крафта). */
    public static void addWarpToItem(ItemStack craftresult, int amount) {
        CommonInternals.warpMap.put(craftresult.getItem(), amount);
    }

    public static int getWarp(ItemStack in) {
        if (in == null) {
            return 0;
        }
        return CommonInternals.warpMap.getOrDefault(in.getItem(), 0);
    }

    // --- Аспекты предметов -------------------------------------------------------------------------------------------

    /**
     * Есть ли у предмета явно заданные аспекты. В оригинале таблица искалась по строке, а ключи были числами,
     * поэтому метод почти всегда возвращал false — это влияет на registerComplexObjectTag и сохранено 1:1.
     */
    public static boolean exists(ItemStack item) {
        return false;
    }

    public static void registerObjectTag(ItemStack item, AspectList aspects) {
        if (aspects == null) {
            aspects = new AspectList();
        }
        try {
            CommonInternals.objectTags().put(CommonInternals.generateUniqueItemstackId(item), aspects);
        } catch (Exception ignored) {
        }
    }

    /** В оригинале — по имени OreDictionary; в порте — по тегу предметов (каждому предмету тега). */
    public static void registerObjectTag(TagKey<Item> tag, AspectList aspects) {
        if (aspects == null) {
            aspects = new AspectList();
        }
        for (ItemStack ore : ThaumcraftApiHelper.getOresWithWildCards(tag)) {
            try {
                ItemStack oc = ore.copy();
                oc.setCount(1);
                registerObjectTag(oc, aspects.copy());
            } catch (Exception ignored) {
            }
        }
    }

    public static void registerComplexObjectTag(ItemStack item, AspectList aspects) {
        if (!exists(item)) {
            AspectList tmp = AspectHelper.generateTags(item);
            if (tmp != null && tmp.size() > 0) {
                for (Aspect tag : tmp.getAspects()) {
                    aspects.add(tag, tmp.getAmount(tag));
                }
            }
            registerObjectTag(item, aspects);
        } else {
            AspectList tmp = AspectHelper.getObjectAspects(item);
            for (Aspect tag : aspects.getAspects()) {
                tmp.merge(tag, tmp.getAmount(tag));
            }
            registerObjectTag(item, tmp);
        }
    }

    public static void registerComplexObjectTag(TagKey<Item> tag, AspectList aspects) {
        if (aspects == null) {
            aspects = new AspectList();
        }
        for (ItemStack ore : ThaumcraftApiHelper.getOresWithWildCards(tag)) {
            try {
                ItemStack oc = ore.copy();
                oc.setCount(1);
                registerComplexObjectTag(oc, aspects.copy());
            } catch (Exception ignored) {
            }
        }
    }

    // --- Аспекты существ ---------------------------------------------------------------------------------------------

    /** entityName — id типа сущности 1.21 (в оригинале — имя EntityList 1.12, см. ConfigAspects). */
    public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT... nbt) {
        CommonInternals.scanEntities.add(new EntityTags(entityName, aspects, nbt));
    }

    /** Значение NBT по ключу (порт ThaumcraftApiHelper.getNBTDataFromId). */
    public static Object getNBTDataFromId(CompoundTag tag, String key) {
        Tag t = tag.get(key);
        if (t instanceof NumericTag n) {
            return switch (t.getId()) {
                case Tag.TAG_BYTE -> n.getAsByte();
                case Tag.TAG_SHORT -> n.getAsShort();
                case Tag.TAG_INT -> n.getAsInt();
                case Tag.TAG_LONG -> n.getAsLong();
                case Tag.TAG_FLOAT -> n.getAsFloat();
                case Tag.TAG_DOUBLE -> n.getAsDouble();
                default -> null;
            };
        }
        if (t instanceof StringTag s) {
            return s.getAsString();
        }
        return null;
    }

    public static class EntityTags {
        public String entityName;
        public EntityTagsNBT[] nbts;
        public AspectList aspects;

        public EntityTags(String entityName, AspectList aspects, EntityTagsNBT... nbts) {
            this.entityName = entityName;
            this.nbts = nbts;
            this.aspects = aspects;
        }
    }

    public static class EntityTagsNBT {
        public String name;
        public Object value;

        public EntityTagsNBT(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }


    /** Схема многоблочной конструкции (порт ThaumcraftApi.BluePrint): слои сверху вниз, ингредиенты для книги. */
    public static class BluePrint implements IThaumcraftRecipe {
        Part[][][] parts;
        String research;
        ItemStack displayStack;
        ItemStack[] ingredientList;
        private String group;

        public BluePrint(String research, Part[][][] parts, ItemStack... ingredientList) {
            this.parts = parts;
            this.research = research;
            this.ingredientList = ingredientList;
        }

        public BluePrint(String research, ItemStack display, Part[][][] parts, ItemStack... ingredientList) {
            this.parts = parts;
            this.research = research;
            this.displayStack = display;
            this.ingredientList = ingredientList;
        }

        public Part[][][] getParts() {
            return this.parts;
        }

        @Override
        public String getResearch() {
            return this.research;
        }

        public ItemStack[] getIngredientList() {
            return this.ingredientList;
        }

        public ItemStack getDisplayStack() {
            return this.displayStack;
        }

        @Override
        public String getGroup() {
            return this.group;
        }

        public BluePrint setGroup(ResourceLocation loc) {
            this.group = loc.toString();
            return this;
        }
    }

    /**
     * Добавить предмет в мешки с добычей (addLootBagItem): weight — вес, bagTypes — 0 обычный, 1 необычный, 2 редкий;
     * без типов — во все три.
     */
    public static void addLootBagItem(net.minecraft.world.item.ItemStack item, int weight, int... bagTypes) {
        if (bagTypes == null || bagTypes.length == 0) {
            bagTypes = new int[] {0, 1, 2};
        }
        for (int rarity : bagTypes) {
            java.util.List<thaumcraft.common.lib.utils.WeightedRandomLoot> list = switch (rarity) {
                case 1 -> thaumcraft.common.lib.utils.WeightedRandomLoot.lootBagUncommon;
                case 2 -> thaumcraft.common.lib.utils.WeightedRandomLoot.lootBagRare;
                default -> thaumcraft.common.lib.utils.WeightedRandomLoot.lootBagCommon;
            };
            list.add(new thaumcraft.common.lib.utils.WeightedRandomLoot(item, weight));
        }
    }

    /**
     * Бонус плавки адской печи: in — предмет (Item / ItemStack) или тег предметов (OreDictionary-имя оригинала); out выпадает
     * с шансом chance за каждые мехи + 1.
     */
    public record SmeltBonus(Object in, net.minecraft.world.item.ItemStack out, float chance) {
        public boolean matches(net.minecraft.world.item.ItemStack stack) {
            if (this.in instanceof net.minecraft.tags.TagKey<?> tag) {
                @SuppressWarnings("unchecked")
                net.minecraft.tags.TagKey<net.minecraft.world.item.Item> t = (net.minecraft.tags.TagKey<net.minecraft.world.item.Item>) tag;
                return stack.is(t);
            }
            if (this.in instanceof net.minecraft.world.item.ItemStack is) {
                return stack.is(is.getItem());
            }
            return this.in instanceof net.minecraft.world.level.ItemLike il && stack.is(il.asItem());
        }
    }

    public static void addSmeltingBonus(Object in, net.minecraft.world.item.ItemStack out, float chance) {
        thaumcraft.api.internal.CommonInternals.smeltingBonus.add(new SmeltBonus(in, out, chance));
    }

    public static void addSmeltingBonus(Object in, net.minecraft.world.item.ItemStack out) {
        addSmeltingBonus(in, out, 0.33F);
    }

    // --- семена для пересадки (печать урожая) ---
    private static final java.util.Map<net.minecraft.world.level.block.Block, ItemStack> seedList = new java.util.concurrent.ConcurrentHashMap<>();

    /** Явно задать семя растения-блока (registerSeed оригинала, для дополнений). */
    public static void registerSeed(net.minecraft.world.level.block.Block block, ItemStack seed) {
        seedList.put(block, seed);
    }

    /**
     * Семя для блока растения. В оригинале список строился из всех IPlantable-предметов (их getPlant); в 1.21 —
     * предмет, который ставит этот блок (Item.byBlock), если блок — посадка (куст, посев, стебель, тростник, адский
     * нарост). Пусто — пересаживать нечем.
     */
    public static ItemStack getSeed(net.minecraft.world.level.block.Block block) {
        ItemStack s = seedList.get(block);
        if (s != null) {
            return s;
        }
        if (block instanceof net.minecraft.world.level.block.BushBlock || block instanceof net.minecraft.world.level.block.SugarCaneBlock) {
            Item item = Item.byBlock(block);
            if (item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }
}
