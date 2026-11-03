package thaumcraft.common.entities.construct;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.container.ContainerArcaneBore;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.network.fx.PacketFXBoreDig;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Магический бур (порт EntityArcaneBore): с киркой и сигналом красного камня снизу (или над выключенными рельсами
 * активатора) копает по спирали круг перед собой: радиус — по зачаровываемости кирки (+2 за «Разрушение»), глубина —
 * радиус×8 (+16 за «Проходку»); 1 Вис на 4 блока из ауры; кирка изнашивается в 50 раз медленнее. Добыча — в соседний
 * инвентарь или назад. Хозяин: удар — повернуть к себе, Shift+ПКМ — разобрать, ПКМ — окно.
 */
public class EntityArcaneBore extends EntityOwnedConstruct {
    private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(EntityArcaneBore.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(EntityArcaneBore.class, EntityDataSerializers.BOOLEAN);
    BlockPos digTarget = null;
    BlockPos digTargetPrev = null;
    final float digCost = 0.25F;
    long soundDelay = 0L;
    int breakCounter = 0;
    int digDelay = 0;
    int digDelayMax = 0;
    float radInc = 0.0F;
    public int spiral = 0;
    public float currentRadius = 0.0F;
    private float charge = 0.0F;
    public boolean clientDigging = false;

    public EntityArcaneBore(EntityType<? extends EntityArcaneBore> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FACING, Direction.DOWN);
        builder.define(ACTIVE, false);
    }

    private int ench(ResourceKey<Enchantment> key) {
        Holder<Enchantment> h = this.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(h, this.getMainHandItem());
    }

