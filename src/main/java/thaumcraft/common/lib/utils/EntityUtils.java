package thaumcraft.common.lib.utils;

import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Порт EntityUtils (методы добавляются по мере надобности). */
public class EntityUtils {
    /**
     * Существо, на которое смотрит entity, в пределах range (ближе minrange не считается), с запасом padding;
     * nonCollide — учитывать и существ без столкновений. Существа за блоками не видны. Порт getPointedEntity оригинала.
     */
    public static Entity getPointedEntity(Level world, Entity entity, double minrange, double range, float padding, boolean nonCollide) {
        Vec3 hitVec = entity.position().add(0.0, entity.getEyeHeight(), 0.0);
        Vec3 lookVec = entity.getLookAngle();
        Entity pointedEntity = null;
        double d = range;
        Vec3 vec3d2 = hitVec.add(lookVec.x * d, lookVec.y * d, lookVec.z * d);
        float f1 = padding;
        AABB bb = entity.getBoundingBox();
        List<Entity> list = world.getEntities(entity, bb.expandTowards(lookVec.x * d, lookVec.y * d, lookVec.z * d).inflate(f1, f1, f1));
        double d2 = 0.0;
        for (Entity e : list) {
            if (!(hitVec.distanceTo(e.position()) < minrange) && (e.isPickable() || nonCollide)
                    && world.clip(new ClipContext(hitVec, new Vec3(e.getX(), e.getY() + e.getEyeHeight(), e.getZ()), ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS) {
                float f2 = Math.max(0.8F, e.getPickRadius());
                AABB axisalignedbb = e.getBoundingBox().inflate(f2, f2, f2);
                var hit = axisalignedbb.clip(hitVec, vec3d2);
                if (axisalignedbb.contains(hitVec)) {
                    if (0.0 < d2 || d2 == 0.0) {
                        pointedEntity = e;
                        d2 = 0.0;
                    }
                } else if (hit.isPresent()) {
                    double d3 = hitVec.distanceTo(hit.get());
                    if (d3 < d2 || d2 == 0.0) {
                        pointedEntity = e;
                        d2 = d3;
                    }
                }
            }
        }
        return pointedEntity;
    }

    /** Точка у руки сущности (откуда летят искры соли мира и т.п.). */
    public static Vec3 posToHand(Entity e, InteractionHand hand) {
        double px = e.getX();
        double py = e.getBoundingBox().minY + e.getBbHeight() / 2.0F + 0.25;
        double pz = e.getZ();
        float m = hand == InteractionHand.MAIN_HAND ? 0.0F : 180.0F;
        px += -Mth.cos((e.getYRot() + m) / 180.0F * 3.141593F) * 0.3F;
        pz += -Mth.sin((e.getYRot() + m) / 180.0F * 3.141593F) * 0.3F;
        Vec3 vec3d = e.getViewVector(1.0F);
        px += vec3d.x * 0.3;
        py += vec3d.y * 0.3;
        pz += vec3d.z * 0.3;
        return new Vec3(px, py, pz);
    }

    /** Свои: тот же, верхом/везёт, одна команда, приручённый им; игроки — если PvP выключен. */
    public static boolean isFriendly(net.minecraft.world.entity.Entity source, net.minecraft.world.entity.Entity target) {
        if (source == null || target == null) {
            return false;
        }
        if (source.getId() == target.getId() || source.isPassengerOfSameVehicle(target) || source.hasPassenger(target) || target.hasPassenger(source)
                || source.isAlliedTo(target)) {
            return true;
        }
        if (target instanceof net.minecraft.world.entity.OwnableEntity own && own.getOwner() != null && own.getOwner().equals(source)) {
            return true;
        }
        return !target.level().isClientSide && target instanceof net.minecraft.world.entity.player.Player && target.getServer() != null
                && !target.getServer().isPvpAllowed();
    }
}
