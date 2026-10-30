package thaumcraft.common.config;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.EntitySpecialItem;

/** Типы сущностей (порт ConfigEntities; пополняется по этапам, основная часть — этап 16). */
public class ConfigEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Thaumcraft.MODID);

    // EntityRegistry.registerModEntity(…, "SpecialItem", …, 64, 20, true)
    public static final Supplier<EntityType<EntitySpecialItem>> SPECIAL_ITEM = ENTITIES.register("special_item",
            () -> EntityType.Builder.<EntitySpecialItem>of(EntitySpecialItem::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(20).build("special_item"));

    // EntityRegistry.registerModEntity(…, "FollowItem", …, 64, 20, false)
    public static final Supplier<EntityType<thaumcraft.common.entities.EntityFollowingItem>> FOLLOWING_ITEM = ENTITIES.register("following_item",
            () -> EntityType.Builder.<thaumcraft.common.entities.EntityFollowingItem>of(thaumcraft.common.entities.EntityFollowingItem::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(20).build("following_item"));

    // EntityRegistry.registerModEntity(…, "FocusProjectile", …, 64, 20, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityFocusProjectile>> FOCUS_PROJECTILE = ENTITIES.register("focus_projectile",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityFocusProjectile>of(thaumcraft.common.entities.projectile.EntityFocusProjectile::new,
                    MobCategory.MISC).sized(0.15F, 0.15F).clientTrackingRange(4).updateInterval(20).build("focus_projectile"));

    // EntityRegistry.registerModEntity(…, "FocusCloud", …, 64, 20, true); размер — по радиусу (getDimensions)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityFocusCloud>> FOCUS_CLOUD = ENTITIES.register("focus_cloud",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityFocusCloud>of(thaumcraft.common.entities.projectile.EntityFocusCloud::new,
                    MobCategory.MISC).sized(0.15F, 0.15F).clientTrackingRange(4).updateInterval(20).build("focus_cloud"));

    // EntityRegistry.registerModEntity(…, "Focusmine", …, 64, 20, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityFocusMine>> FOCUS_MINE = ENTITIES.register("focus_mine",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityFocusMine>of(thaumcraft.common.entities.projectile.EntityFocusMine::new,
                    MobCategory.MISC).sized(0.15F, 0.15F).clientTrackingRange(4).updateInterval(20).build("focus_mine"));

    // EntityRegistry.registerModEntity(…, "Alumentum", …, 64, 20, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityAlumentum>> ALUMENTUM = ENTITIES.register("alumentum",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityAlumentum>of(thaumcraft.common.entities.projectile.EntityAlumentum::new,
                    MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(20).build("alumentum"));

    // EntityRegistry.registerModEntity(…, "Grapple", …, 64, 20, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityGrapple>> GRAPPLE = ENTITIES.register("grapple",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityGrapple>of(thaumcraft.common.entities.projectile.EntityGrapple::new,
                    MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(4).updateInterval(20).build("grapple"));

    // EntityRegistry.registerModEntity(…, "Spellbat", …, 64, 3, false, …)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntitySpellBat>> SPELL_BAT = ENTITIES.register("spellbat",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntitySpellBat>of(thaumcraft.common.entities.monster.EntitySpellBat::new,
                    MobCategory.MONSTER).sized(0.5F, 0.9F).clientTrackingRange(4).updateInterval(3).build("spellbat"));

    // EntityRegistry.registerModEntity(…, "Wisp", …, 64, 3, false)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityWisp>> WISP = ENTITIES.register("wisp",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityWisp>of(thaumcraft.common.entities.monster.EntityWisp::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.9F).clientTrackingRange(4).updateInterval(3).build("wisp"));
    // EntityRegistry.registerModEntity(…, "Firebat", …, 64, 3, false); огнеупорна
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityFireBat>> FIRE_BAT = ENTITIES.register("firebat",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityFireBat>of(thaumcraft.common.entities.monster.EntityFireBat::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.9F).fireImmune().clientTrackingRange(4).updateInterval(3).build("firebat"));
    // EntityRegistry.registerModEntity(…, "ThaumSlime", …, 64, 3, true); размер — по размеру слизня, как у ванильного
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityThaumicSlime>> THAUMIC_SLIME = ENTITIES.register("thaumslime",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityThaumicSlime>of(thaumcraft.common.entities.monster.EntityThaumicSlime::new,
                    MobCategory.MONSTER).sized(0.52F, 0.52F).eyeHeight(0.325F).clientTrackingRange(4).updateInterval(3).build("thaumslime"));
    // EntityRegistry.registerModEntity(…, "MindSpider", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityMindSpider>> MIND_SPIDER = ENTITIES.register("mindspider",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityMindSpider>of(thaumcraft.common.entities.monster.EntityMindSpider::new, MobCategory.MONSTER).sized(0.7F, 0.5F).eyeHeight(0.45F)
                    .clientTrackingRange(4).updateInterval(3).build("mindspider"));
    // EntityRegistry.registerModEntity(…, "BrainyZombie", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityBrainyZombie>> BRAINY_ZOMBIE = ENTITIES.register("brainyzombie",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityBrainyZombie>of(thaumcraft.common.entities.monster.EntityBrainyZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8).build("brainyzombie"));
    // EntityRegistry.registerModEntity(…, "GiantBrainyZombie", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityGiantBrainyZombie>> GIANT_BRAINY_ZOMBIE = ENTITIES.register("giantbrainyzombie",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityGiantBrainyZombie>of(thaumcraft.common.entities.monster.EntityGiantBrainyZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F)
                    .eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8).build("giantbrainyzombie"));
    // EntityRegistry.registerModEntity(…, "TaintCrawler", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.tainted.EntityTaintCrawler>> TAINT_CRAWLER = ENTITIES.register("taintcrawler",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.tainted.EntityTaintCrawler>of(thaumcraft.common.entities.monster.tainted.EntityTaintCrawler::new, MobCategory.MONSTER).sized(0.5F, 0.4F).eyeHeight(0.1F)
                    .clientTrackingRange(4).updateInterval(3).build("taintcrawler"));
    // EntityRegistry.registerModEntity(…, "TaintSwarm", …, 64, 3, false)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.tainted.EntityTaintSwarm>> TAINT_SWARM = ENTITIES.register("taintswarm",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.tainted.EntityTaintSwarm>of(thaumcraft.common.entities.monster.tainted.EntityTaintSwarm::new, MobCategory.MONSTER).sized(2.0F, 2.0F)
                    .clientTrackingRange(4).updateInterval(3).build("taintswarm"));
    // EntityRegistry.registerModEntity(…, "Taintacle", …, 64, 3, false)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.tainted.EntityTaintacle>> TAINTACLE = ENTITIES.register("taintacle",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.tainted.EntityTaintacle>of(thaumcraft.common.entities.monster.tainted.EntityTaintacle::new, MobCategory.MONSTER).sized(0.8F, 3.0F)
                    .clientTrackingRange(4).updateInterval(3).build("taintacle"));
    // EntityRegistry.registerModEntity(…, "TaintacleTiny", …, 64, 3, false)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.tainted.EntityTaintacleSmall>> TAINTACLE_SMALL = ENTITIES.register("taintacletiny",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.tainted.EntityTaintacleSmall>of(thaumcraft.common.entities.monster.tainted.EntityTaintacleSmall::new, MobCategory.MONSTER).sized(0.22F, 1.0F)
                    .clientTrackingRange(4).updateInterval(3).build("taintacletiny"));
    // EntityRegistry.registerModEntity(…, "EldritchCrab", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityEldritchCrab>> ELDRITCH_CRAB = ENTITIES.register("eldritchcrab",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityEldritchCrab>of(thaumcraft.common.entities.monster.EntityEldritchCrab::new, MobCategory.MONSTER).sized(0.8F, 0.6F)
                    .clientTrackingRange(8).updateInterval(3).build("eldritchcrab"));
    // EntityRegistry.registerModEntity(…, "InhabitedZombie", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityInhabitedZombie>> INHABITED_ZOMBIE = ENTITIES.register("inhabitedzombie",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityInhabitedZombie>of(thaumcraft.common.entities.monster.EntityInhabitedZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F)
                    .eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8).build("inhabitedzombie"));
    // EntityRegistry.registerModEntity(…, "EldritchGuardian", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.EntityEldritchGuardian>> ELDRITCH_GUARDIAN = ENTITIES.register("eldritchguardian",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.EntityEldritchGuardian>of(thaumcraft.common.entities.monster.EntityEldritchGuardian::new, MobCategory.MONSTER).sized(0.8F, 2.25F)
                    .eyeHeight(2.1F).clientTrackingRange(8).updateInterval(3).build("eldritchguardian"));
    // EntityRegistry.registerModEntity(…, "EldritchOrb", …, 64, 20, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityEldritchOrb>> ELDRITCH_ORB = ENTITIES.register("eldritchorb",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityEldritchOrb>of(thaumcraft.common.entities.projectile.EntityEldritchOrb::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(20).build("eldritchorb"));
    // EntityRegistry.registerModEntity(…, "EldritchGolem", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.boss.EntityEldritchGolem>> ELDRITCH_GOLEM = ENTITIES.register("eldritchgolem",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.boss.EntityEldritchGolem>of(thaumcraft.common.entities.monster.boss.EntityEldritchGolem::new, MobCategory.MONSTER).sized(1.75F, 3.5F)
                    .eyeHeight(3.0F).fireImmune().clientTrackingRange(8).updateInterval(3).build("eldritchgolem"));
    // EntityRegistry.registerModEntity(…, "GolemOrb", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityGolemOrb>> GOLEM_ORB = ENTITIES.register("golemorb",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityGolemOrb>of(thaumcraft.common.entities.projectile.EntityGolemOrb::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(3).build("golemorb"));
    // EntityRegistry.registerModEntity(…, "EldritchWarden", …, 64, 3, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.boss.EntityEldritchWarden>> ELDRITCH_WARDEN = ENTITIES.register("eldritchwarden",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.boss.EntityEldritchWarden>of(thaumcraft.common.entities.monster.boss.EntityEldritchWarden::new, MobCategory.MONSTER).sized(1.5F, 3.5F)
                    .eyeHeight(3.1F).clientTrackingRange(8).updateInterval(3).build("eldritchwarden"));
    // EntityRegistry.registerModEntity(…, "FluxRift", …, 64, 20, false); рамка — по точкам разлома
    public static final Supplier<EntityType<thaumcraft.common.entities.EntityFluxRift>> FLUX_RIFT = ENTITIES.register("flux_rift",
            () -> EntityType.Builder.<thaumcraft.common.entities.EntityFluxRift>of(thaumcraft.common.entities.EntityFluxRift::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F).clientTrackingRange(4).updateInterval(20).fireImmune().build("flux_rift"));
    // EntityRegistry.registerModEntity(…, "BottleTaint", …, 64, 10, true)
    public static final Supplier<EntityType<thaumcraft.common.entities.projectile.EntityBottleTaint>> BOTTLE_TAINT = ENTITIES.register("bottle_taint",
            () -> EntityType.Builder.<thaumcraft.common.entities.projectile.EntityBottleTaint>of(thaumcraft.common.entities.projectile.EntityBottleTaint::new,
                    MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("bottle_taint"));
    // EntityRegistry.registerModEntity(…, "TaintSeed", …, 64, 20, false)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.tainted.EntityTaintSeed>> TAINT_SEED = ENTITIES.register("taint_seed",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.tainted.EntityTaintSeed>of(thaumcraft.common.entities.monster.tainted.EntityTaintSeed::new,
                    MobCategory.MONSTER).sized(1.5F, 1.25F).clientTrackingRange(4).updateInterval(20).build("taint_seed"));
    // EntityRegistry.registerModEntity(…, "TaintSeedPrime", …, 64, 20, false)
    public static final Supplier<EntityType<thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime>> TAINT_SEED_PRIME = ENTITIES.register("taint_seed_prime",
            () -> EntityType.Builder.<thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime>of(
                    thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime::new, MobCategory.MONSTER).sized(2.0F, 2.0F).clientTrackingRange(4)
                    .updateInterval(20).build("taint_seed_prime"));

    /**
     * Виды, которые могут стать чемпионами, и надбавка к шансу (championWhiteList IMC оригинала; проверка по классу —
     * подклассы тоже, как isAssignableFrom). Только для чтения после загрузки класса.
     */
    public static final java.util.Map<Class<?>, Integer> championModWhitelist = java.util.Map.of(
            net.minecraft.world.entity.monster.Zombie.class, 0,
            net.minecraft.world.entity.monster.Spider.class, 0,
            net.minecraft.world.entity.monster.Blaze.class, 0,
            net.minecraft.world.entity.monster.EnderMan.class, 0,
            net.minecraft.world.entity.monster.Skeleton.class, 0,
            net.minecraft.world.entity.monster.Witch.class, 1,
            thaumcraft.common.entities.monster.EntityEldritchCrab.class, 0,
            thaumcraft.common.entities.monster.tainted.EntityTaintacle.class, 2,
            thaumcraft.common.entities.monster.EntityInhabitedZombie.class, 3);

    public static final DeferredRegister<net.minecraft.world.entity.ai.attributes.Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, Thaumcraft.MODID);
    /** ThaumcraftApiHelper.CHAMPION_MOD: −2 — не проверялся, −1 — обычный, 0–13 — модификатор чемпиона; синхронизируется с клиентом. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.Attribute> CHAMPION_MOD =
            ATTRIBUTES.register("mob_mod", () -> new net.minecraft.world.entity.ai.attributes.RangedAttribute("attribute.name.thaumcraft.mob_mod", -2.0, -2.0, 100.0).setSyncable(true));
    /** ChampionModTainted.TAINTED_MOD: 1 — поведение заражённого существа уже заменено. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.Attribute> TAINTED_MOD =
            ATTRIBUTES.register("mob_mod_taint", () -> new net.minecraft.world.entity.ai.attributes.RangedAttribute("attribute.name.thaumcraft.mob_mod_taint", 0.0, 0.0, 1.0));

    /**
     * entityConstuct оригинала: атрибуты чемпиона добавлялись каждому EntityCreature при создании. В 1.21 набор атрибутов
     * задаётся типу, поэтому они добавляются всем живым типам (проверка «существо ли это» — в коде событий). Урон атаки
     * добавляется тем, у кого его нет: makeTainted регистрировал его мирным существам.
     */
    public static void modifyAttributes(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent event) {
        for (EntityType<? extends net.minecraft.world.entity.LivingEntity> type : event.getTypes()) {
            if (!event.has(type, CHAMPION_MOD)) {
                event.add(type, CHAMPION_MOD);
            }
            if (!event.has(type, TAINTED_MOD)) {
                event.add(type, TAINTED_MOD);
            }
            if (!event.has(type, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                event.add(type, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 4.0);
            }
        }
    }

    /** Атрибуты живых сущностей (applyEntityAttributes оригинала). */
    public static void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(SPELL_BAT.get(), thaumcraft.common.entities.monster.EntitySpellBat.createAttributes().build());
        event.put(FIRE_BAT.get(), thaumcraft.common.entities.monster.EntityFireBat.createAttributes().build());
        event.put(THAUMIC_SLIME.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.2).build());
        event.put(MIND_SPIDER.get(), thaumcraft.common.entities.monster.EntityMindSpider.createAttributes().build());
        event.put(BRAINY_ZOMBIE.get(), thaumcraft.common.entities.monster.EntityBrainyZombie.createAttributes().build());
        event.put(GIANT_BRAINY_ZOMBIE.get(), thaumcraft.common.entities.monster.EntityGiantBrainyZombie.createAttributes().build());
        event.put(TAINT_CRAWLER.get(), thaumcraft.common.entities.monster.tainted.EntityTaintCrawler.createAttributes().build());
        event.put(TAINT_SWARM.get(), thaumcraft.common.entities.monster.tainted.EntityTaintSwarm.createAttributes().build());
        event.put(TAINTACLE.get(), thaumcraft.common.entities.monster.tainted.EntityTaintacle.createAttributes().build());
        event.put(TAINTACLE_SMALL.get(), thaumcraft.common.entities.monster.tainted.EntityTaintacleSmall.createAttributes().build());
        event.put(ELDRITCH_CRAB.get(), thaumcraft.common.entities.monster.EntityEldritchCrab.createAttributes().build());
        event.put(INHABITED_ZOMBIE.get(), thaumcraft.common.entities.monster.EntityInhabitedZombie.createAttributes().build());
        event.put(ELDRITCH_GUARDIAN.get(), thaumcraft.common.entities.monster.EntityEldritchGuardian.createAttributes().build());
        event.put(ELDRITCH_GOLEM.get(), thaumcraft.common.entities.monster.boss.EntityEldritchGolem.createAttributes().build());
        event.put(ELDRITCH_WARDEN.get(), thaumcraft.common.entities.monster.boss.EntityEldritchWarden.createAttributes().build());
        event.put(WISP.get(), thaumcraft.common.entities.monster.EntityWisp.createAttributes().build());
        event.put(TAINT_SEED.get(), thaumcraft.common.entities.monster.tainted.EntityTaintSeed.createAttributes().build());
        event.put(TAINT_SEED_PRIME.get(), thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime.createAttributes().build());
    }
}
