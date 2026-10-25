package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.devices.TileWaterJug;

/** Неиссякаемая урна (порт BlockWaterJug): ведром/бутылкой из неё набирается вода; полная — брызги сверху. */
public class BlockWaterJug extends BaseEntityBlock {
    public static final MapCodec<BlockWaterJug> CODEC = simpleCodec(BlockWaterJug::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    public BlockWaterJug(Properties properties) {
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
        return new TileWaterJug(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ConfigBlocks.EVERFULL_URN_TILE.get(), TileWaterJug::tick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Как в оригинале — взаимодействие с самим баком (ведро воды можно и вылить в урну). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof TileWaterJug tile) {
            if (FluidUtil.interactWithFluidHandler(player, hand, tile.tank)) {
                player.inventoryMenu.broadcastChanges();
                tile.setChanged();
                tile.syncTile(false);
                world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.3F);
            }
        }
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        if (world.getBlockEntity(pos) instanceof TileWaterJug tile && tile.tank.getFluidAmount() >= tile.tank.getCapacity()) {
            IFXDispatcher.get().jarSplashFx(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
