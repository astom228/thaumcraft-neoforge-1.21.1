package thaumcraft.common.lib.events;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.lib.network.misc.PacketMiscEvent;
import thaumcraft.common.lib.utils.EntityUtils;
import top.theillusivec4.curios.api.CuriosApi;

/** События Искажения (порт WarpEvents). Сервер. */
public class WarpEvents {
    /**
     * Раз в 2000 тиков жизни игрока (без «Защиты от Искажения», не в «режиме слабака»): временное Искажение −1; со счётчиком
     * Искажения > 0 и шансом √счётчика % — случайное событие по силе eff (0…Искажение + Искажение снаряжения; маска «Злой призрак»
     * ослабляет): звуки, сообщения, эффекты, туман, пауки, портал. Затем — открытие «!BATHSALTS», «ELDRITCHMINOR/MAJOR».
     */
    public static void checkWarpEvent(ServerPlayer player) {
        IPlayerWarp wc = ThaumcraftCapabilities.getWarp(player);
        ThaumcraftApi.internalMethods.addWarpToPlayer(player, -1, IPlayerWarp.EnumWarpType.TEMPORARY);
        int tw = wc.get(IPlayerWarp.EnumWarpType.TEMPORARY);
        int nw = wc.get(IPlayerWarp.EnumWarpType.NORMAL);
        int pw = wc.get(IPlayerWarp.EnumWarpType.PERMANENT);
        int warp = tw + nw + pw;
        int actualwarp = pw + nw;
        int gearWarp = getWarpFromGear(player);
        warp += gearWarp;
        int warpCounter = wc.getCounter();
        var rand = player.getRandom();
        int r = rand.nextInt(100);
        if (warpCounter > 0 && warp > 0 && r <= Math.sqrt(warpCounter)) {
            warp = Math.min(100, (warp + warp + warpCounter) / 3);
            warpCounter = (int) (warpCounter - Math.max(5.0, Math.sqrt(warpCounter) * 2.0 - gearWarp * 2));
            wc.setCounter(warpCounter);
            int eff = rand.nextInt(warp) + gearWarp;
            ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helm.getItem() instanceof ItemFortressArmor && ItemFortressArmor.getMask(helm) == 0) {
                eff -= 2 + rand.nextInt(4);
            }
            PacketDistributor.sendToPlayer(player, new PacketMiscEvent(PacketMiscEvent.WARP_EVENT));
            if (eff > 0) {
                int amp = Math.min(3, warp / 15);
                if (eff <= 4) {
                    if (!ModConfig.CONFIG_GRAPHICS.nostress.get()) {
                        // в 1.12 world.playSound(player, …) слышали все, кроме самого игрока; здесь — только он (DEVIATIONS 49)
                        playTo(player, SoundEvents.CREEPER_PRIMED, player.getX(), player.getY(), player.getZ(), 1.0F, 0.5F);
                    }
                } else if (eff <= 8) {
                    if (!ModConfig.CONFIG_GRAPHICS.nostress.get()) {
                        playTo(player, SoundEvents.GENERIC_EXPLODE.value(), player.getX() + (rand.nextFloat() - rand.nextFloat()) * 10.0F,
                                player.getY() + (rand.nextFloat() - rand.nextFloat()) * 10.0F, player.getZ() + (rand.nextFloat() - rand.nextFloat()) * 10.0F,
                                4.0F, (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
                    }
                } else if (eff <= 12) {
                    message(player, "warp.text.11");
                } else if (eff <= 16) {
                    player.addEffect(incurable(ConfigPotions.VIS_EXHAUST, 5000, amp));
                    message(player, "warp.text.1");
                } else if (eff <= 20) {
                    player.addEffect(incurable(ConfigPotions.THAUMARHIA, Math.min(32000, 10 * warp), 0));
                    message(player, "warp.text.15");
                } else if (eff <= 24) {
                    player.addEffect(incurable(ConfigPotions.UNNATURAL_HUNGER, 5000, amp));
                    message(player, "warp.text.2");
                } else if (eff <= 28) {
                    message(player, "warp.text.12");
                } else if (eff <= 32) {
                    spawnMist(player, warp, 1);
                } else if (eff <= 36) {
                    player.addEffect(new MobEffectInstance(ConfigPotions.BLURRED_VISION, Math.min(32000, 10 * warp), 0, true, true));
                } else if (eff <= 40) {
                    player.addEffect(incurable(ConfigPotions.SUN_SCORNED, 5000, amp));
                    message(player, "warp.text.5");
                } else if (eff <= 44) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, amp, true, true));
                    message(player, "warp.text.9");
                } else if (eff <= 48) {
                    MobEffectInstance pe = new MobEffectInstance(ConfigPotions.INFECTIOUS_VIS_EXHAUST, 6000, amp);
                    pe.getCures().clear();
                    player.addEffect(pe);
                    message(player, "warp.text.1");
                } else if (eff <= 52) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Math.min(40 * warp, 6000), 0, true, true));
                    message(player, "warp.text.10");
                } else if (eff <= 56) {
                    player.addEffect(incurable(ConfigPotions.DEATH_GAZE, 6000, amp));
                    message(player, "warp.text.4");
                } else if (eff <= 60) {
                    suddenlySpiders(player, warp, false);
                } else if (eff <= 64) {
                    message(player, "warp.text.13");
                } else if (eff <= 68) {
                    spawnMist(player, warp, warp / 30);
                } else if (eff <= 72) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(32000, 5 * warp), 0, true, true));
                } else if (eff == 76) {
                    if (nw > 0) {
                        ThaumcraftApi.internalMethods.addWarpToPlayer(player, -1, IPlayerWarp.EnumWarpType.NORMAL);
                    }
                    message(player, "warp.text.14");
                } else if (eff <= 80) {
                    player.addEffect(incurable(ConfigPotions.UNNATURAL_HUNGER, 6000, amp));
                    message(player, "warp.text.2");
                } else if (eff <= 88) {
                    spawnPortal(player);
                } else if (eff <= 92) {
                    suddenlySpiders(player, warp, true);
                } else {
                    spawnMist(player, warp, warp / 15);
                }
            }
            if (actualwarp > 10 && !ThaumcraftCapabilities.knowsResearch(player, "BATHSALTS") && !ThaumcraftCapabilities.knowsResearch(player, "!BATHSALTS")) {
                message(player, "warp.text.8");
                ThaumcraftApi.internalMethods.completeResearch(player, "!BATHSALTS");
            }
            if (actualwarp > 25 && !ThaumcraftCapabilities.knowsResearch(player, "ELDRITCHMINOR")) {
                ThaumcraftApi.internalMethods.completeResearch(player, "ELDRITCHMINOR");
            }
            if (actualwarp > 50 && !ThaumcraftCapabilities.knowsResearch(player, "ELDRITCHMAJOR")) {
                ThaumcraftApi.internalMethods.completeResearch(player, "ELDRITCHMAJOR");
            }
        }
    }

    /** Эффект, который не снимается молоком (getCurativeItems().clear() оригинала; гнилую плоть для голода — см. PlayerEvents). */
    private static MobEffectInstance incurable(Holder<MobEffect> effect, int duration, int amp) {
        MobEffectInstance pe = new MobEffectInstance(effect, duration, amp, true, true);
        pe.getCures().clear();
        return pe;
    }

    /** Звук, который слышит только этот игрок. */
    private static void playTo(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, double x, double y, double z, float vol, float pitch) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(Holder.direct(sound), SoundSource.AMBIENT, x, y, z, vol, pitch,
                player.getRandom().nextLong()));
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
    }

    private static void spawnMist(ServerPlayer player, int warp, int guardian) {
        PacketDistributor.sendToPlayer(player, new PacketMiscEvent(PacketMiscEvent.MIST_EVENT));
        if (guardian > 0) {
            guardian = Math.min(8, guardian);
            for (int a = 0; a < guardian; a++) {
                spawnGuardian(player);
            }
        }
        message(player, "warp.text.6");
    }

    /** Потусторонний страж в 7–24 блоках на твёрдой опоре (50 попыток), цель — игрок. Незагруженные чанки пропускаются. */
    private static void spawnGuardian(ServerPlayer player) {
        var level = player.serverLevel();
        var eg = thaumcraft.common.config.ConfigEntities.ELDRITCH_GUARDIAN.get().create(level);
        if (eg == null) {
            return;
        }
        var rand = player.getRandom();
        int i = net.minecraft.util.Mth.floor(player.getX());
        int j = net.minecraft.util.Mth.floor(player.getY());
        int k = net.minecraft.util.Mth.floor(player.getZ());
        for (int l = 0; l < 50; l++) {
            int x = i + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
            int y = j + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
            int z = k + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
            net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(x, y - 1, z);
            if (level.isLoaded(below) && level.getBlockState(below).isCollisionShapeFullBlock(level, below)) {
                eg.setPos(x, y, z);
                if (level.isUnobstructed(eg) && level.noCollision(eg) && !level.containsAnyLiquid(eg.getBoundingBox())) {
                    eg.setTarget(player);
                    level.addFreshEntity(eg);
                    break;
                }
            }
        }
    }

    /** Малый портал культистов в 7–24 блоках на непрозрачной опоре (50 попыток); незагруженные чанки пропускаются. */
    private static void spawnPortal(ServerPlayer player) {
        var level = player.serverLevel();
        var eg = thaumcraft.common.config.ConfigEntities.CULTIST_PORTAL_LESSER.get().create(level);
        if (eg == null) {
            return;
        }
        var rand = player.getRandom();
        int i = net.minecraft.util.Mth.floor(player.getX());
        int j = net.minecraft.util.Mth.floor(player.getY());
        int k = net.minecraft.util.Mth.floor(player.getZ());
        for (int l = 0; l < 50; l++) {
            int i1 = i + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
            int j1 = j + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
            int k1 = k + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
            net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(i1, j1 - 1, k1);
            eg.setPos(i1 + 0.5, j1 + 1.0, k1 + 0.5);
            if (level.isLoaded(below) && level.getBlockState(below).isSolidRender(level, below) && level.isUnobstructed(eg) && level.noCollision(eg)
                    && !level.containsAnyLiquid(eg.getBoundingBox())) {
                eg.finalizeSpawn(level, level.getCurrentDifficultyAt(eg.blockPosition()), net.minecraft.world.entity.MobSpawnType.EVENT, null);
                level.addFreshEntity(eg);
                message(player, "warp.text.16");
                break;
            }
        }
    }

    /** До min(50, warp) пауков разума в 7–24 блоках на твёрдой опоре; ненастоящие видны только этому игроку и безвредны. */
    private static void suddenlySpiders(ServerPlayer player, int warp, boolean real) {
        int spawns = Math.min(50, warp);
        var rand = player.getRandom();
        var level = player.serverLevel();
        for (int a = 0; a < spawns; a++) {
            var spider = thaumcraft.common.config.ConfigEntities.MIND_SPIDER.get().create(level);
            if (spider == null) {
                continue;
            }
            boolean success = false;
            for (int l = 0; l < 50; l++) {
                int x = net.minecraft.util.Mth.floor(player.getX()) + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
                int y = net.minecraft.util.Mth.floor(player.getY()) + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
                int z = net.minecraft.util.Mth.floor(player.getZ()) + net.minecraft.util.Mth.nextInt(rand, 7, 24) * net.minecraft.util.Mth.nextInt(rand, -1, 1);
                net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(x, y - 1, z);
                if (level.isLoaded(below) && level.getBlockState(below).isCollisionShapeFullBlock(level, below)) {
                    spider.setPos(x, y, z);
                    if (level.noCollision(spider) && level.isUnobstructed(spider) && !level.containsAnyLiquid(spider.getBoundingBox())) {
                        success = true;
                        break;
                    }
                }
            }
            if (success) {
                spider.setTarget(player);
                if (!real) {
                    spider.setViewer(player.getName().getString());
                    spider.setHarmless(true);
                }
                level.addFreshEntity(spider);
            }
        }
        message(player, "warp.text.7");
    }

    /**
     * Смертоносный взгляд: всех живых, на кого игрок смотрит (конус 0.75, дальность 8 + 3·уровень, не больше 24) и кого видит,
     * иссушает на 4 с и делает враждебными к игроку. Игроков — только при включённом PvP; уже иссушённых — пропускает.
     */
    public static void checkDeathGaze(ServerPlayer player) {
        MobEffectInstance pe = player.getEffect(ConfigPotions.DEATH_GAZE);
        if (pe == null) {
            return;
        }
        int range = Math.min(8 + pe.getAmplifier() * 3, 24);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> e != player)) {
            if (entity.isPickable() && entity.isAlive() && EntityUtils.isVisibleTo(0.75F, player, entity, range) && player.hasLineOfSight(entity)
                    && (!(entity instanceof Player) || player.server.isPvpAllowed()) && !entity.hasEffect(MobEffects.WITHER)) {
                entity.setLastHurtByMob(player);
                player.setLastHurtMob(entity);
                if (entity instanceof Mob mob) {
                    mob.setTarget(player);
                }
                entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
            }
        }
    }

    /** Искажение снаряжения: предмет в основной руке, броня и безделушки Curios (getFinalWarp каждого). */
    public static int getWarpFromGear(Player player) {
        int w = PlayerEvents.getFinalWarp(player.getMainHandItem(), player);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            w += PlayerEvents.getFinalWarp(player.getItemBySlot(slot), player);
        }
        var curios = CuriosApi.getCuriosInventory(player).map(h -> h.getEquippedCurios()).orElse(null);
        if (curios != null) {
            for (int a = 0; a < curios.getSlots(); a++) {
                w += PlayerEvents.getFinalWarp(curios.getStackInSlot(a), player);
            }
        }
        return w;
    }

}
