package thaumcraft.common.lib.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.common.lib.events.ServerEvents;

/** Соль мира на конкретный блок → результат (через 50 тиков, с облачком), если исследование известно. */
public class DustTriggerSimple implements IDustTrigger {
    final Block target;
    final ItemStack result;
    final String research;

    public DustTriggerSimple(String research, Block target, ItemStack result) {
        this.target = target;
        this.result = result;
        this.research = research;
    }

    @Override
    public Placement getValidFace(Level world, Player player, BlockPos pos, Direction face) {
        return world.getBlockState(pos).getBlock() != this.target
                || this.research != null && !ThaumcraftCapabilities.getKnowledge(player).isResearchKnown(this.research)
                ? null : new Placement(0, 0, 0, null);
    }

    @Override
    public void execute(Level world, Player player, BlockPos pos, Placement placement, Direction side) {
        execute(world, player, pos, this.result);
    }

    /** Общая часть DustTriggerSimple / DustTriggerOre. */
    static void execute(Level world, Player player, BlockPos pos, ItemStack result) {
        EventHooks.firePlayerCraftingEvent(player, result, new SimpleContainer(1));
        BlockState state = world.getBlockState(pos);
        ServerEvents.addRunnableServer(world, () -> ServerEvents.addSwapper(world, pos, s -> s == state, result, false, 0, player, true, true, -9999,
                false, false, 0, ServerEvents.DEFAULT_PREDICATE, 0.0F), 50);
    }
}
