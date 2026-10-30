package thaumcraft.api.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.research.ResearchCategory;

/** Внутренние методы, доступные через ThaumcraftApi.internalMethods (методы добавляются по этапам). */
public interface IInternalMethodHandler {
    boolean addKnowledge(Player player, IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory field, int amount);

    boolean progressResearch(Player player, String researchkey);

    boolean completeResearch(Player player, String researchkey);

    boolean doesPlayerHaveRequisites(Player player, String researchkey);

    void addWarpToPlayer(Player player, int amount, IPlayerWarp.EnumWarpType type);

    int getActualWarp(Player player);

    AspectList getObjectAspects(ItemStack is);

    AspectList generateTags(ItemStack is);

    float drainFlux(Level world, BlockPos pos, float amount, boolean simulate);

    float getFlux(Level world, BlockPos pos);

    float drainVis(Level world, BlockPos pos, float amount, boolean simulate);

    void addVis(Level world, BlockPos pos, float amount);

    float getTotalAura(Level world, BlockPos pos);

    float getVis(Level world, BlockPos pos);

    int getAuraBase(Level world, BlockPos pos);

    void addFlux(Level world, BlockPos pos, float amount, boolean showEffect);

    boolean shouldPreserveAura(Level world, Player player, BlockPos pos);

    void registerSeal(thaumcraft.api.golems.seals.ISeal seal);

    thaumcraft.api.golems.seals.ISeal getSeal(String key);

    thaumcraft.api.golems.seals.ISealEntity getSealEntity(net.minecraft.resources.ResourceKey<Level> dim, thaumcraft.api.golems.seals.SealPos pos);

    void addGolemTask(net.minecraft.resources.ResourceKey<Level> dim, thaumcraft.api.golems.tasks.Task task);

    net.minecraft.world.item.ItemStack getSealStack(String key);
}
