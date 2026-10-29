package thaumcraft.common.lib.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.items.curios.ItemThaumonomicon;
import thaumcraft.common.items.resources.ItemCrystalEssence;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * События игрока (порт PlayerEvents оригинала; остальные обработчики добавляются на своих этапах).
 * attachCapabilitiesPlayer и cloneCapabilitiesEvent заменены Data Attachments с copyOnDeath (ThaumcraftAttachments).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class PlayerEvents {
    /** livingTick оригинала (часть для игрока на сервере): отложенная синхронизация знаний и периодические находки. */
    @SubscribeEvent
    public static void livingTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            thaumcraft.common.items.armor.ItemBootsTraveller.handleSpeedMods(event.getEntity());
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            handleRunicArmor(player);
            if (player.tickCount % 20 == 0 && ResearchManager.syncList.remove(player.getUUID())) {
                ThaumcraftCapabilities.getKnowledge(player).sync(player);
            }
            if (player.tickCount % 200 == 0) {
                ConfigResearch.checkPeriodicStuff(player);
            }
            // handleWarp оригинала: событие Искажения раз в 2000 тиков, смертоносный взгляд — раз в секунду
            if (!thaumcraft.common.config.ModConfig.CONFIG_MISC.wussMode.get() && player.tickCount > 0 && player.tickCount % 2000 == 0
                    && !player.hasEffect(thaumcraft.common.config.ConfigPotions.WARP_WARD)) {
                WarpEvents.checkWarpEvent(player);
            }
            if (player.tickCount % 20 == 0 && player.hasEffect(thaumcraft.common.config.ConfigPotions.DEATH_GAZE)) {
                WarpEvents.checkDeathGaze(player);
            }
        }
    }

    /**
     * onFallDamage оригинала (LivingHurtEvent 1.12 — до брони, как LivingIncomingDamageEvent): сапоги странника (с зарядом и без)
     * уменьшают урон от падения — f/2 − 1, меньше 1 — отмена; кольцо облаков — затем ещё f/3 − 2.
     * Здесь же — эффект удара по рунической защите (часть EntityEvents.entityHurt оригинала для игрока).
     */
    @SubscribeEvent
    public static void onFallDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.getAbsorptionAmount() >= 1.0F && hasRunicShield(sp)) {
            int target = -1;
            if (event.getSource().getEntity() != null) {
                target = event.getSource().getEntity().getId();
            }
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
                target = -2;
            }
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK)) {
                target = -3;
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(sp.serverLevel(), null, sp.getX(), sp.getY(), sp.getZ(), 32.0,
                    new thaumcraft.common.lib.network.fx.PacketFXShield(sp.getId(), target));
        }
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL) && event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(thaumcraft.api.items.ItemsTC.travellerBoots.get())) {
                float f = Math.max(0.0F, event.getAmount() / 2.0F - 1.0F);
                if (f < 1.0F) {
                    event.setCanceled(true);
                    return;
                }
                event.setAmount(f);
            }
            if (thaumcraft.common.lib.utils.BaubleUtils.isBaubleEquipped(player, thaumcraft.api.items.ItemsTC.ringCloud.get()) >= 0) {
                float f = Math.max(0.0F, event.getAmount() / 3.0F - 2.0F);
                if (f < 1.0F) {
                    event.setCanceled(true);
                    return;
                }
                event.setAmount(f);
            }
        }
    }

    /** Повязка любопытства (pickupXP оригинала): половина опыта сферы (больше 1) — шанс знаний: 5%·d теории, иначе 20%·d наблюдения. */
    @SubscribeEvent
    public static void pickupXP(net.neoforged.neoforge.event.entity.player.PlayerXpEvent.PickupXp event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide || event.getOrb().value <= 1
                || thaumcraft.common.lib.utils.BaubleUtils.isBaubleEquipped(player, thaumcraft.api.items.ItemsTC.bandCuriosity.get()) < 0) {
            return;
        }
        int d = event.getOrb().value / 2;
        event.getOrb().value -= d;
        float r = player.getRandom().nextFloat();
        IPlayerKnowledge.EnumKnowledgeType type = r < 0.05 * d ? IPlayerKnowledge.EnumKnowledgeType.THEORY
                : (r < 0.2 * d ? IPlayerKnowledge.EnumKnowledgeType.OBSERVATION : null);
        if (type != null) {
            String[] cats = thaumcraft.api.research.ResearchCategories.researchCategories.keySet().toArray(new String[0]);
            String cat = cats[player.getRandom().nextInt(cats.length)];
            thaumcraft.api.ThaumcraftApi.internalMethods.addKnowledge(player, type, thaumcraft.api.research.ResearchCategories.getResearchCategory(cat), 1);
        }
    }

    /** Оберег бессмертия (onDeath оригинала): как тотем — исчезает, здоровье 1, снимает эффекты, регенерация II 45 с, поглощение II 5 с. */
    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int slot = thaumcraft.common.lib.utils.BaubleUtils.isBaubleEquipped(player, thaumcraft.api.items.ItemsTC.charmUndying.get());
        if (slot < 0) {
            return;
        }
        net.neoforged.neoforge.items.IItemHandler baubles = thaumcraft.common.lib.utils.BaubleUtils.getBaubles(player);
        ItemStack charm = baubles.getStackInSlot(slot).copy();
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        net.minecraft.advancements.CriteriaTriggers.USED_TOTEM.trigger(player, charm);
        baubles.extractItem(slot, 1, false);
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 900, 1));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 1));
        player.level().broadcastEntityEvent(player, (byte) 35);
        event.setCanceled(true);
    }

    /** Сапоги странника с зарядом — прыжок выше (playerJumps оригинала). */
    /**
     * finishedUsingItem оригинала: при неестественном голоде гнилая плоть и мозг зомби ослабляют его (уровень −1, −30 с),
     * обычная еда — только сообщение.
     */
    @SubscribeEvent
    public static void finishedUsingItem(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        var e = event.getEntity();
        if (e.level().isClientSide || !e.hasEffect(thaumcraft.common.config.ConfigPotions.UNNATURAL_HUNGER)) {
            return;
        }
        net.minecraft.world.item.ItemStack item = event.getItem();
        if (item.is(net.minecraft.world.item.Items.ROTTEN_FLESH) || item.is(thaumcraft.api.items.ItemsTC.brain.get())) {
            net.minecraft.world.effect.MobEffectInstance pe = e.getEffect(thaumcraft.common.config.ConfigPotions.UNNATURAL_HUNGER);
            int amp = pe.getAmplifier() - 1;
            int duration = pe.getDuration() - 600;
            e.removeEffect(thaumcraft.common.config.ConfigPotions.UNNATURAL_HUNGER);
            if (duration > 0 && amp >= 0) {
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(thaumcraft.common.config.ConfigPotions.UNNATURAL_HUNGER, duration, amp, true, false));
            }
            e.sendSystemMessage(net.minecraft.network.chat.Component.translatable("warp.text.hunger.2")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN, net.minecraft.ChatFormatting.ITALIC));
        } else if (item.has(net.minecraft.core.component.DataComponents.FOOD)) {
            e.sendSystemMessage(net.minecraft.network.chat.Component.translatable("warp.text.hunger.1")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC));
        }
    }

    @SubscribeEvent
    public static void playerJumps(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            ItemStack is = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
            if (is.is(thaumcraft.api.items.ItemsTC.travellerBoots.get()) && thaumcraft.api.items.RechargeHelper.getCharge(is) > 0) {
                player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.275F, 0.0));
            }
        }
    }

    @SubscribeEvent
    public static void pickupItem(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemEntity().getItem();
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (stack.getItem() instanceof ItemCrystalEssence && !knowledge.isResearchKnown("!gotcrystals")) {
                knowledge.addResearch("!gotcrystals");
                knowledge.sync(player);
                player.sendSystemMessage(Component.translatable("got.crystals").withStyle(ChatFormatting.DARK_PURPLE));
            }
            if (stack.getItem() instanceof ItemThaumonomicon && !knowledge.isResearchKnown("!gotthaumonomicon")) {
                knowledge.addResearch("!gotthaumonomicon");
                knowledge.sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void wakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge.isResearchKnown("!gotcrystals") && !knowledge.isResearchKnown("!gotdream")) {
                knowledge.addResearch("!gotdream");
                knowledge.sync(player);
                ItemStack book = ConfigItems.startBook(player.getName().getString());
                if (!player.getInventory().add(book)) {
                    // InventoryUtils.dropItemAtEntity
                    player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + player.getEyeHeight() / 2.0F, player.getZ(), book));
                }
                player.sendSystemMessage(Component.translatable("got.dream").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    /** playerJoin: при появлении игрока в мире (вход, возрождение, смена измерения) — синхронизация знаний и Искажения. */
    @SubscribeEvent
    public static void playerJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            if (ThaumcraftCapabilities.getKnowledge(player) instanceof PlayerKnowledge pk) {
                pk.markFullSync();
            }
            ThaumcraftCapabilities.getKnowledge(player).sync(player);
            ThaumcraftCapabilities.getWarp(player).sync(player);
        }
    }

    private static final net.minecraft.resources.ResourceLocation RUNIC_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "runic_shielding");

    /**
     * Руническая защита (handleRunicArmor оригинала): раз в секунду — сумма зарядов брони и безделушек; щит — поглощение урона,
     * восстанавливается по 1 за shieldCost Вис ауры раз в shieldRecharge мс, после обнуления — пауза shieldWait мс.
     * В 1.21 поглощение ограничено атрибутом max_absorption — щит добавляет к нему временный модификатор на сумму зарядов.
     * Время — игровые тики (мс / 50), а не системные часы (DECISIONS 103).
     */
    private static void handleRunicArmor(ServerPlayer player) {
        thaumcraft.common.lib.capabilities.RunicShield rs = player.getData(thaumcraft.common.lib.capabilities.ThaumcraftAttachments.RUNIC);
        if (player.tickCount % 20 == 0) {
            int max = 0;
            for (ItemStack s : player.getInventory().armor) {
                max += getRunicCharge(s);
            }
            net.neoforged.neoforge.items.IItemHandler baubles = thaumcraft.common.lib.utils.BaubleUtils.getBaubles(player);
            if (baubles != null) {
                for (int a = 0; a < baubles.getSlots(); a++) {
                    max += getRunicCharge(baubles.getStackInSlot(a));
                }
            }
            if (rs.lastMaxCharge >= 0) {
                int charge = rs.lastMaxCharge;
                if (charge > max) {
                    player.setAbsorptionAmount(player.getAbsorptionAmount() - (charge - max));
                }
                if (max <= 0) {
                    rs.lastMaxCharge = -1;
                }
            }
            net.minecraft.world.entity.ai.attributes.AttributeInstance cap = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION);
            if (cap != null) {
                net.minecraft.world.entity.ai.attributes.AttributeModifier cur = cap.getModifier(RUNIC_ID);
                if (max <= 0) {
                    if (cur != null) {
                        cap.removeModifier(RUNIC_ID);
                    }
                } else if (cur == null || cur.amount() != max) {
                    cap.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RUNIC_ID, max,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                }
                if (player.getAbsorptionAmount() > player.getMaxAbsorption()) {
                    player.setAbsorptionAmount(player.getMaxAbsorption());
                }
            }
            if (max > 0) {
                rs.max = max;
                rs.lastMaxCharge = max;
            } else {
                rs.max = 0;
            }
        }
        if (rs.max > 0) {
            long time = player.level().getGameTime();
            int charge = (int) player.getAbsorptionAmount();
            if (charge == 0 && rs.lastCharge > 0) {
                rs.nextCycle = time + thaumcraft.common.config.ModConfig.CONFIG_MISC.shieldWait.get() / 50;
                rs.lastCharge = 0;
            }
            net.minecraft.core.BlockPos pos = player.blockPosition();
            int cost = thaumcraft.common.config.ModConfig.CONFIG_MISC.shieldCost.get();
            if (charge < rs.max && rs.nextCycle < time && !thaumcraft.api.aura.AuraHelper.shouldPreserveAura(player.level(), player, pos)
                    && thaumcraft.api.aura.AuraHelper.getVis(player.level(), pos) >= cost) {
                thaumcraft.api.aura.AuraHelper.drainVis(player.level(), pos, cost, false);
                rs.nextCycle = time + thaumcraft.common.config.ModConfig.CONFIG_MISC.shieldRecharge.get() / 50;
                player.setAbsorptionAmount(charge + 1);
                rs.lastCharge = charge + 1;
            }
        }
    }

    /** Есть ли у игрока действующая руническая защита (runicInfo и lastMaxCharge оригинала). */
    public static boolean hasRunicShield(net.minecraft.world.entity.player.Player player) {
        thaumcraft.common.lib.capabilities.RunicShield rs = player.getData(thaumcraft.common.lib.capabilities.ThaumcraftAttachments.RUNIC);
        return rs.max > 0 && rs.lastMaxCharge >= 0;
    }

    /** Заряд рунической защиты предмета (TC.RUNIC оригинала). */
    public static int getRunicCharge(net.minecraft.world.item.ItemStack stack) {
        return stack.getOrDefault(thaumcraft.common.lib.TCDataComponents.RUNIC_CHARGE.get(), 0);
    }

    /** Искажение от предмета: IWarpingGear + TC.WARP оригинала. */
    public static int getFinalWarp(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int warp = 0;
        if (stack.getItem() instanceof thaumcraft.api.items.IWarpingGear armor) {
            warp += armor.getWarp(stack, player);
        }
        return warp + stack.getOrDefault(thaumcraft.common.lib.TCDataComponents.WARPING.get(), 0);
    }

    /** Скидка Вис от предмета, %. */
    public static int getFinalDiscount(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof thaumcraft.api.items.IVisDiscountGear gear ? gear.getVisDiscount(stack, player) : 0;
    }
}
