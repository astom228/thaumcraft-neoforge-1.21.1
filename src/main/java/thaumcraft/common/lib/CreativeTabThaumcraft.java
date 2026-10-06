package thaumcraft.common.lib;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.items.ItemGenericEssentiaContainer;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.items.curios.ItemThaumonomicon;

/** Вкладка «Thaumcraft» в творческом режиме. Иконка — очки откровения, как в оригинале. */
public final class CreativeTabThaumcraft {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Thaumcraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TABTC = TABS.register("thaumcraft", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumcraft"))
            .icon(() -> new ItemStack(ItemsTC.goggles.get()))
            .displayItems((params, output) -> ConfigItems.ITEMS.getEntries().forEach(item -> {
                // ItemGenericEssentiaContainer.getSubItems: по стеку на каждый аспект
                if (item.get() instanceof ItemThaumonomicon book && book.isCheat()) {
                    // ItemThaumonomicon.getSubItems: «шпаргалка» — только если разрешена в конфиге
                    if (ModConfig.CONFIG_MISC.allowCheatSheet.get()) {
                        output.accept(item.get());
                    }
                } else if (item.get() instanceof ItemGenericEssentiaContainer container) {
                    container.addCreativeVariants(output);
                } else {
                    output.accept(item.get());
                }
            }))
            .build());
}
