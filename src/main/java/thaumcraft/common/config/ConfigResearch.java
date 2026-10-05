package thaumcraft.common.config;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
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
import thaumcraft.api.research.ResearchCategories;
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
                rl("textures/items/thaumonomicon_cheat.png"), rl("textures/gui/gui_research_back_1.jpg"), BACK_OVER);
        ResearchCategories.registerCategory("AUROMANCY", "UNLOCKAUROMANCY",
                new AspectList().add(Aspect.AURA, 20).add(Aspect.MAGIC, 20).add(Aspect.FLUX, 15).add(Aspect.CRYSTAL, 5).add(Aspect.COLD, 5)
                        .add(Aspect.AIR, 5),
                rl("textures/research/cat_auromancy.png"), rl("textures/gui/gui_research_back_2.jpg"), BACK_OVER);
        ResearchCategories.registerCategory("ALCHEMY", "UNLOCKALCHEMY",
                new AspectList().add(Aspect.ALCHEMY, 30).add(Aspect.FLUX, 10).add(Aspect.MAGIC, 10).add(Aspect.LIFE, 5).add(Aspect.AVERSION, 5)
                        .add(Aspect.DESIRE, 5).add(Aspect.WATER, 5),
                rl("textures/research/cat_alchemy.png"), rl("textures/gui/gui_research_back_3.jpg"), BACK_OVER);
        ResearchCategories.registerCategory("ARTIFICE", "UNLOCKARTIFICE",
                new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.CRAFT, 10).add(Aspect.METAL, 10).add(Aspect.TOOL, 10).add(Aspect.ENERGY, 10)
                        .add(Aspect.LIGHT, 5).add(Aspect.FLIGHT, 5).add(Aspect.TRAP, 5).add(Aspect.FIRE, 5),
                rl("textures/research/cat_artifice.png"), rl("textures/gui/gui_research_back_4.jpg"), BACK_OVER);
        ResearchCategories.registerCategory("INFUSION", "UNLOCKINFUSION",
                new AspectList().add(Aspect.MAGIC, 30).add(Aspect.PROTECT, 10).add(Aspect.TOOL, 10).add(Aspect.FLUX, 5).add(Aspect.CRAFT, 5)
                        .add(Aspect.SOUL, 5).add(Aspect.EARTH, 3),
                rl("textures/research/cat_infusion.png"), rl("textures/gui/gui_research_back_7.jpg"), BACK_OVER);
        ResearchCategories.registerCategory("GOLEMANCY", "UNLOCKGOLEMANCY",
                new AspectList().add(Aspect.MAN, 20).add(Aspect.MOTION, 10).add(Aspect.MIND, 10).add(Aspect.MECHANISM, 10).add(Aspect.EXCHANGE, 5)
                        .add(Aspect.SENSES, 5).add(Aspect.BEAST, 5).add(Aspect.ORDER, 5),
                rl("textures/research/cat_golemancy.png"), rl("textures/gui/gui_research_back_5.jpg"), BACK_OVER);
        ResearchCategories.registerCategory("ELDRITCH", "UNLOCKELDRITCH",
                new AspectList().add(Aspect.ELDRITCH, 20).add(Aspect.DARKNESS, 10).add(Aspect.MAGIC, 5).add(Aspect.MIND, 5).add(Aspect.VOID, 5)
                        .add(Aspect.DEATH, 5).add(Aspect.UNDEAD, 5).add(Aspect.ENTROPY, 5),
                rl("textures/research/cat_eldritch.png"), rl("textures/gui/gui_research_back_6.jpg"), BACK_OVER);
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
