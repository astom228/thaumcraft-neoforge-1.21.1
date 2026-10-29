package thaumcraft.common.lib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.items.consumables.ItemBathSalts;

/** События сущностей (порт EntityEvents; остальное — с этапами существ). */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class EntityEvents {
    /** Истаявшие соли для ванн превращают источник воды под собой в очищающую жидкость. */
    @SubscribeEvent
    public static void itemExpire(ItemExpireEvent event) {
        ItemEntity item = event.getEntity();
        Level world = item.level();
        if (world.isClientSide || !(item.getItem().getItem() instanceof ItemBathSalts)) {
            return;
        }
        BlockPos bp = item.blockPosition();
        BlockState bs = world.getBlockState(bp);
        if (bs.is(Blocks.WATER) && bs.getValue(LiquidBlock.LEVEL) == 0) {
            world.setBlockAndUpdate(bp, BlocksTC.purifyingFluid.get().defaultBlockState());
        }
    }
}
