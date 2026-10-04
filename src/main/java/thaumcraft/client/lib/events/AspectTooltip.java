package thaumcraft.client.lib.events;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.lib.UtilsFX;

/**
 * Строка значков аспектов в подсказке предмета. В оригинале в подсказку добавлялись строки из пробелов шириной
 * 18 пикселей на аспект и высотой ceil(18/9) = 2 строки, а значки рисовались поверх фона (HudHandler.renderAspectsInGui).
 */
public record AspectTooltip(AspectList aspects) implements TooltipComponent {

    public static class Client implements ClientTooltipComponent {
        private final AspectList tags;

        public Client(AspectTooltip data) {
            this.tags = data.aspects();
        }

        private int count() {
            int index = 0;
            for (Aspect tag : this.tags.getAspects()) {
                if (tag != null) {
                    index++;
                }
            }
            return index;
        }

        @Override
        public int getHeight() {
            return 20;
        }

        @Override
        public int getWidth(Font font) {
            return this.count() * 18;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics g) {
            int index = 0;
            for (Aspect tag : this.tags.getAspectsSortedByAmount()) {
                if (tag != null) {
                    UtilsFX.drawTag(g, x + index * 18, y + 2, tag, this.tags.getAmount(tag), 0);
                    index++;
                }
            }
        }
    }
}
