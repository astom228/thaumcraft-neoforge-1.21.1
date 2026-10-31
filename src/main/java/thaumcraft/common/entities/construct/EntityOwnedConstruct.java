package thaumcraft.common.entities.construct;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import thaumcraft.common.lib.SoundsTC;

/**
 * Конструкт с владельцем (порт EntityOwnedConstruct): големы, турели, бур. Не дышит, не исчезает, свой с командой
 * владельца; чужой игрок взаимодействовать не может. Появившийся не из своего предмета (validSpawn) — исчезает.
 */
public class EntityOwnedConstruct extends PathfinderMob implements OwnableEntity {
    protected static final EntityDataAccessor<Byte> TAMED = SynchedEntityData.defineId(EntityOwnedConstruct.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UNIQUE_ID = SynchedEntityData.defineId(EntityOwnedConstruct.class,
            EntityDataSerializers.OPTIONAL_UUID);
    boolean validSpawn = false;

    public EntityOwnedConstruct(EntityType<? extends EntityOwnedConstruct> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TAMED, (byte) 0);
        builder.define(OWNER_UNIQUE_ID, Optional.empty());
    }

    public boolean isOwned() {
        return (this.entityData.get(TAMED) & 4) != 0;
    }

    public void setOwned(boolean tamed) {
        byte b0 = this.entityData.get(TAMED);
        this.entityData.set(TAMED, tamed ? (byte) (b0 | 4) : (byte) (b0 & -5));
    }

    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UNIQUE_ID).orElse(null);
    }

    public UUID getOwnerId() {
        return this.getOwnerUUID();
    }

    public void setOwnerId(UUID id) {
        this.entityData.set(OWNER_UNIQUE_ID, Optional.ofNullable(id));
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.clack.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundsTC.clack.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.tool.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getTarget() != null && this.isAlliedTo(this.getTarget())) {
            this.setTarget(null);
        }
        if (!this.level().isClientSide && !this.validSpawn) {
            this.discard();
        }
    }

    public void setValidSpawn() {
        this.validSpawn = true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("v", this.validSpawn);
        tag.putString("OwnerUUID", this.getOwnerId() == null ? "" : this.getOwnerId().toString());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.validSpawn = tag.getBoolean("v");
        String s = tag.getString("OwnerUUID");
        if (!s.isEmpty()) {
            try {
                this.setOwnerId(UUID.fromString(s));
                this.setOwned(true);
            } catch (IllegalArgumentException e) {
                this.setOwned(false);
            }
        }
    }

    public LivingEntity getOwnerEntity() {
        UUID uuid = this.getOwnerId();
        return uuid == null ? null : this.level().getPlayerByUUID(uuid);
    }

    @Override
    public LivingEntity getOwner() {
        return this.getOwnerEntity();
    }

    public boolean isOwner(LivingEntity e) {
        return e != null && e == this.getOwnerEntity();
    }

    @Override
    public PlayerTeam getTeam() {
        if (this.isOwned() && this.getOwnerEntity() instanceof Player p) {
            return p.getTeam();
        }
        return super.getTeam();
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (this.isOwned()) {
            LivingEntity owner = this.getOwnerEntity();
            if (other == owner) {
                return true;
            }
            if (owner != null) {
                return owner.isAlliedTo(other);
            }
        }
        return super.isAlliedTo(other);
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide && this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && this.hasCustomName()
                && this.getOwnerEntity() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(this.getCombatTracker().getDeathMessage());
        }
        super.die(cause);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || player.getMainHandItem().getItem() instanceof NameTagItem) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide && !this.isOwner(player)) {
            player.displayClientMessage(Component.translatable("tc.notowned").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
}
