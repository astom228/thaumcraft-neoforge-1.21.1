package thaumcraft.common.items.tools;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.items.IArchitect;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Лопата Землекопа (порт ItemElementalShovel): ПКМ по блоку (без блок-сущности) ставит такие же блоки из инвентаря 3×3 перед
 * гранью (трава — землёй, если травы нет); ориентация квадрата — клавиша «разное» (G, компонент thaumcraft:shovel_orientation,
 * NBT "or" оригинала). С Shift — подсветка будущих блоков (IArchitect). Из рецепта — «Разрушительный».
 */
public class ItemElementalShovel extends ShovelItem implements IArchitect, ICreativeVariants {
    public ItemElementalShovel(Tier tier, Properties props) {
        super(tier, props.attributes(ShovelItem.createAttributes(tier, 1.5F, -3.0F)));
    }

    public static byte getOrientation(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.SHOVEL_ORIENTATION.get(), 0).byteValue();
    }

    public static void setOrientation(ItemStack stack, byte o) {
        stack.set(TCDataComponents.SHOVEL_ORIENTATION.get(), (int) (o % 3));
    }

    /** Смещение клетки (aa, bb) квадрата 3×3 в зависимости от ориентации, грани и взгляда игрока (одинаково в оригинале дважды). */
    private static BlockPos offset(byte o, Direction side, Player player, int aa, int bb) {
        int xx = 0;
        int yy = 0;
        int zz = 0;
        int s = side.get3DDataValue();
        if (o == 1) {
            yy = bb;
            if (s <= 1) {
                int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5) & 3;
                if (l != 0 && l != 2) {
                    zz = aa;
                } else {
                    xx = aa;
                }
            } else if (s <= 3) {
                zz = aa;
            } else {
                xx = aa;
            }
        } else if (o == 2) {
            if (s <= 1) {
                int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5) & 3;
                yy = bb;
                if (l != 0 && l != 2) {
                    zz = aa;
                } else {
                    xx = aa;
                }
            } else {
                zz = bb;
                xx = aa;
            }
        } else if (s <= 1) {
            xx = aa;
            zz = bb;
        } else if (s <= 3) {
            xx = aa;
            yy = bb;
        } else {
            zz = aa;
            yy = bb;
        }
        return new BlockPos(xx, yy, zz);
    }

    private static boolean replaceable(Level world, BlockPos p2) {
        return world.isLoaded(p2) && (world.isEmptyBlock(p2) || world.getBlockState(p2).canBeReplaced());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        Direction side = ctx.getClickedFace();
        BlockState bs = world.getBlockState(pos);
        if (player == null || world.getBlockEntity(pos) != null) {
            return InteractionResult.FAIL;
        }
        ItemStack held = ctx.getItemInHand();
        byte o = getOrientation(held);
        boolean any = false;
        for (int aa = -1; aa <= 1; aa++) {
            for (int bb = -1; bb <= 1; bb++) {
                BlockPos p2 = pos.relative(side).offset(offset(o, side, player, aa, bb));
                if (!replaceable(world, p2)) {
                    continue;
                }
                if (world.isClientSide) {
                    // сервер решает, есть ли блоки в инвентаре; клиент только машет рукой
                    any = true;
                    continue;
                }
                BlockState place = null;
                if (player.getAbilities().instabuild
                        || InventoryUtils.consumePlayerItem(player, new ItemStack(bs.getBlock().asItem()), false, false)) {
                    place = bs;
                } else if (bs.is(Blocks.GRASS_BLOCK)
                        && (player.getAbilities().instabuild || InventoryUtils.consumePlayerItem(player, new ItemStack(Blocks.DIRT), false, false))) {
                    place = Blocks.DIRT.defaultBlockState();
                }
                if (place != null && !bs.getBlock().asItem().equals(net.minecraft.world.item.Items.AIR)) {
                    world.playSound(null, p2, bs.getSoundType(world, pos, player).getBreakSound(), SoundSource.BLOCKS, 0.6F,
                            0.9F + world.random.nextFloat() * 0.2F);
                    world.setBlockAndUpdate(p2, place);
                    held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(ctx.getHand()));
                    // в 1.12 — на клиенте; здесь клиент не знает, хватило ли блоков, — эффект пакетом всем рядом
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear((net.minecraft.server.level.ServerLevel) world, null,
                            p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, 32.0,
                            new thaumcraft.common.lib.network.fx.PacketFXBlockBamf(p2, 8401408, false, false, side));
                    any = true;
                }
            }
        }
        return any ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.FAIL;
    }

    @Override
    public ArrayList<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos pos, Direction side, Player player) {
        ArrayList<BlockPos> b = new ArrayList<>();
        if (!player.isShiftKeyDown()) {
            return b;
        }
        byte o = getOrientation(stack);
        for (int aa = -1; aa <= 1; aa++) {
            for (int bb = -1; bb <= 1; bb++) {
                BlockPos p2 = pos.relative(side).offset(offset(o, side, player, aa, bb));
                if (replaceable(world, p2)) {
                    b.add(p2);
                }
            }
        }
        return b;
    }

    @Override
    public boolean showAxis(ItemStack stack, Level world, Player player, Direction side, EnumAxis axis) {
        return false;
    }

    /** Utils.rayTrace оригинала — луч взгляда на дальность взаимодействия с блоками. */
    @Override
    public HitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player) {
        double reach = player instanceof Player p ? p.blockInteractionRange() : 4.5;
        return world.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    @Override
    public boolean useBlockHighlight(ItemStack stack) {
        return true;
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        ItemStack w1 = new ItemStack(this);
        EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.DESTRUCTIVE, 1);
        output.accept(w1);
    }
}
