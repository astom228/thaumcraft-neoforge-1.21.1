package thaumcraft.common.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;
import thaumcraft.common.world.aura.AuraHandler;

/** Реализация внутренних методов API (порт InternalMethodHandler; методы добавляются по этапам). */
public class InternalMethodHandler implements IInternalMethodHandler {
    @Override
    public void addWarpToPlayer(Player player, int amount, IPlayerWarp.EnumWarpType type) {
        if (amount != 0 && !player.level().isClientSide && player instanceof ServerPlayer sp) {
            IPlayerWarp pw = ThaumcraftCapabilities.getWarp(player);
            int cur = pw.get(type);
            if (amount < 0 && cur + amount < 0) {
                amount = cur;
            }
            pw.add(type, amount);
            if (type == IPlayerWarp.EnumWarpType.PERMANENT) {
                PacketDistributor.sendToPlayer(sp, new PacketWarpMessage(amount, (byte) 0));
            }
            if (type == IPlayerWarp.EnumWarpType.NORMAL) {
                PacketDistributor.sendToPlayer(sp, new PacketWarpMessage(amount, (byte) 1));
            }
            if (type == IPlayerWarp.EnumWarpType.TEMPORARY) {
                PacketDistributor.sendToPlayer(sp, new PacketWarpMessage(amount, (byte) 2));
            }
            if (amount > 0) {
                pw.setCounter(pw.get(IPlayerWarp.EnumWarpType.TEMPORARY) + pw.get(IPlayerWarp.EnumWarpType.PERMANENT)
                        + pw.get(IPlayerWarp.EnumWarpType.NORMAL));
            }
            if (type != IPlayerWarp.EnumWarpType.TEMPORARY
                    && ThaumcraftCapabilities.knowsResearchStrict(player, "FIRSTSTEPS")
                    && !ThaumcraftCapabilities.knowsResearchStrict(player, "WARP")) {
                // TODO этап 7: this.completeResearch(player, "WARP") — ResearchManager
                player.displayClientMessage(Component.translatable("research.WARP.warn"), true);
            }
            pw.sync(sp);
        }
    }

    @Override
    public int getActualWarp(Player player) {
        IPlayerWarp wc = ThaumcraftCapabilities.getWarp(player);
        return wc.get(IPlayerWarp.EnumWarpType.NORMAL) + wc.get(IPlayerWarp.EnumWarpType.PERMANENT);
    }

    @Override
    public AspectList getObjectAspects(ItemStack is) {
        return ThaumcraftCraftingManager.getObjectTags(is);
    }

    @Override
    public AspectList generateTags(ItemStack is) {
        return ThaumcraftCraftingManager.generateTags(is);
    }

    @Override
    public float drainFlux(Level world, BlockPos pos, float amount, boolean simulate) {
        return AuraHandler.drainFlux(world, pos, amount, simulate);
    }

    @Override
    public float getFlux(Level world, BlockPos pos) {
        return AuraHandler.getFlux(world, pos);
    }

    @Override
    public float drainVis(Level world, BlockPos pos, float amount, boolean simulate) {
        return AuraHandler.drainVis(world, pos, amount, simulate);
    }

    @Override
    public void addVis(Level world, BlockPos pos, float amount) {
        AuraHandler.addVis(world, pos, amount);
    }

    @Override
    public float getTotalAura(Level world, BlockPos pos) {
        return AuraHandler.getTotalAura(world, pos);
    }

    @Override
    public float getVis(Level world, BlockPos pos) {
        return AuraHandler.getVis(world, pos);
    }

    @Override
    public int getAuraBase(Level world, BlockPos pos) {
        return AuraHandler.getAuraBase(world, pos);
    }

    @Override
    public void addFlux(Level world, BlockPos pos, float amount, boolean showEffect) {
        if (!world.isClientSide) {
            AuraHandler.addFlux(world, pos, amount);
            if (showEffect && amount > 0.0F) {
                // TODO этап 6: PacketFXPollute(pos, amount) всем в радиусе 32
            }
        }
    }

    @Override
    public boolean shouldPreserveAura(Level world, Player player, BlockPos pos) {
        return AuraHandler.shouldPreserveAura(world, player, pos);
    }
}
