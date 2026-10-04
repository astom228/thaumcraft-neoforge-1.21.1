package thaumcraft.common.config;

import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

/** Регистр предметов. Сами предметы объявлены в {@link thaumcraft.api.items.ItemsTC}. */
public class ConfigItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Thaumcraft.MODID);
}
