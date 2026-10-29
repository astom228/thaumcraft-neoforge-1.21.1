package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Мотыга Роста (порт ItemElementalHoe): вспахивает 3×3 (с Shift — одну клетку); если вспахать нечего — действует как костная
 * мука (3 прочности). Зачаровывается слабо (5).
 */
public class ItemElementalHoe extends HoeItem {
    public ItemElementalHoe(Tier tier, Properties props) {
        super(tier, props.attributes(ItemThaumiumHoe.hoeAttributes(tier)));
    }

    @Override
    public int getEnchantmentValue() {
        return 5;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return super.useOn(ctx);
        }
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        boolean did = false;
        for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
                BlockPos pp = pos.offset(xx, 0, zz);
                if (!world.isLoaded(pp)) {
                    continue;
                }
                UseOnContext c2 = new UseOnContext(world, player, ctx.getHand(), ctx.getItemInHand(),
                        new BlockHitResult(ctx.getClickLocation().add(xx, 0, zz), ctx.getClickedFace(), pp, ctx.isInside()));
                if (super.useOn(c2).consumesAction()) {
                    if (world.isClientSide) {
                        IFXDispatcher.get().drawBamf(pp.getX() + 0.5, pp.getY() + 1.01, pp.getZ() + 0.5, 0.3F, 0.12F, 0.1F, xx == 0 && zz == 0, false,
                                Direction.UP);
                    }
                    did = true;
                }
            }
        }
        if (!did) {
            // Utils.useBonemealAtLoc оригинала
            did = BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), world, pos, player);
            if (did) {
                if (player != null) {
                    ctx.getItemInHand().hurtAndBreak(3, player, LivingEntity.getSlotForHand(ctx.getHand()));
                }
                if (!world.isClientSide) {
                    world.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 0);
                } else {
                    IFXDispatcher.get().drawBlockMistParticles(pos, 4259648);
                }
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
