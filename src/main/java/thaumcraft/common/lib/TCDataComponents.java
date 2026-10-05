package thaumcraft.common.lib;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
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
}
