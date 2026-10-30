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

    /**
     * Луч до существа (getPointedEntityRay оригинала): от startVec по lookVec на range, кроме ignoreEntity; точка попадания —
     * на расширенной рамке существа. Существа за блоками не видны. Если старт внутри рамки, в оригинале бралась точка
     * несуществующего пересечения (NullPointerException) — здесь сама точка старта.
     */
    public static net.minecraft.world.phys.EntityHitResult getPointedEntityRay(Level world, Entity ignoreEntity, Vec3 startVec, Vec3 lookVec, double minrange,
                                                                           double range, float padding, boolean nonCollide) {
        net.minecraft.world.phys.EntityHitResult pointedEntityRay = null;
        double d = range;
        Vec3 vec3d2 = startVec.add(lookVec.x * d, lookVec.y * d, lookVec.z * d);
        float f1 = padding;
        AABB bb = ignoreEntity != null ? ignoreEntity.getBoundingBox() : new AABB(startVec, startVec).inflate(0.5);
        List<Entity> list = world.getEntities(ignoreEntity, bb.expandTowards(lookVec.x * d, lookVec.y * d, lookVec.z * d).inflate(f1, f1, f1));
        double d2 = 0.0;
        for (Entity entity : list) {
            if (!(startVec.distanceTo(entity.position()) < minrange) && (entity.isPickable() || nonCollide)
                    && world.clip(new ClipContext(startVec, new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ()),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()))
                    .getType() == HitResult.Type.MISS) {
                float f2 = Math.max(0.8F, entity.getPickRadius());
                AABB axisalignedbb = entity.getBoundingBox().inflate(f2, f2, f2);
                var hit = axisalignedbb.clip(startVec, vec3d2);
                if (axisalignedbb.contains(startVec)) {
                    if (0.0 < d2 || d2 == 0.0) {
                        pointedEntityRay = new net.minecraft.world.phys.EntityHitResult(entity, hit.orElse(startVec));
                        d2 = 0.0;
                    }
                } else if (hit.isPresent()) {
                    double d3 = startVec.distanceTo(hit.get());
                    if (d3 < d2 || d2 == 0.0) {
                        pointedEntityRay = new net.minecraft.world.phys.EntityHitResult(entity, hit.get());
                        d2 = d3;
                    }
                }
            }
        }
        return pointedEntityRay;
    }

    /** Существа класса в кубе ±range вокруг точки, кроме entity. */
    public static <T extends Entity> List<T> getEntitiesInRange(Level world, double x, double y, double z, Entity entity, Class<T> classEntity, double range) {
        List<T> out = new java.util.ArrayList<>();
        for (T ent : world.getEntitiesOfClass(classEntity, new AABB(x, y, z, x, y, z).inflate(range, range, range))) {
            if (entity == null || entity.getId() != ent.getId()) {
                out.add(ent);
            }
        }
        return out;
    }

    /** То же вокруг существа, ближние первыми. */
    public static <T extends Entity> List<T> getEntitiesInRangeSorted(Level world, Entity entity, Class<T> classEntity, double range) {
        List<T> list = getEntitiesInRange(world, entity.getX(), entity.getY(), entity.getZ(), entity, classEntity, range);
        list.sort(java.util.Comparator.comparingDouble(a -> entity.position().distanceToSqr(a.position())));
        return list;
    }

    /** ent2 в конусе обзора ent (угол fov, дальность range). */
    public static boolean isVisibleTo(float fov, Entity ent, Entity ent2, float range) {
        double[] x = {ent2.getX(), ent2.getBoundingBox().minY + ent2.getBbHeight() / 2.0F, ent2.getZ()};
        double[] t = {ent.getX(), ent.getBoundingBox().minY + ent.getEyeHeight(), ent.getZ()};
        Vec3 q = ent.getLookAngle().scale(range);
        Vec3 l = q.add(ent.getX(), ent.getBoundingBox().minY + ent.getEyeHeight(), ent.getZ());
        return Utils.isLyingInCone(x, t, new double[] {l.x, l.y, l.z}, fov);
    }

    /** Между серединами существ нет блоков со столкновением. */
    public static boolean canEntityBeSeen(Entity entity, Entity entity2) {
        Vec3 a = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() / 2.0F, entity.getZ());
        Vec3 b = new Vec3(entity2.getX(), entity2.getY() + entity2.getBbHeight() / 2.0F, entity2.getZ());
        return entity.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    /** world.rayTraceBlocks(start, end) 1.12: блоки по рамке выделения, без жидкостей; промах — null. */
    public static net.minecraft.world.phys.BlockHitResult rayTraceBlocks(Level world, Vec3 start, Vec3 end) {
        net.minecraft.world.phys.BlockHitResult r = world.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return r.getType() == HitResult.Type.MISS ? null : r;
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

    /** Видит ли игрок аспекты в мире: IGoggles в основной руке, броне или безделушках. */
    public static boolean hasGoggles(Entity e) {
        if (!(e instanceof net.minecraft.world.entity.player.Player viewer)) {
            return false;
        }
        if (viewer.getMainHandItem().getItem() instanceof thaumcraft.api.items.IGoggles g && g.showIngamePopups(viewer.getMainHandItem(), viewer)) {
            return true;
        }
        for (net.minecraft.world.item.ItemStack s : viewer.getInventory().armor) {
            if (s.getItem() instanceof thaumcraft.api.items.IGoggles g && g.showIngamePopups(s, viewer)) {
                return true;
            }
        }
        net.neoforged.neoforge.items.IItemHandler baubles = BaubleUtils.getBaubles(viewer);
        if (baubles != null) {
            for (int a = 0; a < baubles.getSlots(); a++) {
                net.minecraft.world.item.ItemStack s = baubles.getStackInSlot(a);
                if (s.getItem() instanceof thaumcraft.api.items.IGoggles g && g.showIngamePopups(s, viewer)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Видит ли игрок скрытое: IRevealer в руках, броне или безделушках. */
    public static boolean hasRevealer(Entity e) {
        if (!(e instanceof net.minecraft.world.entity.player.Player viewer)) {
            return false;
        }
        if (viewer.getMainHandItem().getItem() instanceof thaumcraft.api.items.IRevealer r && r.showNodes(viewer.getMainHandItem(), viewer)) {
            return true;
        }
        if (viewer.getOffhandItem().getItem() instanceof thaumcraft.api.items.IRevealer r && r.showNodes(viewer.getOffhandItem(), viewer)) {
            return true;
        }
        for (net.minecraft.world.item.ItemStack s : viewer.getInventory().armor) {
            if (s.getItem() instanceof thaumcraft.api.items.IRevealer r && r.showNodes(s, viewer)) {
                return true;
            }
        }
        net.neoforged.neoforge.items.IItemHandler baubles = BaubleUtils.getBaubles(viewer);
        if (baubles != null) {
            for (int a = 0; a < baubles.getSlots(); a++) {
                net.minecraft.world.item.ItemStack s = baubles.getStackInSlot(a);
                if (s.getItem() instanceof thaumcraft.api.items.IRevealer r && r.showNodes(s, viewer)) {
                    return true;
                }
            }
        }
        return false;
    }
    private static net.minecraft.resources.ResourceLocation rl(String p) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(thaumcraft.Thaumcraft.MODID, p);
    }

    private static final net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation ADD = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
    public static final net.minecraft.world.entity.ai.attributes.AttributeModifier CHAMPION_HEALTH = new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("champion_health"), 100.0, ADD);
    public static final net.minecraft.world.entity.ai.attributes.AttributeModifier CHAMPION_DAMAGE = new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("champion_damage"), 2.0,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final net.minecraft.world.entity.ai.attributes.AttributeModifier BOLDBUFF = new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("bold_speed"), 0.3,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    public static final net.minecraft.world.entity.ai.attributes.AttributeModifier MIGHTYBUFF = new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("mighty_damage"), 2.0,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    /** Здоровье боссов за каждого лишнего игрока; 5 и 6 — один и тот же id, как одинаковый UUID в оригинале. */
    public static final net.minecraft.world.entity.ai.attributes.AttributeModifier[] HPBUFF = {
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("health_buff_1"), 50.0, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("health_buff_2"), 50.0, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("health_buff_3"), 50.0, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("health_buff_4"), 50.0, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("health_buff_5"), 50.0, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("health_buff_5"), 25.0, ADD)};
    public static final net.minecraft.world.entity.ai.attributes.AttributeModifier[] DMGBUFF = {
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("damage_buff_1"), 0.5, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("damage_buff_2"), 0.5, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("damage_buff_3"), 0.5, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("damage_buff_4"), 0.5, ADD),
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(rl("damage_buff_5"), 0.5, ADD)};

    /** Снять и заново наложить постоянный модификатор (removeModifier + applyModifier оригинала). */
    public static void reapply(net.minecraft.world.entity.ai.attributes.AttributeInstance ai, net.minecraft.world.entity.ai.attributes.AttributeModifier mod) {
        if (ai != null) {
            ai.removeModifier(mod.id());
            ai.addPermanentModifier(mod);
        }
    }

    /**
     * Сделать чемпионом со случайным модификатором (криперы — только «Смелый»): +100 здоровья, урон ×3, название
     * с модификатором; боссы получают своё имя (generateName). persist — не исчезать вдали.
     */
    public static void makeChampion(net.minecraft.world.entity.monster.Monster entity, boolean persist) {
        var modai = entity.getAttribute(thaumcraft.api.ThaumcraftApiHelper.CHAMPION_MOD);
        if (modai == null || modai.getValue() > -2.0) {
            return;
        }
        int type = entity.getRandom().nextInt(thaumcraft.common.entities.monster.mods.ChampionModifier.mods.length);
        if (entity instanceof net.minecraft.world.entity.monster.Creeper) {
            type = 0;
        }
        reapply(modai, thaumcraft.common.entities.monster.mods.ChampionModifier.mods[type].attributeMod);
        if (!(entity instanceof thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss boss)) {
            reapply(entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH), CHAMPION_HEALTH);
            reapply(entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), CHAMPION_DAMAGE);
            entity.heal(25.0F);
            entity.setCustomName(thaumcraft.common.entities.monster.mods.ChampionModifier.mods[type].getModName().copy().append(" ").append(entity.getName()));
        } else {
            boss.generateName();
        }
        if (persist) {
            entity.setPersistenceRequired();
        }
        switch (type) {
            case 0 -> reapply(entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), BOLDBUFF);
            case 3 -> reapply(entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), MIGHTYBUFF);
            case 5 -> {
                int bh = (int) entity.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) / 2;
                entity.setAbsorptionAmount(entity.getAbsorptionAmount() + bh);
            }
            default -> {
            }
        }
    }

    /** Заразить (модификатор 13): существо, почти убитое заражением Выбросом; +25 здоровья, +0.5 урона. Чемпионов не трогает. */
    public static void makeTainted(net.minecraft.world.entity.LivingEntity target) {
        var modai = target.getAttribute(thaumcraft.api.ThaumcraftApiHelper.CHAMPION_MOD);
        if (modai == null || modai.getValue() > -1.0) {
            return;
        }
        if (modai.getValue() == -1.0) {
            reapply(modai, thaumcraft.common.entities.monster.mods.ChampionModifier.ATTRIBUTE_MINUS_ONE);
        }
        reapply(modai, thaumcraft.common.entities.monster.mods.ChampionModifier.mods[13].attributeMod);
        if (!(target instanceof thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss boss)) {
            reapply(target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH), HPBUFF[5]);
            reapply(target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), DMGBUFF[0]);
            target.heal(25.0F);
        } else {
            boss.generateName();
        }
    }

    /** Особый предмет (не горит, парит), падающий из существа с высоты dropheight. */
    public static net.minecraft.world.entity.item.ItemEntity entityDropSpecialItem(Entity entity, net.minecraft.world.item.ItemStack stack, float dropheight) {
        if (stack.isEmpty() || entity.level().isClientSide) {
            return null;
        }
        var item = new thaumcraft.common.entities.EntitySpecialItem(entity.level(), entity.getX(), entity.getY() + dropheight, entity.getZ(), stack);
        item.setDefaultPickUpDelay();
        item.setDeltaMovement(0.0, 0.1F, 0.0);
        entity.level().addFreshEntity(item);
        return item;
    }
}
