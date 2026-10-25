package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.essentia.TileJar;
import thaumcraft.common.tiles.essentia.TileJarBrain;
import thaumcraft.common.tiles.essentia.TileJarFillable;

/**
 * Банка (порт BlockJar): защищённая, пустотная, мозг в банке. При разрушении выпадает вместе с содержимым (эссенция, ярлык,
 * опыт), скоба — отдельно. ПКМ: скоба — закрыть банку; Shift по стороне с ярлыком — снять ярлык; Shift пустой рукой — вылить
 * эссенцию (Выброс в ауру); мозг — отдаёт часть опыта. Компаратор — по заполнению. Мозг усиливает стол зачарований.
 */
public class BlockJar extends BaseEntityBlock implements ILabelable {
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);
    private final BiFunction<BlockPos, BlockState, ? extends TileJar> factory;
    private final java.util.function.Supplier<? extends BlockEntityType<? extends TileJar>> type;

    public BlockJar(BiFunction<BlockPos, BlockState, ? extends TileJar> factory,
                    java.util.function.Supplier<? extends BlockEntityType<? extends TileJar>> type, Properties properties) {
        super(properties);
        this.factory = factory;
        this.type = type;
    }

    /** Кодек типа блока (в 1.21 обязателен; сериализуется только набор свойств — вид банки задаётся при регистрации). */
    public static final MapCodec<BlockJar> CODEC = simpleCodec(p -> new BlockJar(TileJarFillable::new, thaumcraft.common.config.ConfigBlocks.WARDED_JAR_TILE, p));

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
        return this.factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        return t == this.type.get() ? (l, p, s, be) -> TileJar.tick(l, p, s, (TileJar) be) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** dropBlockAsItemWithChance / harvestBlock оригинала: банка с содержимым (+ скоба отдельно). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity te = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        List<ItemStack> out = new ArrayList<>();
        ItemStack drop = new ItemStack(this);
        if (te instanceof TileJar jar) {
            drop.applyComponents(jar.collectComponents());
            if (te instanceof TileJarFillable f && f.blocked) {
                out.add(new ItemStack(ItemsTC.jarBrace.get()));
            }
        }
        out.add(drop);
        return out;
    }

    /** onBlockPlacedBy: банка «смотрит» на игрока (сторона, на которой будет ярлык). */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity ent, ItemStack stack) {
        super.setPlacedBy(world, pos, state, ent, stack);
        if (ent != null) {
            this.face(world, pos, ent);
        }
    }

    private void face(Level world, BlockPos pos, LivingEntity ent) {
        int l = Mth.floor(ent.getYRot() * 4.0F / 360.0F + 0.5) & 3;
        if (world.getBlockEntity(pos) instanceof TileJarFillable tile) {
            if (l == 0) {
                tile.facing = 2;
            }
            if (l == 1) {
                tile.facing = 5;
            }
            if (l == 2) {
                tile.facing = 3;
            }
            if (l == 3) {
                tile.facing = 4;
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        this.activate(state, world, pos, player, stack, hit.getDirection());
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        this.activate(state, world, pos, player, ItemStack.EMPTY, hit.getDirection());
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    /**
     * onBlockActivated оригинала (обе стороны). Изменения банки (скоба, снятый ярлык, вылитая эссенция) в оригинале на сервере
     * только помечались, но не отправлялись клиентам — другие игроки их не видели (DEVIATIONS.md): теперь отправляются.
     */
    private void activate(BlockState state, Level world, BlockPos pos, Player player, ItemStack held, Direction side) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof TileJarBrain brain) {
            brain.eatDelay = 40;
            if (!world.isClientSide) {
                int var6x = world.random.nextInt(Math.min(brain.xp + 1, 64));
                if (var6x > 0) {
                    brain.xp -= var6x;
                    int xp = var6x;
                    while (xp > 0) {
                        int var2x = ExperienceOrb.getExperienceValue(xp);
                        xp -= var2x;
                        world.addFreshEntity(new ExperienceOrb(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, var2x));
                    }
                    world.sendBlockUpdated(pos, state, state, 3);
                    brain.setChanged();
                }
            } else {
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.jar.get(), SoundSource.BLOCKS, 0.2F, 1.0F, false);
            }
        }
        if (!(te instanceof TileJarFillable jar)) {
            return;
        }
        if (!jar.blocked && held.is(ItemsTC.jarBrace.get())) {
            jar.blocked = true;
            held.shrink(1);
            if (world.isClientSide) {
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.key.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
            } else {
                jar.setChanged();
                jar.syncTile(false);
            }
        } else if (player.isShiftKeyDown() && jar.aspectFilter != null && side.ordinal() == jar.facing) {
            jar.aspectFilter = null;
            if (world.isClientSide) {
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.page.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
            } else {
                world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5F + side.getStepX() / 3.0F, pos.getY() + 0.5F,
                        pos.getZ() + 0.5F + side.getStepZ() / 3.0F, new ItemStack(ItemsTC.label.get())));
                jar.setChanged();
                jar.syncTile(false);
            }
        } else if (player.isShiftKeyDown() && held.isEmpty()) {
            if (jar.aspectFilter == null) {
                jar.aspect = null;
            }
            if (world.isClientSide) {
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.jar.get(), SoundSource.BLOCKS, 0.4F, 1.0F, false);
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.5F,
                        1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.3F, false);
            } else {
                AuraHelper.polluteAura(world, pos, jar.amount, true);
            }
            jar.amount = 0;
            jar.setChanged();
            if (!world.isClientSide) {
                jar.syncTile(false);
            }
        }
    }

    @Override
    public boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack labelstack) {
        Level world = player.level();
        if (world.getBlockEntity(pos) instanceof TileJarFillable jar && jar.aspectFilter == null) {
            var labelAspects = ((IEssentiaContainerItem) labelstack.getItem()).getAspects(labelstack);
            if (jar.amount == 0 && labelAspects == null) {
                return false;
            }
            if (jar.amount == 0 && labelAspects != null) {
                jar.aspect = labelAspects.getAspects()[0];
            }
            this.face(world, pos, player);
            jar.aspectFilter = jar.aspect;
            BlockState st = world.getBlockState(pos);
            world.sendBlockUpdated(pos, st, st, 3);
            jar.setChanged();
            jar.syncTile(false);
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.jar.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
            return true;
        }
        return false;
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof TileJarBrain ? 5.0F : 0.0F;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        if (world.getBlockEntity(pos) instanceof TileJarBrain tile && tile.xp >= tile.xpMax) {
            IFXDispatcher.get().spark(pos.getX() + 0.5F, pos.getY() + 0.8F, pos.getZ() + 0.5F, 3.0F, 0.2F + rand.nextFloat() * 0.2F, 1.0F,
                    0.3F + rand.nextFloat() * 0.2F, 0.5F);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileJarBrain b) {
            float r = (float) b.xp / b.xpMax;
            return Mth.floor(r * 14.0F) + (b.xp > 0 ? 1 : 0);
        } else if (tile instanceof TileJarFillable f) {
            float r = f.amount / 250.0F;
            return Mth.floor(r * 14.0F) + (f.amount > 0 ? 1 : 0);
        }
        return 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** Аспект с ярлыка банки-предмета (для подсказки). */
    public static Aspect filterOf(ItemStack stack) {
        String f = stack.get(thaumcraft.common.lib.TCDataComponents.ASPECT_FILTER.get());
        return f == null ? null : Aspect.getAspect(f);
    }
}
