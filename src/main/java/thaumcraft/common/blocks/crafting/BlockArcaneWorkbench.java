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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

/** Магический верстак (порт BlockArcaneWorkbench): правый клик — меню; содержимое выпадает при разрушении. */
public class BlockArcaneWorkbench extends BaseEntityBlock {
    public static final MapCodec<BlockArcaneWorkbench> CODEC = simpleCodec(BlockArcaneWorkbench::new);

    public BlockArcaneWorkbench(Properties properties) {
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
        return new TileArcaneWorkbench(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            openGui(level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** openGui(…, 13, …) оригинала; используется и зарядником. */
    public static void openGui(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof TileArcaneWorkbench tile) {
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ContainerArcaneWorkbench(id, inv, tile),
                    Component.translatable("block.thaumcraft.arcane_workbench")), buf -> buf.writeBlockPos(pos));
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileArcaneWorkbench tile) {
            tile.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
