package thaumcraft.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;

/** Объект, содержащий аспект: ключ "!аспект" (регистрируется для каждого аспекта) и наблюдение в трёх категориях. */
public class ScanAspect implements IScanThing {
    String research;
    Aspect aspect;

    public ScanAspect(String research, Aspect aspect) {
        this.research = research;
        this.aspect = aspect;
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) {
            return false;
        }
        AspectList al = null;
        if (obj instanceof Entity e && !(obj instanceof ItemEntity)) {
            al = AspectHelper.getEntityAspects(e);
        } else {
            ItemStack is = null;
            if (obj instanceof ItemStack stack) {
                is = stack;
            }
            if (obj instanceof ItemEntity ie) {
                is = ie.getItem();
            }
            if (obj instanceof BlockPos pos) {
                // в оригинале — new ItemStack(block, 1, meta) без учёта «взять блок»
                Block b = player.level().getBlockState(pos).getBlock();
                is = new ItemStack(b);
            }
            if (is != null) {
                al = AspectHelper.getObjectAspects(is);
            }
        }
        return al != null && al.getAmount(this.aspect) > 0;
    }

    @Override
    public void onSuccess(Player player, Object obj) {
        ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("AUROMANCY"), 1);
        ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS"), 1);
        ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("ALCHEMY"), 1);
    }

    @Override
    public String getResearchKey(Player player, Object object) {
        return this.research;
    }
}
