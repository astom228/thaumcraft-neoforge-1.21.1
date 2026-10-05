package thaumcraft.common.items.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Таумометр (порт ItemThaumometer): правый клик — сканирование существа или блока (или неба), в руке — аура чанка на HUD
 * раз в секунду и подсветка ещё не изученного (на клиенте раз в 5 тиков).
 */
public class ItemThaumometer extends Item {
    public ItemThaumometer(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player p, InteractionHand hand) {
        if (world.isClientSide) {
            this.drawFX(world, p);
            world.playLocalSound(p.getX(), p.getY(), p.getZ(), SoundsTC.scan.get(), SoundSource.PLAYERS, 0.5F, 1.0F, false);
        } else {
            this.doScan(world, p);
        }
        return InteractionResultHolder.success(p.getItemInHand(hand));
    }

    /** onUpdate оригинала; как в нём, «в руке» — выбранный слот или слот 0 (в том числе левая рука). */
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int itemSlot, boolean isSelected) {
        boolean held = isSelected || itemSlot == 0;
        if (held && !world.isClientSide && entity.tickCount % 20 == 0 && entity instanceof ServerPlayer sp) {
            this.updateAura(world, sp);
        }
        if (held && world.isClientSide && entity.tickCount % 5 == 0 && entity instanceof Player player) {
            Entity target = EntityUtils.getPointedEntity(world, entity, 1.0, 16.0, 5.0F, true);
            if (target != null && ScanningManager.isThingStillScannable(player, target)) {
                IFXDispatcher.get().scanHighlight(target);
            }
            BlockHitResult mop = this.getRayTraceResultFromPlayerWild(world, player);
            if (mop.getType() == HitResult.Type.BLOCK && ScanningManager.isThingStillScannable(player, mop.getBlockPos())) {
                IFXDispatcher.get().scanHighlight(mop.getBlockPos());
            }
        }
    }

    /** Луч на 16 блоков с разбросом взгляда ±24° (подсветка «рыщет» вокруг прицела). */
    protected BlockHitResult getRayTraceResultFromPlayerWild(Level worldIn, Player playerIn) {
        float f = playerIn.xRotO + (playerIn.getXRot() - playerIn.xRotO) + worldIn.random.nextInt(25) - worldIn.random.nextInt(25);
        float f1 = playerIn.yRotO + (playerIn.getYRot() - playerIn.yRotO) + worldIn.random.nextInt(25) - worldIn.random.nextInt(25);
        double d0 = playerIn.xo + (playerIn.getX() - playerIn.xo);
        double d1 = playerIn.yo + (playerIn.getY() - playerIn.yo) + playerIn.getEyeHeight();
        double d2 = playerIn.zo + (playerIn.getZ() - playerIn.zo);
        Vec3 vec3 = new Vec3(d0, d1, d2);
        float f2 = Mth.cos(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f3 = Mth.sin(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = -Mth.cos(-f * (float) (Math.PI / 180.0));
        float f5 = Mth.sin(-f * (float) (Math.PI / 180.0));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d3 = 16.0;
        Vec3 vec31 = vec3.add(f6 * d3, f5 * d3, f7 * d3);
        return worldIn.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, playerIn));
    }

    /** Чанк — как в оригинале, по (int)x >> 4 (у отрицательных координат возле границы берётся соседний чанк). */
    private void updateAura(Level world, ServerPlayer player) {
        AuraChunk ac = AuraHandler.getAuraChunk(world, (int) player.getX() >> 4, (int) player.getZ() >> 4);
        if (ac != null) {
            if ((ac.getFlux() > ac.getVis() || ac.getFlux() > ac.getBase() / 3) && !ThaumcraftCapabilities.knowsResearch(player, "FLUX")) {
                ResearchManager.startResearchWithPopup(player, "FLUX");
                player.displayClientMessage(Component.translatable("research.FLUX.warn").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            PacketDistributor.sendToPlayer(player, new PacketAuraToClient(ac));
        }
    }

    private void drawFX(Level worldIn, Player playerIn) {
        Entity target = EntityUtils.getPointedEntity(worldIn, playerIn, 1.0, 9.0, 0.0F, true);
        if (target != null) {
            for (int a = 0; a < 10; a++) {
                IFXDispatcher.get().blockRunes(target.getX() - 0.5, target.getY() + target.getEyeHeight() / 2.0F, target.getZ() - 0.5,
                        0.3F + worldIn.random.nextFloat() * 0.7F, 0.0F, 0.3F + worldIn.random.nextFloat() * 0.7F,
                        (int) (target.getBbHeight() * 15.0F), 0.03F);
            }
        } else {
            BlockHitResult mop = getPlayerPOVHitResult(worldIn, playerIn, ClipContext.Fluid.ANY);
            if (mop.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = mop.getBlockPos();
                for (int a = 0; a < 10; a++) {
                    IFXDispatcher.get().blockRunes(pos.getX(), pos.getY() + 0.25, pos.getZ(),
                            0.3F + worldIn.random.nextFloat() * 0.7F, 0.0F, 0.3F + worldIn.random.nextFloat() * 0.7F, 15, 0.03F);
                }
            }
        }
    }

    public void doScan(Level worldIn, Player playerIn) {
        if (worldIn.isClientSide) {
            return;
        }
        Entity target = EntityUtils.getPointedEntity(worldIn, playerIn, 1.0, 9.0, 0.0F, true);
        if (target != null) {
            ScanningManager.scanTheThing(playerIn, target);
        } else {
            BlockHitResult mop = getPlayerPOVHitResult(worldIn, playerIn, ClipContext.Fluid.ANY);
            if (mop.getType() == HitResult.Type.BLOCK) {
                ScanningManager.scanTheThing(playerIn, mop.getBlockPos());
            } else {
                ScanningManager.scanTheThing(playerIn, null);
            }
        }
    }
}
