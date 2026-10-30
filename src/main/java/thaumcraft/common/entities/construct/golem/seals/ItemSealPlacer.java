package thaumcraft.common.entities.construct.golem.seals;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISeal;

/**
 * Печать управления (порт ItemSealPlacer): ПКМ по грани блока ставит печать своего типа (если тип разрешает это
 * место); чистая печать (blank) ничего не ставит. В руках видны печати и их зоны. В 1.12 — один предмет с метой.
 */
public class ItemSealPlacer extends Item implements ISealDisplayer {
    /** Ключ типа печати → предмет (только чтение после регистрации предметов). */
    private static final Map<String, Supplier<? extends Item>> BY_KEY = new HashMap<>();
    private final String sealKey;

    public ItemSealPlacer(String sealKey, Properties props) {
        super(props);
        this.sealKey = sealKey;
    }

    public static void bind(String sealKey, Supplier<? extends Item> item) {
        BY_KEY.put(sealKey, item);
    }

    public String getSealKey() {
        return this.sealKey;
    }

    public static ItemStack getSealStack(String sealKey) {
        Supplier<? extends Item> s = BY_KEY.get(sealKey);
        return s == null ? ItemStack.EMPTY : new ItemStack(s.get());
    }

    /** onItemUseFirst оригинала: срабатывает раньше блока (печать можно ставить и на сундук), Shift — пропуск. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (this.sealKey == null || player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos();
        Direction side = ctx.getClickedFace();
        if (!player.mayUseItemAt(pos, side, stack)) {
            return InteractionResult.FAIL;
        }
        ISeal seal = SealHandler.newSeal(this.sealKey);
        if (seal != null && seal.canPlaceAt(world, pos, side)) {
            if (SealHandler.addSealEntity(world, pos, side, seal, player) && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return true;
    }
}
