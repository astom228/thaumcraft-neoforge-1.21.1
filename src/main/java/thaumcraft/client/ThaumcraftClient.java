package thaumcraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import thaumcraft.Thaumcraft;

/** Клиентская точка входа (в оригинале — ClientProxy). */
@Mod(value = Thaumcraft.MODID, dist = Dist.CLIENT)
public class ThaumcraftClient {
    public ThaumcraftClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
