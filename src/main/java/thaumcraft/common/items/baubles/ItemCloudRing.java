package thaumcraft.common.items.baubles;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.fx.IFXDispatcher;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Кольцо облаков (порт ItemCloudRing): двойной прыжок в воздухе (на клиенте у своего игрока — движение игрока задаёт клиент)
 * и меньший урон от падения (PlayerEvents.onFallDamage). jumpList оригинала (статическая карта по имени игрока) — поле
 * состояния только для локального игрока: null — на земле, true — второй прыжок доступен, false — использован.
 */
public class ItemCloudRing extends Item implements ICurioItem {
    public ItemCloudRing(Properties props) {
        super(props);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity player = slotContext.entity();
        if (player.level().isClientSide && player instanceof Player p && p.isLocalPlayer()) {
            ClientJump.tick(p);
        }
    }

    /** Клиентская часть (в отдельном классе, чтобы на сервере не загружались клиентские классы). */
    private static final class ClientJump {
        private static Boolean jumpState = null;

        static void tick(Player player) {
            boolean spacePressed = Minecraft.getInstance().options.keyJump.consumeClick();
            if (spacePressed && jumpState == null) {
                jumpState = true;
            }
            if (spacePressed && !player.onGround() && player.noJumpDelay == 0 && Boolean.TRUE.equals(jumpState)) {
                IFXDispatcher.get().drawBamf(player.getX(), player.getY() + 0.5, player.getZ(), 1.0F, 1.0F, 1.0F, false, false,
                        net.minecraft.core.Direction.UP);
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                        0.1F, 1.0F + (float) player.getRandom().nextGaussian() * 0.05F, false);
                jumpState = false;
                Vec3 m = player.getDeltaMovement();
                double my = 0.75;
                if (player.hasEffect(MobEffects.JUMP)) {
                    my += (player.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.1F;
                }
                double mx = m.x;
                double mz = m.z;
                if (player.isSprinting()) {
                    float f = player.getYRot() * (float) (Math.PI / 180.0);
                    mx -= Mth.sin(f) * 0.2F;
                    mz += Mth.cos(f) * 0.2F;
                }
                player.setDeltaMovement(mx, my, mz);
                net.neoforged.neoforge.common.CommonHooks.onLivingJump(player);
            }
            if (player.onGround() && player.noJumpDelay == 0) {
                jumpState = null;
            }
        }
    }
}
