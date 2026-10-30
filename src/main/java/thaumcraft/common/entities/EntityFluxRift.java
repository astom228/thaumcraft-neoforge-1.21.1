package thaumcraft.common.entities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.crafting.IStabilizable;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime;
import thaumcraft.common.items.casters.foci.FocusEffectFlux;
import thaumcraft.common.items.casters.foci.FocusMediumCloud;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;
import thaumcraft.common.world.aura.AuraHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Разлом Выброса (порт EntityFluxRift): извилистая трещина из точек (по семени), растёт, поглощая Выброс чанка, рушит блоки
 * и ранит всё вдоль себя. Стабильность (−100…100) понемногу падает; при отрицательной — события (огонёк — этап 16, главное
 * семя порчи, «Выбросный фаг» вокруг, облако Выброса у ближайшего игрока, обрушение). Стабилизатор поднимает стабильность.
 * Обрушение: разлом сжимается, потом исчезает с изначальной жемчужиной и — по нестабильности — порчей, слабостью и
 * Искажением для окружающих.
 */
public class EntityFluxRift extends Entity implements IStabilizable {
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(EntityFluxRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(EntityFluxRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STABILITY = SynchedEntityData.defineId(EntityFluxRift.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> COLLAPSE = SynchedEntityData.defineId(EntityFluxRift.class, EntityDataSerializers.BOOLEAN);
    /** event, weight, cost, nearTaintAllowed — таблица событий оригинала. */
    private static final int[][] EVENTS = {{0, 50, 5, 1}, {1, 5, 0, 0}, {2, 25, 10, 1}, {3, 15, 15, 1}, {4, 1, 0, 1}};
    int lastSize = -1;
    int lastSeed = 0;
    public final List<Vec3> points = new ArrayList<>();
    public final List<Float> pointsWidth = new ArrayList<>();

    public EntityFluxRift(EntityType<? extends EntityFluxRift> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0).define(SIZE, 5).define(STABILITY, 0.0F).define(COLLAPSE, false);
    }

    public boolean getCollapse() {
        return this.entityData.get(COLLAPSE);
    }

    public void setCollapse(boolean b) {
        this.entityData.set(COLLAPSE, b);
    }

    public float getRiftStability() {
        return this.entityData.get(STABILITY);
    }

    public void setRiftStability(float s) {
        this.entityData.set(STABILITY, Mth.clamp(s, -100.0F, 100.0F));
    }

    public int getRiftSize() {
        return this.entityData.get(SIZE);
    }

    public void setRiftSize(int s) {
        this.entityData.set(SIZE, s);
        this.updateShape();
    }

    public int getRiftSeed() {
        return this.entityData.get(SEED);
    }

    public void setRiftSeed(int s) {
        this.entityData.set(SEED, s);
    }

    /** setSize оригинала: точки по семени, рамка — по крайним точкам. */
    protected void updateShape() {
        this.calcSteps(new Random(this.getRiftSeed()));
        this.lastSize = this.getRiftSize();
        this.lastSeed = this.getRiftSeed();
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        if (this.points == null || this.points.isEmpty()) {
            return super.makeBoundingBox();
        }
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE;
        double x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
        for (Vec3 v : this.points) {
            x0 = Math.min(x0, v.x);
            x1 = Math.max(x1, v.x);
            y0 = Math.min(y0, v.y);
            y1 = Math.max(y1, v.y);
            z0 = Math.min(z0, v.z);
            z1 = Math.max(z1, v.z);
        }
        return new AABB(this.getX() + x0, this.getY() + y0, this.getZ() + z0, this.getX() + x1, this.getY() + y1, this.getZ() + z1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("RiftSize", this.getRiftSize());
        nbt.putInt("RiftSeed", this.getRiftSeed());
        nbt.putFloat("Stability", this.getRiftStability());
        nbt.putBoolean("collapse", this.getCollapse());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        this.setRiftSeed(nbt.getInt("RiftSeed"));
        this.setRiftSize(nbt.getInt("RiftSize"));
        // в 1.12 стабильность читалась как getInteger (дробная часть терялась) — здесь как записана
        this.setRiftStability(nbt.getFloat("Stability"));
        this.setCollapse(nbt.getBoolean("collapse"));
    }

    @Override
    public void move(MoverType type, Vec3 pos) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.lastSize != this.getRiftSize() || this.lastSeed != this.getRiftSeed()) {
            this.updateShape();
        }
        Level w = this.level();
        if (!w.isClientSide) {
            if (this.getRiftSeed() == 0) {
                this.setRiftSeed(this.random.nextInt());
            }
            if (this.points.size() > 1) {
                int pi = this.random.nextInt(this.points.size() - 1);
                Vec3 v1 = this.points.get(pi).add(this.getX(), this.getY(), this.getZ());
                Vec3 v2 = this.points.get(pi + 1).add(this.getX(), this.getY(), this.getZ());
                BlockHitResult rt = w.clip(new ClipContext(v1, v2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (rt.getType() == HitResult.Type.BLOCK) {
                    BlockPos p = rt.getBlockPos();
                    var bs = w.getBlockState(p);
                    if (w.isLoaded(p) && !bs.isAir() && bs.getDestroySpeed(w, p) >= 0.0F) {
                        w.levelEvent(2001, p, net.minecraft.world.level.block.Block.getId(bs));
                        w.removeBlock(p, false);
                    }
                }
                for (Entity e : w.getEntities(this, new AABB(v1, v1).inflate(0.5))) {
                    if (e.isAlive() && !(e instanceof Player pl && pl.isCreative())) {
                        e.hurt(this.damageSources().fellOutOfWorld(), 2.0F);
                        if (e instanceof ItemEntity) {
                            e.discard();
                        }
                    }
                }
            }
            if (this.getCollapse()) {
                this.setRiftSize(this.getRiftSize() - 1);
                if (this.random.nextBoolean()) {
                    AuraHelper.addVis(w, this.blockPosition(), 1.0F);
                } else {
                    AuraHelper.polluteAura(w, this.blockPosition(), 1.0F, false);
                }
                if (this.random.nextInt(10) == 0) {
                    w.explode(this, this.getX() + this.random.nextGaussian() * 2.0, this.getY() + this.random.nextGaussian() * 2.0,
                            this.getZ() + this.random.nextGaussian() * 2.0, this.random.nextFloat() / 2.0F, Level.ExplosionInteraction.NONE);
                }
                if (this.getRiftSize() <= 1) {
                    this.completeCollapse();
                    return;
                }
            }
            if (this.tickCount % 120 == 0) {
                this.setRiftStability(this.getRiftStability() - 0.2F);
            }
            if (this.tickCount % 600 == this.getId() % 600) {
                float taint = AuraHandler.getFlux(w, this.blockPosition());
                double size = Math.sqrt(this.getRiftSize() * 2);
                if (taint >= size && this.getRiftSize() < 100 && this.getStability() != EnumStability.VERY_STABLE) {
                    AuraHandler.drainFlux(w, this.blockPosition(), (float) size, false);
                    this.setRiftSize(this.getRiftSize() + 1);
                }
                if (this.getRiftStability() < 0.0F && this.random.nextInt(1000) < Math.abs(this.getRiftStability()) + this.getRiftSize()) {
                    this.executeRiftEvent((ServerLevel) w);
                }
            }
        } else if (this.points.size() > 2) {
            int pi = 1 + this.random.nextInt(this.points.size() - 2);
            Vec3 v1 = this.points.get(pi).add(this.getX(), this.getY(), this.getZ());
            if (!this.getCollapse() && this.getRiftStability() < 0.0F && this.random.nextInt(150) < Math.abs(this.getRiftStability())) {
                IFXDispatcher.get().drawCurlyWisp(v1.x, v1.y, v1.z, 0.0, 0.0, 0.0, 0.1F + this.pointsWidth.get(pi) * 3.0F, 1.0F, 1.0F, 1.0F, 0.25F, null, 1, 0, 0);
            }
            if (this.getCollapse()) {
                IFXDispatcher.get().drawCurlyWisp(v1.x, v1.y, v1.z, 0.0, 0.0, 0.0, 0.1F + this.pointsWidth.get(pi) * 3.0F, 1.0F,
                        0.3F + this.random.nextFloat() * 0.1F, 0.3F + this.random.nextFloat() * 0.1F, 0.4F, null, 1, 0, 0);
            }
        }
    }

    /**
     * Появление разлома в случайной клетке чанка: на высоте осадков (без неба — первая пустая клетка снизу), не ближе 32 к
     * другому разлому, размер √(Выброс·3) > 5, Выброс чанка тратится. Игрокам рядом — «f_toomuchflux».
     */
    public static void createRift(ServerLevel world, BlockPos pos) {
        pos = pos.offset(world.random.nextInt(16), 0, world.random.nextInt(16));
        if (!world.isLoaded(pos)) {
            return;
        }
        BlockPos p2 = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
        int top = world.getMinBuildHeight() + world.getLogicalHeight();
        if (!world.dimensionType().hasSkyLight()) {
            for (p2 = new BlockPos(p2.getX(), world.getMinBuildHeight() + 10, p2.getZ()); !world.isEmptyBlock(p2); p2 = p2.above(world.random.nextInt(5) + 1)) {
                if (p2.getY() > top - 5) {
                    return;
                }
            }
        }
        if (p2.getY() >= top - 4) {
            return;
        }
        if (!world.getEntitiesOfClass(EntityFluxRift.class, new AABB(p2).inflate(32.0)).isEmpty()) {
            return;
        }
        EntityFluxRift rift = ConfigEntities.FLUX_RIFT.get().create(world);
        if (rift == null) {
            return;
        }
        rift.setRiftSeed(world.random.nextInt());
        rift.moveTo(p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, world.random.nextInt(360), 0.0F);
        float taint = AuraHandler.getFlux(world, p2);
        double size = Math.sqrt(taint * 3.0F);
        if (size > 5.0 && world.addFreshEntity(rift)) {
            rift.setRiftSize((int) size);
            AuraHandler.drainFlux(world, p2, (float) size, false);
            for (Player target : world.getEntitiesOfClass(Player.class, new AABB(p2).inflate(32.0))) {
                if (!ThaumcraftCapabilities.getKnowledge(target).isResearchKnown("f_toomuchflux")) {
                    target.displayClientMessage(Component.translatable("tc.fluxevent.3").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
                    ThaumcraftApi.internalMethods.completeResearch(target, "f_toomuchflux");
                }
            }
        }
    }

    private void executeRiftEvent(ServerLevel w) {
        int total = 0;
        for (int[] e : EVENTS) {
            total += e[1];
        }
        int roll = this.random.nextInt(total);
        int[] ei = EVENTS[0];
        for (int[] e : EVENTS) {
            roll -= e[1];
            if (roll < 0) {
                ei = e;
                break;
            }
        }
        if (ei[3] == 0 && TaintHelper.isNearTaintSeed(w, this.blockPosition())) {
            return;
        }
        boolean didit = false;
        switch (ei[0]) {
            case 0 -> {
                // TODO этап 16: огонёк (EntityWisp), иногда — огонёк Выброса
            }
            case 1 -> {
                EntityTaintSeedPrime seed = ConfigEntities.TAINT_SEED_PRIME.get().create(w);
                if (seed != null) {
                    seed.moveTo(this.getX() + this.random.nextGaussian() * 5.0, this.getY() + this.random.nextGaussian() * 5.0,
                            this.getZ() + this.random.nextGaussian() * 5.0, w.random.nextInt(360), 0.0F);
                    if (w.addFreshEntity(seed)) {
                        didit = true;
                        seed.boost = this.getRiftSize();
                        this.discard();
                    }
                }
            }
            case 2 -> {
                for (LivingEntity target : w.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(16.0))) {
                    didit = true;
                    if (target instanceof Player p) {
                        p.displayClientMessage(Component.translatable("tc.fluxevent.2").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
                    }
                    MobEffectInstance pe = new MobEffectInstance(ConfigPotions.INFECTIOUS_VIS_EXHAUST, 3000, 2);
                    pe.getCures().clear();
                    target.addEffect(pe);
                }
            }
            case 3 -> {
                Player target = w.getNearestPlayer(this, 16.0);
                if (target != null) {
                    FocusPackage p = new FocusPackage(target);
                    FocusMediumRoot root = new FocusMediumRoot();
                    root.setupFromCasterToTarget(target, target, 0.5);
                    p.addNode(root);
                    FocusMediumCloud fp = new FocusMediumCloud();
                    fp.initialize();
                    fp.getSetting("radius").setValue(Mth.nextInt(this.random, 1, 3));
                    fp.getSetting("duration").setValue(Mth.nextInt(this.random, Math.min(this.getRiftSize() / 2, 30), Math.min(this.getRiftSize(), 120)));
                    p.addNode(fp);
                    p.addNode(new FocusEffectFlux());
                    FocusEngine.castFocusPackage(target, p, true);
                }
            }
            default -> this.setCollapse(true);
        }
        if (didit) {
            this.setRiftStability(this.getRiftStability() + ei[2]);
        }
    }

    /** Точки трещины: две ветви от центра, каждая — ceil(size/3) шагов по 0.2 со случайным изгибом, ширина сужается к концам. */
    private void calcSteps(Random rr) {
        this.points.clear();
        this.pointsWidth.clear();
        Vec3 right = new Vec3(rr.nextGaussian(), rr.nextGaussian(), rr.nextGaussian()).normalize();
        Vec3 left = right.scale(-1.0);
        Vec3 lr = Vec3.ZERO;
        Vec3 ll = Vec3.ZERO;
        int steps = Mth.ceil(this.getRiftSize() / 3.0F);
        float girth = this.getRiftSize() / 300.0F;
        double angle = 0.33;
        float dec = girth / steps;
        for (int a = 0; a < steps; a++) {
            girth -= dec;
            right = right.xRot((float) (rr.nextGaussian() * angle)).yRot((float) (rr.nextGaussian() * angle));
            lr = lr.add(right.scale(0.2));
            this.points.add(lr);
            this.pointsWidth.add(girth);
            left = left.xRot((float) (rr.nextGaussian() * angle)).yRot((float) (rr.nextGaussian() * angle));
            ll = ll.add(left.scale(0.2));
            this.points.add(0, ll);
            this.pointsWidth.add(0, girth);
        }
        lr = lr.add(right.scale(0.1));
        this.points.add(lr);
        this.pointsWidth.add(0.0F);
        ll = ll.add(left.scale(0.1));
        this.points.add(0, ll);
        this.pointsWidth.add(0, 0.0F);
    }

    @Override
    public void addStability(int multiples) {
        if (this.getRiftStability() < this.random.nextInt(100)) {
            this.setRiftStability(this.getRiftStability() + 0.02F * multiples);
        }
    }

    @Override
    public EnumStability getStability() {
        float s = this.getRiftStability();
        return s > 50.0F ? EnumStability.VERY_STABLE : (s >= 0.0F ? EnumStability.STABLE : (s > -25.0F ? EnumStability.UNSTABLE : EnumStability.VERY_UNSTABLE));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    private void completeCollapse() {
        ItemStack pearl = new ItemStack(ItemsTC.primordialPearl.get());
        pearl.setDamageValue(4 + this.random.nextInt(4));
        this.spawnAtLocation(pearl, 0.0F);
        PacketDistributor.sendToPlayersNear((ServerLevel) this.level(), null, this.getX(), this.getY(), this.getZ(), 64.0,
                new PacketFXBlockBamf(this.getX(), this.getY(), this.getZ(), 0, true, true, null));
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(32.0), e -> e != null);
        EnumStability st = this.getStability();
        if (st != EnumStability.VERY_STABLE) {
            // switch с проваливанием оригинала: очень нестабильный — порча, + слабость; нестабильный — слабость; затем всем — Искажение
            if (st == EnumStability.VERY_UNSTABLE) {
                for (LivingEntity p : list) {
                    int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 120.0);
                    if (w > 0) {
                        p.addEffect(new MobEffectInstance(ConfigPotions.FLUX_TAINT, w * 20, 0));
                    }
                }
            }
            if (st == EnumStability.VERY_UNSTABLE || st == EnumStability.UNSTABLE) {
                for (LivingEntity p : list) {
                    int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 300.0);
                    if (w > 0) {
                        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, w * 20, 0));
                    }
                }
            }
            for (LivingEntity p : list) {
                if (p instanceof ServerPlayer sp) {
                    int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 25.0);
                    if (w > 0) {
                        ThaumcraftApi.internalMethods.addWarpToPlayer(sp, w, IPlayerWarp.EnumWarpType.NORMAL);
                        ThaumcraftApi.internalMethods.addWarpToPlayer(sp, w, IPlayerWarp.EnumWarpType.TEMPORARY);
                    }
                }
            }
        }
        this.discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 64.0 * 64.0 * 4;
    }
}
