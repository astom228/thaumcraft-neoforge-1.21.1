package thaumcraft.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Настройки мода (порт ModConfig оригинала: те же имена, значения по умолчанию и диапазоны).
 * Графика — клиентский конфиг (thaumcraft-client.toml), разное и мир — общий (thaumcraft-common.toml).
 * Доступ: {@code ModConfig.CONFIG_GRAPHICS.crooked.get()}.
 * Логика postInitLoot/modCompatibility/postInitMisc оригинала переносится на соответствующих этапах.
 */
public class ModConfig {
    public static final float auraSize = 4.0F;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec COMMON_SPEC;

    public static class CONFIG_GRAPHICS {
        public static ModConfigSpec.BooleanValue largeTagText;
        public static ModConfigSpec.BooleanValue disableShaders;
        public static ModConfigSpec.BooleanValue nostress;
        public static ModConfigSpec.BooleanValue crooked;
        public static ModConfigSpec.BooleanValue dialBottom;
        public static ModConfigSpec.BooleanValue showTags;
        public static ModConfigSpec.BooleanValue blueBiome;
        public static ModConfigSpec.BooleanValue showGolemEmotes;

        static void build(ModConfigSpec.Builder b) {
            b.translation("thaumcraft.config.graphics").push("graphics");
            largeTagText = b.comment("Setting this to true will make the amount text in aspect tags twice as large. Useful for certain resolutions and custom fonts.")
                    .define("largeTagText", false);
            disableShaders = b.comment("This setting will disable certain thaumcraft shaders for those who experience FPS drops.")
                    .define("disableShaders", false);
            nostress = b.comment("Set to true to disable anxiety triggers like the heartbeat sound.")
                    .define("nostress", false);
            crooked = b.comment("Hate crooked labels, kittens, puppies and all things awesome? If yes, set this to false.")
                    .define("crooked", true);
            dialBottom = b.comment("Set to true to have the wand dial display in the bottom left instead of the top left.")
                    .define("dialBottom", false);
            showTags = b.comment("Item aspects are hidden by default and pressing shift reveals them.",
                            "Changing this setting to 'true' will reverse this behaviour and always",
                            "display aspects unless shift is pressed.")
                    .define("showTags", false);
            blueBiome = b.comment("Set this to true to get the old blue magical forest back.")
                    .define("blueBiome", false);
            showGolemEmotes = b.comment("Will golems display emote particles if they recieve orders or encounter problems")
                    .define("showGolemEmotes", true);
            b.pop();
        }
    }

    public static class CONFIG_MISC {
        public static ModConfigSpec.BooleanValue wussMode;
        public static ModConfigSpec.BooleanValue allowCheatSheet;
        public static ModConfigSpec.IntValue shieldRecharge;
        public static ModConfigSpec.IntValue shieldWait;
        public static ModConfigSpec.IntValue shieldCost;

        static void build(ModConfigSpec.Builder b) {
            b.translation("thaumcraft.config.misc").push("misc");
            wussMode = b.comment("Setting this to true disables Warp, Taint spread and similar mechanics. You wuss.")
                    .define("wussMode", false);
            allowCheatSheet = b.comment("Enables a version of the Thauminomicon in creative mode that grants you all the research when you first use it.")
                    .define("allowCheatSheet", false);
            shieldRecharge = b.comment("How many milliseconds passes between runic shielding recharge ticks.", "Lower values equals faster recharge. Minimum of 500.")
                    .defineInRange("shieldRecharge", 2000, 500, 500000);
            shieldWait = b.comment("How many milliseconds passes after a shield has been reduced to zero", "before it can start recharging again. Minimum of 0.")
                    .defineInRange("shieldWait", 4000, 0, 50000);
            shieldCost = b.comment("How much vis it costs to reacharge a single unit of shielding. Minimum of 0.")
                    .defineInRange("shieldCost", 1, 0, 500);
            b.pop();
        }
    }

