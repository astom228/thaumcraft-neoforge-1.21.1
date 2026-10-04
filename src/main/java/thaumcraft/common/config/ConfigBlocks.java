package thaumcraft.common.config;

import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

/** Регистр блоков. Сами блоки объявлены в {@link thaumcraft.api.blocks.BlocksTC}. */
public class ConfigBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Thaumcraft.MODID);
}
