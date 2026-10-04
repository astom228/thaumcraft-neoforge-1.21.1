package thaumcraft.common.lib.utils;

import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.Thaumcraft;

/**
 * Перевод ссылок на предметы 1.12 («minecraft:stonebrick» + metadata 1) в предметы 1.21 штатным DataFixer игры
 * (данные мира 1.12.2 → текущая версия). Используется для переноса таблиц оригинала (ConfigAspects и др.).
 * Metadata 32767 («любая») раскрывается в metadata 0..15, как цикл ThaumcraftApi.exists/getObjectTags оригинала.
 */
public final class LegacyItems {
    /** Версия данных Minecraft 1.12.2. */
    private static final int DATA_VERSION_1_12_2 = 1343;
    public static final int WILDCARD = 32767;
    private static final Map<String, Item> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    /** Предмет 1.21 для id 1.12 и metadata (null, если такого нет). */
    public static Item item(String legacyId, int meta) {
        String key = legacyId + "#" + meta;
        Item cached = CACHE.get(key);
        if (cached != null) {
            return cached == Items.AIR ? null : cached;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", legacyId.contains(":") ? legacyId : "minecraft:" + legacyId);
        tag.putByte("Count", (byte) 1);
        tag.putShort("Damage", (short) meta);
        Item result = Items.AIR;
        try {
            Dynamic<Tag> fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, tag),
                    DATA_VERSION_1_12_2, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            String id = fixed.get("id").asString("");
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
                result = BuiltInRegistries.ITEM.get(rl);
            }
        } catch (Exception e) {
            Thaumcraft.log.debug("Failed to convert 1.12 item {}", key, e);
        }
        CACHE.put(key, result);
        if (result == Items.AIR && WARNED.add(key)) {
            Thaumcraft.log.warn("1.12 item {} has no 1.21 equivalent", key);
        }
        return result == Items.AIR ? null : result;
    }

    /** Стеки 1.21 для ссылки 1.12 (с раскрытием 32767, без повторов). */
    public static List<ItemStack> stacks(String legacyId, int meta) {
        Set<Item> items = new LinkedHashSet<>();
        if (meta == WILDCARD) {
            for (int m = 0; m < 16; m++) {
                Item it = itemQuiet(legacyId, m);
                if (it != null) {
                    items.add(it);
                }
            }
            if (items.isEmpty()) {
                item(legacyId, 0); // для предупреждения в логе
            }
        } else {
            Item it = item(legacyId, meta);
            if (it != null) {
                items.add(it);
            }
        }
        List<ItemStack> out = new ArrayList<>();
        for (Item it : items) {
            out.add(new ItemStack(it));
        }
        return out;
    }

    /** Без предупреждения: при раскрытии 32767 многие metadata просто не существуют. */
    private static Item itemQuiet(String legacyId, int meta) {
        String key = legacyId + "#" + meta;
        WARNED.add(key);
        return item(legacyId, meta);
    }

    private LegacyItems() {
    }
}
