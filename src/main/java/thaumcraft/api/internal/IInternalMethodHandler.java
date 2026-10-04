package thaumcraft.api.internal;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.capabilities.IPlayerWarp;

/** Внутренние методы, доступные через ThaumcraftApi.internalMethods (методы добавляются по этапам). */
public interface IInternalMethodHandler {
    void addWarpToPlayer(Player player, int amount, IPlayerWarp.EnumWarpType type);

    int getActualWarp(Player player);
}
