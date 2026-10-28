package thaumcraft.common.blocks.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

/** Фокусный манипулятор (порт BlockFocalManipulator): щелчок — экран; при разрушении фокус выпадает. */
public class BlockFocalManipulator extends BaseEntityBlock {
    public static final MapCodec<BlockFocalManipulator> CODEC = simpleCodec(BlockFocalManipulator::new);

    public BlockFocalManipulator(Properties properties) {
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
        return new TileFocalManipulator(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ConfigBlocks.FOCAL_MANIPULATOR_TILE.get(), TileFocalManipulator::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide && player instanceof ServerPlayer sp && world.getBlockEntity(pos) instanceof TileFocalManipulator tile) {
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ContainerFocalManipulator(id, inv, tile),
                    Component.translatable("block.thaumcraft.focal_manipulator")), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof TileFocalManipulator tile) {
            tile.dropContents();
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}
