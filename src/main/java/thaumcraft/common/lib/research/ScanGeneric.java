package thaumcraft.common.lib.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ScanningManager;

/**
 * Любой объект с аспектами: ключ «уже изучено» и наблюдение во всех категориях по формуле категории.
 * Ключи: сущность — "!" + ID типа (в оригинале — имя EntityList), предмет — "!" + ID предмета (в оригинале к ID
 * добавлялась metadata; варианты в 1.21 — отдельные ID).
 */
public class ScanGeneric implements IScanThing {
    @Override
    public boolean checkThing(Player player, Object obj) {
        AspectList al = this.aspects(player, obj);
        return al != null && al.size() > 0;
    }

    private AspectList aspects(Player player, Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Entity e && !(obj instanceof ItemEntity)) {
            return AspectHelper.getEntityAspects(e);
        }
        ItemStack is = ScanningManager.getItemFromParms(player, obj);
        return !is.isEmpty() ? AspectHelper.getObjectAspects(is) : null;
    }

    @Override
    public void onSuccess(Player player, Object obj) {
        AspectList al = this.aspects(player, obj);
        if (al != null) {
            for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category, category.applyFormula(al));
            }
        }
    }

    @Override
    public String getResearchKey(Player player, Object obj) {
        if (obj instanceof Entity e && !(obj instanceof ItemEntity)) {
            return "!" + EntityType.getKey(e.getType());
        }
        ItemStack is = ScanningManager.getItemFromParms(player, obj);
        if (!is.isEmpty()) {
            return "!" + BuiltInRegistries.ITEM.getKey(is.getItem());
        }
        return null;
    }
}
