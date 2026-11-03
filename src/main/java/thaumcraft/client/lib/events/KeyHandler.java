package thaumcraft.client.lib.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.lib.network.misc.PacketFocusChangeToServer;
import thaumcraft.common.lib.network.misc.PacketItemKeyToServer;

/**
 * Клавиши перчатки (порт KeyHandler): F — удерживать для радиального меню фокусов (с Shift — снять фокус), G — «разное»
 * (с Ctrl / Shift — варианты). Только клиент. F с колокольчиком голема в оригинале
 * шлёт PacketItemKeyToServer(0), который сервер не обрабатывает, — не перенесено.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class KeyHandler {
    public static final KeyMapping keyF = new KeyMapping("key.thaumcraft.change_focus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F, "key.categories.thaumcraft");
    public static final KeyMapping keyG = new KeyMapping("key.thaumcraft.misc_toggle", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.thaumcraft");
    private static boolean keyPressedF = false;
    private static boolean keyPressedG = false;
    public static boolean radialActive = false;
    public static boolean radialLock = false;
    public static long lastPressF = 0L;
    public static long lastPressG = 0L;

    /** playerTick (START) оригинала. */
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        boolean inGame = mc.screen == null;
        if (keyF.isDown()) {
            if (inGame) {
                if (!keyPressedF) {
                    lastPressF = System.currentTimeMillis();
                    radialLock = false;
                }
                if (!radialLock && (player.getMainHandItem().getItem() instanceof ICaster || player.getOffhandItem().getItem() instanceof ICaster)) {
                    if (player.isShiftKeyDown()) {
                        PacketDistributor.sendToServer(new PacketFocusChangeToServer("REMOVE"));
                    } else {
                        radialActive = true;
                    }
                }
                keyPressedF = true;
            }
        } else {
            radialActive = false;
            if (keyPressedF) {
                lastPressF = System.currentTimeMillis();
            }
            keyPressedF = false;
        }
        if (keyG.isDown()) {
            if (inGame) {
                if (!keyPressedG) {
                    lastPressG = System.currentTimeMillis();
                    PacketDistributor.sendToServer(new PacketItemKeyToServer(1, Screen.hasControlDown() ? 1 : (Screen.hasShiftDown() ? 2 : 0)));
                }
                keyPressedG = true;
            }
        } else {
            if (keyPressedG) {
                lastPressG = System.currentTimeMillis();
            }
            keyPressedG = false;
        }
    }
}
