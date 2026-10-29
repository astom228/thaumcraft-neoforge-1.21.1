package thaumcraft.common.blocks.basic;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.misc.TileBarrierStone;

/**
 * Мостовые камни (порт BlockPavingStone): высота 15/16. Камень странника — идущим по нему существам скорость II и прыжок I на 2 с.
 * Барьерный камень (с блок-сущностью TileBarrierStone) — ставит над собой два невидимых барьера, которые не пропускают мобов
 * (игроков пропускают); под сигналом редстоуна барьер открыт. Руны над камнем показывают состояние.
 */
public class BlockPavingStone extends BaseEntityBlock {
    public static final MapCodec<BlockPavingStone> CODEC = simpleCodec(BlockPavingStone::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

    public BlockPavingStone(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    private boolean isBarrier() {
        return this == BlocksTC.pavingStoneBarrier.get();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.isBarrier() ? new TileBarrierStone(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !this.isBarrier() ? null : createTickerHelper(type, ConfigBlocks.BARRIER_STONE_TILE.get(), TileBarrierStone::tick);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity e) {
        if (!level.isClientSide && !this.isBarrier() && e instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false));
        }
        super.stepOn(level, pos, state, e);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!this.isBarrier()) {
            return;
        }
        if (world.getBestNeighborSignal(pos) > 0) {
            for (int a = 0; a < 4; a++) {
                IFXDispatcher.get().blockRunes(pos.getX(), pos.getY() + 0.7F, pos.getZ(), 0.2F + random.nextFloat() * 0.4F, random.nextFloat() * 0.3F,
                        0.8F + random.nextFloat() * 0.2F, 20, -0.02F);
            }
        } else if (world.getBlockState(pos.above(1)).is(BlocksTC.barrier.get()) || world.getBlockState(pos.above(2)).is(BlocksTC.barrier.get())) {
            for (int a = 0; a < 6; a++) {
                IFXDispatcher.get().blockRunes(pos.getX(), pos.getY() + 0.7F, pos.getZ(), 0.9F + random.nextFloat() * 0.1F, random.nextFloat() * 0.3F,
                        random.nextFloat() * 0.3F, 24, -0.02F);
            }
        } else {
            List<Entity> list = world.getEntities((Entity) null, new AABB(pos).inflate(1.0));
            for (Entity entity : list) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    IFXDispatcher.get().blockRunes(pos.getX(), pos.getY() + 0.6F + random.nextFloat() * Math.max(0.8F, entity.getEyeHeight()), pos.getZ(),
                            0.6F + random.nextFloat() * 0.4F, 0.0F, 0.3F + random.nextFloat() * 0.7F, 20, 0.0F);
                    break;
                }
            }
        }
    }
}
