package thaumcraft.common.lib;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Теги вместо записей OreDictionary оригинала (api/OreDictionaryEntries): имя ore-словаря → тег c:<вид>/<материал>.
 * Например, ingotThaumium → c:ingots/thaumium, nuggetVoid → c:nuggets/void, oreAmber → c:ores/amber.
 */
public final class TCTags {
    public static final class Blocks {
        public static final TagKey<Block> ORES_AMBER = c("ores/amber");
        /** Блоки, сквозь которые не проходит «Разлом» (portableHoleBlackList оригинала: кровати, поршни, двери, адская печь). */
        public static final TagKey<Block> PORTABLE_HOLE_BLACKLIST = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(thaumcraft.Thaumcraft.MODID, "portable_hole_blacklist"));
        public static final TagKey<Block> ORES_CINNABAR = c("ores/cinnabar");
        public static final TagKey<Block> ORES_QUARTZ = c("ores/quartz");
        public static final TagKey<Block> STORAGE_BLOCKS_THAUMIUM = c("storage_blocks/thaumium");
        public static final TagKey<Block> STORAGE_BLOCKS_VOID = c("storage_blocks/void");
        public static final TagKey<Block> STORAGE_BLOCKS_BRASS = c("storage_blocks/brass");

        private static TagKey<Block> c(String path) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
        }
    }

    public static final class Items {
        public static final TagKey<Item> ORES_AMBER = c("ores/amber");
        public static final TagKey<Item> ORES_CINNABAR = c("ores/cinnabar");
        public static final TagKey<Item> ORES_QUARTZ = c("ores/quartz");
        public static final TagKey<Item> STORAGE_BLOCKS_THAUMIUM = c("storage_blocks/thaumium");
        public static final TagKey<Item> STORAGE_BLOCKS_VOID = c("storage_blocks/void");
        public static final TagKey<Item> STORAGE_BLOCKS_BRASS = c("storage_blocks/brass");
        public static final TagKey<Item> GEMS_AMBER = c("gems/amber");
        public static final TagKey<Item> QUICKSILVER = c("quicksilver");
        public static final TagKey<Item> INGOTS_THAUMIUM = c("ingots/thaumium");
        public static final TagKey<Item> INGOTS_VOID = c("ingots/void");
        public static final TagKey<Item> INGOTS_BRASS = c("ingots/brass");
        public static final TagKey<Item> NUGGETS_IRON = c("nuggets/iron");
        public static final TagKey<Item> NUGGETS_COPPER = c("nuggets/copper");
        public static final TagKey<Item> NUGGETS_TIN = c("nuggets/tin");
        public static final TagKey<Item> NUGGETS_SILVER = c("nuggets/silver");
        public static final TagKey<Item> NUGGETS_LEAD = c("nuggets/lead");
        public static final TagKey<Item> NUGGETS_QUICKSILVER = c("nuggets/quicksilver");
        public static final TagKey<Item> NUGGETS_THAUMIUM = c("nuggets/thaumium");
        public static final TagKey<Item> NUGGETS_VOID = c("nuggets/void");
        public static final TagKey<Item> NUGGETS_BRASS = c("nuggets/brass");
        public static final TagKey<Item> NUGGETS_QUARTZ = c("nuggets/quartz");
        public static final TagKey<Item> NUGGETS_MEAT = c("nuggets/meat");
        public static final TagKey<Item> PLATES_IRON = c("plates/iron");
        public static final TagKey<Item> PLATES_BRASS = c("plates/brass");
        public static final TagKey<Item> PLATES_THAUMIUM = c("plates/thaumium");
        public static final TagKey<Item> PLATES_VOID = c("plates/void");
        public static final TagKey<Item> CLUSTERS_IRON = c("clusters/iron");
        public static final TagKey<Item> CLUSTERS_GOLD = c("clusters/gold");
        public static final TagKey<Item> CLUSTERS_COPPER = c("clusters/copper");
        public static final TagKey<Item> CLUSTERS_TIN = c("clusters/tin");
        public static final TagKey<Item> CLUSTERS_SILVER = c("clusters/silver");
        public static final TagKey<Item> CLUSTERS_LEAD = c("clusters/lead");
        public static final TagKey<Item> CLUSTERS_CINNABAR = c("clusters/cinnabar");
        public static final TagKey<Item> CLUSTERS_QUARTZ = c("clusters/quartz");
        /** OreDictionary "nitor" — все ниторы. */
        public static final TagKey<Item> NITOR = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("thaumcraft", "nitor"));

        /** Слоты Curios (BaubleType оригинала → слоты Curios, DEVIATIONS 1). */
        public static final TagKey<Item> CURIOS_NECKLACE = curios("necklace");
        public static final TagKey<Item> CURIOS_RING = curios("ring");
        public static final TagKey<Item> CURIOS_BELT = curios("belt");
        public static final TagKey<Item> CURIOS_HEAD = curios("head");
        public static final TagKey<Item> CURIOS_CHARM = curios("charm");

        private static TagKey<Item> curios(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", path));
        }

        private static TagKey<Item> c(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        }
    }
}
