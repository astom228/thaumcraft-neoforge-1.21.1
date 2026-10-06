package thaumcraft.client.gui;

import net.minecraft.client.Minecraft;

/**
 * Открытие экранов книги из общего кода (openGui с клиентским экраном в оригинале). Вызывается только на клиенте
 * (ветка isClientSide), поэтому сервер этот класс не загружает.
 */
public final class ResearchScreens {
    public static void openBrowser() {
        Minecraft.getInstance().setScreen(new GuiResearchBrowser());
    }

    private ResearchScreens() {
    }
}
