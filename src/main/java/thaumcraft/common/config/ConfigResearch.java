package thaumcraft.common.config;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ScanBlock;
import thaumcraft.api.research.ScanEntity;
import thaumcraft.api.research.ScanItem;
import thaumcraft.api.research.ScanOreDictionary;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.common.lib.TCTags;
import thaumcraft.common.lib.research.ScanEnchantment;
import thaumcraft.common.lib.research.ScanGeneric;
import thaumcraft.common.lib.research.ScanPotion;
import thaumcraft.common.lib.research.ScanSky;
import thaumcraft.common.lib.research.theorycraft.*;
import thaumcraft.api.research.theorycraft.*;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * Категории исследований, файлы JSON и периодические находки игрока. Порт ConfigResearch оригинала.
 * Сканируемые объекты — подшаг 7.3, теоретизирование — 7.6, Искажение банки с мозгом — с банкой.
 */
public class ConfigResearch {
    public static String[] TCCategories = new String[] {"BASICS", "ALCHEMY", "AUROMANCY", "ARTIFICE", "INFUSION", "GOLEMANCY", "ELDRITCH"};
    private static final ResourceLocation BACK_OVER = rl("textures/gui/gui_research_back_over.png");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }

    /** init оригинала: при FMLCommonSetupEvent (enqueueWork). */
    public static void init() {
        initCategories();
        initScannables();
        initTheorycraft();
        for (String cat : TCCategories) {
            ThaumcraftApi.registerResearchLocation(rl("research/" + cat.toLowerCase(Locale.ROOT)));
        }
        ThaumcraftApi.registerResearchLocation(rl("research/scans"));
    }

    /** postInit оригинала: после запуска всех модов (FMLLoadCompleteEvent). */
    public static void postInit() {
        ResearchManager.parseAllResearch();
    }

    private static void initCategories() {
        ResearchCategories.registerCategory("BASICS", null,
                new AspectList().add(Aspect.PLANT, 5).add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 5).add(Aspect.AIR, 5).add(Aspect.FIRE, 5)
                        .add(Aspect.EARTH, 3).add(Aspect.WATER, 5),
                rl("textures/items/thaumonomicon_cheat.png"), rl("textures/gui/gui_research_back_1.png"), BACK_OVER);
        ResearchCategories.registerCategory("AUROMANCY", "UNLOCKAUROMANCY",
                new AspectList().add(Aspect.AURA, 20).add(Aspect.MAGIC, 20).add(Aspect.FLUX, 15).add(Aspect.CRYSTAL, 5).add(Aspect.COLD, 5)
                        .add(Aspect.AIR, 5),
                rl("textures/research/cat_auromancy.png"), rl("textures/gui/gui_research_back_2.png"), BACK_OVER);
        ResearchCategories.registerCategory("ALCHEMY", "UNLOCKALCHEMY",
                new AspectList().add(Aspect.ALCHEMY, 30).add(Aspect.FLUX, 10).add(Aspect.MAGIC, 10).add(Aspect.LIFE, 5).add(Aspect.AVERSION, 5)
                        .add(Aspect.DESIRE, 5).add(Aspect.WATER, 5),
                rl("textures/research/cat_alchemy.png"), rl("textures/gui/gui_research_back_3.png"), BACK_OVER);
        ResearchCategories.registerCategory("ARTIFICE", "UNLOCKARTIFICE",
                new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.CRAFT, 10).add(Aspect.METAL, 10).add(Aspect.TOOL, 10).add(Aspect.ENERGY, 10)
                        .add(Aspect.LIGHT, 5).add(Aspect.FLIGHT, 5).add(Aspect.TRAP, 5).add(Aspect.FIRE, 5),
                rl("textures/research/cat_artifice.png"), rl("textures/gui/gui_research_back_4.png"), BACK_OVER);
        ResearchCategories.registerCategory("INFUSION", "UNLOCKINFUSION",
                new AspectList().add(Aspect.MAGIC, 30).add(Aspect.PROTECT, 10).add(Aspect.TOOL, 10).add(Aspect.FLUX, 5).add(Aspect.CRAFT, 5)
                        .add(Aspect.SOUL, 5).add(Aspect.EARTH, 3),
                rl("textures/research/cat_infusion.png"), rl("textures/gui/gui_research_back_7.png"), BACK_OVER);
        ResearchCategories.registerCategory("GOLEMANCY", "UNLOCKGOLEMANCY",
                new AspectList().add(Aspect.MAN, 20).add(Aspect.MOTION, 10).add(Aspect.MIND, 10).add(Aspect.MECHANISM, 10).add(Aspect.EXCHANGE, 5)
                        .add(Aspect.SENSES, 5).add(Aspect.BEAST, 5).add(Aspect.ORDER, 5),
                rl("textures/research/cat_golemancy.png"), rl("textures/gui/gui_research_back_5.png"), BACK_OVER);
        ResearchCategories.registerCategory("ELDRITCH", "UNLOCKELDRITCH",
                new AspectList().add(Aspect.ELDRITCH, 20).add(Aspect.DARKNESS, 10).add(Aspect.MAGIC, 5).add(Aspect.MIND, 5).add(Aspect.VOID, 5)
                        .add(Aspect.DEATH, 5).add(Aspect.UNDEAD, 5).add(Aspect.ENTROPY, 5),
                rl("textures/research/cat_eldritch.png"), rl("textures/gui/gui_research_back_6.png"), BACK_OVER);
    }

    private static void initScannables() {
        ScanningManager.addScannableThing(new ScanGeneric());
        for (Holder<MobEffect> pot : BuiltInRegistries.MOB_EFFECT.holders().toList()) {
            ScanningManager.addScannableThing(new ScanPotion(pot));
        }
        ScanningManager.addScannableThing(new ScanEntity("!Wisp", thaumcraft.common.entities.monster.EntityWisp.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ThaumSlime", thaumcraft.common.entities.monster.EntityThaumicSlime.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Firebat", thaumcraft.common.entities.monster.EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Pech", thaumcraft.common.entities.monster.EntityPech.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!BrainyZombie", thaumcraft.common.entities.monster.EntityBrainyZombie.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchCrab", thaumcraft.common.entities.monster.EntityEldritchCrab.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchCrab", thaumcraft.common.entities.monster.EntityInhabitedZombie.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!CrimsonCultist", thaumcraft.common.entities.monster.cult.EntityCultist.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchGuardian", thaumcraft.common.entities.monster.EntityEldritchGuardian.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!TaintCrawler", thaumcraft.common.entities.monster.tainted.EntityTaintCrawler.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Taintacle", thaumcraft.common.entities.monster.tainted.EntityTaintacle.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!TaintSeed", thaumcraft.common.entities.monster.tainted.EntityTaintSeed.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!TaintSwarm", thaumcraft.common.entities.monster.tainted.EntityTaintSwarm.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!FluxRift", thaumcraft.common.entities.EntityFluxRift.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_golem", net.minecraft.world.entity.animal.AbstractGolem.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_BAT", thaumcraft.common.entities.monster.EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", thaumcraft.common.entities.monster.EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", thaumcraft.common.entities.monster.tainted.EntityTaintSwarm.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", thaumcraft.common.entities.monster.EntityWisp.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ORMOB", thaumcraft.api.entities.IEldritchMob.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ORBOSS", thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss.class, true));
        // TODO этап 18: f_golem — собственные конструкты (EntityOwnedConstruct)
        ScanningManager.addScannableThing(new ScanEntity("f_SPIDER", Spider.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_BAT", Bat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Bat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Parrot.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Ghast.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Blaze.class, true));
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK1", BlocksTC.stoneAncient.get(), BlocksTC.stoneAncientTile.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK2", BlocksTC.stoneEldritchTile.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK3", BlocksTC.stoneAncientGlyphed.get()));
        ScanningManager.addScannableThing(new ScanBlock("ORE", BlocksTC.oreAmber.get(), BlocksTC.oreCinnabar.get(), BlocksTC.crystalAir.get(),
                BlocksTC.crystalFire.get(), BlocksTC.crystalWater.get(), BlocksTC.crystalEarth.get(), BlocksTC.crystalOrder.get(),
                BlocksTC.crystalEntropy.get(), BlocksTC.crystalTaint.get()));
        ScanningManager.addScannableThing(new ScanBlock("!OREAMBER", BlocksTC.oreAmber.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORECINNABAR", BlocksTC.oreCinnabar.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORECRYSTAL", BlocksTC.crystalAir.get(), BlocksTC.crystalFire.get(),
                BlocksTC.crystalWater.get(), BlocksTC.crystalEarth.get(), BlocksTC.crystalOrder.get(), BlocksTC.crystalEntropy.get(),
                BlocksTC.crystalTaint.get()));
        ScanningManager.addScannableThing(new ScanBlock("PLANTS", BlocksTC.logGreatwood.get(), BlocksTC.logSilverwood.get(),
                BlocksTC.saplingGreatwood.get(), BlocksTC.saplingSilverwood.get(), BlocksTC.cinderpearl.get(), BlocksTC.shimmerleaf.get(),
                BlocksTC.vishroom.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", BlocksTC.logGreatwood.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", BlocksTC.logSilverwood.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", BlocksTC.saplingGreatwood.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", BlocksTC.saplingSilverwood.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTCINDERPEARL", BlocksTC.cinderpearl.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTSHIMMERLEAF", BlocksTC.shimmerleaf.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTVISHROOM", BlocksTC.vishroom.get()));
        // TODO: PRIMPEARL — вместе с изначальной жемчужиной
        ScanningManager.addScannableThing(new ScanItem("!DRAGONBREATH", new ItemStack(Items.DRAGON_BREATH)));
        ScanningManager.addScannableThing(new ScanItem("!TOTEMUNDYING", new ItemStack(Items.TOTEM_OF_UNDYING)));
        ScanningManager.addScannableThing(new ScanBlock("f_TELEPORT", Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME));
        ScanningManager.addScannableThing(new ScanItem("f_TELEPORT", new ItemStack(Items.ENDER_PEARL)));
        ScanningManager.addScannableThing(new ScanEntity("f_TELEPORT", EnderMan.class, true));
        ScanningManager.addScannableThing(new ScanItem("f_BRAIN", new ItemStack(ItemsTC.brain.get())));
        ScanningManager.addScannableThing(new ScanEntity("f_BRAIN", thaumcraft.common.entities.monster.EntityBrainyZombie.class, true));
        ScanningManager.addScannableThing(new ScanBlock("f_DISPENSER", Blocks.DISPENSER));
        ScanningManager.addScannableThing(new ScanItem("f_DISPENSER", new ItemStack(Blocks.DISPENSER)));
        ScanningManager.addScannableThing(new ScanItem("f_MATCLAY", new ItemStack(Items.CLAY_BALL)));
        ScanningManager.addScannableThing(new ScanBlock("f_MATCLAY", Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
                Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA,
                Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
                Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA));
        // ScanMaterial("f_MATCLAY", Material.CLAY): материалов в 1.21 нет; из ванильных блоков материал CLAY был у блока глины
        ScanningManager.addScannableThing(new ScanBlock("f_MATCLAY", Blocks.CLAY));
        ScanningManager.addScannableThing(new ScanOreDictionary("f_MATIRON", Tags.Items.ORES_IRON, Tags.Items.INGOTS_IRON,
                Tags.Items.STORAGE_BLOCKS_IRON, TCTags.Items.PLATES_IRON));
        ScanningManager.addScannableThing(new ScanOreDictionary("f_MATBRASS", TCTags.Items.INGOTS_BRASS, TCTags.Items.STORAGE_BLOCKS_BRASS,
                TCTags.Items.PLATES_BRASS));
        ScanningManager.addScannableThing(new ScanOreDictionary("f_MATTHAUMIUM", TCTags.Items.INGOTS_THAUMIUM,
                TCTags.Items.STORAGE_BLOCKS_THAUMIUM, TCTags.Items.PLATES_THAUMIUM));
        ScanningManager.addScannableThing(new ScanOreDictionary("f_MATVOID", TCTags.Items.INGOTS_VOID, TCTags.Items.STORAGE_BLOCKS_VOID,
                TCTags.Items.PLATES_VOID));
        ScanningManager.addScannableThing(new ScanEntity("f_arrow", AbstractArrow.class, true));
        ScanningManager.addScannableThing(new ScanItem("f_arrow", new ItemStack(Items.ARROW)));
        ScanningManager.addScannableThing(new ScanEntity("f_fireball", AbstractHurtingProjectile.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_spit", LlamaSpit.class, true));
        ScanningManager.addScannableThing(new ScanSky());
    }

    private static void initTheorycraft() {
        TheorycraftManager.registerAid(new AidBookshelf());
        TheorycraftManager.registerAid(new AidBrainInAJar());
        // TODO: AidPortalCrimson (этап 16), AidBasicArtifice/Auromancy/Golemancy/Eldritch —
        //  вместе с матрицей наполнения, фокусным манипулятором, сборщиком големов и алтарём
        TheorycraftManager.registerAid(new AidGlyphedStone());
        TheorycraftManager.registerAid(new AidPortal.AidPortalEnd());
        TheorycraftManager.registerAid(new AidPortal.AidPortalNether());
        TheorycraftManager.registerAid(new AidBasicAlchemy());
        TheorycraftManager.registerAid(new AidEnchantmentTable());
        TheorycraftManager.registerAid(new AidBeacon());
        TheorycraftManager.registerCard(CardStudy.class);
        TheorycraftManager.registerCard(CardAnalyze.class);
        TheorycraftManager.registerCard(CardBalance.class);
        TheorycraftManager.registerCard(CardNotation.class);
        TheorycraftManager.registerCard(CardPonder.class);
        TheorycraftManager.registerCard(CardRethink.class);
        TheorycraftManager.registerCard(CardReject.class);
        TheorycraftManager.registerCard(CardExperimentation.class);
        TheorycraftManager.registerCard(CardCurio.class);
        TheorycraftManager.registerCard(CardInspired.class);
        TheorycraftManager.registerCard(CardEnchantment.class);
        TheorycraftManager.registerCard(CardBeacon.class);
        TheorycraftManager.registerCard(CardCelestial.class);
        TheorycraftManager.registerCard(CardConcentrate.class);
        TheorycraftManager.registerCard(CardReactions.class);
        TheorycraftManager.registerCard(CardSynthesis.class);
        TheorycraftManager.registerCard(CardCalibrate.class);
        TheorycraftManager.registerCard(CardMindOverMatter.class);
        TheorycraftManager.registerCard(CardTinker.class);
        TheorycraftManager.registerCard(CardMeasure.class);
        TheorycraftManager.registerCard(CardChannel.class);
        TheorycraftManager.registerCard(CardInfuse.class);
        TheorycraftManager.registerCard(CardFocus.class);
        TheorycraftManager.registerCard(CardAwareness.class);
        TheorycraftManager.registerCard(CardSpellbinding.class);
        TheorycraftManager.registerCard(CardSculpting.class);
        TheorycraftManager.registerCard(CardScripting.class);
        TheorycraftManager.registerCard(CardSynergy.class);
        TheorycraftManager.registerCard(CardDarkWhispers.class);
        TheorycraftManager.registerCard(CardGlyphs.class);
        TheorycraftManager.registerCard(CardPortal.class);
        TheorycraftManager.registerCard(CardRevelation.class);
        TheorycraftManager.registerCard(CardRealization.class);
    }

    private static final Set<ResourceKey<Enchantment>> ENCHANTMENT_SCANS = ConcurrentHashMap.newKeySet();

    /**
     * Сканеры зачарований (в оригинале — для каждого зачарования из реестра при запуске). В 1.21 зачарования — реестр мира:
     * сканеры добавляются при запуске сервера и при входе клиента; один ключ повторно не добавляется.
     */
    public static void registerEnchantmentScans(RegistryAccess access) {
        access.registry(Registries.ENCHANTMENT).ifPresent(reg -> {
            for (ResourceKey<Enchantment> key : reg.registryKeySet()) {
                if (ENCHANTMENT_SCANS.add(key)) {
                    ScanningManager.addScannableThing(new ScanEnchantment(key));
                }
            }
        });
    }

    /**
     * Находки «m_…» (раз в 200 тиков). Типы биомов BiomeDictionary → теги c:is_nether / c:is_end.
     * Высота мира 1.12 (getActualHeight: 128 в Незере, иначе 256) сохранена — пороги те же, что в оригинале.
     */
    public static void checkPeriodicStuff(ServerPlayer player) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        if (!knowledge.isResearchKnown("m_hellandback") && biome.is(Tags.Biomes.IS_NETHER)) {
            knowledge.addResearch("m_hellandback");
            knowledge.sync(player);
            player.displayClientMessage(Component.translatable("got.hellandback").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        if (!knowledge.isResearchKnown("m_endoftheworld") && biome.is(Tags.Biomes.IS_END)) {
            knowledge.addResearch("m_endoftheworld");
            knowledge.sync(player);
            player.displayClientMessage(Component.translatable("got.endoftheworld").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        if (knowledge.isResearchKnown("UNLOCKAUROMANCY@1") && !knowledge.isResearchKnown("UNLOCKAUROMANCY@2")) {
            if (player.getY() < 10.0 && !knowledge.isResearchKnown("m_deepdown")) {
                knowledge.addResearch("m_deepdown");
                knowledge.sync(player);
                player.displayClientMessage(Component.translatable("got.deepdown").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            int actualHeight = player.level().dimensionType().ultraWarm() ? 128 : 256;
            if (player.getY() > actualHeight * 0.4 && !knowledge.isResearchKnown("m_uphigh")) {
                knowledge.addResearch("m_uphigh");
                knowledge.sync(player);
                player.displayClientMessage(Component.translatable("got.uphigh").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
        ServerStatsCounter sms = player.getStats();
        if (sms != null) {
            if (!knowledge.isResearchKnown("m_walker") && sms.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) > 160000) {
                knowledge.addResearch("m_walker");
                knowledge.sync(player);
            }
            if (!knowledge.isResearchKnown("m_runner") && sms.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM)) > 80000) {
                knowledge.addResearch("m_runner");
                knowledge.sync(player);
            }
            if (!knowledge.isResearchKnown("m_jumper") && sms.getValue(Stats.CUSTOM.get(Stats.JUMP)) > 500) {
                knowledge.addResearch("m_jumper");
                knowledge.sync(player);
            }
            if (!knowledge.isResearchKnown("m_swimmer") && sms.getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM)) > 8000) {
                knowledge.addResearch("m_swimmer");
                knowledge.sync(player);
            }
        }
    }
}
