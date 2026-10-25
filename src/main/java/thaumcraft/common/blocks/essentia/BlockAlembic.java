package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.essentia.TileAlembic;

/**
 * Алембик (порт BlockAlembic). Shift + пустая рука: по грани с ярлыком — снять ярлык, иначе — вылить эссенцию (в Выброс).
 * Ярлык клеится на боковую грань. Сигнал компаратора — по заполненности.
 */
public class BlockAlembic extends BaseEntityBlock implements ILabelable {
    public static final MapCodec<BlockAlembic> CODEC = simpleCodec(BlockAlembic::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public BlockAlembic(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileAlembic(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Без Shift (или с предметом, который не обходит блок) — как в оригинале, клик просто поглощается. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        this.activate(state, world, pos, player, hit.getDirection());
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        this.activate(state, world, pos, player, hit.getDirection());
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    private void activate(BlockState state, Level world, BlockPos pos, Player player, Direction side) {
        if (!(world.getBlockEntity(pos) instanceof TileAlembic te) || !player.isShiftKeyDown()) {
            return;
        }
        if (te.aspectFilter != null && side.ordinal() == te.facing) {
            if (world.isClientSide) {
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.page.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
            } else {
                te.aspectFilter = null;
                te.facing = Direction.DOWN.ordinal();
                te.setChanged();
                te.syncTile(true);
                world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5F + side.getStepX() / 3.0F, pos.getY() + 0.5F,
                        pos.getZ() + 0.5F + side.getStepZ() / 3.0F, new ItemStack(ItemsTC.label.get())));
            }
        } else if (player.getMainHandItem().isEmpty()) {
            if (world.isClientSide) {
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.jar.get(), SoundSource.BLOCKS, 0.4F, 1.0F, false);
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.5F,
                        1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.3F, false);
            } else {
                AuraHelper.polluteAura(world, pos, te.amount, true);
                te.aspect = null;
                te.amount = 0;
                te.setChanged();
                te.syncTile(true);
            }
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof TileAlembic tile) {
            float r = (float) tile.amount / tile.maxAmount;
            return Mth.floor(r * 14.0F) + (tile.amount > 0 ? 1 : 0);
        }
        return 0;
    }

    @Override
    public boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack labelstack) {
        Level world = player.level();
        if (world.getBlockEntity(pos) instanceof TileAlembic te && side.ordinal() > 1 && te.aspectFilter == null) {
            Aspect la = null;
            AspectList al = ((IEssentiaContainerItem) labelstack.getItem()).getAspects(labelstack);
            if (al != null) {
                la = al.getAspects()[0];
            }
            if (te.amount == 0 && la == null) {
                return false;
            }
            Aspect aspect = null;
            if (te.amount == 0 && la != null) {
                aspect = la;
            }
            if (te.amount > 0) {
                aspect = te.aspect;
            }
            if (aspect == null) {
                return false;
            }
            te.aspectFilter = aspect;
            te.facing = side.ordinal();
            te.setChanged();
            te.syncTile(true);
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.page.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
