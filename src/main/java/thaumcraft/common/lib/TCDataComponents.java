package thaumcraft.common.lib;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;

/** Компоненты данных предметов (вместо NBT стеков оригинала). */
public class TCDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Thaumcraft.MODID);

    /** Аспекты в предмете (в оригинале — NBT "Aspects": [{key, amount}]). Значение изменяемое — читать и записывать копии. */
    public static final Supplier<DataComponentType<AspectList>> ASPECTS = COMPONENTS.registerComponentType("aspects",
            b -> b.persistent(AspectList.CODEC).networkSynchronized(AspectList.STREAM_CODEC.cast()));
}
