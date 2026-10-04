package thaumcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigAspects;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.CreativeTabThaumcraft;
import thaumcraft.common.lib.SoundsTC;
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
        ConfigBlocks.BLOCKS.register(modEventBus);
        ConfigItems.ITEMS.register(modEventBus);
        SoundsTC.SOUNDS.register(modEventBus);
        CreativeTabThaumcraft.TABS.register(modEventBus);
        ConfigRecipes.SERIALIZERS.register(modEventBus);
        ThaumcraftAttachments.ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, ModConfig.CLIENT_SPEC);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // ConfigAspects.registerEntityAspects (в оригинале — postInit); не зависит от данных мира
        event.enqueueWork(ConfigAspects::initEntities);
    }
}