    private static boolean isPickaxe(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof PickaxeItem || s.is(ItemTags.PICKAXES));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.setYRot(this.getYHeadRot());
            if (this.tickCount % 50 == 0) {
                this.heal(1.0F);
            }
            if (this.tickCount % 10 == 0 && this.getCharge() < 10.0F) {
                this.rechargeVis();
            }
            BlockPos bp = this.blockPosition();
            if (BaseRailBlock.isRail(this.level(), bp.below())) {
                bp = bp.below();
            }
            BlockState rail = this.level().getBlockState(bp);
            if (BaseRailBlock.isRail(rail)) {
                if (rail.is(BlocksTC.activatorRail.get())) {
                    this.setActive(!rail.getValue(PoweredRailBlock.POWERED));
                }
            } else {
                this.setActive(this.level().hasNeighborSignal(this.blockPosition().below()));
            }
        }
        if (!this.isActive()) {
            this.digTarget = null;
            this.getLookControl().setLookAt(this.getX() + this.getFacing().getStepX(), this.getY(), this.getZ() + this.getFacing().getStepZ(), 10.0F, 33.0F);
        }
        if (this.digTarget != null && this.getCharge() >= this.digCost && !this.level().isClientSide) {
            this.getLookControl().setLookAt(this.digTarget.getX() + 0.5, this.digTarget.getY(), this.digTarget.getZ() + 0.5, 10.0F, 90.0F);
            if (this.digDelay-- <= 0 && this.dig()) {
                this.setCharge(this.getCharge() - this.digCost);
                if (this.soundDelay < this.level().getGameTime()) {
                    this.soundDelay = this.level().getGameTime() + 24 + this.level().random.nextInt(2);
                    this.playSound(SoundsTC.rumble.get(), 0.25F, 0.9F + this.level().random.nextFloat() * 0.2F);
                }
            }
        }
        if (!this.level().isClientSide && this.digTarget == null && this.isActive() && this.validInventory()) {
            this.findNextBlockToDig();
            if (this.digTarget != null) {
                this.level().broadcastEntityEvent(this, (byte) 16);
                PacketDistributor.sendToPlayersNear((ServerLevel) this.level(), null, this.digTarget.getX(), this.digTarget.getY(), this.digTarget.getZ(), 32.0,
                        new PacketFXBoreDig(this.digTarget, this.getId(), this.digDelayMax));
            } else {
                this.level().broadcastEntityEvent(this, (byte) 17);
                this.getLookControl().setLookAt(this.getX() + this.getFacing().getStepX() * 2, this.getY() + this.getFacing().getStepY() * 2 + this.getEyeHeight(),
                        this.getZ() + this.getFacing().getStepZ() * 2, 10.0F, 33.0F);
            }
        }
    }

    /** Кирка, которой осталось больше одной единицы прочности. */
    public boolean validInventory() {
        ItemStack s = this.getMainHandItem();
        return isPickaxe(s) && !(s.isDamageableItem() && s.getDamageValue() + 1 >= s.getMaxDamage());
    }

    public int getDigRadius() {
        int r = 0;
        ItemStack s = this.getMainHandItem();
        if (isPickaxe(s)) {
            r = s.getItem().getEnchantmentValue(s) / 3;
            r += EnumInfusionEnchantment.getInfusionEnchantmentLevel(s, EnumInfusionEnchantment.DESTRUCTIVE) * 2;
        }
        return r <= 1 ? 2 : r;
    }

    public int getDigDepth() {
        return this.getDigRadius() * 8 + EnumInfusionEnchantment.getInfusionEnchantmentLevel(this.getMainHandItem(), EnumInfusionEnchantment.BURROWING) * 16;
    }

    public int getFortune() {
        if (!this.validInventory()) {
            return 0;
        }
        return Math.max(this.ench(Enchantments.FORTUNE), EnumInfusionEnchantment.getInfusionEnchantmentLevel(this.getMainHandItem(), EnumInfusionEnchantment.SOUNDING));
    }

    public int getDigSpeed(BlockState blockState) {
        int speed = 0;
        if (this.validInventory()) {
            speed += (int) (this.getMainHandItem().getDestroySpeed(blockState) / 2.0F);
            speed += this.ench(Enchantments.EFFICIENCY);
        }
        return speed;
    }

    public int getRefining() {
        return EnumInfusionEnchantment.getInfusionEnchantmentLevel(this.getMainHandItem(), EnumInfusionEnchantment.REFINING);
    }

    public boolean hasSilkTouch() {
        return !this.getMainHandItem().isEmpty() && this.ench(Enchantments.SILK_TOUCH) > 0;
    }

    /**
     * Сломать блок цели заместителем «FakeThaumcraftBore» с киркой бура; выпавшее вокруг цели — в соседний инвентарь или
     * назад (с «Очисткой» — самородные кластеры). В оригинале добычу перехватывало событие HarvestDrops, затем ещё
     * собирались предметы вокруг; здесь — только сбор предметов вокруг цели (результат тот же).
     */
    private boolean dig() {
        boolean b = false;
        ServerLevel world = (ServerLevel) this.level();
        if (this.digTarget != null && !world.isEmptyBlock(this.digTarget)) {
            BlockState digBs = world.getBlockState(this.digTarget);
            boolean silktouch = this.hasSilkTouch();
            FakePlayer fp = FakePlayerFactory.get(world, BORE_PROFILE);
            fp.setPos(this.getX(), this.getY(), this.getZ());
            fp.setItemInHand(InteractionHand.MAIN_HAND, this.getMainHandItem().copy());
            int fortune = silktouch ? 0 : this.getFortune();
            boolean harvested = BlockUtils.harvestBlock(world, fp, this.digTarget, false, false, fortune, false);
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (!harvested && !world.isEmptyBlock(this.digTarget)) {
                // защита территории или иная отмена — блок не трогаем
                this.digTarget = null;
                return false;
            }
            List<ItemStack> items = new ArrayList<>();
            for (ItemEntity e : world.getEntitiesOfClass(ItemEntity.class, new AABB(this.digTarget).inflate(1.0))) {
                items.add(e.getItem().copy());
                e.discard();
            }
            int refining = this.getRefining();
            for (ItemStack is : items) {
                ItemStack dropped = is.copy();
                if (!silktouch && refining > 0) {
                    dropped = Utils.findSpecialMiningResult(is, (refining + 1) * 0.125F, world.random);
                }
                if (dropped != null && !dropped.isEmpty()) {
                    boolean e = false;
                    for (Direction f : Direction.values()) {
                        BlockPos p = this.blockPosition().relative(f);
                        if (ThaumcraftApiHelper.getItemHandlerAt(world, p, f) != null) {
                            InventoryUtils.ejectStackAt(world, this.blockPosition(), f, dropped);
                            e = true;
                            break;
                        }
                    }
                    if (!e) {
                        InventoryUtils.ejectStackAt(world, this.blockPosition(), this.getFacing().getOpposite(), dropped);
                    }
                }
            }
            ItemStack pick = this.getMainHandItem();
            if (!pick.isEmpty()) {
                this.breakCounter++;
                if (this.breakCounter >= 50) {
                    this.breakCounter = 0;
                    pick.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
                }
            }
            b = world.isEmptyBlock(this.digTarget) || world.removeBlock(this.digTarget, false);
        }
        this.digTarget = null;
        return b;
    }

    private static final com.mojang.authlib.GameProfile BORE_PROFILE = new com.mojang.authlib.GameProfile(
            java.util.UUID.nameUUIDFromBytes("FakeThaumcraftBore".getBytes()), "FakeThaumcraftBore");

    private void findNextBlockToDig() {
        int digRadius = this.getDigRadius();
        if (this.digTargetPrev == null || this.distanceToSqr(Vec3.atCenterOf(this.digTargetPrev)) > (digRadius + 1) * (digRadius + 1)) {
            this.digTargetPrev = this.blockPosition();
        }
        if (this.radInc == 0.0F) {
            this.radInc = 1.0F;
        }
        int digDepth = this.getDigDepth();
        Direction f = this.getFacing();
        int x = this.digTargetPrev.getX();
        int z = this.digTargetPrev.getZ();
        int y = this.digTargetPrev.getY();
        BlockPos end = new BlockPos(x + f.getStepX() * digDepth, y + f.getStepY() * digDepth, z + f.getStepZ() * digDepth);
        BlockHitResult mop = this.level().clip(new ClipContext(Vec3.atCenterOf(this.digTargetPrev), Vec3.atCenterOf(end), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (mop.getType() != HitResult.Type.MISS) {
            Vec3 digger = new Vec3(this.getX() + f.getStepX(), this.getY() + this.getEyeHeight() + f.getStepY(), this.getZ() + f.getStepZ());
            BlockHitResult mop2 = this.level().clip(new ClipContext(digger, Vec3.atCenterOf(mop.getBlockPos()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    this));
            if (mop2.getType() != HitResult.Type.MISS) {
                BlockPos hp = mop2.getBlockPos();
                BlockState blockState = this.level().getBlockState(hp);
                float hardness = blockState.getDestroySpeed(this.level(), hp);
                if (hardness > -1.0F && !blockState.getCollisionShape(this.level(), hp).isEmpty()) {
                    this.digDelay = Math.max(10 - this.getDigSpeed(blockState), (int) (hardness * 2.0F) - this.getDigSpeed(blockState) * 2);
                    if (this.digDelay < 1) {
                        this.digDelay = 1;
                    }
                    this.digDelayMax = this.digDelay;
                    if (!hp.equals(this.blockPosition()) && !hp.equals(this.blockPosition().below())) {
                        this.digTarget = hp;
                        return;
                    }
                }
            }
        }
        while (x == this.digTargetPrev.getX() && z == this.digTargetPrev.getZ() && y == this.digTargetPrev.getY()) {
            if (Math.abs(this.currentRadius) > digRadius) {
                this.currentRadius = digRadius;
            }
            this.spiral = (int) (this.spiral + (3.0F + Math.max(0.0F, (10.0F - Math.abs(this.currentRadius)) * 2.0F)));
            if (this.spiral >= 360) {
                this.spiral -= 360;
                this.currentRadius = this.currentRadius + this.radInc;
                if (this.currentRadius > digRadius || this.currentRadius < -digRadius) {
                    this.currentRadius = 0.0F;
                }
            }
            Vec3 vsource = new Vec3((int) this.getX() + 0.5 + f.getStepX(), this.getY() + f.getStepY() + this.getEyeHeight(), (int) this.getZ() + 0.5 + f.getStepZ());
            Vec3 vtar = new Vec3(0.0, this.currentRadius, 0.0);
            vtar = rotateAroundZ(vtar, this.spiral / 180.0F * (float) Math.PI);
            vtar = rotateAroundY(vtar, (float) (Math.PI / 2) * f.getStepX());
            vtar = rotateAroundX(vtar, (float) (Math.PI / 2) * f.getStepY());
            Vec3 vres = vsource.add(vtar);
            x = Mth.floor(vres.x);
            y = Mth.floor(vres.y);
            z = Mth.floor(vres.z);
        }
        this.digTargetPrev = new BlockPos(x, y, z);
    }

    /** Utils.rotateAround* оригинала. */
    public static Vec3 rotateAroundX(Vec3 vec, float angle) {
        float c = Mth.cos(angle);
        float s = Mth.sin(angle);
        return new Vec3(vec.x, vec.y * c + vec.z * s, vec.z * c - vec.y * s);
    }

    public static Vec3 rotateAroundY(Vec3 vec, float angle) {
        float c = Mth.cos(angle);
        float s = Mth.sin(angle);
        return new Vec3(vec.x * c + vec.z * s, vec.y, vec.z * c - vec.x * s);
    }

    public static Vec3 rotateAroundZ(Vec3 vec, float angle) {
        float c = Mth.cos(angle);
        float s = Mth.sin(angle);
        return new Vec3(vec.x * c + vec.y * s, vec.y * c - vec.x * s, vec.z);
    }

    /** EnumFacing.getDirectionFromEntityLiving 1.12: рядом по горизонтали — вверх/вниз по высоте глаз, иначе — к существу. */
    private static Direction directionFromEntity(BlockPos pos, LivingEntity e) {
        if (Math.abs(e.getX() - (pos.getX() + 0.5F)) < 2.0 && Math.abs(e.getZ() - (pos.getZ() + 0.5F)) < 2.0) {
            double d = e.getY() + e.getEyeHeight();
            if (d - pos.getY() > 2.0) {
                return Direction.UP;
            }
            if (pos.getY() - d > 0.0) {
                return Direction.DOWN;
            }
        }
        return e.getDirection().getOpposite();
    }

    /** Удар хозяина поворачивает бур к нему (кроме «вниз») и не ранит. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity le && this.isOwner(le)) {
            Direction f = directionFromEntity(this.blockPosition(), le);
            if (f != Direction.DOWN) {
                this.setFacing(f);
            }
            return false;
        }
        this.setYRot((float) (this.getYRot() + this.getRandom().nextGaussian() * 45.0));
        this.setXRot((float) (this.getXRot() + this.getRandom().nextGaussian() * 20.0));
        return super.hurt(source, amount);
    }

    protected void rechargeVis() {
        this.setCharge(this.getCharge() + AuraHandler.drainVis(this.level(), this.blockPosition(), 10.0F, false));
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide) {
            this.dropStuff();
        }
    }

    protected void dropStuff() {
        if (!this.getMainHandItem().isEmpty()) {
            this.spawnAtLocation(this.getMainHandItem(), 0.5F);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected void dropEquipment() {
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof NameTagItem) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide && this.isOwner(player) && this.isAlive()) {
            if (player.isShiftKeyDown()) {
                this.playSound(SoundsTC.zap.get(), 1.0F, 1.0F);
                this.dropStuff();
                this.spawnAtLocation(new ItemStack(ItemsTC.arcaneBore.get()), 0.5F);
                this.discard();
                player.swing(hand, true);
            } else if (player instanceof ServerPlayer sp) {
                sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ContainerArcaneBore(id, inv, this), Component.translatable("entity.thaumcraft.arcanebore")),
                        buf -> buf.writeVarInt(this.getId()));
            }
            return InteractionResult.SUCCESS;
        }
        if (this.level().isClientSide && this.isOwner(player)) {
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void knockback(double strength, double x, double z) {
        super.knockback(strength, x / 10.0, z / 10.0);
        Vec3 m = this.getDeltaMovement();
        if (m.y > 0.1) {
            this.setDeltaMovement(m.x, 0.1, m.z);
        }
    }

    public boolean isActive() {
        return this.entityData.get(ACTIVE);
    }

    public void setActive(boolean active) {
        this.entityData.set(ACTIVE, active);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setCharge(nbt.getFloat("charge"));
        this.setFacing(Direction.from3DDataValue(nbt.getByte("faceing")));
        this.setActive(nbt.getBoolean("active"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("charge", this.getCharge());
        nbt.putByte("faceing", (byte) this.getFacing().get3DDataValue());
        nbt.putBoolean("active", this.isActive());
    }

    public Direction getFacing() {
        return this.entityData.get(FACING);
    }

    public void setFacing(Direction face) {
        this.entityData.set(FACING, face);
    }

    public float getCharge() {
        return this.charge;
    }

    public void setCharge(float c) {
        this.charge = c;
    }

    @Override
    public void move(MoverType mt, Vec3 v) {
        super.move(mt, new Vec3(v.x / 5.0, v.y, v.z / 5.0));
    }

    @Override
    public void kill() {
        this.hurt(this.damageSources().fellOutOfWorld(), 400.0F);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        float b = EntityTurretCrossbow.lootingBonus(level, source);
        if (this.random.nextFloat() < 0.2F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.mindClockwork.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.2F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.morphicResonator.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.2F + b) {
            this.spawnAtLocation(new ItemStack(BlocksTC.crystalAir.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.2F + b) {
            this.spawnAtLocation(new ItemStack(BlocksTC.crystalEarth.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.5F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.mechanismSimple.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.5F + b) {
            this.spawnAtLocation(new ItemStack(ItemsTC.plateBrass.get()), 0.5F);
        }
        if (this.random.nextFloat() < 0.5F + b) {
            this.spawnAtLocation(new ItemStack(BlocksTC.plankGreatwood.get()), 0.5F);
        }
    }

    @Override
    public int getMaxHeadXRot() {
        return 10;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            this.clientDigging = true;
        } else if (id == 17) {
            this.clientDigging = false;
        } else {
            super.handleEntityEvent(id);
        }
    }
}
