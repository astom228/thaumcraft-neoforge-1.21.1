package thaumcraft.common.lib.events;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.network.fx.PacketFXSlash;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.lib.utils.Utils;

/**
 * Порт ToolEvents: блоки, которые нельзя ломать, пока они ждут превращения (многоблочные структуры от соли мира), и действие
 * наполняющих зачарований (Дуга, Зондирование, Проходчик, Очистка, Разрушитель, Собиратель, Сборщик эссенции).
 * Всё — только на сервере; данные (блокировки, последняя грань удара) очищаются при остановке.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ToolEvents {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> blockedBlocks = new ConcurrentHashMap<>();
    /** Грань, по которой игрок последний раз ударил блок (для «Разрушителя»). */
    private static final Map<Integer, net.minecraft.core.Direction> lastFaceClicked = new ConcurrentHashMap<>();
    /** Защита от рекурсии «Разрушителя» (соседние блоки добываются с тем же событием выпадения). Только серверный поток. */
    private static boolean blockDestructiveRecursion = false;

    public static void addBlockedBlock(Level world, BlockPos pos) {
        blockedBlocks.computeIfAbsent(world.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    public static void clearBlockedBlock(Level world, BlockPos pos) {
        Set<BlockPos> list = blockedBlocks.get(world.dimension());
        if (list != null) {
            list.remove(pos);
        }
    }

    @SubscribeEvent
    public static void breakBlockEvent(BlockEvent.BreakEvent event) {
        LevelAccessor la = event.getLevel();
        if (la instanceof Level world) {
            Set<BlockPos> list = blockedBlocks.get(world.dimension());
            if (list != null && list.contains(event.getPos())) {
                event.setCanceled(true);
            }
        }
        // «Проходчик»: подходящим инструментом по бревну или руде вместо этого блока добывается самый дальний такой же
        if (la instanceof Level world && !world.isClientSide && event.getPlayer() != null && !event.isCanceled()) {
            net.minecraft.world.entity.player.Player player = event.getPlayer();
            net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty() && heldItem.isCorrectToolForDrops(event.getState())
                    && EnumInfusionEnchantment.getInfusionEnchantments(heldItem).contains(EnumInfusionEnchantment.BURROWING) && !player.isShiftKeyDown()
                    && (Utils.isWoodLog(world, event.getPos()) || Utils.isOreBlock(world, event.getPos()))) {
                event.setCanceled(true);
                heldItem.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                BlockUtils.breakFurthestBlock(world, event.getPos(), event.getState(), player);
            }
        }
    }


    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        blockedBlocks.clear();
        lastFaceClicked.clear();
    }

    /** «Дуга»: удар задевает до (уровень) ближайших противников вокруг цели на половину урона, отбрасывая их. */
    @SubscribeEvent
    public static void playerAttack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            return;
        }
        net.minecraft.world.entity.Entity target = event.getTarget();
        if (!EnumInfusionEnchantment.getInfusionEnchantments(heldItem).contains(EnumInfusionEnchantment.ARCING) || !target.isAlive()) {
            return;
        }
        int rank = EnumInfusionEnchantment.getInfusionEnchantmentLevel(heldItem, EnumInfusionEnchantment.ARCING);
        java.util.List<net.minecraft.world.entity.Entity> targets = player.level().getEntities(player,
                target.getBoundingBox().inflate(1.5 + rank, 1.0F + rank / 2.0F, 1.5 + rank));
        int count = 0;
        if (targets.size() > 1) {
            for (net.minecraft.world.entity.Entity e : targets) {
                if (!e.isRemoved() && !EntityUtils.isFriendly(player, e)) {
                    if (e instanceof net.minecraft.world.entity.Mob && e.getId() != target.getId()
                            && !(e instanceof net.minecraft.world.entity.player.Player p && p.getName().equals(player.getName())) && e.isAlive()) {
                        float f = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                            player.setLastHurtMob(le);
                        }
                        if (e.hurt(player.damageSources().playerAttack(player), f * 0.5F)) {
                            if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(sl, e, player.damageSources().playerAttack(player));
                            }
                            e.push(-net.minecraft.util.Mth.sin(player.getYRot() * (float) Math.PI / 180.0F) * 0.5F, 0.1,
                                    net.minecraft.util.Mth.cos(player.getYRot() * (float) Math.PI / 180.0F) * 0.5F);
                            count++;
                            sendNear(player.level(), target.getX(), target.getY(), target.getZ(), new PacketFXSlash(target.getId(), e.getId()));
                        }
                    }
                    if (count >= rank) {
                        break;
                    }
                }
            }
            if (count > 0) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundsTC.wind.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F,
                        0.9F + player.level().random.nextFloat() * 0.2F);
                sendNear(player.level(), player.getX(), player.getY(), player.getZ(), new PacketFXSlash(player.getId(), target.getId()));
            }
        }
    }

    private static void sendNear(Level level, double x, double y, double z, net.minecraft.network.protocol.common.custom.CustomPacketPayload msg) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(sl, null, x, y, z, 64.0, msg);
        }
    }

    /** «Зондирование»: ПКМ по блоку (без Shift) — износ 5 и подсветка руд вокруг (только у этого игрока). */
    @SubscribeEvent
    public static void playerRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        net.minecraft.world.item.ItemStack heldItem = event.getItemStack();
        if (!heldItem.isEmpty() && EnumInfusionEnchantment.getInfusionEnchantments(heldItem).contains(EnumInfusionEnchantment.SOUNDING)
                && !player.isShiftKeyDown()) {
            int lvl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(heldItem, EnumInfusionEnchantment.SOUNDING);
            heldItem.hurtAndBreak(5, player, event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                    : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            BlockPos p = event.getPos();
            event.getLevel().playSound(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, SoundsTC.wandfail.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.2F,
                    0.2F + event.getLevel().random.nextFloat() * 0.2F);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new thaumcraft.common.lib.network.fx.PacketFXScanSource(p.asLong(), (byte) lvl));
        }
    }

    /** Грань удара по блоку (для «Разрушителя»). */
    @SubscribeEvent
    public static void playerInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide && event.getFace() != null) {
            lastFaceClicked.put(event.getEntity().getId(), event.getFace());
        }
    }

    /** EnumFacing.getDirectionFromEntityLiving 1.12: близко по горизонтали — вверх/вниз по высоте глаз, иначе против взгляда. */
    private static net.minecraft.core.Direction directionFromEntity(BlockPos pos, net.minecraft.world.entity.LivingEntity e) {
        if (Math.abs(e.getX() - (pos.getX() + 0.5F)) < 2.0 && Math.abs(e.getZ() - (pos.getZ() + 0.5F)) < 2.0) {
            double d0 = e.getY() + e.getEyeHeight();
            if (d0 - pos.getY() > 2.0) {
                return net.minecraft.core.Direction.UP;
            }
            if (pos.getY() - d0 > 0.0) {
                return net.minecraft.core.Direction.DOWN;
            }
        }
        return e.getDirection().getOpposite();
    }

    /**
     * Добыча блока (HarvestDropsEvent оригинала → BlockDropsEvent): «Очистка» — руда в самородный кластер с шансом
     * 0.125·(1 + уровень); «Разрушитель» — ещё 8 блоков вокруг в плоскости грани удара; «Собиратель» — добыча летит к игроку.
     */
    @SubscribeEvent
    public static void harvestBlockEvent(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        Level world = event.getLevel();
        if (BlockUtils.isHarvestingWithoutExperience()) {
            event.setDroppedExperience(0);
        }
        if (world.isClientSide || !(event.getBreaker() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            return;
        }
        java.util.List<EnumInfusionEnchantment> list = EnumInfusionEnchantment.getInfusionEnchantments(heldItem);
        if (list.isEmpty()) {
            return;
        }
        net.minecraft.world.level.block.state.BlockState state = event.getState();
        BlockPos pos = event.getPos();
        boolean silk = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), heldItem) > 0;
        if (!(silk || heldItem.isCorrectToolForDrops(state) || heldItem.getDestroySpeed(state) > 1.0F)) {
            return;
        }
        if (list.contains(EnumInfusionEnchantment.REFINING)) {
            int fortune = 1 + EnumInfusionEnchantment.getInfusionEnchantmentLevel(heldItem, EnumInfusionEnchantment.REFINING);
            float chance = fortune * 0.125F;
            boolean b = false;
            for (net.minecraft.world.entity.item.ItemEntity ie : event.getDrops()) {
                net.minecraft.world.item.ItemStack is = ie.getItem();
                net.minecraft.world.item.ItemStack smr = Utils.findSpecialMiningResult(is, chance, world.random);
                if (!net.minecraft.world.item.ItemStack.isSameItem(is, smr)) {
                    ie.setItem(smr);
                    b = true;
                }
            }
            if (b) {
                world.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.2F,
                        0.7F + world.random.nextFloat() * 0.2F);
            }
        }
        if (!blockDestructiveRecursion && list.contains(EnumInfusionEnchantment.DESTRUCTIVE) && !player.isShiftKeyDown()) {
            blockDestructiveRecursion = true;
            try {
                net.minecraft.core.Direction face = lastFaceClicked.get(player.getId());
                if (face == null) {
                    face = directionFromEntity(pos, player);
                }
                for (int aa = -1; aa <= 1; aa++) {
                    for (int bb = -1; bb <= 1; bb++) {
                        if (aa != 0 || bb != 0) {
                            int xx = 0;
                            int yy = 0;
                            int zz = 0;
                            if (face.ordinal() <= 1) {
                                xx = aa;
                                zz = bb;
                            } else if (face.ordinal() <= 3) {
                                xx = aa;
                                yy = bb;
                            } else {
                                zz = aa;
                                yy = bb;
                            }
                            BlockPos p2 = pos.offset(xx, yy, zz);
                            if (!world.isLoaded(p2)) {
                                continue;
                            }
                            net.minecraft.world.level.block.state.BlockState bl = world.getBlockState(p2);
                            if (bl.getDestroySpeed(world, p2) >= 0.0F && (heldItem.isCorrectToolForDrops(bl) || heldItem.getDestroySpeed(bl) > 1.0F)) {
                                heldItem.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                                BlockUtils.harvestBlock(world, player, p2);
                            }
                        }
                    }
                }
            } finally {
                blockDestructiveRecursion = false;
            }
        }
        if (list.contains(EnumInfusionEnchantment.COLLECTOR) && !player.isShiftKeyDown()) {
            java.util.List<net.minecraft.world.item.ItemStack> stacks = new java.util.ArrayList<>();
            for (net.minecraft.world.entity.item.ItemEntity ie : event.getDrops()) {
                stacks.add(ie.getItem());
            }
            InventoryUtils.dropHarvestsAtPos(world, pos, stacks, true, 10, player);
            event.getDrops().clear();
        }
    }

    /** Добыча с существ: «Собиратель» — к игроку; «Сборщик эссенции» — вис-кристаллы из аспектов существа. */
    @SubscribeEvent
    public static void livingDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player) || player.level().isClientSide) {
            return;
        }
        net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            return;
        }
        java.util.List<EnumInfusionEnchantment> list = EnumInfusionEnchantment.getInfusionEnchantments(heldItem);
        net.minecraft.world.entity.LivingEntity victim = event.getEntity();
        Level world = victim.level();
        if (list.contains(EnumInfusionEnchantment.COLLECTOR)) {
            java.util.List<net.minecraft.world.entity.item.ItemEntity> drops = new java.util.ArrayList<>(event.getDrops());
            event.getDrops().clear();
            for (net.minecraft.world.entity.item.ItemEntity ei : drops) {
                thaumcraft.common.entities.EntityFollowingItem nei = new thaumcraft.common.entities.EntityFollowingItem(world, ei.getX(), ei.getY(), ei.getZ(),
                        ei.getItem().copy(), player, 10);
                nei.setDeltaMovement(ei.getDeltaMovement());
                nei.setDefaultPickUpDelay();
                ei.discard();
                event.getDrops().add(nei);
            }
        }
        if (list.contains(EnumInfusionEnchantment.ESSENCE)) {
            thaumcraft.api.aspects.AspectList as = thaumcraft.api.aspects.AspectHelper.getEntityAspects(victim);
            if (as != null && as.size() > 0) {
                thaumcraft.api.aspects.AspectList aspects = as.copy();
                int q = EnumInfusionEnchantment.getInfusionEnchantmentLevel(heldItem, EnumInfusionEnchantment.ESSENCE);
                thaumcraft.api.aspects.Aspect[] al = aspects.getAspects();
                int b = world.random.nextInt(5) < q ? 0 : 99;
                while (b < q && al != null && al.length > 0) {
                    thaumcraft.api.aspects.Aspect aspect = al[world.random.nextInt(al.length)];
                    if (aspects.getAmount(aspect) > 0) {
                        aspects.remove(aspect, 1);
                        net.minecraft.world.item.ItemStack stack = thaumcraft.api.ThaumcraftApiHelper.makeCrystal(aspect);
                        double ey = victim.getY() + victim.getEyeHeight();
                        if (list.contains(EnumInfusionEnchantment.COLLECTOR)) {
                            event.getDrops().add(new thaumcraft.common.entities.EntityFollowingItem(world, victim.getX(), ey, victim.getZ(), stack, player, 10));
                        } else {
                            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(world, victim.getX(), ey, victim.getZ(), stack));
                        }
                        b++;
                    }
                    al = aspects.getAspects();
                    if (world.random.nextInt(q) == 0) {
                        b += 1 + world.random.nextInt(2);
                    }
                }
            }
        }
    }

}
