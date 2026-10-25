package thaumcraft.common.items.consumables;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemGenericEssentiaContainer;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Фиал (порт ItemPhial; metadata 0/1 → предметы phial / essentia_phial): пустой набирает 10 эссенции из банки или алембика,
 * полный выливает её в банку (этапы 10.2, 10.4). Полный фиал без аспектов превращается в пустой.
 */
public class ItemPhial extends ItemGenericEssentiaContainer {
    private final boolean filled;

    public ItemPhial(boolean filled, Properties properties) {
        super(10, properties);
        this.filled = filled;
    }

    public boolean isFilled() {
        return this.filled;
    }

    public static ItemStack makePhial(Aspect aspect, int amt) {
        ItemStack i = new ItemStack(ItemsTC.phialFilled.get());
        ((ItemPhial) i.getItem()).setAspects(i, new AspectList().add(aspect, amt));
        return i;
    }

    public static ItemStack makeFilledPhial(Aspect aspect) {
        return makePhial(aspect, 10);
    }

    /** getSubItems оригинала: пустой фиал и полный каждого аспекта — во вкладке; здесь — полные (пустой добавляется сам). */
    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        if (this.filled) {
            super.addCreativeVariants(output);
        } else {
            output.accept(this);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide && this.filled && !stack.has(TCDataComponents.ASPECTS.get()) && entity instanceof Player p
                && p.getInventory().getItem(slot) == stack) {
            p.getInventory().setItem(slot, new ItemStack(ItemsTC.phial.get(), stack.getCount()));
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
    }

    /** doesSneakBypassUse: с Shift фиал тоже работает с банкой, а не с блоком. */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, Player player) {
        return true;
    }

    /** Пустой фиал набирает 10 эссенции из алембика или банки, полный — выливает в банку. */
    @Override
    public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        net.minecraft.core.BlockPos pos = context.getClickedPos();
        net.minecraft.world.level.block.state.BlockState bi = world.getBlockState(pos);
        if (player == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (!this.filled && bi.is(thaumcraft.api.blocks.BlocksTC.alembic.get())
                && world.getBlockEntity(pos) instanceof thaumcraft.common.tiles.essentia.TileAlembic tile && tile.amount >= this.base) {
            if (world.isClientSide) {
                player.swing(context.getHand());
                return net.minecraft.world.InteractionResult.PASS;
            }
            ItemStack phial = new ItemStack(ItemsTC.phialFilled.get());
            this.setAspects(phial, new AspectList().add(tile.aspect, this.base));
            if (tile.takeFromContainer(tile.aspect, this.base)) {
                stack.shrink(1);
                if (!player.getInventory().add(phial)) {
                    world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(world, player.getX(), player.getY(), player.getZ(), phial));
                }
                player.playSound(net.minecraft.sounds.SoundEvents.BOTTLE_FILL, 0.25F, 1.0F);
                player.inventoryMenu.broadcastChanges();
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }
        boolean isJar = bi.is(thaumcraft.api.blocks.BlocksTC.jarNormal.get()) || bi.is(thaumcraft.api.blocks.BlocksTC.jarVoid.get());
        if (!this.filled && isJar && world.getBlockEntity(pos) instanceof thaumcraft.common.tiles.essentia.TileJarFillable tile) {
            if (tile.amount >= this.base) {
                if (world.isClientSide) {
                    player.swing(context.getHand());
                    return net.minecraft.world.InteractionResult.PASS;
                }
                Aspect asp = Aspect.getAspect(tile.aspect.getTag());
                if (tile.takeFromContainer(asp, this.base)) {
                    stack.shrink(1);
                    ItemStack phial = new ItemStack(ItemsTC.phialFilled.get());
                    this.setAspects(phial, new AspectList().add(asp, this.base));
                    if (!player.getInventory().add(phial)) {
                        world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(world, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, phial));
                    }
                    player.playSound(net.minecraft.sounds.SoundEvents.BOTTLE_FILL, 0.25F, 1.0F);
                    player.inventoryMenu.broadcastChanges();
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            }
        }
        AspectList al = this.getAspects(stack);
        if (al != null && al.size() == 1) {
            Aspect aspect = al.getAspects()[0];
            if (this.filled && isJar && world.getBlockEntity(pos) instanceof thaumcraft.common.tiles.essentia.TileJarFillable tile) {
                if (tile.amount <= 250 - this.base && tile.doesContainerAccept(aspect)) {
                    if (world.isClientSide) {
                        player.swing(context.getHand());
                        return net.minecraft.world.InteractionResult.PASS;
                    }
                    if (tile.addToContainer(aspect, this.base) == 0) {
                        world.sendBlockUpdated(pos, bi, bi, 3);
                        tile.setChanged();
                        stack.shrink(1);
                        ItemStack empty = new ItemStack(ItemsTC.phial.get());
                        if (!player.getInventory().add(empty)) {
                            world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(world, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, empty));
                        }
                        player.playSound(net.minecraft.sounds.SoundEvents.BOTTLE_FILL, 0.25F, 1.0F);
                        player.inventoryMenu.broadcastChanges();
                        return net.minecraft.world.InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
}
