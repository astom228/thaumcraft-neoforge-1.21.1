package thaumcraft.common.entities.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.fx.PacketFXBlockArc;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.tiles.misc.TileBanner;

/**
 * Большой портал культистов (порт EntityCultistPortalGreater), босс: 500 здоровья, броня 5, красная полоса из 6 делений.
 * Сначала ставит вокруг 4 знамени культа (в 6 блоках), затем, пока в 48 блоках есть игрок, выпускает 12 волн культистов
 * (тем чаще, чем их меньше) и на 13-й — Багрового претора; до этого лечится каждый тик. Позже каждый выпуск стоит ему
 * 5–9 здоровья. Касание — 8 магического урона. Выпадает изначальная жемчужина.
 */
public class EntityCultistPortalGreater extends Monster {
    protected final ServerBossEvent bossInfo = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_6).setDarkenScreen(true);
    int stage = 0;
    int stagecounter = 200;
    public int pulse = 0;

    public EntityCultistPortalGreater(EntityType<? extends EntityCultistPortalGreater> type, Level world) {
        super(type, world);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public int getArmorValue() {
        return 5;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("stage", this.stage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.stage = nbt.getInt("stage");
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
    }

    @Override
    public void aiStep() {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 4096.0;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.stagecounter <= 0) {
                if (this.level().getNearestPlayer(this, 48.0) != null) {
                    this.level().broadcastEntityEvent(this, (byte) 16);
                    if (this.stage < 5) {
                        this.stagecounter = 15 + this.random.nextInt(10 - this.stage) - this.stage;
                        this.spawnMinions();
                    } else if (this.stage == 12) {
                        this.stagecounter = 50 + this.getTiming() * 2 + this.random.nextInt(50);
                        this.spawnBoss();
                    } else {
                        int t = this.getTiming();
                        this.stagecounter = t + this.random.nextInt(5 + t / 3);
                        this.spawnMinions();
                    }
                    this.stage++;
                } else {
                    this.stagecounter = 30 + this.random.nextInt(30);
                }
            } else {
                this.stagecounter--;
                if (this.stagecounter == 160 && this.stage == 0) {
                    this.placeBanners();
                }
                // TODO этап 17: на этапе 0 (счётчик 20–150, раз в 13 тиков) вокруг появляются ящики с добычей (lootCrate*)
            }
            if (this.stage < 12) {
                this.heal(1.0F);
            }
            // в оригинале полоса обновлялась из ИИ-тика, который у портала не вызывался (DEVIATIONS 58)
            this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
        }
        if (this.pulse > 0) {
            this.pulse--;
        }
    }

    private void placeBanners() {
        this.level().broadcastEntityEvent(this, (byte) 16);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos bp = new BlockPos((int) this.getX() - dir.getStepX() * 6, (int) this.getY(), (int) this.getZ() + dir.getStepZ() * 6);
            if (!this.level().isLoaded(bp)) {
                continue;
            }
            this.level().setBlock(bp, BlocksTC.bannerCrimsonCult.get().defaultBlockState(), 3);
            if (this.level().getBlockEntity(bp) instanceof TileBanner te) {
                int face = switch (dir) {
                    case NORTH -> 8;
                    case WEST -> 12;
                    case EAST -> 4;
                    default -> 0;
                };
                te.setBannerFacing((byte) face);
                PacketDistributor.sendToPlayersNear((ServerLevel) this.level(), null, this.getX(), this.getY(), this.getZ(), 32.0,
                        new PacketFXBlockArc(bp, this, 0.5F + this.random.nextFloat() * 0.2F, 0.0F, 0.0F));
                this.playSound(SoundsTC.wandfail.get(), 1.0F, 1.0F);
            }
        }
    }

    int getTiming() {
        return EntityUtils.getEntitiesInRange(this.level(), this.getX(), this.getY(), this.getZ(), this, EntityCultist.class, 32.0).size() * 20;
    }

    private void summon(Mob cultist) {
        cultist.setPos(this.getX() + this.random.nextFloat() - this.random.nextFloat(), this.getY() + 0.25, this.getZ() + this.random.nextFloat() - this.random.nextFloat());
        ServerLevel sl = (ServerLevel) this.level();
        cultist.finalizeSpawn(sl, sl.getCurrentDifficultyAt(cultist.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        cultist.restrictTo(this.blockPosition(), 32);
        this.level().addFreshEntity(cultist);
        cultist.spawnAnim();
        cultist.playSound(SoundsTC.wandfail.get(), 1.0F, 1.0F);
    }

    void spawnMinions() {
        EntityType<? extends EntityCultist> type = this.random.nextFloat() > 0.33 ? ConfigEntities.CULTIST_KNIGHT.get() : ConfigEntities.CULTIST_CLERIC.get();
        EntityCultist cultist = type.create(this.level());
        if (cultist == null) {
            return;
        }
        this.summon(cultist);
        if (this.stage > 12) {
            this.hurt(this.damageSources().fellOutOfWorld(), 5 + this.random.nextInt(5));
        }
    }

    void spawnBoss() {
        EntityCultistLeader leader = ConfigEntities.CULTIST_LEADER.get().create(this.level());
        if (leader != null) {
            this.summon(leader);
        }
    }

    @Override
    public void playerTouch(Player p) {
        if (this.distanceToSqr(p) < 3.0 && p.hurt(this.damageSources().indirectMagic(this, this), 8.0F)) {
            this.playSound(SoundsTC.zap.get(), 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 1.0F);
        }
    }

    @Override
    protected float getSoundVolume() {
        return 0.75F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 540;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.monolith.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundsTC.zap.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.shock.get();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        EntityUtils.entityDropSpecialItem(this, new ItemStack(ItemsTC.primordialPearl.get()), this.getBbHeight() / 2.0F);
    }

    @Override
    public void handleEntityEvent(byte msg) {
        if (msg == 16) {
            this.pulse = 10;
        } else {
            super.handleEntityEvent(msg);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, Level.ExplosionInteraction.NONE);
        }
        super.die(cause);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossInfo.removePlayer(player);
    }

}
