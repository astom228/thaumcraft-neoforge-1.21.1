package thaumcraft.common.lib;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;

/** Компоненты данных предметов (вместо NBT стеков оригинала). */
public class TCDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Thaumcraft.MODID);

    /** Аспекты в предмете (в оригинале — NBT "Aspects": [{key, amount}]). Значение изменяемое — читать и записывать копии. */
    public static final Supplier<DataComponentType<AspectList>> ASPECTS = COMPONENTS.registerComponentType("aspects",
            b -> b.persistent(AspectList.CODEC).networkSynchronized(AspectList.STREAM_CODEC.cast()));

    /**
     * Зачарования, которые требует «зачарованный заполнитель» (в оригинале — обычный NBT "ench" на стеке-заполнителе).
     * Хранятся ключи, а не Holder: стеки исследований создаются при запуске, до загрузки реестра зачарований мира.
     */
    public static final Supplier<DataComponentType<Map<ResourceKey<Enchantment>, Integer>>> PLACEHOLDER_ENCHANTMENTS = COMPONENTS.registerComponentType(
            "placeholder_enchantments",
            b -> b.persistent(Codec.unboundedMap(ResourceKey.codec(Registries.ENCHANTMENT), Codec.INT))
                    .networkSynchronized(ByteBufCodecs.map(LinkedHashMap::new, ResourceKey.streamCodec(Registries.ENCHANTMENT), ByteBufCodecs.VAR_INT)));

    /** Фильтр банки — тег аспекта с ярлыка (в оригинале — NBT "AspectFilter" у предмета-банки). */
    public static final Supplier<DataComponentType<String>> ASPECT_FILTER = COMPONENTS.registerComponentType("aspect_filter",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** Опыт мозга в банке (в оригинале — NBT "xp" у предмета). */
    public static final Supplier<DataComponentType<Integer>> BRAIN_XP = COMPONENTS.registerComponentType("brain_xp",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Связь зеркала или ручного зеркала (в оригинале — NBT linkX/linkY/linkZ/linkDim; предмет-зеркало со связью — metadata 1). */
    public static final Supplier<DataComponentType<GlobalPos>> MIRROR_LINK = COMPONENTS.registerComponentType("mirror_link",
            b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    /** Наполняющие зачарования предмета (в оригинале — NBT "infench"). */
    public static final Supplier<DataComponentType<java.util.List<thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.Entry>>> INFUSION_ENCHANTMENTS =
            COMPONENTS.registerComponentType("infusion_enchantments", b -> b.persistent(thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.Entry.CODEC.listOf())
                    .networkSynchronized(thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.Entry.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** Искажение от предмета (в оригинале — NBT "TC.WARP"); учитывается системой Искажения (этап 15). */
    public static final Supplier<DataComponentType<Integer>> WARPING = COMPONENTS.registerComponentType("warping",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Заряд рунической защиты (в оригинале — NBT "TC.RUNIC"); действие защиты — этап 13. */
    public static final Supplier<DataComponentType<Integer>> RUNIC_CHARGE = COMPONENTS.registerComponentType("runic_charge",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Заряд Вис предмета IRechargable (в оригинале — NBT "tc.charge"). */
    public static final Supplier<DataComponentType<Integer>> CHARGE = COMPONENTS.registerComponentType("charge",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Запас хода сапог странника в секундах до следующего списания заряда (в оригинале — NBT "energy"). */
    public static final Supplier<DataComponentType<Integer>> TRAVELLER_ENERGY = COMPONENTS.registerComponentType("traveller_energy",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Вид оберега зелёного сердца: 0 — обычный, 1 — «Дарующий жизнь», 2 — «Поддерживающий» (в оригинале — NBT "type"). */
    public static final Supplier<DataComponentType<Integer>> VERDANT_TYPE = COMPONENTS.registerComponentType("verdant_type",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Ориентация квадрата лопаты Землекопа, 0–2 (в оригинале — NBT "or"). */
    public static final Supplier<DataComponentType<Integer>> SHOVEL_ORIENTATION = COMPONENTS.registerComponentType("shovel_orientation",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Маска шлема «Таумиумной крепости»: 0–2 (в оригинале — NBT "mask"). */
    public static final Supplier<DataComponentType<Integer>> FORTRESS_MASK = COMPONENTS.registerComponentType("fortress_mask",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Очки откровения в шлеме «Таумиумной крепости» (в оригинале — NBT "goggles"). */
    public static final Supplier<DataComponentType<Boolean>> FORTRESS_GOGGLES = COMPONENTS.registerComponentType("fortress_goggles",
            b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Крюкомёт заряжен — крюк в полёте или держит (в оригинале — NBT "loaded"). */
    public static final Supplier<DataComponentType<Boolean>> GRAPPLE_LOADED = COMPONENTS.registerComponentType("grapple_loaded",
            b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Аспект, нарисованный на знамени (в оригинале — NBT "aspect" у предмета знамени). */
    public static final Supplier<DataComponentType<String>> BANNER_ASPECT = COMPONENTS.registerComponentType("banner_aspect",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** Пакет фокуса (в оригинале — NBT "package" у фокуса): формат FocusPackage.serialize. Значение не изменять — только заменять. */
    public static final Supplier<DataComponentType<net.minecraft.nbt.CompoundTag>> FOCUS_PACKAGE = COMPONENTS.registerComponentType("focus_package",
            b -> b.persistent(net.minecraft.nbt.CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));

    /** Фокус в перчатке (в оригинале — NBT "focus"): один предмет. */
    public static final Supplier<DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> CASTER_FOCUS =
            COMPONENTS.registerComponentType("caster_focus", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
                    .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));

    /** Область «Плана» у перчатки: размеры x, y, z и выбранная ось (в оригинале — NBT "areax/areay/areaz/aread"). */
    public record CasterArea(int x, int y, int z, int dim) {
        public static final Codec<CasterArea> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(CasterArea::x), Codec.INT.fieldOf("y").forGetter(CasterArea::y),
                Codec.INT.fieldOf("z").forGetter(CasterArea::z), Codec.INT.fieldOf("dim").forGetter(CasterArea::dim)).apply(i, CasterArea::new));
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, CasterArea> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(ByteBufCodecs.VAR_INT, CasterArea::x, ByteBufCodecs.VAR_INT, CasterArea::y,
                        ByteBufCodecs.VAR_INT, CasterArea::z, ByteBufCodecs.VAR_INT, CasterArea::dim, CasterArea::new);
    }

    public static final Supplier<DataComponentType<CasterArea>> CASTER_AREA = COMPONENTS.registerComponentType("caster_area",
            b -> b.persistent(CasterArea.CODEC).networkSynchronized(CasterArea.STREAM_CODEC));

    /** Содержимое сумки для фокусов (в оригинале — NBT "Items" сумки): 18 слотов. */
    public static final Supplier<DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> POUCH_CONTENTS =
            COMPONENTS.registerComponentType("pouch_contents", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
                    .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));

    /** Блок, выбранный перчаткой для фокуса с выбором блока (в оригинале — NBT "picked"). */
    public static final Supplier<DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> CASTER_PICKED =
            COMPONENTS.registerComponentType("caster_picked", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
                    .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));
}
