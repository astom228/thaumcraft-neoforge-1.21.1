package thaumcraft.common.entities.construct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ConfigEntities;

/**
 * Турель в предмете (порт ItemTurretPlacer; в 1.12 — один предмет с метой: 0 — арбалет, 1 — улучшенный, 2 — бур).
 * Ставит на свободные 2 клетки по высоте (не на нижнюю грань), если там нет существ; хозяин — поставивший.
 */
public class ItemTurretPlacer extends Item {
    public static final int BASIC = 0;
    public static final int ADVANCED = 1;
    public static final int BORE = 2;
    private final int kind;

    public ItemTurretPlacer(int kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    private static boolean replaceable(Level world, BlockPos p) {
        return world.isEmptyBlock(p) || world.getBlockState(p).canBeReplaced();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Direction side = ctx.getClickedFace();
        Player player = ctx.getPlayer();
        Level world = ctx.getLevel();
        if (side == Direction.DOWN || player == null) {
            return InteractionResult.PASS;
        }
        BlockPos pos = ctx.getClickedPos();
        BlockPos blockpos = world.getBlockState(pos).canBeReplaced() ? pos : pos.relative(side);
        if (!player.mayUseItemAt(blockpos, side, stack)) {
            return InteractionResult.PASS;
        }
        BlockPos blockpos1 = blockpos.above();
        if (!replaceable(world, blockpos) || !replaceable(world, blockpos1)) {
            return InteractionResult.PASS;
        }
        if (!world.getEntities(null, new AABB(blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockpos.getX() + 1.0, blockpos.getY() + 2.0,
                blockpos.getZ() + 1.0)).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide) {
            return InteractionResult.PASS;
        }
        world.removeBlock(blockpos, false);
        world.removeBlock(blockpos1, false);
        EntityOwnedConstruct turret = switch (this.kind) {
            case BASIC -> ConfigEntities.TURRET_BASIC.get().create(world);
            case ADVANCED -> ConfigEntities.TURRET_ADVANCED.get().create(world);
            default -> createBore(world, player);
        };
        if (turret != null) {
            turret.moveTo(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5, 0.0F, 0.0F);
            turret.setOwned(true);
            turret.setValidSpawn();
            turret.setOwnerId(player.getUUID());
            world.addFreshEntity(turret);
            world.playSound(null, turret.getX(), turret.getY(), turret.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /** Магический бур смотрит туда же, куда игрок (по горизонтали). */
    protected EntityOwnedConstruct createBore(Level world, Player player) {
        EntityArcaneBore bore = ConfigEntities.ARCANE_BORE.get().create(world);
        if (bore != null) {
            bore.setFacing(player.getDirection());
        }
        return bore;
    }
}
