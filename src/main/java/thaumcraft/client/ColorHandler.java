package thaumcraft.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemGenericEssentiaContainer;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.blocks.basic.BlockCandle;

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
        // basicColourHandler: свечи окрашиваются цветом карты (MapColor) своего красителя
        for (DeferredBlock<Block> candle : BlocksTC.candles.values()) {
            event.register((state, level, pos, tintIndex) -> ((BlockCandle) state.getBlock()).dye.getMapColor().col, candle.get());
        }
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // В оригинале предмет берёт цвет блока без мира: базовый цвет листвы / белый
        event.register((stack, tintIndex) -> FoliageColor.getDefaultColor(), BlocksTC.leafGreatwood.get());
        event.register((stack, tintIndex) -> 0xFFFFFF, BlocksTC.leafSilverwood.get());
        // itemEssentiaColourHandler: вис-кристалл окрашивается цветом первого аспекта
        event.register((stack, tintIndex) -> {
            AspectList al = ((ItemGenericEssentiaContainer) stack.getItem()).getAspects(stack);
            return al != null ? 0xFF000000 | al.getAspects()[0].getColor() : 0xFFFFFFFF;
        }, ItemsTC.crystalEssence.get());
        for (DeferredBlock<Block> candle : BlocksTC.candles.values()) {
            int color = ((BlockCandle) candle.get()).dye.getMapColor().col;
            event.register((stack, tintIndex) -> color, candle.get());
        }
    }
}
