package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Саженцы великого и серебряного дерева. Рост как в оригинале: случайный тик, свет сверху не меньше 9, шанс 1/7;
 * стадия 0 → 1 → дерево. Костная мука срабатывает с шансом 25%.
 */
public class BlockSaplingTC extends BushBlock implements BonemealableBlock {
    public static final MapCodec<BlockSaplingTC> CODEC = simpleCodec(BlockSaplingTC::new);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 1);
    // В оригинале AABB 0.1..0.9 по X/Z и 0..0.8 по Y
    protected static final VoxelShape SAPLING_AABB = Block.box(1.6, 0.0, 1.6, 14.4, 12.8, 14.4);

    public BlockSaplingTC(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SAPLING_AABB;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
            this.grow(level, pos, state, random);
        }
    }

    public void grow(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            this.generateTree(level, pos, state, random);
        }
    }

    public void generateTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        // ставит воздух, пытается вырастить дерево, при неудаче возвращает саженец
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 4);
        var w = new thaumcraft.common.world.objects.TreeWriter(level, null);
        boolean ok = this == thaumcraft.api.blocks.BlocksTC.saplingSilverwood.get()
                ? new thaumcraft.common.world.objects.WorldGenSilverwoodTrees(true, 7, 4).generate(w, random, pos)
                : new thaumcraft.common.world.objects.WorldGenGreatwoodTrees(true, false).generate(w, random, pos);
        if (!ok) {
            level.setBlock(pos, state, 4);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return level.random.nextFloat() < 0.25;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        this.grow(level, pos, state, random);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }
}