    public static class CONFIG_WORLD {
        public static ModConfigSpec.ConfigValue<String> overworldDim;
        public static ModConfigSpec.ConfigValue<String> dimensionOuterId;
        public static ModConfigSpec.IntValue oreDensity;
        public static ModConfigSpec.BooleanValue generateMagicForest;
        public static ModConfigSpec.IntValue biomeMagicalForestWeight;
        public static ModConfigSpec.DoubleValue taintSpreadRate;
        public static ModConfigSpec.IntValue taintSpreadArea;
        public static ModConfigSpec.BooleanValue generateAura;
        public static ModConfigSpec.BooleanValue generateStructure;
        public static ModConfigSpec.BooleanValue generateCinnabar;
        public static ModConfigSpec.BooleanValue generateAmber;
        public static ModConfigSpec.BooleanValue generateQuartz;
        public static ModConfigSpec.BooleanValue generateCrystals;
        public static ModConfigSpec.BooleanValue generateTrees;
        public static ModConfigSpec.ConfigValue<String> regenKey;
        public static ModConfigSpec.BooleanValue regenAura;
        public static ModConfigSpec.BooleanValue regenStructure;
        public static ModConfigSpec.BooleanValue regenCinnabar;
        public static ModConfigSpec.BooleanValue regenAmber;
        public static ModConfigSpec.BooleanValue regenQuartz;
        public static ModConfigSpec.BooleanValue regenCrystals;
        public static ModConfigSpec.BooleanValue regenTrees;
        public static ModConfigSpec.BooleanValue allowSpawnAngryZombie;
        public static ModConfigSpec.BooleanValue allowSpawnFireBat;
        public static ModConfigSpec.BooleanValue allowSpawnWisp;
        public static ModConfigSpec.BooleanValue allowSpawnTaintacle;
        public static ModConfigSpec.BooleanValue allowSpawnPech;
        public static ModConfigSpec.BooleanValue allowSpawnElder;
        public static ModConfigSpec.BooleanValue allowChampionMobs;

        static void build(ModConfigSpec.Builder b) {
            b.translation("thaumcraft.config.world").push("world");
            // В 1.12 — числовой id измерения (0 и -42); в 1.21 измерения задаются ResourceLocation.
            overworldDim = b.comment("The dimension considered to be your 'overworld'. Certain TC structures will only spawn in this dim.")
                    .gameRestart().define("overworldDim", "minecraft:overworld");
            dimensionOuterId = b.comment("Outer lands dimension id")
                    .gameRestart().define("dimensionOuterId", "thaumcraft:outer_lands");
            oreDensity = b.comment("The % of normal ore amounts that will be spawned. For example 50 will spawn half", "the ores while 200 will spawn double. Default 100")
                    .gameRestart().defineInRange("oreDensity", 100, 1, 500);
            generateMagicForest = b.define("generateMagicForest", true);
            biomeMagicalForestWeight = b.comment("Higher values increases number of magical forest biomes. If you are using biome", "addon mods you probably want to increase this weight quite a bit")
                    .gameRestart().defineInRange("biomeMagicalForestWeight", 5, 0, 100);
            taintSpreadRate = b.comment("The % chance of taint fibres spreading on a block tick.", "Setting this to 0 will effectively stop taint fibre spread.")
                    .defineInRange("taintSpreadRate", 100.0, 0.0, 500.0);
            taintSpreadArea = b.comment("The range at which taint can spread from a taint seed.", "This value is only a base and will be modified by flux levels.")
                    .defineInRange("taintSpreadArea", 32, 8, 256);
            generateAura = b.define("generateAura", true);
            generateStructure = b.define("generateStructure", true);
            generateCinnabar = b.define("generateCinnabar", true);
            generateAmber = b.define("generateAmber", true);
            generateQuartz = b.define("generateQuartz", true);
            generateCrystals = b.define("generateCrystals", true);
            generateTrees = b.define("generateTrees", true);
            regenKey = b.comment("This key is used to keep track of which chunk have been generated/regenerated.",
                            "Changing it will cause the regeneration code to run again, so only change it if you want it to happen.",
                            "Useful to regen only one world feature at a time.")
                    .define("regenKey", "DEFAULT");
            regenAura = b.define("regenAura", false);
            regenStructure = b.define("regenStructure", false);
            regenCinnabar = b.define("regenCinnabar", false);
            regenAmber = b.define("regenAmber", false);
            regenQuartz = b.define("regenQuartz", false);
            regenCrystals = b.define("regenCrystals", false);
            regenTrees = b.define("regenTrees", false);
            allowSpawnAngryZombie = b.define("allowSpawnAngryZombie", true);
            allowSpawnFireBat = b.define("allowSpawnFireBat", true);
            allowSpawnWisp = b.define("allowSpawnWisp", true);
            allowSpawnTaintacle = b.define("allowSpawnTaintacle", true);
            allowSpawnPech = b.define("allowSpawnPech", true);
            allowSpawnElder = b.define("allowSpawnElder", true);
            allowChampionMobs = b.comment("Setting this to false will disable spawning champion mobs. Even when false they will still",
                            "have a greatly reduced chance of spawning in certain dangerous places.")
                    .define("allowChampionMobs", true);
            b.pop();
        }
    }

    static {
        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        CONFIG_GRAPHICS.build(client);
        CLIENT_SPEC = client.build();

        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        CONFIG_MISC.build(common);
        CONFIG_WORLD.build(common);
        COMMON_SPEC = common.build();
    }
}
