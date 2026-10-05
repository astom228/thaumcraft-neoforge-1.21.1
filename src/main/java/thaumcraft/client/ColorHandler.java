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
import thaumcraft.common.blocks.world.ore.BlockCrystal;
import thaumcraft.common.blocks.misc.BlockNitor;

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
        // crystalColourHandler: вис-кристаллы — цвет аспекта
        for (DeferredBlock<Block> c : CRYSTALS()) {
            event.register((state, level, pos, tintIndex) -> ((BlockCrystal) state.getBlock()).aspect.getColor(), c.get());
        }
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
        // itemCrystalPlanterColourHandler: предмет кристалла — цвет аспекта
        for (DeferredBlock<Block> c : CRYSTALS()) {
            int color = ((BlockCrystal) c.get()).aspect.getColor();
            event.register((stack, tintIndex) -> color, c.get());
        }
        // Ниторы: оба слоя модели предмета окрашиваются цветом красителя (itemBlockColourHandler оригинала)
        for (DeferredBlock<Block> n : BlocksTC.nitor.values()) {
            int color = ((BlockNitor) n.get()).dye.getMapColor().col;
            event.register((stack, tintIndex) -> color, n.get());
        }
        for (DeferredBlock<Block> candle : BlocksTC.candles.values()) {
            int color = ((BlockCandle) candle.get()).dye.getMapColor().col;
            event.register((stack, tintIndex) -> color, candle.get());
        }
    }

    private static java.util.List<DeferredBlock<Block>> CRYSTALS() {
        return java.util.List.of(BlocksTC.crystalAir, BlocksTC.crystalFire, BlocksTC.crystalWater, BlocksTC.crystalEarth,
                BlocksTC.crystalOrder, BlocksTC.crystalEntropy, BlocksTC.crystalTaint);
    }
}
