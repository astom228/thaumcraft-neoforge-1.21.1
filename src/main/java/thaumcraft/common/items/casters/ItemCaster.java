package thaumcraft.common.items.casters;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.casters.IInteractWithCaster;

/**
 * Перчатка заклинателя (порт ItemCaster), этап 11 — только щелчок по блокам и BlockEntity с IInteractWithCaster
 * (руническая матрица, трубки). Фокусы, расход Вис, радиальное меню, триггеры кастера — этап 12 (DECISIONS 92).
 */
public class ItemCaster extends Item implements ICaster {
    final int area;

    public ItemCaster(int area, Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
        this.area = area;
    }

    /** onItemUseFirst оригинала: сначала блок, затем BlockEntity; обработанный щелчок — PASS, как в оригинале. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockState bs = world.getBlockState(pos);
        if (bs.getBlock() instanceof IInteractWithCaster b
                && b.onCasterRightClick(world, stack, player, pos, context.getClickedFace(), context.getHand())) {
            return InteractionResult.PASS;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IInteractWithCaster t && t.onCasterRightClick(world, stack, player, pos, context.getClickedFace(), context.getHand())) {
            return InteractionResult.PASS;
        }
        // TODO этап 12: CasterTriggerRegistry, фокус (архитектор)
        return InteractionResult.PASS;
    }

    @Override
    public float getConsumptionModifier(ItemStack stack, Player player, boolean crafting) {
        float consumptionModifier = 1.0F;
        if (player != null) {
            consumptionModifier -= CasterManager.getTotalVisDiscount(player);
        }
        return Math.max(consumptionModifier, 0.1F);
    }

    /** TODO этап 12: расход Вис из ауры (CasterManager.consumeVisFromInventory / аура вокруг игрока). */
    @Override
    public boolean consumeVis(ItemStack stack, Player player, float amount, boolean crafting, boolean simulate) {
        return false;
    }

    /** TODO этап 12: фокус в перчатке. */
    @Override
    public Item getFocus(ItemStack stack) {
        return null;
    }

    @Override
    public ItemStack getFocusStack(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setFocus(ItemStack stack, ItemStack focus) {
    }

    @Override
    public ItemStack getPickedBlock(ItemStack stack) {
        return ItemStack.EMPTY;
    }
}
