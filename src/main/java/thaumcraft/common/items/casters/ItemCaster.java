package thaumcraft.common.items.casters;

import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.casters.CasterTriggerRegistry;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.casters.IFocusBlockPicker;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.api.casters.IInteractWithCaster;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Перчатка заклинателя (порт ItemCaster). ПКМ — заклинание фокуса за Вис из ауры (area: 0 — свой чанк, 1 — ещё 4 соседних
 * через 16 блоков, 2 — 3×3 чанка), с перезарядкой по сложности фокуса. Щелчок по блоку: IInteractWithCaster блока/BlockEntity,
 * затем триггеры CasterTriggerRegistry, затем выбор блока (Shift) для фокуса с IFocusBlockPicker.
 */
public class ItemCaster extends Item implements ICaster, thaumcraft.api.items.IArchitect {
    /** Порядок EnumFacing.HORIZONTALS 1.12: S, W, N, E. */
    private static final Direction[] HORIZONTALS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    final int area;
    final DecimalFormat myFormatter = new DecimalFormat("#######.#");

    public ItemCaster(int area, Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
        this.area = area;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack.getItem() == this && newStack.getItem() == this) {
            int s1 = 0;
            int s2 = 0;
            ItemFocus oldf = this.getFocus(oldStack);
            ItemFocus newf = this.getFocus(newStack);
            if (oldf != null && oldf.getSortingHelper(this.getFocusStack(oldStack)) != null) {
                s1 = oldf.getSortingHelper(this.getFocusStack(oldStack)).hashCode();
            }
            if (newf != null && newf.getSortingHelper(this.getFocusStack(newStack)) != null) {
                s2 = newf.getSortingHelper(this.getFocusStack(newStack)).hashCode();
            }
            return s1 != s2;
        }
        return newStack.getItem() != oldStack.getItem();
    }

    private float getAuraPool(Player player) {
        Level w = player.level();
        BlockPos p = player.blockPosition();
        float tot;
        switch (this.area) {
            case 1 -> {
                tot = AuraHandler.getVis(w, p);
                for (Direction face : HORIZONTALS) {
                    tot += AuraHandler.getVis(w, p.relative(face, 16));
                }
            }
            case 2 -> {
                tot = 0.0F;
                for (int xx = -1; xx <= 1; xx++) {
                    for (int zz = -1; zz <= 1; zz++) {
                        tot += AuraHandler.getVis(w, p.offset(xx * 16, 0, zz * 16));
                    }
                }
            }
            default -> tot = AuraHandler.getVis(w, p);
        }
        return tot;
    }

    @Override
    public boolean consumeVis(ItemStack is, Player player, float amount, boolean crafting, boolean sim) {
        amount *= this.getConsumptionModifier(is, player, crafting);
        float tot = this.getAuraPool(player);
        if (tot < amount) {
            return false;
        }
        if (sim) {
            return true;
        }
        Level w = player.level();
        BlockPos p = player.blockPosition();
        switch (this.area) {
            case 1 -> {
                float i = amount / 5.0F;
                while (amount > 0.0F) {
                    float before = amount;
                    if (i > amount) {
                        i = amount;
                    }
                    amount -= AuraHandler.drainVis(w, p, i, false);
                    if (amount <= 0.0F) {
                        return true;
                    }
                    if (i > amount) {
                        i = amount;
                    }
                    for (Direction face : HORIZONTALS) {
                        amount -= AuraHandler.drainVis(w, p.relative(face, 16), i, false);
                        if (amount <= 0.0F) {
                            return true;
                        }
                    }
                    if (amount >= before) {
                        break; // защита от вечного цикла: за проход ничего не взято (в оригинале — зависание сервера)
                    }
                }
            }
            case 2 -> {
                float i = amount / 9.0F;
                while (amount > 0.0F) {
                    float before = amount;
                    if (i > amount) {
                        i = amount;
                    }
                    for (int xx = -1; xx <= 1; xx++) {
                        for (int zz = -1; zz <= 1; zz++) {
                            amount -= AuraHandler.drainVis(w, p.offset(xx * 16, 0, zz * 16), i, false);
                            if (amount <= 0.0F) {
                                return true;
                            }
                        }
                    }
                    if (amount >= before) {
                        break; // защита от вечного цикла: за проход ничего не взято (в оригинале — зависание сервера)
                    }
                }
            }
            default -> amount -= AuraHandler.drainVis(w, p, amount, false);
        }
        return amount <= 0.0F;
    }

    @Override
    public float getConsumptionModifier(ItemStack stack, Player player, boolean crafting) {
        float consumptionModifier = 1.0F;
        if (player != null) {
            consumptionModifier -= CasterManager.getTotalVisDiscount(player);
        }
        return Math.max(consumptionModifier, 0.1F);
    }

    @Override
    public ItemFocus getFocus(ItemStack stack) {
        ItemStack fs = this.getFocusStack(stack);
        return fs != null && !fs.isEmpty() && fs.getItem() instanceof ItemFocus f ? f : null;
    }

    /** Копия фокуса в перчатке; без фокуса — null (как в оригинале). */
    @Override
    public ItemStack getFocusStack(ItemStack stack) {
        ItemContainerContents c = stack.get(TCDataComponents.CASTER_FOCUS.get());
        return c == null ? null : c.copyOne();
    }

    @Override
    public void setFocus(ItemStack stack, ItemStack focus) {
        if (focus != null && !focus.isEmpty()) {
            stack.set(TCDataComponents.CASTER_FOCUS.get(), ItemContainerContents.fromItems(List.of(focus.copy())));
        } else {
            stack.remove(TCDataComponents.CASTER_FOCUS.get());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // в оригинале строка «Расход Вис» — при любом NBT у перчатки; здесь — при фокусе (иначе NBT не было)
        ItemStack focus = this.getFocusStack(stack);
        if (focus != null && !focus.isEmpty()) {
            Component text = Component.empty();
            float amt = ((ItemFocus) focus.getItem()).getVisCost(focus);
            if (amt > 0.0F) {
                text = Component.literal(this.myFormatter.format(amt) + " ").withStyle(ChatFormatting.RESET)
                        .append(Component.translatable("item.Focus.cost1"));
            }
            tooltip.add(Component.translatable("tc.vis.cost").withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA).append(" ").append(text));
            ItemFocus f = this.getFocus(stack);
            if (f != null) {
                tooltip.add(focus.getHoverName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
                f.addFocusInformation(focus, tooltip);
            }
        }
    }

    /** Раз в 10 тиков, пока в руке перчатка, — аура вокруг (по площади перчатки) в HUD. */
    @Override
    public void inventoryTick(ItemStack is, Level w, Entity e, int slot, boolean currentItem) {
        if (!w.isClientSide && e.tickCount % 10 == 0 && e instanceof ServerPlayer player) {
            for (ItemStack h : player.getHandSlots()) {
                if (!h.isEmpty() && h.getItem() instanceof ICaster) {
                    this.updateAura(w, player);
                    break;
                }
            }
        }
    }

    private void updateAura(Level world, ServerPlayer player) {
        float cv = 0.0F;
        float cf = 0.0F;
        short bv = 0;
        int cx = (int) player.getX() >> 4;
        int cz = (int) player.getZ() >> 4;
        switch (this.area) {
            case 1 -> {
                AuraChunk acx = AuraHandler.getAuraChunk(world, cx, cz);
                if (acx != null) {
                    cv = acx.getVis();
                    cf = acx.getFlux();
                    bv = acx.getBase();
                    for (Direction face : HORIZONTALS) {
                        acx = AuraHandler.getAuraChunk(world, cx + face.getStepX(), cz + face.getStepZ());
                        if (acx != null) {
                            cv += acx.getVis();
                            cf += acx.getFlux();
                            bv += acx.getBase();
                        }
                    }
                }
            }
            case 2 -> {
                for (int xx = -1; xx <= 1; xx++) {
                    for (int zz = -1; zz <= 1; zz++) {
                        AuraChunk acx = AuraHandler.getAuraChunk(world, cx + xx, cz + zz);
                        if (acx != null) {
                            cv += acx.getVis();
                            cf += acx.getFlux();
                            bv += acx.getBase();
                        }
                    }
                }
            }
            default -> {
                AuraChunk ac = AuraHandler.getAuraChunk(world, cx, cz);
                if (ac != null) {
                    cv = ac.getVis();
                    cf = ac.getFlux();
                    bv = ac.getBase();
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new PacketAuraToClient(new AuraChunk(null, bv, cv, cf)));
    }

    /** onItemUseFirst оригинала: блок, BlockEntity, триггеры перчатки, выбор блока для фокуса. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Direction side = context.getClickedFace();
        InteractionHand hand = context.getHand();
        BlockState bs = world.getBlockState(pos);
        if (bs.getBlock() instanceof IInteractWithCaster b && b.onCasterRightClick(world, stack, player, pos, side, hand)) {
            return InteractionResult.PASS;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IInteractWithCaster t && t.onCasterRightClick(world, stack, player, pos, side, hand)) {
            return InteractionResult.PASS;
        }
        if (CasterTriggerRegistry.hasTrigger(bs)) {
            return CasterTriggerRegistry.performTrigger(world, stack, player, pos, side, bs) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        ItemStack fb = this.getFocusStack(stack);
        if (fb != null && !fb.isEmpty()) {
            FocusPackage core = ItemFocus.getPackage(fb);
            if (core != null) {
                for (IFocusElement fe : core.nodes) {
                    if (fe instanceof IFocusBlockPicker && player.isShiftKeyDown() && world.getBlockEntity(pos) == null) {
                        if (!world.isClientSide) {
                            // getSilkTouchDrop 1.12 — предмет самого блока
                            ItemStack isout = new ItemStack(bs.getBlock().asItem());
                            this.storePickedBlock(stack, isout);
                            return InteractionResult.SUCCESS;
                        }
                        player.swing(hand);
                        return InteractionResult.PASS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack focusStack = this.getFocusStack(held);
        ItemFocus focus = this.getFocus(held);
        FocusPackage core = focus != null ? ItemFocus.getPackage(focusStack) : null;
        // фокус без заклинания: в оригинале — NPE при заклинании (DEVIATIONS 37)
        if (focus != null && core != null && !CasterManager.isOnCooldown(player)) {
            CasterManager.setCooldown(player, focus.getActivationTime(focusStack));
            if (player.isShiftKeyDown()) {
                for (IFocusElement fe : core.nodes) {
                    if (fe instanceof IFocusBlockPicker) {
                        return InteractionResultHolder.pass(held);
                    }
                }
            }
            if (world.isClientSide) {
                return InteractionResultHolder.success(held);
            }
            if (this.consumeVis(held, player, focus.getVisCost(focusStack), false, false)) {
                FocusEngine.castFocusPackage(player, core);
                player.swing(hand);
                return InteractionResultHolder.success(held);
            }
            return InteractionResultHolder.fail(held);
        }
        return super.use(world, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public void storePickedBlock(ItemStack stack, ItemStack stackout) {
        stack.set(TCDataComponents.CASTER_PICKED.get(), ItemContainerContents.fromItems(List.of(stackout.copy())));
    }

    /** Выбранный блок, если в фокусе есть узел с выбором блока; иначе null (как в оригинале). */
    @Override
    public ItemStack getPickedBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = null;
        ItemContainerContents picked = stack.get(TCDataComponents.CASTER_PICKED.get());
        if (this.getFocus(stack) != null && picked != null) {
            FocusPackage fp = ItemFocus.getPackage(this.getFocusStack(stack));
            if (fp != null) {
                for (IFocusElement fe : fp.nodes) {
                    if (fe instanceof IFocusBlockPicker) {
                        out = picked.copyOne();
                        break;
                    }
                }
            }
        }
        return out;
    }

    // IArchitect: подсветка области — от узла фокуса с IArchitect («План»)

    private thaumcraft.api.items.IArchitect architect(ItemStack stack) {
        if (this.getFocus(stack) == null) {
            return null;
        }
        FocusPackage fp = ItemFocus.getPackage(this.getFocusStack(stack));
        if (fp != null) {
            for (IFocusElement fe : fp.nodes) {
                if (fe instanceof thaumcraft.api.items.IArchitect a) {
                    return a;
                }
            }
        }
        return null;
    }

    @Override
    public java.util.ArrayList<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos pos, Direction side, Player player) {
        thaumcraft.api.items.IArchitect a = this.architect(stack);
        return a != null ? a.getArchitectBlocks(stack, world, pos, side, player) : null;
    }

    @Override
    public boolean showAxis(ItemStack stack, Level world, Player player, Direction side, EnumAxis axis) {
        thaumcraft.api.items.IArchitect a = this.architect(stack);
        return a != null && a.showAxis(stack, world, player, side, axis);
    }

    @Override
    public net.minecraft.world.phys.HitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player) {
        if (this.getFocus(stack) != null) {
            FocusPackage fp = ItemFocus.getPackage(this.getFocusStack(stack));
            if (fp != null && FocusEngine.doesPackageContainElement(fp, "thaumcraft.PLAN")) {
                return ((thaumcraft.api.items.IArchitect) FocusEngine.getElement("thaumcraft.PLAN")).getArchitectMOP(this.getFocusStack(stack), world, player);
            }
        }
        return null;
    }

    @Override
    public boolean useBlockHighlight(ItemStack stack) {
        return false;
    }
}
