package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.IScanThing;
import thaumcraft.common.items.curios.ItemCelestialNotes;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Взгляд таумометром в небо Верхнего мира (после «Наблюдения за небом»): солнце, луна (по фазе) или ночью звёзды в одной из
 * четырёх сторон — раз в игровой день каждое; тратит бумагу (нужны письменные принадлежности) и даёт небесные заметки.
 */
public class ScanSky implements IScanThing {
    /** Смотрит ли игрок на светило (иначе ночью — на звёзды). */
    private static int[] aim(Player player) {
        int yaw = (int) (player.getYRot() + 90.0F) % 360;
        int pitch = (int) Math.abs(player.getXRot());
        int ca = (int) ((player.level().getTimeOfDay(0.0F) + 0.25) * 360.0) % 360;
        boolean night = ca > 180;
        boolean inRangeYaw;
        boolean inRangePitch;
        if (night) {
            ca -= 180;
        }
        if (ca > 90) {
            inRangeYaw = Math.abs(Math.abs(yaw) - 180) < 10;
            inRangePitch = Math.abs(180 - ca - pitch) < 7;
        } else {
            inRangeYaw = Math.abs(yaw) < 10;
            inRangePitch = Math.abs(ca - pitch) < 7;
        }
        return new int[] {inRangeYaw && inRangePitch ? 1 : 0, night ? 1 : 0};
    }

    private static boolean canScan(Player player, Object obj) {
        return obj == null && !(player.getXRot() > 0.0F) && player.level().canSeeSky(player.blockPosition().above())
                && ThaumcraftCapabilities.knowsResearchStrict(player, "CELESTIALSCANNING");
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        if (canScan(player, obj) && player.level().dimension() == Level.OVERWORLD) {
            int[] a = aim(player);
            return a[0] == 1 || a[1] == 1;
        }
        return false;
    }

    /** Как в оригинале, здесь проверка измерения не повторяется (checkThing её уже выполнил). */
    @Override
    public void onSuccess(Player player, Object object) {
        if (!canScan(player, object)) {
            return;
        }
        int[] a = aim(player);
        boolean night = a[1] == 1;
        int worldDay = (int) (player.level().getGameTime() / 24000L);
        String pk = "CEL_" + worldDay + "_";
        String key;
        int variant;
        if (a[0] == 1) {
            int phase = player.level().getMoonPhase();
            key = pk + (night ? "Moon" + phase : "Sun");
            variant = night ? 5 + phase : 0;
        } else if (night) {
            Direction face = player.getDirection();
            int num = face.get3DDataValue() - 2;
            key = pk + "Star" + num;
            variant = 1 + num;
        } else {
            return;
        }
        if (ThaumcraftCapabilities.knowsResearch(player, key)) {
            player.displayClientMessage(Component.translatable("tc.celestial.fail.1", ""), true);
            return;
        }
        if (InventoryUtils.isPlayerCarryingAmount(player, new ItemStack(Items.PAPER), true)
                && InventoryUtils.isPlayerCarryingAmount(player, new ItemStack(ItemsTC.scribingTools.get()), true)) {
            InventoryUtils.consumePlayerItem(player, new ItemStack(Items.PAPER), true, true);
            ItemStack stack = new ItemStack(ItemsTC.celestialNotes.get(ItemCelestialNotes.VARIANTS[variant]).get());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            ThaumcraftApi.internalMethods.progressResearch(player, key);
        } else {
            player.displayClientMessage(Component.translatable("tc.celestial.fail.2", ""), true);
        }
        this.cleanResearch(player, pk);
    }

    /** Убрать отметки «CEL_» прошлых дней. */
    private void cleanResearch(Player player, String pk) {
        List<String> list = new ArrayList<>();
        for (String key : ThaumcraftCapabilities.getKnowledge(player).getResearchList()) {
            if (key.startsWith("CEL_") && !key.startsWith(pk)) {
                list.add(key);
            }
        }
        for (String key : list) {
            ThaumcraftCapabilities.getKnowledge(player).removeResearch(key);
        }
        ResearchManager.syncList.add(player.getUUID());
    }

    @Override
    public String getResearchKey(Player player, Object object) {
        return "";
    }
}
