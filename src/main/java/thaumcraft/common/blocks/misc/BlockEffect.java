package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Невидимый блок-эффект (порт BlockEffect): «искры» (урон 1 и замедление, иногда гаснет), «гниль» (иссушение, замедление и
 * голод — от «Проклятия»), «мерцание» (просто свет 15). Искры и гниль исчезают при случайном тике. Заменяемый, проходимый.
 * TODO этап 15: гниль не действует на существ Потустороннего (IEldritchMob).
 */
public class BlockEffect extends Block {
    public BlockEffect(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (this == BlocksTC.effectShock.get()) {
            if (entity instanceof LivingEntity le) {
                ServerEvents.addRunnableServer(world, () -> {
                    le.hurt(world.damageSources().magic(), 1.0F);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, true, true));
                }, 0);
            }
            if (!world.isClientSide && world.random.nextInt(100) == 0) {
                world.removeBlock(pos, false);
            }
        } else if (this == BlocksTC.effectSap.get() && entity instanceof LivingEntity le && !le.hasEffect(MobEffects.WITHER)) {
            ServerEvents.addRunnableServer(world, () -> {
                le.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, true, true));
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, true));
                le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 1, true, true));
            }, 0);
        }
    }

    /** Запланированный тик (updateTick оригинала обрабатывал и его): сок надзирателя исчезает через 250–400 тиков. */
    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (this != BlocksTC.effectGlimmer.get()) {
            world.removeBlock(pos, false);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (this != BlocksTC.effectGlimmer.get()) {
            world.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level w, BlockPos pos, RandomSource r) {
        if (this == BlocksTC.effectGlimmer.get()) {
            return;
        }
        float h = r.nextFloat() * 0.33F;
        if (this == BlocksTC.effectShock.get()) {
            IFXDispatcher.get().spark(pos.getX() + w.random.nextFloat(), pos.getY() + 0.1515F + h / 2.0F, pos.getZ() + w.random.nextFloat(), 3.0F + h * 6.0F,
                    0.65F + w.random.nextFloat() * 0.1F, 1.0F, 1.0F, 0.8F);
        } else {
            IFXDispatcher.get().spark(pos.getX() + w.random.nextFloat(), pos.getY() + 0.1515F + h / 2.0F, pos.getZ() + w.random.nextFloat(), 3.0F + h * 6.0F,
                    0.3F - w.random.nextFloat() * 0.1F, 0.0F, 0.5F + w.random.nextFloat() * 0.2F, 1.0F);
        }
        if (r.nextInt(50) == 0) {
            w.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundsTC.jacobs.get(), SoundSource.AMBIENT, 0.25F, 1.0F + (r.nextFloat() - r.nextFloat()) * 0.2F,
                    false);
        }
    }
}
