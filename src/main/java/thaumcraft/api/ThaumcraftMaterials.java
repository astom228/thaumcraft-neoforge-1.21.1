package thaumcraft.api;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.api.items.ItemsTC;

/**
 * Материалы инструментов и брони (порт ThaumcraftMaterials). Уровень добычи 1.12 → тег «неподходящих» блоков:
 * 3 — как алмаз, 4 — как незерит. Прочность брони — множитель оригинала (ArmorItem.Type.getDurability), защита
 * {ботинки, поножи, нагрудник, шлем} — как в оригинале. Текстуры брони задают сами предметы (getArmorTexture),
 * слои материала нужны для числа проходов рендера и окраски.
 */
public class ThaumcraftMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "thaumcraft");

    public static final Tier TOOLMAT_THAUMIUM = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 500, 7.0F, 2.5F, 22,
            () -> Ingredient.of(ItemsTC.ingotThaumium.get()));
    public static final Tier TOOLMAT_VOID = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 150, 8.0F, 3.0F, 10,
            () -> Ingredient.of(ItemsTC.ingotVoid.get()));
    public static final Tier TOOLMAT_ELEMENTAL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1500, 9.0F, 3.0F, 18,
            () -> Ingredient.of(ItemsTC.ingotThaumium.get()));

    /** Множители прочности брони (maxDamageFactor 1.12). */
    public static final int DURABILITY_THAUMIUM = 25;
    public static final int DURABILITY_SPECIAL = 25;
    public static final int DURABILITY_VOID = 10;
    public static final int DURABILITY_VOIDROBE = 18;
    public static final int DURABILITY_FORTRESS = 40;

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_THAUMIUM = armor("thaumium", new int[] {2, 5, 6, 2}, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 1.0F, () -> Ingredient.of(ItemsTC.ingotThaumium.get()), false);
    /** Сапоги странника (чинятся кожей — задаёт предмет). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_SPECIAL = armor("special", new int[] {1, 2, 3, 1}, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, () -> Ingredient.EMPTY, false);
    /** Роба тауматурга: в оригинале тот же SPECIAL; отдельный материал в порте — из-за окрашиваемого слоя и слоя "_overlay". */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_ROBE = armor("robe", new int[] {1, 2, 3, 1}, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, () -> Ingredient.of(ItemsTC.fabric.get()), true);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_VOID = armor("void", new int[] {3, 6, 8, 3}, 10,
            SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, () -> Ingredient.of(ItemsTC.ingotVoid.get()), false);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_VOIDROBE = armor("voidrobe", new int[] {4, 7, 9, 4}, 10,
            SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, () -> Ingredient.of(ItemsTC.ingotVoid.get()), true);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_FORTRESS = armor("fortress", new int[] {3, 6, 7, 3}, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 3.0F, () -> Ingredient.EMPTY, false);

    /**
     * Доспех «Таумиумной крепости»: в 1.12 — ISpecialArmor (поглощение защита/25 и т.д., а прочность материала не учитывалась —
     * вместо неё +1 к стойкости за каждую часть). Здесь — обычная броня с теми же числами защиты и стойкостью 1 на часть
     * (DECISIONS 107); ARMORMAT_FORTRESS со стойкостью 3 — у брони претора культа (обычная броня и в оригинале).
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_FORTRESS_SPECIAL = armor("fortress_special", new int[] {3, 6, 7, 3}, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 1.0F, () -> Ingredient.of(ItemsTC.ingotThaumium.get()), false);

    /** reduction — {ботинки, поножи, нагрудник, шлем}, как в EnumHelper.addArmorMaterial 1.12; layered — второй слой "_overlay" (роба). */
    private static DeferredHolder<ArmorMaterial, ArmorMaterial> armor(String name, int[] reduction, int enchantability, Holder<SoundEvent> sound,
                                                                      float toughness, Supplier<Ingredient> repair, boolean layered) {
        return ARMOR_MATERIALS.register(name, () -> {
            EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
            defense.put(ArmorItem.Type.BOOTS, reduction[0]);
            defense.put(ArmorItem.Type.LEGGINGS, reduction[1]);
            defense.put(ArmorItem.Type.CHESTPLATE, reduction[2]);
            defense.put(ArmorItem.Type.HELMET, reduction[3]);
            defense.put(ArmorItem.Type.BODY, reduction[2]);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thaumcraft", name);
            List<ArmorMaterial.Layer> layers = layered
                    ? List.of(new ArmorMaterial.Layer(id, "", true), new ArmorMaterial.Layer(id, "_overlay", false))
                    : List.of(new ArmorMaterial.Layer(id));
            return new ArmorMaterial(defense, enchantability, sound, repair, layers, toughness, 0.0F);
        });
    }
}
