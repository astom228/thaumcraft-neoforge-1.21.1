package thaumcraft.client.lib.ender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import thaumcraft.common.config.ModConfig;

/** Порт ShaderHelper: шейдер ender и параметры времени/поворота, задаваемые при отрисовке буфера разлома. */
public final class ShaderHelper {
    private static ShaderInstance endShader;

    private ShaderHelper() {
    }

    /** RegisterShadersEvent вызывает при загрузке и перезагрузке ресурсов; жизненным циклом программы управляет Minecraft. */
    public static void setEndShader(ShaderInstance shader) {
        endShader = shader;
    }

    /** Supplier ShaderStateShard выполняется при отрисовке, а не при записи геометрии в общий буфер. */
    public static ShaderInstance getEndShader() {
        if (endShader == null || ModConfig.CONFIG_GRAPHICS.disableShaders.get()) {
            return GameRenderer.getPositionTexColorShader();
        }
        Minecraft mc = Minecraft.getInstance();
        var camera = mc.getCameraEntity();
        endShader.safeGetUniform("Time").set(camera == null ? 0 : camera.tickCount);
        endShader.safeGetUniform("Yaw").set(mc.player == null ? 0.0F : (float) Math.toRadians(mc.player.getYRot()));
        endShader.safeGetUniform("Pitch").set(mc.player == null ? 0.0F : -(float) Math.toRadians(mc.player.getXRot()));
        return endShader;
    }
}
