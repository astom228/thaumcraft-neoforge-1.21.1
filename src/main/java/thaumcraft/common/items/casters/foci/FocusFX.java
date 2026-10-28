package thaumcraft.common.items.casters.foci;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

/** Общее для эффектов фокусов: вспышка попадания (игрокам в 64 блоках), источники урона, перчатка в руках. */
final class FocusFX {
    private FocusFX() {
    }

    static void impact(Level world, Vec3 at, String key) {
        impact(world, at.x, at.y, at.z, key);
    }

    static void impact(Level world, double x, double y, double z, String key) {
        if (world instanceof ServerLevel sl) {
            PacketDistributor.sendToPlayersNear(sl, null, x, y, z, 64.0, new PacketFXFocusPartImpact(x, y, z, new String[] {key}));
        }
    }

    /** DamageSource с прямым и исходным виновником (EntityDamageSourceIndirect 1.12). */
    static DamageSource source(Level world, ResourceKey<DamageType> type, Entity direct, Entity causing) {
        return new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, causing);
    }

    /** Перчатка в основной, иначе во второй руке (у существа без перчатки — пусто). */
    static ItemStack casterStack(LivingEntity caster) {
        if (caster == null) {
            return ItemStack.EMPTY;
        }
        if (caster.getMainHandItem().getItem() instanceof ICaster) {
            return caster.getMainHandItem();
        }
        if (caster.getOffhandItem().getItem() instanceof ICaster) {
            return caster.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    static Player player(LivingEntity caster) {
        return caster instanceof Player p ? p : null;
    }
}
