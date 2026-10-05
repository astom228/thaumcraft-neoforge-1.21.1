package thaumcraft.api.research;

import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;

/** Приложение к исследованию: страница, открывающаяся после другого исследования. Порт ResearchAddendum оригинала. */
public class ResearchAddendum {
    String text;
    ResourceLocation[] recipes;
    String[] research;

    public String getText() {
        return this.text;
    }

    public String getTextLocalized() {
        return Language.getInstance().getOrDefault(this.getText());
    }

    public void setText(String text) {
        this.text = text;
    }

    public ResourceLocation[] getRecipes() {
        return this.recipes;
    }

    public void setRecipes(ResourceLocation[] recipes) {
        this.recipes = recipes;
    }

    public String[] getResearch() {
        return this.research;
    }

    public void setResearch(String[] research) {
        this.research = research;
    }
}
