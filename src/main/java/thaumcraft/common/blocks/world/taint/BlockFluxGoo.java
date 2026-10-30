package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Флюкс-слизь (порт BlockFluxGoo — конечная жидкость Forge BlockFluidFinite, 8 уровней). Здесь — блок со свойством LEVEL
 * (0–7, высота (LEVEL+1)/8) и переносом логики BlockFluidFinite: стекает вниз, выравнивается с соседями, объём сохраняется.
 * Сквозь неё можно пройти; замедляет и насылает «Лихорадку Выброса» (уровень LEVEL/3, без лечения молоком). На каждом тике
 * (случайном или запланированном) с шансом 1/4 тает на уровень, отдавая Выброс в ауру; последний уровень — Выброс или волокна
 * порчи. Слизни из слизи — этап 16.
 */
public class BlockFluxGoo extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);
    /** tickRate Forge = вязкость / 200 (вязкость флюкс-слизи 6000). */
    private static final int TICK_RATE = 30;
    private static final int QUANTA = 8;
    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        for (int i = 0; i < 8; i++) {
            SHAPES[i] = Block.box(0, 0, 0, 16, 2 * (i + 1), 16);
        }
    }

    public BlockFluxGoo(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 7));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** isReplaceable оригинала: можно поставить блок поверх, пока слизи меньше половины. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        return state.getValue(LEVEL) < 4;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        int md = state.getValue(LEVEL);
        // TODO этап 16: таумический слизень (EntityThaumicSlime) впитывает слизь и растёт
        float q = (md + 1) / (float) QUANTA;
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0F - q, 1.0, 1.0F - q));
        if (!world.isClientSide && entity instanceof LivingEntity le) {
            MobEffectInstance pe = new MobEffectInstance(ConfigPotions.VIS_EXHAUST, 600, md / 3, true, true);
            pe.getCures().clear();
            le.addEffect(pe);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.tick(state, level, pos, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        int meta = state.getValue(LEVEL);
        if (meta >= 2 && meta < 6 && world.isEmptyBlock(pos.above()) && rand.nextInt(50) == 0) {
            // TODO этап 16: слизень размера 1 (EntityThaumicSlime) вместо слизи
            this.flow(state, world, pos, rand);
        } else if (meta >= 6 && world.isEmptyBlock(pos.above()) && rand.nextInt(50) == 0) {
            // TODO этап 16: слизень размера 2 (EntityThaumicSlime) вместо слизи
            this.flow(state, world, pos, rand);
        } else if (rand.nextInt(4) == 0) {
            if (meta == 0) {
                if (rand.nextBoolean()) {
                    AuraHelper.polluteAura(world, pos, 1.0F, true);
                    world.removeBlock(pos, false);
                } else {
                    world.setBlockAndUpdate(pos, BlocksTC.taintFibre.get().defaultBlockState());
                }
            } else {
                world.setBlock(pos, state.setValue(LEVEL, meta - 1), 2);
                AuraHelper.polluteAura(world, pos, 1.0F, true);
            }
        } else {
            this.flow(state, world, pos, rand);
        }
    }

    // ------------------------------------------------------------------ BlockFluidFinite (Forge 1.12) -----------------

    /** updateTick BlockFluidFinite: сначала вниз, затем выравнивание по горизонтали. */
    private void flow(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        boolean changed = false;
        int quantaRemaining = state.getValue(LEVEL) + 1;
        int prevRemaining = quantaRemaining;
        quantaRemaining = this.tryToFlowVerticallyInto(world, pos, quantaRemaining);
        if (quantaRemaining < 1) {
            return;
        } else if (quantaRemaining != prevRemaining) {
            changed = true;
            if (quantaRemaining == 1) {
                world.setBlock(pos, state.setValue(LEVEL, quantaRemaining - 1), 2);
                return;
            }
        } else if (quantaRemaining == 1) {
            return;
        }
        int lowerThan = quantaRemaining - 1;
        int total = quantaRemaining;
        int count = 1;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos off = pos.relative(side);
            if (!world.isLoaded(off)) {
                continue;
            }
            if (this.displaceIfPossible(world, off)) {
                world.removeBlock(off, false);
            }
            int quanta = this.getQuantaValueBelow(world, off, lowerThan);
            if (quanta >= 0) {
                count++;
                total += quanta;
            }
        }
        if (count == 1) {
            if (changed) {
                world.setBlock(pos, state.setValue(LEVEL, quantaRemaining - 1), 2);
            }
            return;
        }
        int each = total / count;
        int rem = total % count;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos off = pos.relative(side);
            if (!world.isLoaded(off)) {
                continue;
            }
            int quanta = this.getQuantaValueBelow(world, off, lowerThan);
            if (quanta >= 0) {
                int newQuanta = each;
                if (rem == count || rem > 1 && rand.nextInt(count - rem) != 0) {
                    newQuanta++;
                    rem--;
                }
                if (newQuanta != quanta) {
                    if (newQuanta == 0) {
                        world.removeBlock(off, false);
                    } else {
                        world.setBlock(off, this.defaultBlockState().setValue(LEVEL, newQuanta - 1), 2);
                    }
                    world.scheduleTick(off, this, TICK_RATE);
                }
                count--;
            }
        }
        if (rem > 0) {
            each++;
        }
        world.setBlock(pos, state.setValue(LEVEL, each - 1), 2);
    }

    private int tryToFlowVerticallyInto(ServerLevel world, BlockPos pos, int amtToInput) {
        BlockPos other = pos.below();
        if (other.getY() < world.getMinBuildHeight()) {
            world.removeBlock(pos, false);
            return 0;
        }
        int amt = this.getQuantaValueBelow(world, other, QUANTA);
        if (amt >= 0) {
            amt += amtToInput;
            if (amt > QUANTA) {
                world.setBlock(other, this.defaultBlockState().setValue(LEVEL, QUANTA - 1), 3);
                world.scheduleTick(other, this, TICK_RATE);
                return amt - QUANTA;
            } else if (amt > 0) {
                world.setBlock(other, this.defaultBlockState().setValue(LEVEL, amt - 1), 3);
                world.scheduleTick(other, this, TICK_RATE);
                world.removeBlock(pos, false);
                return 0;
            }
            return amtToInput;
        }
        if (this.displaceIfPossible(world, other)) {
            world.setBlock(other, this.defaultBlockState().setValue(LEVEL, amtToInput - 1), 3);
            world.scheduleTick(other, this, TICK_RATE);
            world.removeBlock(pos, false);
            return 0;
        }
        return amtToInput;
    }

    /** getQuantaValueBelow: своя слизь — уровень+1 (−1, если не меньше belowThis), воздух — 0, иначе −1. */
    private int getQuantaValueBelow(BlockGetter world, BlockPos pos, int belowThis) {
        BlockState s = world.getBlockState(pos);
        if (s.getBlock() == this) {
            int q = s.getValue(LEVEL) + 1;
            return q >= belowThis ? -1 : q;
        }
        return s.isAir() ? 0 : -1;
    }

    /** displaceIfPossible: вытесняет волокна порчи (displacements оригинала) и прочее заменяемое, кроме жидкостей и себя. */
    private boolean displaceIfPossible(Level world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        if (s.isAir()) {
            return true;
        }
        if (s.getBlock() == this) {
            return false;
        }
        if (s.getBlock() == BlocksTC.taintFibre.get()) {
            return true;
        }
        if (!s.getFluidState().isEmpty() || s.blocksMotion()) {
            return false;
        }
        if (s.canBeReplaced()) {
            Block.dropResources(s, world, pos);
            return true;
        }
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        int meta = state.getValue(LEVEL);
        if (rand.nextInt(44) <= meta) {
            IFXDispatcher.get().fluxGooFx(pos.getX() + rand.nextFloat(), pos.getY() + 0.125F * meta, pos.getZ() + rand.nextFloat());
        }
    }
}
