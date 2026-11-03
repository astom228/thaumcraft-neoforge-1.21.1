package thaumcraft.client.compat.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.UtilsFX;

/** Эссенция — отдельный ингредиент JEI, не притворяется кристаллом или жидкостью. */
public record AspectIngredient(Aspect aspect, int amount) {
    public static final IIngredientType<AspectIngredient> TYPE = () -> AspectIngredient.class;
    public static final Codec<AspectIngredient> CODEC = Codec.STRING.comapFlatMap(tag -> {
        Aspect aspect = Aspect.getAspect(tag);
        return aspect == null ? DataResult.error(() -> "Unknown aspect: " + tag) : DataResult.success(new AspectIngredient(aspect, 1));
    }, value -> value.aspect().getTag());

    @SuppressWarnings("removal") // Методы остаются обязательными в API выбранного JEI 19.
    public static final class Helper implements IIngredientHelper<AspectIngredient> {
        @Override public IIngredientType<AspectIngredient> getIngredientType() { return TYPE; }
        @Override public String getDisplayName(AspectIngredient value) { return value.aspect().getName(); }
        @Override public String getUniqueId(AspectIngredient value, UidContext context) { return "thaumcraft:" + value.aspect().getTag(); }
        @Override public Object getUid(AspectIngredient value, UidContext context) { return getResourceLocation(value); }
        @Override public ResourceLocation getResourceLocation(AspectIngredient value) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", value.aspect().getTag()); }
        @Override public AspectIngredient copyIngredient(AspectIngredient value) { return value; }
        @Override public AspectIngredient normalizeIngredient(AspectIngredient value) { return new AspectIngredient(value.aspect(), 1); }
        @Override public long getAmount(AspectIngredient value) { return value.amount(); }
        @Override public boolean isValidIngredient(AspectIngredient value) { return value != null && value.aspect() != null && value.amount() > 0; }
        @Override public String getErrorInfo(AspectIngredient value) { return String.valueOf(value); }
    }

    @SuppressWarnings("removal")
    public static final class Renderer implements IIngredientRenderer<AspectIngredient> {
        @Override public void render(GuiGraphics graphics, AspectIngredient value) {
            UtilsFX.drawTag(graphics, 0, 0, value.aspect(), value.amount() > 1 ? value.amount() : 0, 0);
        }
        @Override public List<Component> getTooltip(AspectIngredient value, TooltipFlag flags) {
            return List.of(Component.literal(value.aspect().getName()), value.aspect().getLocalizedDescription(),
                    Component.translatable("jei.thaumcraft.amount", value.amount()));
        }
    }
}
