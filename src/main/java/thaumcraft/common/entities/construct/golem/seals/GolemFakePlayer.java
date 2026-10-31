package thaumcraft.common.entities.construct.golem.seals;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.api.golems.IGolemAPI;

/**
 * Игрок-заместитель голема для работы с блоками («FakeThaumcraftGolem» оригинала; там — профиль без UUID, здесь —
 * постоянный UUID, как требует NeoForge). Стоит там же, где голем; руки очищаются после каждого действия.
 */
public final class GolemFakePlayer {
    private static final GameProfile PROFILE = new GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes()), "FakeThaumcraftGolem");

    private GolemFakePlayer() {
    }

    public static FakePlayer get(Level world, IGolemAPI golem) {
        FakePlayer fp = FakePlayerFactory.get((ServerLevel) world, PROFILE);
        fp.setPos(golem.getGolemEntity().getX(), golem.getGolemEntity().getY(), golem.getGolemEntity().getZ());
        fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        fp.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        return fp;
    }

    /**
     * Разрешено ли ломать блок: событие ломания от имени заместителя (в оригинале големы ломали без события). Нужно, чтобы
     * защита территорий могла запретить чужим големам ломать постройки (DEVIATIONS).
     */
    public static boolean canBreak(Level world, FakePlayer fp, BlockPos pos) {
        return !CommonHooks.fireBlockBreak(world, GameType.SURVIVAL, fp, pos, world.getBlockState(pos)).isCanceled();
    }
}
