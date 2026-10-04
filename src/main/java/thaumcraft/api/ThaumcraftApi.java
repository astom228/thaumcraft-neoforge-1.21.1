package thaumcraft.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.common.lib.InternalMethodHandler;

/** Порт ThaumcraftApi (содержимое добавляется по этапам). */
public class ThaumcraftApi {
    /** В оригинале задаётся в CommonProxy.preInit. */
    public static IInternalMethodHandler internalMethods = new InternalMethodHandler();

    // --- Аспекты предметов -------------------------------------------------------------------------------------------

    /**
     * Есть ли у предмета явно заданные аспекты. В оригинале таблица искалась по строке, а ключи были числами,
     * поэтому метод почти всегда возвращал false — это влияет на registerComplexObjectTag и сохранено 1:1.
     */
    public static boolean exists(ItemStack item) {
        return false;
    }

    public static void registerObjectTag(ItemStack item, AspectList aspects) {
        if (aspects == null) {
            aspects = new AspectList();
        }
        try {
            CommonInternals.objectTags().put(CommonInternals.generateUniqueItemstackId(item), aspects);
        } catch (Exception ignored) {
        }
    }

    /** В оригинале — по имени OreDictionary; в порте — по тегу предметов (каждому предмету тега). */
    public static void registerObjectTag(TagKey<Item> tag, AspectList aspects) {
        if (aspects == null) {
            aspects = new AspectList();
        }
        for (ItemStack ore : ThaumcraftApiHelper.getOresWithWildCards(tag)) {
            try {
                ItemStack oc = ore.copy();
                oc.setCount(1);
                registerObjectTag(oc, aspects.copy());
            } catch (Exception ignored) {
            }
        }
    }

    public static void registerComplexObjectTag(ItemStack item, AspectList aspects) {
        if (!exists(item)) {
            AspectList tmp = AspectHelper.generateTags(item);
            if (tmp != null && tmp.size() > 0) {
                for (Aspect tag : tmp.getAspects()) {
                    aspects.add(tag, tmp.getAmount(tag));
                }
            }
            registerObjectTag(item, aspects);
        } else {
            AspectList tmp = AspectHelper.getObjectAspects(item);
            for (Aspect tag : aspects.getAspects()) {
                tmp.merge(tag, tmp.getAmount(tag));
            }
            registerObjectTag(item, tmp);
        }
    }

    public static void registerComplexObjectTag(TagKey<Item> tag, AspectList aspects) {
        if (aspects == null) {
            aspects = new AspectList();
        }
        for (ItemStack ore : ThaumcraftApiHelper.getOresWithWildCards(tag)) {
            try {
                ItemStack oc = ore.copy();
                oc.setCount(1);
                registerComplexObjectTag(oc, aspects.copy());
            } catch (Exception ignored) {
            }
        }
    }

    // --- Аспекты существ ---------------------------------------------------------------------------------------------

    /** entityName — id типа сущности 1.21 (в оригинале — имя EntityList 1.12, см. ConfigAspects). */
    public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT... nbt) {
        CommonInternals.scanEntities.add(new EntityTags(entityName, aspects, nbt));
    }

    /** Значение NBT по ключу (порт ThaumcraftApiHelper.getNBTDataFromId). */
    public static Object getNBTDataFromId(CompoundTag tag, String key) {
        Tag t = tag.get(key);
        if (t instanceof NumericTag n) {
            return switch (t.getId()) {
                case Tag.TAG_BYTE -> n.getAsByte();
                case Tag.TAG_SHORT -> n.getAsShort();
                case Tag.TAG_INT -> n.getAsInt();
                case Tag.TAG_LONG -> n.getAsLong();
                case Tag.TAG_FLOAT -> n.getAsFloat();
                case Tag.TAG_DOUBLE -> n.getAsDouble();
                default -> null;
            };
        }
        if (t instanceof StringTag s) {
            return s.getAsString();
        }
        return null;
    }

    public static class EntityTags {
        public String entityName;
        public EntityTagsNBT[] nbts;
        public AspectList aspects;

        public EntityTags(String entityName, AspectList aspects, EntityTagsNBT... nbts) {
            this.entityName = entityName;
            this.nbts = nbts;
            this.aspects = aspects;
        }
    }

    public static class EntityTagsNBT {
        public String name;
        public Object value;

        public EntityTagsNBT(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

}
