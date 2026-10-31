package thaumcraft.common.blocks.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.container.ContainerGolemBuilder;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

/**
 * Пресс големов (порт BlockGolemBuilder): поршень (вверх) многоблока «решётка, котёл, наковальня, каменный стол» после соли
 * мира. Невидим — модель рисует TileGolemBuilderRenderer. Снятие любой части возвращает исходные блоки; выпадает поршень.
 */
public class BlockGolemBuilder extends BaseEntityBlock {
    public static final MapCodec<BlockGolemBuilder> CODEC = simpleCodec(BlockGolemBuilder::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Разбор многоблока идёт на сервере в одном тике; флаг не даёт заполнителям запускать разбор повторно. */
    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> false);

    public BlockGolemBuilder(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static boolean isIgnoring() {
        return IGNORE.get();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileGolemBuilder(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ConfigBlocks.GOLEM_BUILDER_TILE.get(), TileGolemBuilder::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(Blocks.PISTON);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        openGui(world, pos, player);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    public static void openGui(Level world, BlockPos pos, Player player) {
        if (!world.isClientSide && player instanceof ServerPlayer sp && world.getBlockEntity(pos) instanceof TileGolemBuilder tile) {
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ContainerGolemBuilder(id, inv, tile), Component.translatable("block.thaumcraft.golem_press")),
                    buf -> buf.writeBlockPos(pos));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof TileGolemBuilder tile) {
                tile.dropContents();
            }
            destroy(world, pos, pos);
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    /**
     * Разобрать пресс: заполнители вокруг поршня — обратно в решётку, котёл, наковальню и стол; сам пресс (если ломали не
     * его) — в поршень вверх. В оригинале позиции сравнивались по ссылке (всегда «не равны»), и сломанный заполнитель
     * восстанавливался вдобавок к выпавшему предмету — здесь сравнение по значению (DEVIATIONS).
     */
    public static void destroy(Level world, BlockPos pos, BlockPos startpos) {
        if (IGNORE.get() || world.isClientSide) {
            return;
        }
        IGNORE.set(true);
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = 0; b <= 1; b++) {
                    for (int c = -1; c <= 1; c++) {
                        BlockPos p = pos.offset(a, b, c);
                        if (p.equals(startpos)) {
                            continue;
                        }
                        BlockState bs = world.getBlockState(p);
                        if (bs.is(BlocksTC.placeholderBars.get())) {
                            world.setBlock(p, Blocks.IRON_BARS.defaultBlockState(), 3);
                        } else if (bs.is(BlocksTC.placeholderAnvil.get())) {
                            world.setBlock(p, Blocks.ANVIL.defaultBlockState(), 3);
                        } else if (bs.is(BlocksTC.placeholderCauldron.get())) {
                            world.setBlock(p, Blocks.CAULDRON.defaultBlockState(), 3);
                        } else if (bs.is(BlocksTC.placeholderTable.get())) {
                            world.setBlock(p, BlocksTC.tableStone.get().defaultBlockState(), 3);
                        }
                    }
                }
            }
            if (!pos.equals(startpos)) {
                world.setBlock(pos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 3);
            }
        } finally {
            IGNORE.set(false);
        }
    }
}
