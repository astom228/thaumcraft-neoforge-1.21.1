package thaumcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigAspects;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.CreativeTabThaumcraft;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.lib.TCMenus;
import thaumcraft.common.lib.capabilities.ThaumcraftAttachments;
import thaumcraft.common.lib.crafting.ConfigRecipes;

@Mod(Thaumcraft.MODID)
public class Thaumcraft {
    public static final String MODID = "thaumcraft";
    public static final String MODNAME = "Thaumcraft";
    public static final String VERSION = "6.1.BETA17";
    public static final Logger log = LogUtils.getLogger();

    public Thaumcraft(IEventBus modEventBus, ModContainer modContainer) {
        BlocksTC.init();
        ItemsTC.init();
        thaumcraft.common.config.ConfigFluids.FLUID_TYPES.register(modEventBus);
        thaumcraft.common.config.ConfigFluids.FLUIDS.register(modEventBus);
        ConfigBlocks.BLOCKS.register(modEventBus);
        ConfigBlocks.TILES.register(modEventBus);
        thaumcraft.api.ThaumcraftMaterials.ARMOR_MATERIALS.register(modEventBus);
        ConfigItems.ITEMS.register(modEventBus);
        thaumcraft.common.items.SpawnEggs.EGGS.register(modEventBus);
        thaumcraft.common.world.MobSpawns.SERIALIZERS.register(modEventBus);
        thaumcraft.common.world.features.TCFeatures.FEATURES.register(modEventBus);
        ConfigEntities.ENTITIES.register(modEventBus);
        ConfigEntities.ATTRIBUTES.register(modEventBus);
        SoundsTC.SOUNDS.register(modEventBus);
        CreativeTabThaumcraft.TABS.register(modEventBus);
        ConfigRecipes.SERIALIZERS.register(modEventBus);
        ConfigRecipes.TYPES.register(modEventBus);
        ThaumcraftAttachments.ATTACHMENTS.register(modEventBus);
        TCDataComponents.COMPONENTS.register(modEventBus);
        TCMenus.MENUS.register(modEventBus);
        thaumcraft.common.config.ConfigPotions.EFFECTS.register(modEventBus);
        thaumcraft.common.lib.events.RareEarthsLootModifier.LOOT_MODIFIERS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(ConfigBlocks::registerCapabilities);
        modEventBus.addListener(ConfigEntities::registerAttributes);
        modEventBus.addListener(ConfigEntities::modifyAttributes);
        modEventBus.addListener(thaumcraft.common.items.SpawnEggs::addToTabs);
        modEventBus.addListener(thaumcraft.common.world.MobSpawns::registerPlacements);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, ModConfig.CLIENT_SPEC);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // ConfigAspects.registerEntityAspects (в оригинале — postInit); не зависит от данных мира
        event.enqueueWork(ConfigAspects::initEntities);
        // registerBiomes оригинала: Магический лес в Верхнем мире (регион TerraBlender)
        event.enqueueWork(thaumcraft.common.world.biomes.TCRegion::register);
        // ConfigItems.initFoci (в оригинале — preInit): элементы фокусов, до чтения любых пакетов
        event.enqueueWork(ConfigItems::initFoci);
        // типы управляющих печатей (до загрузки чанков с печатями)
        event.enqueueWork(ConfigItems::initSeals);
        // статический блок GolemProperties: материалы и детали големов (до загрузки миров)
        event.enqueueWork(thaumcraft.common.entities.construct.golem.GolemProperties::init);
        // init оригинала: категории и файлы исследований (дополнения регистрируют свои там же)
        event.enqueueWork(ConfigResearch::init);
        // ConfigRecipes.initializeCompoundRecipes (в оригинале — init)
        event.enqueueWork(ConfigRecipes::initializeCompoundRecipes);
        event.enqueueWork(ConfigRecipes::initializeFakeRecipes);
        event.enqueueWork(ConfigRecipes::initializeAlchemyRecipes);
        event.enqueueWork(ConfigRecipes::initializeInfusionRecipes);
        event.enqueueWork(ConfigRecipes::initializeSmeltingBonus);
        // самородные кластеры для «Очистки» (initMisc / modCompatibility оригинала)
        event.enqueueWork(thaumcraft.common.lib.utils.Utils::initSpecialMining);
        // роба тауматурга отмывается в котле с водой, как кожаная броня (onItemUseFirst оригинала)
        event.enqueueWork(ConfigItems::registerCauldron);
        // BehaviorDispenseAlumetum: раздатчик бросает алюментум
        event.enqueueWork(() -> net.minecraft.world.level.block.DispenserBlock.registerProjectileBehavior(thaumcraft.api.items.ItemsTC.alumentum.get()));
        // ModConfig.postInitLoot оригинала: содержимое мешков с добычей
        event.enqueueWork(thaumcraft.common.config.ConfigLoot::postInitLoot);
    }

    /** postInit оригинала: загрузка JSON исследований после того, как все моды зарегистрировали категории и файлы. */
    private void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(ConfigResearch::postInit);
    }
}
