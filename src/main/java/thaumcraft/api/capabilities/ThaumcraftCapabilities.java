package thaumcraft.api.capabilities;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.lib.capabilities.ThaumcraftAttachments;

/** Доступ к знаниям и Искажению игрока (в оригинале — через capabilities, здесь — Data Attachments). */
public class ThaumcraftCapabilities {
    public static IPlayerKnowledge getKnowledge(@Nonnull Player player) {
        return player.getData(ThaumcraftAttachments.KNOWLEDGE);
    }

    public static IPlayerWarp getWarp(@Nonnull Player player) {
        return player.getData(ThaumcraftAttachments.WARP);
    }

    /**
     * Знает ли игрок исследования. Поддерживает "a&&b" и "a||b".
     * В оригинале "||" передавался в String.split как регулярное выражение (делил строку посимвольно) — исправлено (DEVIATIONS.md).
     */
    public static boolean knowsResearch(@Nonnull Player player, @Nonnull String... research) {
        for (String r : research) {
            if (r.contains("&&")) {
                if (!knowsResearch(player, r.split("&&"))) {
                    return false;
                }
            } else if (r.contains("||")) {
                for (String str : r.split("\\|\\|")) {
                    if (knowsResearch(player, str)) {
                        return true;
                    }
                }
            } else if (!getKnowledge(player).isResearchKnown(r)) {
                return false;
            }
        }
        return true;
    }

    /** То же, но без "@стадия" требует завершённого исследования. */
    public static boolean knowsResearchStrict(@Nonnull Player player, @Nonnull String... research) {
        for (String r : research) {
            if (r.contains("&&")) {
                if (!knowsResearchStrict(player, r.split("&&"))) {
                    return false;
                }
            } else if (r.contains("||")) {
                for (String str : r.split("\\|\\|")) {
                    if (knowsResearchStrict(player, str)) {
                        return true;
                    }
                }
            } else if (r.contains("@")) {
                if (!getKnowledge(player).isResearchKnown(r)) {
                    return false;
                }
            } else if (!getKnowledge(player).isResearchComplete(r)) {
                return false;
            }
        }
        return true;
    }
}
