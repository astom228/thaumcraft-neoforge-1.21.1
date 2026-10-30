package thaumcraft.common.lib.events;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.items.consumables.ItemBathSalts;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.fx.PacketFXShield;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * События сущностей (порт EntityEvents): соли для ванн, чемпионы (появление, эффекты, добыча), заражение Выбросом,
 * исследования от урона, мозги из зомби, «Багровый обряд» из культистов, кристаллы от растворения.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class EntityEvents {
    /** Истаявшие соли для ванн превращают источник воды под собой в очищающую жидкость. */
    @SubscribeEvent
    public static void itemExpire(ItemExpireEvent event) {
        ItemEntity item = event.getEntity();
        Level world = item.level();
        if (world.isClientSide || !(item.getItem().getItem() instanceof ItemBathSalts)) {
            return;
        }
        BlockPos bp = item.blockPosition();
        BlockState bs = world.getBlockState(bp);
        if (bs.is(Blocks.WATER) && bs.getValue(LiquidBlock.LEVEL) == 0) {
            world.setBlockAndUpdate(bp, BlocksTC.purifyingFluid.get().defaultBlockState());
        }
    }

    /** livingTick: модификаторы чемпиона типа 0 (защита, бессмертие, заражение) — каждый тик, на сервере. */
    @SubscribeEvent
    public static void livingTick(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof PathfinderMob mob) || !mob.isAlive()) {
            return;
        }
        Integer t = ThaumcraftApiHelper.getChampionMod(mob);
        if (t == null || t < 0) {
            return;
        }
        if (t >= ChampionModifier.mods.length) {
            mob.discard();
        } else if (ChampionModifier.mods[t].type == 0) {
            ChampionModifier.mods[t].effect.performEffect(mob, null, null, 0.0F);
        }
    }

    /** entityHurt оригинала (LivingHurtEvent — до брони, как LivingIncomingDamageEvent). Маски — FortressArmorEvents, руническая защита — PlayerEvents. */
    @SubscribeEvent
    public static void entityHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        if (entity instanceof ServerPlayer player) {
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && ThaumcraftCapabilities.knowsResearchStrict(player, "BASEAUROMANCY@2")
                    && !ThaumcraftCapabilities.knowsResearch(player, "f_onfire")) {
                gotResearch(player, "f_onfire", "got.onfire");
            }
            if (source.getDirectEntity() != null && ThaumcraftCapabilities.knowsResearchStrict(player, "FOCUSPROJECTILE@2")) {
                if (!ThaumcraftCapabilities.knowsResearch(player, "f_arrow") && source.getDirectEntity() instanceof AbstractArrow) {
                    gotResearch(player, "f_arrow", "got.projectile");
                }
                if (!ThaumcraftCapabilities.knowsResearch(player, "f_fireball") && source.getDirectEntity() instanceof AbstractHurtingProjectile) {
                    gotResearch(player, "f_fireball", "got.projectile");
                }
                if (!ThaumcraftCapabilities.knowsResearch(player, "f_spit") && source.getDirectEntity() instanceof LlamaSpit) {
                    gotResearch(player, "f_spit", "got.projectile");
                }
            }
            return;
        }
        if (entity instanceof Player) {
            return;
        }
        if (entity.getHealth() < 2.0F && !entity.isInvertedHealAndHarm() && entity.isAlive() && !(entity instanceof ITaintedMob)
                && entity.hasEffect(ConfigPotions.FLUX_TAINT) && entity.getRandom().nextBoolean()) {
            // TODO этап 18: кроме собственных конструктов (EntityOwnedConstruct)
            EntityUtils.makeTainted(entity);
            return;
        }
        if (entity instanceof Monster mob) {
            Integer cai = ThaumcraftApiHelper.getChampionMod(mob);
            int t = cai == null ? -2 : cai;
            if (t >= 0 || mob instanceof IEldritchMob) {
                if ((t == 5 || mob instanceof IEldritchMob) && mob.getAbsorptionAmount() > 0.0F) {
                    int target = -1;
                    if (source.getEntity() != null) {
                        target = source.getEntity().getId();
                    }
                    if (source.is(DamageTypes.FALL)) {
                        target = -2;
                    }
                    if (source.is(DamageTypes.FALLING_BLOCK)) {
                        target = -3;
                    }
                    PacketDistributor.sendToPlayersNear((ServerLevel) mob.level(), null, mob.getX(), mob.getY(), mob.getZ(), 32.0, new PacketFXShield(mob.getId(), target));
                    mob.playSound(SoundsTC.runicShieldCharge.get(), 0.66F, 1.1F + mob.getRandom().nextFloat() * 0.1F);
                } else if (t >= 0 && ChampionModifier.mods[t].type == 2 && source.getEntity() instanceof LivingEntity attacker) {
                    event.setAmount(ChampionModifier.mods[t].effect.performEffect(mob, attacker, source, event.getAmount()));
                }
            }
        }
        if (event.getAmount() > 0.0F && source.getEntity() instanceof Monster mob) {
            Integer t = ThaumcraftApiHelper.getChampionMod(mob);
            if (t != null && t >= 0 && ChampionModifier.mods[t].type == 1) {
                event.setAmount(ChampionModifier.mods[t].effect.performEffect(mob, entity, source, event.getAmount()));
            }
        }
    }

    private static void gotResearch(ServerPlayer player, String key, String message) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        knowledge.addResearch(key);
        knowledge.sync(player);
        player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        Level world = entity.level();
        if (world.isClientSide) {
            return;
        }
        boolean fakeplayer = event.getSource().getEntity() instanceof FakePlayer;
        int looting = lootingLevel(event.getSource());
        if (event.isRecentlyHit() && !fakeplayer && entity instanceof Monster && !(entity instanceof EntityThaumcraftBoss)) {
            Integer t = ThaumcraftApiHelper.getChampionMod(entity);
            if (t != null && t >= 0 && t != 13) {
                int i = 5 + world.random.nextInt(3);
                while (i > 0) {
                    int j = ExperienceOrb.getExperienceValue(i);
                    i -= j;
                    world.addFreshEntity(new ExperienceOrb(world, entity.getX(), entity.getY(), entity.getZ(), j));
                }
                int lb = Math.min(2, Mth.floor((world.random.nextInt(9) + looting) / 5.0F));
                ItemStack bag = new ItemStack(lb == 0 ? ItemsTC.lootBagCommon.get() : lb == 1 ? ItemsTC.lootBagUncommon.get() : ItemsTC.lootBagRare.get());
                event.getDrops().add(new ItemEntity(world, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), bag));
            }
        }
        if (entity instanceof Zombie && !(entity instanceof EntityBrainyZombie) && event.isRecentlyHit() && world.random.nextInt(10) - looting < 1) {
            event.getDrops().add(new ItemEntity(world, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), new ItemStack(ItemsTC.brain.get())));
        }
        // культисты: «Багровый обряд» — 1/4, пока не изучен культ (!CrimsonCultist@2), потом 1/20; 1/50, если обряд уже с собой
        if (entity instanceof thaumcraft.common.entities.monster.cult.EntityCultist && !fakeplayer && event.getSource().getEntity() instanceof Player p) {
            int c = !ThaumcraftCapabilities.getKnowledge(p).isResearchKnown("!CrimsonCultist@2") ? 4 : 20;
            if (p.getInventory().contains(new ItemStack(ItemsTC.curioRites.get()))) {
                c = 50;
            }
            if (world.random.nextInt(c) == 0) {
                event.getDrops().add(new ItemEntity(world, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), new ItemStack(ItemsTC.curioRites.get())));
            }
        }
        if (event.getSource().is(DamageSourceThaumcraft.DISSOLVE)) {
            AspectList aspects = AspectHelper.getEntityAspects(entity);
            if (aspects != null && aspects.size() > 0) {
                Aspect[] al = aspects.getAspects();
                int q = Mth.nextInt(world.random, 1, 1 + aspects.visSize() / 10);
                for (int a = 0; a < q; a++) {
                    Aspect aspect = al[world.random.nextInt(al.length)];
                    event.getDrops().add(new ItemEntity(world, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), ThaumcraftApiHelper.makeCrystal(aspect)));
                }
            }
        }
    }

    /** Уровень «Добычи» оружия убийцы (getLootingLevel события 1.12). */
    private static int lootingLevel(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity killer) {
            var looting = killer.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.LOOTING);
            if (looting.isPresent()) {
                return EnchantmentHelper.getEnchantmentLevel(looting.get(), killer);
            }
        }
        return 0;
    }

    /**
     * entitySpawns: каждый ещё не проверенный монстр (CHAMPION_MOD = −2) один раз бросает жребий на чемпиона —
     * только виды из белого списка (ConfigEntities.championModWhitelist) и с базовым здоровьем от 10.
     * Шанс выше на сложной сложности, в жутких биомах, Незере и Энде. Сброс отметки заражённого не нужен —
     * она временная (см. ChampionModTainted).
     */
    @SubscribeEvent
    public static void entitySpawns(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Monster mob)) {
            return;
        }
        var modai = mob.getAttribute(ThaumcraftApiHelper.CHAMPION_MOD);
        if (modai == null || modai.getValue() >= -1.0) {
            return;
        }
        Level world = event.getLevel();
        boolean allow = ModConfig.CONFIG_WORLD.allowChampionMobs.get();
        int c = world.random.nextInt(100);
        if (world.getDifficulty() == Difficulty.EASY || !allow) {
            c += 2;
        }
        if (world.getDifficulty() == Difficulty.HARD) {
            c -= allow ? 2 : 0;
        }
        // TODO этап 17: в Чужих землях (dimensionOuterId) c -= 3
        var bg = world.getBiome(mob.blockPosition());
        if (bg.is(Tags.Biomes.IS_SPOOKY) || bg.is(Tags.Biomes.IS_NETHER) || bg.is(Tags.Biomes.IS_END)) {
            c -= allow ? 2 : 1;
        }
        int cc = 0;
        boolean whitelisted = false;
        for (var e : ConfigEntities.championModWhitelist.entrySet()) {
            if (e.getKey().isInstance(mob)) {
                whitelisted = true;
                if (allow || mob instanceof EntityThaumcraftBoss) {
                    cc = Math.max(cc, e.getValue() - 1);
                }
            }
        }
        c -= cc;
        if (whitelisted && c <= 0 && mob.getAttributeBaseValue(Attributes.MAX_HEALTH) >= 10.0) {
            EntityUtils.makeChampion(mob, false);
        } else {
            EntityUtils.reapply(modai, ChampionModifier.ATTRIBUTE_MOD_NONE);
        }
    }
}
