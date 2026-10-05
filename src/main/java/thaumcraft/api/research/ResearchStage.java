package thaumcraft.api.research;

import java.util.Locale;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.capabilities.IPlayerKnowledge;

/** Стадия исследования: текст, рецепты и требования для перехода к следующей стадии. Порт ResearchStage оригинала. */
public class ResearchStage {
    String text;
    ResourceLocation[] recipes;
    ItemStack[] obtain;
    ItemStack[] craft;
    int[] craftReference;
    Knowledge[] know;
    String[] research;
    int warp;

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

    public ItemStack[] getObtain() {
        return this.obtain;
    }

    public void setObtain(ItemStack[] obtain) {
        this.obtain = obtain;
    }

    public ItemStack[] getCraft() {
        return this.craft;
    }

    public void setCraft(ItemStack[] craft) {
        this.craft = craft;
    }

    public int[] getCraftReference() {
        return this.craftReference;
    }

    public void setCraftReference(int[] craftReference) {
        this.craftReference = craftReference;
    }

    public Knowledge[] getKnow() {
        return this.know;
    }

    public void setKnow(Knowledge[] know) {
        this.know = know;
    }

    public String[] getResearch() {
        return this.research;
    }

    public void setResearch(String[] research) {
        this.research = research;
    }

    public int getWarp() {
        return this.warp;
    }

    public void setWarp(int warp) {
        this.warp = warp;
    }

    public static class Knowledge {
        public IPlayerKnowledge.EnumKnowledgeType type;
        public ResearchCategory category;
        public int amount = 0;

        public Knowledge(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category, int num) {
            this.type = type;
            this.category = category;
            this.amount = num;
        }

        /** "ТИП;количество" или "ТИП;КАТЕГОРИЯ;количество". Неизвестный тип — исключение (запись исследования отбрасывается). */
        public static Knowledge parse(String text) {
            String[] s = text.split(";");
            if (s.length == 2) {
                int num = 0;
                try {
                    num = Integer.parseInt(s[1]);
                } catch (Exception ignored) {
                }
                IPlayerKnowledge.EnumKnowledgeType t = IPlayerKnowledge.EnumKnowledgeType.valueOf(s[0].toUpperCase(Locale.ROOT));
                if (!t.hasFields() && num > 0) {
                    return new Knowledge(t, null, num);
                }
            } else if (s.length == 3) {
                int num = 0;
                try {
                    num = Integer.parseInt(s[2]);
                } catch (Exception ignored) {
                }
                IPlayerKnowledge.EnumKnowledgeType t = IPlayerKnowledge.EnumKnowledgeType.valueOf(s[0].toUpperCase(Locale.ROOT));
                ResearchCategory f = ResearchCategories.getResearchCategory(s[1].toUpperCase(Locale.ROOT));
                if (f != null && num > 0) {
                    return new Knowledge(t, f, num);
                }
            }
            return null;
        }
    }
}
