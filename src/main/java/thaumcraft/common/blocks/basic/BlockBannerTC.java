package thaumcraft.common.blocks.basic;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumcraft.common.tiles.misc.TileBanner;

/**
 * Знамя Thaumcraft (порт BlockBannerTC): 16 цветов и знамя Багрового культа (dye = null). Рисуется рендером блок-сущности,
 * без коллизии. Фиал эссенции — нарисовать аспект (фиал тратится), Shift+ПКМ — стереть (у знамени культа — нельзя).
 */
public class BlockBannerTC extends BaseEntityBlock {
    public static final MapCodec<BlockBannerTC> CODEC = simpleCodec(p -> new BlockBannerTC(null, p));
    private static final VoxelShape STANDING = Shapes.box(0.33, 0.0, 0.33, 0.66, 2.0, 0.66);
    @Nullable
    public final DyeColor dye;

    public BlockBannerTC(@Nullable DyeColor dye, Properties props) {
        super(props);
        this.dye = dye;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (!(level.getBlockEntity(pos) instanceof TileBanner tile)) {
            return Shapes.block();
        }
        if (!tile.getWall()) {
            return STANDING;
        }
        return switch (tile.getBannerFacing()) {
            case 0 -> Shapes.box(0.0, -1.0, 0.0, 1.0, 1.0, 0.25);
            case 4 -> Shapes.box(0.75, -1.0, 0.0, 1.0, 1.0, 1.0);
            case 8 -> Shapes.box(0.0, -1.0, 0.75, 1.0, 1.0, 1.0);
            case 12 -> Shapes.box(0.0, -1.0, 0.0, 0.25, 1.0, 1.0);
            default -> Shapes.block();
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level w, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (!p.isShiftKeyDown() && stack.getItem() instanceof ItemPhial) {
            if (w.getBlockEntity(pos) instanceof TileBanner te && te.getColor() >= 0) {
                AspectList al = ((IEssentiaContainerItem) stack.getItem()).getAspects(stack);
                if (al != null && al.size() > 0) {
                    if (!w.isClientSide) {
                        te.setAspect(al.getAspects()[0]);
                        stack.shrink(1);
                        te.setChanged();
                        te.syncTile(true);
                        w.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return ItemInteractionResult.sidedSuccess(w.isClientSide);
                }
            }
            return ItemInteractionResult.sidedSuccess(w.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level w, BlockPos pos, Player p, BlockHitResult hit) {
        if (p.isShiftKeyDown() && w.getBlockEntity(pos) instanceof TileBanner te && te.getColor() >= 0) {
            if (!w.isClientSide) {
                te.setAspect(null);
                te.setChanged();
                te.syncTile(true);
                w.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(w.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileBanner(pos, state);
    }

    /** Место для знамени (Blocks.STANDING_BANNER.canPlaceBlockAt 1.12 — заменяемая клетка). */
    public static boolean canPlaceAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced();
    }
}
