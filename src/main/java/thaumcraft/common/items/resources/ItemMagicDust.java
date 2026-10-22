package thaumcraft.common.items.resources;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Соль мира (порт ItemMagicDust): правый клик по блоку проверяет зарегистрированные превращения (IDustTrigger);
 * подходящее срабатывает, тратя щепотку (кроме творческого режима). На клиенте — искры от руки к блоку и звук.
 * Shift + ПКМ — обычное использование блока.
 */
public class ItemMagicDust extends Item {

    public ItemMagicDust(Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        player.swing(context.getHand());
        for (IDustTrigger trigger : IDustTrigger.triggers) {
            IDustTrigger.Placement place = trigger.getValidFace(world, player, pos, context.getClickedFace());
            if (place != null) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                trigger.execute(world, player, pos, place, context.getClickedFace());
                if (!world.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                Vec3 hit = context.getClickLocation();
                this.doSparkles(player, world, pos, hit, context.getHand(), trigger, place);
                break;
            }
        }
        return InteractionResult.PASS;
    }

    private void doSparkles(Player player, Level world, BlockPos pos, Vec3 hit, InteractionHand hand, IDustTrigger trigger, IDustTrigger.Placement place) {
        Vec3 v1 = EntityUtils.posToHand(player, hand);
        Vec3 v2 = new Vec3(pos.getX(), pos.getY(), pos.getZ()).add(0.5, 0.5, 0.5).subtract(v1);
        int cnt = 50;
        for (int a = 0; a < cnt; a++) {
            boolean floaty = a < cnt / 3;
            float r = Mth.randomBetweenInclusive(world.random, 255, 255) / 255.0F;
            float g = Mth.randomBetweenInclusive(world.random, 189, 255) / 255.0F;
            float b = Mth.randomBetweenInclusive(world.random, 64, 255) / 255.0F;
            IFXDispatcher.get().drawSimpleSparkle(world.random, v1.x, v1.y, v1.z, v2.x / 6.0 + world.random.nextGaussian() * 0.05,
                    v2.y / 6.0 + world.random.nextGaussian() * 0.05 + (floaty ? 0.05 : 0.15), v2.z / 6.0 + world.random.nextGaussian() * 0.05,
                    0.5F, r, g, b, world.random.nextInt(5), floaty ? 0.3F + world.random.nextFloat() * 0.5F : 0.85F, floaty ? 0.2F : 0.5F, 16);
        }
        world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundsTC.dust.get(), SoundSource.PLAYERS, 0.33F,
                1.0F + (float) world.random.nextGaussian() * 0.05F, false);
        List<BlockPos> sparkles = trigger.sparkle(world, player, pos, place);
        if (sparkles != null) {
            // в оригинале — точка попадания по блоку
            for (BlockPos p : sparkles) {
                IFXDispatcher.get().drawBlockSparkles(p, hit);
            }
        }
    }
}
