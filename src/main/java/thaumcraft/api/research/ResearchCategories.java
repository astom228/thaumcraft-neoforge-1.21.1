package thaumcraft.api.research;

import java.util.LinkedHashMap;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.AspectList;

/**
 * Реестр категорий исследований. Порт ResearchCategories оригинала.
 * Категории регистрируются при запуске (до загрузки JSON исследований) и дальше не меняются.
 */
public class ResearchCategories {
    public static LinkedHashMap<String, ResearchCategory> researchCategories = new LinkedHashMap<>();

    public static ResearchCategory getResearchCategory(String key) {
        return researchCategories.get(key);
    }

    public static String getCategoryName(String key) {
        return Language.getInstance().getOrDefault("tc.research_category." + key);
    }

    public static ResearchEntry getResearch(String key) {
        for (ResearchCategory cat : researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                if (ri.key.equals(key)) {
                    return ri;
                }
            }
        }
        return null;
    }

    public static synchronized ResearchCategory registerCategory(String key, String researchkey, AspectList formula, ResourceLocation icon,
            ResourceLocation background) {
        if (getResearchCategory(key) == null) {
            ResearchCategory rl = new ResearchCategory(key, researchkey, formula, icon, background);
            researchCategories.put(key, rl);
            return rl;
        }
        return null;
    }

    public static synchronized ResearchCategory registerCategory(String key, String researchkey, AspectList formula, ResourceLocation icon,
            ResourceLocation background, ResourceLocation background2) {
        if (getResearchCategory(key) == null) {
            ResearchCategory rl = new ResearchCategory(key, researchkey, formula, icon, background, background2);
            researchCategories.put(key, rl);
            return rl;
        }
        return null;
    }
}
