package thaumcraft.client.compat.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.advanced.IRecipeManagerPluginHelper;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.LogicalSide;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.lib.crafting.InfusionPreview;
import thaumcraft.common.lib.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import static thaumcraft.common.lib.crafting.InfusionPreview.previousInput;
import static thaumcraft.common.lib.crafting.InfusionPreview.sameState;

/** Загружается только самим JEI на клиенте. Общий код мода на этот класс не ссылается. */
@JeiPlugin
public final class ThaumcraftJeiPlugin implements IModPlugin {
    private static final Map<JeiDisplay.Kind, RecipeType<JeiDisplay>> TYPES;
    static {
        Map<JeiDisplay.Kind, RecipeType<JeiDisplay>> types = new java.util.EnumMap<>(JeiDisplay.Kind.class);
        for (var kind : JeiDisplay.Kind.values()) types.put(kind, RecipeType.create("thaumcraft", kind.name().toLowerCase(java.util.Locale.ROOT), JeiDisplay.class));
        TYPES = Map.copyOf(types);
    }
    private Map<ResourceLocation, InfusionRecipe> infusionSources = Map.of();
    private List<JeiDisplay> infusionDefaults = List.of();
    private List<JeiDisplay> aspectDefaults = List.of();

    public static RecipeType<JeiDisplay> type(JeiDisplay.Kind kind) { return TYPES.get(kind); }
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.parse("thaumcraft:jei"); }

    @Override public void registerIngredients(IModIngredientRegistration registration) {
        registration.register(AspectIngredient.TYPE, Aspect.aspects.values().stream().map(aspect -> new AspectIngredient(aspect, 1)).toList(),
                new AspectIngredient.Helper(), new AspectIngredient.Renderer(), AspectIngredient.CODEC);
    }

    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Не перезаписываем интерпретаторы ванильных/чужих предметов. У TC важны аспект, фокус, заряды и прочие компоненты.
        for (var item : BuiltInRegistries.ITEM) if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("thaumcraft")) {
            registration.registerSubtypeInterpreter(item, new ISubtypeInterpreter<ItemStack>() {
                @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                    var patch = stack.getComponentsPatch().forget(t -> t == DataComponents.DAMAGE || t == DataComponents.CUSTOM_NAME || t == DataComponents.LORE);
                    return patch.isEmpty() ? null : patch;
                }
                @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) { return ""; }
            });
        }
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new ThaumcraftCategory(JeiDisplay.Kind.ARCANE, gui, new ItemStack(BlocksTC.arcaneWorkbench.get())),
                new ThaumcraftCategory(JeiDisplay.Kind.CRUCIBLE, gui, new ItemStack(BlocksTC.crucible.get())),
                new ThaumcraftCategory(JeiDisplay.Kind.INFUSION, gui, new ItemStack(BlocksTC.infusionMatrix.get())),
                new ThaumcraftCategory(JeiDisplay.Kind.ASPECTS, gui, new ItemStack(ItemsTC.thaumometer.get())));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        infusionSources = Map.of();
        infusionDefaults = List.of();
        aspectDefaults = List.of();
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        CommonInternals.withSide(LogicalSide.CLIENT, () -> {
            if (CommonInternals.state(LogicalSide.CLIENT).recipes != level.getRecipeManager()) {
                thaumcraft.common.lib.events.AspectRegistryEvents.build(LogicalSide.CLIENT, level.getRecipeManager(), level.registryAccess());
            }
            List<JeiDisplay> arcane = new ArrayList<>(), crucible = new ArrayList<>(), infusions = new ArrayList<>(), aspects = new ArrayList<>();
            Map<ResourceLocation, InfusionRecipe> sources = new LinkedHashMap<>();
            var items = registration.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                    .filter(stack -> !stack.isEmpty()).map(stack -> stack.copyWithCount(1)).toList();
            for (var holder : level.getRecipeManager().getRecipes()) if (holder.value() instanceof IArcaneRecipe recipe) {
                arcane.add(new JeiDisplay(holder.id(), holder.id(), JeiDisplay.Kind.ARCANE, recipe.getIngredients().stream().map(JeiDisplay::stacks).toList(),
                        recipe instanceof ShapedArcaneRecipe shaped ? shaped.getWidth() : 3, !(recipe instanceof ShapedArcaneRecipe), List.of(), List.of(recipe.getResultItem(level.registryAccess()).copy()),
                        recipe.getCrystals() == null ? new AspectList() : recipe.getCrystals().copy(), recipe.getVis(), 0, recipe.getResearch(), 0));
            }
            ThaumcraftApi.getCraftingRecipes().forEach((id, recipe) -> {
                if (recipe instanceof CrucibleRecipe cr) crucible.add(new JeiDisplay(id, id, JeiDisplay.Kind.CRUCIBLE, List.of(JeiDisplay.stacks(cr.getCatalyst())),
                        1, false, List.of(), List.of(cr.getRecipeOutput().copy()), cr.getAspects().copy(), 0, 0, cr.getResearch(), 0));
                else if (recipe instanceof InfusionRecipe infusion) {
                    sources.put(id, infusion);
                    var candidates = infusion instanceof InfusionRunicAugmentRecipe || infusion instanceof InfusionEnchantmentRecipe ? items : JeiDisplay.stacks(infusion.getRecipeInput());
                    for (var input : candidates) InfusionPreview.of(infusion, input).ifPresent(preview -> infusions.add(JeiDisplay.infusion(id, infusion, preview)));
                }
            });
            for (var input : items) {
                var display = aspectDisplay(input);
                if (display != null) aspects.add(display);
            }
            infusionSources = java.util.Collections.unmodifiableMap(sources);
            infusionDefaults = List.copyOf(infusions);
            aspectDefaults = List.copyOf(aspects);
            registration.addRecipes(type(JeiDisplay.Kind.ARCANE), arcane);
            registration.addRecipes(type(JeiDisplay.Kind.CRUCIBLE), crucible);
            // Наполнение целиком обслуживает advanced lookup: базовое превью не подменяет стоимость заряженного предмета.
            Thaumcraft.log.info("JEI display snapshot: {} arcane, {} crucible, {} infusion previews, {} item aspects", arcane.size(), crucible.size(), infusions.size(), aspects.size());
        });
    }

    static JeiDisplay aspectDisplay(ItemStack input) {
        var aspects = ThaumcraftCraftingManager.getObjectTags(input);
        if (aspects == null || aspects.size() == 0) return null;
        return new JeiDisplay(JeiDisplay.variantId(ResourceLocation.parse("thaumcraft:aspects"), input), ResourceLocation.parse("thaumcraft:aspects"), JeiDisplay.Kind.ASPECTS,
                List.of(List.of(input.copyWithCount(1))), 1, false, List.of(), List.of(), aspects.copy(), 0, 0, "", 0);
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(type(JeiDisplay.Kind.ARCANE), BlocksTC.arcaneWorkbench.get());
        registration.addRecipeCatalysts(type(JeiDisplay.Kind.CRUCIBLE), BlocksTC.crucible.get());
        registration.addRecipeCatalysts(type(JeiDisplay.Kind.INFUSION), BlocksTC.infusionMatrix.get());
        registration.addRecipeCatalysts(type(JeiDisplay.Kind.ASPECTS), ItemsTC.thaumometer.get());
    }

    @Override public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeManagerPlugin(new FocusedRecipes(registration.getRecipeManagerPluginHelper()));
    }
    @Override public void onRuntimeUnavailable() { infusionSources = Map.of(); infusionDefaults = List.of(); aspectDefaults = List.of(); }

    private final class FocusedRecipes implements IRecipeManagerPlugin {
        private final IRecipeManagerPluginHelper helper;
        private FocusedRecipes(IRecipeManagerPluginHelper helper) { this.helper = helper; }
        @Override public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
            List<RecipeType<?>> result = new ArrayList<>();
            for (var kind : List.of(JeiDisplay.Kind.INFUSION, JeiDisplay.Kind.ASPECTS)) {
                if (helper.isRecipeCatalyst(type(kind), focus)) result.add(type(kind));
            }
            if (!infusions(focus).isEmpty()) result.add(type(JeiDisplay.Kind.INFUSION));
            if (focus.getRole() == RecipeIngredientRole.INPUT && focus.checkedCast(VanillaTypes.ITEM_STACK).isPresent()) result.add(type(JeiDisplay.Kind.ASPECTS));
            if (focus.getRole() == RecipeIngredientRole.OUTPUT && focus.checkedCast(AspectIngredient.TYPE).isPresent()) result.add(type(JeiDisplay.Kind.ASPECTS));
            return result.stream().distinct().toList();
        }
        @Override public <T, V> List<T> getRecipes(IRecipeCategory<T> category, IFocus<V> focus) {
            if (helper.isRecipeCatalyst(category.getRecipeType(), focus)) return getRecipes(category);
            if (category.getRecipeType().equals(type(JeiDisplay.Kind.INFUSION))) return cast(category, infusions(focus));
            if (category.getRecipeType().equals(type(JeiDisplay.Kind.ASPECTS)) && focus.getRole() == RecipeIngredientRole.OUTPUT) {
                var aspect = focus.checkedCast(AspectIngredient.TYPE);
                if (aspect.isPresent()) return cast(category, aspectDefaults.stream()
                        .filter(recipe -> recipe.aspects().getAmount(aspect.get().getTypedValue().getIngredient().aspect()) > 0).toList());
            }
            if (category.getRecipeType().equals(type(JeiDisplay.Kind.ASPECTS)) && focus.getRole() == RecipeIngredientRole.INPUT) {
                var item = focus.checkedCast(VanillaTypes.ITEM_STACK);
                if (item.isPresent()) {
                    List<JeiDisplay> result = new ArrayList<>();
                    CommonInternals.withSide(LogicalSide.CLIENT, () -> {
                        var display = aspectDisplay(item.get().getTypedValue().getIngredient());
                        if (display != null) result.add(display);
                    });
                    return cast(category, result);
                }
            }
            return List.of();
        }
        @Override public <T> List<T> getRecipes(IRecipeCategory<T> category) {
            if (category.getRecipeType().equals(type(JeiDisplay.Kind.INFUSION))) return cast(category, infusionDefaults);
            return category.getRecipeType().equals(type(JeiDisplay.Kind.ASPECTS)) ? cast(category, aspectDefaults) : List.of();
        }
        private <T> List<T> cast(IRecipeCategory<T> category, List<JeiDisplay> recipes) {
            List<T> result = new ArrayList<>();
            for (var recipe : recipes) result.add(category.getRecipeType().getRecipeClass().cast(recipe));
            return result;
        }
    }

    private <V> List<JeiDisplay> infusions(IFocus<V> focus) {
        var aspect = focus.checkedCast(AspectIngredient.TYPE);
        if (aspect.isPresent()) return focus.getRole() == RecipeIngredientRole.INPUT
                ? infusionDefaults.stream().filter(recipe -> recipe.aspects().getAmount(aspect.get().getTypedValue().getIngredient().aspect()) > 0).toList() : List.of();
        var typed = focus.checkedCast(VanillaTypes.ITEM_STACK);
        if (typed.isEmpty()) return List.of();
        ItemStack stack = typed.get().getTypedValue().getIngredient();
        boolean output = focus.getRole() == RecipeIngredientRole.OUTPUT;
        if (!output && focus.getRole() != RecipeIngredientRole.INPUT) return List.of();
        Map<ResourceLocation, JeiDisplay> result = new LinkedHashMap<>();
        for (var recipe : infusionDefaults) {
            boolean matches = output ? recipe.outputs().stream().anyMatch(candidate -> sameState(candidate, stack))
                    : recipe.inputs().stream().flatMap(List::stream).anyMatch(candidate -> ThaumcraftApiHelper.getIngredient(candidate).test(stack));
            if (matches) result.put(recipe.id(), recipe);
        }
        for (var entry : infusionSources.entrySet()) {
            var source = entry.getValue();
            ItemStack input = output ? previousInput(source, stack) : stack;
            if (input.isEmpty()) continue;
            InfusionPreview.of(source, input).ifPresent(preview -> {
                if (!output || sameState(preview.output(), stack)) {
                    var display = JeiDisplay.infusion(entry.getKey(), source, preview);
                    result.put(display.id(), display);
                }
            });
        }
        return List.copyOf(result.values());
    }

}
