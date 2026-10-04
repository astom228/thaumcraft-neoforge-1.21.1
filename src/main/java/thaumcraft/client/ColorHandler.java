package thaumcraft.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;

/** Порт ColorHandler оригинала: окраска блоков и предметов. Остальные обработчики добавляются по этапам. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ColorHandler {
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        // Листва: великое дерево — цвет листвы биома, серебряное — белый (без окраски)
        event.register((state, level, pos, tintIndex) -> {
            if (state.getBlock() == BlocksTC.leafSilverwood.get()) {
                return 0xFFFFFF;
            }
            return level != null && pos != null ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor();
        }, BlocksTC.leafGreatwood.get(), BlocksTC.leafSilverwood.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // В оригинале предмет берёт цвет блока без мира: базовый цвет листвы / белый
        event.register((stack, tintIndex) -> FoliageColor.getDefaultColor(), BlocksTC.leafGreatwood.get());
        event.register((stack, tintIndex) -> 0xFFFFFF, BlocksTC.leafSilverwood.get());
    }
}
