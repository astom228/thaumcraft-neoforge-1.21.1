package thaumcraft.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.ResearchEntry;

/** Всплывающее сообщение «Исследование завершено!» (порт ResearchToast), 5 секунд. */
public class ResearchToast implements Toast {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");
    final ResearchEntry entry;

    public ResearchToast(ResearchEntry entry) {
        this.entry = entry;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toastComponent, long delta) {
        Font font = toastComponent.getMinecraft().font;
        g.blit(TEX, 0, 0, 0, 224, 160, 32);
        GuiResearchBrowser.drawResearchIcon(g, this.entry, 6, 8, 0.0F, false);
        g.drawString(font, I18n.get("research.complete"), 30, 7, 10631665, false);
        String s = I18n.get(this.entry.getName());
        float w = font.width(s);
        if (w > 124.0F) {
            w = 124.0F / w;
            g.pose().pushPose();
            g.pose().translate(30.0F, 18.0F, 0.0F);
            g.pose().scale(w, w, w);
            g.drawString(font, s, 0, 0, 16755465, false);
            g.pose().popPose();
        } else {
            g.drawString(font, s, 30, 18, 16755465, false);
        }
        return delta < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }
}
