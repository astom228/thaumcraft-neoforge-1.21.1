package thaumcraft.common.entities.construct.golem;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.entities.construct.golem.gui.SealBaseContainer;
import thaumcraft.common.entities.construct.golem.seals.SealHandler;
import thaumcraft.common.lib.SoundsTC;

/**
 * Колокольчик големанта (порт ItemGolemBell): ПКМ по печати (или по блоку, сквозь который видна печать, до 5 блоков) —
 * окно настройки, с Shift — снять печать (она выпадает). По голему (исследование GOLEMDIRECT) — «за мной»/«стой здесь».
 * С Shift (исследование GOLEMLOGISTICS) — окно логистики: по блоку — доставка к нему, в воздух — себе.
 * Снимать печать могут её владелец, его команда и операторы (в оригинале — кто угодно).
 */
public class ItemGolemBell extends Item implements ISealDisplayer {
    public ItemGolemBell(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        player.swing(hand);
        if (world.isClientSide) {
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.0F + world.random.nextFloat() * 0.1F);
        } else {
            HitResult mop = player.pick(player.blockInteractionRange(), 0.0F, false);
            if (mop.getType() != HitResult.Type.MISS) {
                ISealEntity se = getSeal(player);
                if (se != null) {
                    this.act((ServerPlayer) player, se);
                }
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            if (player.isShiftKeyDown() && thaumcraft.api.capabilities.ThaumcraftCapabilities.knowsResearch(player, "GOLEMLOGISTICS")) {
                openLogistics((ServerPlayer) player, null, null);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
        }
        return super.use(world, player, hand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        player.swing(ctx.getHand());
        if (!ctx.getLevel().isClientSide) {
            ISealEntity se = SealHandler.getSealEntity(ctx.getLevel().dimension(), new SealPos(ctx.getClickedPos(), ctx.getClickedFace()));
            if (se != null) {
                this.act((ServerPlayer) player, se);
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown() && thaumcraft.api.capabilities.ThaumcraftCapabilities.knowsResearch(player, "GOLEMLOGISTICS")) {
                openLogistics((ServerPlayer) player, ctx.getClickedPos(), ctx.getClickedFace());
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /** Окно логистики: заказ доставки к блоку (target) или себе (null). */
    private static void openLogistics(ServerPlayer player, BlockPos target, net.minecraft.core.Direction side) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new thaumcraft.common.container.ContainerLogistics(id, inv, target, side),
                net.minecraft.network.chat.Component.translatable("item.thaumcraft.golem_bell")));
    }

    private void act(ServerPlayer player, ISealEntity se) {
        if (player.isShiftKeyDown()) {
            if (SealBaseContainer.canEdit(player, se)) {
                SealHandler.removeSealEntity(player.level(), se.getSealPos(), false);
                player.level().playSound(null, se.getSealPos().pos, SoundsTC.zap.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        } else {
            SealBaseContainer.open(player, se);
        }
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    /** Печать на луче взгляда в пределах 5 блоков: проверяются грани каждого блока, через которые проходит луч. */
    public static ISealEntity getSeal(Player player) {
        float f = player.getXRot();
        float f1 = player.getYRot();
        Vec3 vec0 = player.getEyePosition();
        float f2 = Mth.cos(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f3 = Mth.sin(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = -Mth.cos(-f * (float) (Math.PI / 180.0));
        float f5 = Mth.sin(-f * (float) (Math.PI / 180.0));
        double d3 = 5.0;
        Vec3 vec2 = new Vec3(f3 * f4 * d3, f5 * d3, f2 * f4 * d3);
        Vec3 vec1 = vec0.add(vec2);
        Vec3 vec3 = vec0.add(vec2.scale(0.1));
        List<AABB> unit = List.of(new AABB(0, 0, 0, 1, 1, 1));
        for (int a = 0; a < vec2.length() * 10.0; a++) {
            BlockPos pos = BlockPos.containing(vec3);
            BlockHitResult mop = AABB.clip(unit, vec0, vec1, pos);
            if (mop != null) {
                ISealEntity se = SealHandler.getSealEntity(player.level().dimension(), new SealPos(pos, mop.getDirection()));
                if (se != null) {
                    return se;
                }
            }
            vec3 = vec3.add(vec2.scale(0.1));
        }
        return null;
    }
}
