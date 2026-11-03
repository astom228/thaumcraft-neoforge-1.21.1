package thaumcraft.client.compat.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.common.lib.crafting.InfusionPreview;
import net.neoforged.fml.LogicalSide;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;

public final class ThaumcraftCategory implements IRecipeCategory<JeiDisplay> {
    private final JeiDisplay.Kind kind;
    private final IDrawable icon;
    private final IDrawable slot;

    public ThaumcraftCategory(JeiDisplay.Kind kind, IGuiHelper gui, ItemStack icon) {
        this.kind = kind;
        this.icon = gui.createDrawableItemStack(icon);
        this.slot = gui.getSlotDrawable();
    }

    @Override public RecipeType<JeiDisplay> getRecipeType() { return ThaumcraftJeiPlugin.type(kind); }
    @Override public Component getTitle() { return Component.translatable("jei.thaumcraft." + kind.name().toLowerCase(java.util.Locale.ROOT)); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 168; }
    @Override public int getHeight() {
        return switch (kind) { case ARCANE -> 130; case CRUCIBLE -> 178; case INFUSION -> 208; case ASPECTS -> 134; };
    }
    private int aspectY() { return kind == JeiDisplay.Kind.ASPECTS ? 40 : kind == JeiDisplay.Kind.CRUCIBLE ? 70 : 100; }
    @Override public ResourceLocation getRegistryName(JeiDisplay recipe) { return recipe.id(); }
    @Override public List<ResourceLocation> getSourceRecipeIds(JeiDisplay recipe) {
        return kind == JeiDisplay.Kind.ASPECTS ? List.of() : List.of(recipe.source());
    }

    private record Saved(ResourceLocation source, ItemStack input) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("recipe").forGetter(Saved::source),
                ItemStack.CODEC.fieldOf("input").forGetter(Saved::input)).apply(i, Saved::new));
    }

    @Override public Codec<JeiDisplay> getCodec(ICodecHelper helper, IRecipeManager manager) {
        if (kind != JeiDisplay.Kind.INFUSION && kind != JeiDisplay.Kind.ASPECTS) return IRecipeCategory.super.getCodec(helper, manager);
        // Сохраняем источник и вход, а не устаревающие цены. Закладки улучшенных предметов не зависят от списка примеров.
        return Saved.CODEC.comapFlatMap(saved -> {
            List<JeiDisplay> result = new ArrayList<>();
            CommonInternals.withSide(LogicalSide.CLIENT, () -> {
                if (kind == JeiDisplay.Kind.ASPECTS) {
                    var display = ThaumcraftJeiPlugin.aspectDisplay(saved.input());
                    if (display != null) result.add(display);
                } else if (ThaumcraftApi.getCraftingRecipes().get(saved.source()) instanceof InfusionRecipe recipe) {
                    InfusionPreview.of(recipe, saved.input()).ifPresent(preview -> result.add(JeiDisplay.infusion(saved.source(), recipe, preview)));
                }
            });
            return result.isEmpty() ? DataResult.error(() -> "Unavailable Thaumcraft recipe: " + saved.source()) : DataResult.success(result.getFirst());
        }, recipe -> new Saved(recipe.source(), kind == JeiDisplay.Kind.INFUSION ? recipe.central().getFirst() : recipe.inputs().getFirst().getFirst()));
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, JeiDisplay recipe, IFocusGroup focuses) {
        if (kind == JeiDisplay.Kind.INFUSION) {
            var central = builder.addSlot(RecipeIngredientRole.INPUT, 65, 2).addItemStacks(recipe.central()).setBackground(slot, -1, -1);
            var output = builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 2).addItemStacks(recipe.outputs()).setBackground(slot, -1, -1);
            builder.createFocusLink(central, output);
        } else if (!recipe.outputs().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 20).addItemStacks(recipe.outputs()).setBackground(slot, -1, -1);
        }
        for (int i = 0; i < recipe.inputs().size(); i++) {
            if (recipe.inputs().get(i).isEmpty()) continue;
            int width = recipe.width();
            int x = 2 + i % width * 18;
            int y = (kind == JeiDisplay.Kind.INFUSION ? 30 : 2) + i / width * 18;
            if (kind == JeiDisplay.Kind.CRUCIBLE) { x = 20; y = 20; }
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStacks(recipe.inputs().get(i)).setBackground(slot, -1, -1);
        }
        Aspect[] aspects = recipe.aspects().getAspectsSortedByName();
        for (int i = 0; i < aspects.length; i++) {
            Aspect aspect = aspects[i];
            int amount = recipe.aspects().getAmount(aspect);
            if (amount <= 0) continue;
            if (kind == JeiDisplay.Kind.ARCANE) {
                var crystal = ThaumcraftApiHelper.makeCrystal(aspect);
                crystal.setCount(amount);
                builder.addSlot(RecipeIngredientRole.INPUT, 2 + i % 9 * 18, 64 + i / 9 * 18).addItemStack(crystal);
            } else {
                var role = kind == JeiDisplay.Kind.ASPECTS ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT;
                builder.addSlot(role, 2 + i % 9 * 18, aspectY() + i / 9 * 18)
                        .addIngredient(AspectIngredient.TYPE, new AspectIngredient(aspect, amount));
            }
        }
    }

    @Override public void draw(JeiDisplay recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        if (kind != JeiDisplay.Kind.ASPECTS) graphics.drawString(font, "→", kind == JeiDisplay.Kind.INFUSION ? 107 : 111, kind == JeiDisplay.Kind.INFUSION ? 6 : 24, 0x404040, false);
        if (kind == JeiDisplay.Kind.ARCANE) {
            graphics.drawString(font, Component.translatable("jei.thaumcraft.vis", recipe.vis()), 2, 90, 0x404040, false);
            if (recipe.shapeless()) graphics.drawString(font, Component.translatable("jei.thaumcraft.shapeless"), 2, 102, 0x404040, false);
        } else {
            if (kind == JeiDisplay.Kind.INFUSION) {
                graphics.drawString(font, Component.translatable("jei.thaumcraft.instability", recipe.instability()), 2, 68, 0x404040, false);
                if (recipe.warpChance() > 0) graphics.drawString(font, Component.translatable("jei.thaumcraft.warp", recipe.warpChance()), 2, 79, 0x703090, false);
            }
            graphics.drawString(font, Component.translatable(kind == JeiDisplay.Kind.ASPECTS ? "jei.thaumcraft.per_item" : "jei.thaumcraft.essentia"), 2, aspectY() - 11, 0x404040, false);
        }
        if (!recipe.research().isEmpty()) graphics.drawString(font, font.plainSubstrByWidth(research(recipe).getString(), 164), 2, getHeight() - 12, 0x555555, false);
    }

    private static Component research(JeiDisplay recipe) {
        var entry = ResearchCategories.getResearch(recipe.research());
        return Component.translatable("jei.thaumcraft.research", entry == null ? recipe.research() : Component.translatable(entry.getName()));
    }

    @Override public void getTooltip(ITooltipBuilder builder, JeiDisplay recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();
        if (mouseY >= getHeight() - 18 && !recipe.research().isEmpty()) {
            tooltip.add(research(recipe));
            tooltip.add(Component.translatable("jei.thaumcraft.view_only"));
        }
        if (kind == JeiDisplay.Kind.ARCANE && mouseY >= 85 && mouseY <= 101) tooltip.add(Component.translatable("jei.thaumcraft.base_vis"));
        builder.addAll(tooltip);
    }
}
