package thaumcraft.api.golems.parts;

import net.minecraft.world.item.ItemStack;

/**
 * Преобразования при отрисовке детали (в 1.12 PartModel вызывал GlStateManager напрямую). Реализуется клиентом поверх
 * PoseStack — так класс детали не зависит от клиентских классов и загружается на сервере.
 */
public interface IPartTransform {
    void translate(double x, double y, double z);

    void rotate(float degrees, float x, float y, float z);

    void scale(float x, float y, float z);

    void push();

    void pop();

    /** Отрисовать предмет как на голове (ItemCameraTransforms.TransformType.HEAD). */
    void renderItemHead(ItemStack stack);
}
