package thaumcraft.common.tiles.essentia;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Мозг в банке (порт TileJarBrain; в оригинале — пакет devices): притягивает сферы опыта в радиусе 8 и поглощает их (до 2000);
 * на клиенте мозг поворачивается к ближайшему игроку и иногда вздыхает. ПКМ — выпустить часть опыта.
 */
public class TileJarBrain extends TileJar {
    protected static final Random rand = new Random();
    public float field_40063_b;
    public float field_40061_d;
    public float field_40059_f;
    public float field_40066_q;
    public float rota;
    public float rotb;
    public int xp = 0;
    public int xpMax = 2000;
    public int eatDelay = 0;
    long lastsigh = System.currentTimeMillis() + 1500L;

    public TileJarBrain(BlockPos pos, BlockState state) {
        super(ConfigBlocks.BRAIN_JAR_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        this.xp = nbttagcompound.getInt("XP");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        nbttagcompound.putInt("XP", this.xp);
        return nbttagcompound;
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.xp = input.getOrDefault(TCDataComponents.BRAIN_XP.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.xp > 0) {
            components.set(TCDataComponents.BRAIN_XP.get(), this.xp);
        }
    }

    /** Опыт всей сферы: в 1.21 близкие сферы сливаются (count), в 1.12 слияния не было. */
    private static int orbValue(ExperienceOrb eo) {
        int count = Math.max(1, eo.saveWithoutId(new CompoundTag()).getInt("Count"));
        return eo.getValue() * count;
    }

    @Override
    public void update() {
        Entity entity = null;
        if (this.xp > this.xpMax) {
            this.xp = this.xpMax;
        }
        BlockPos pos = this.worldPosition;
        if (this.xp < this.xpMax) {
            entity = this.getClosestXPOrb();
            if (entity != null && this.eatDelay == 0) {
                double var3 = (pos.getX() + 0.5 - entity.getX()) / 25.0;
                double var5 = (pos.getY() + 0.5 - entity.getY()) / 25.0;
                double var7 = (pos.getZ() + 0.5 - entity.getZ()) / 25.0;
                double var9 = Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7);
                double var11 = 1.0 - var9;
                if (var11 > 0.0) {
                    var11 *= var11;
                    Vec3 m = entity.getDeltaMovement();
                    entity.setDeltaMovement(m.x + var3 / var9 * var11 * 0.3, m.y + var5 / var9 * var11 * 0.5, m.z + var7 / var9 * var11 * 0.3);
                }
            }
        }
        if (this.level.isClientSide) {
            this.rotb = this.rota;
            if (entity == null) {
                entity = this.level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0, false);
                if (entity != null && this.lastsigh < System.currentTimeMillis()) {
                    this.level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsTC.brain.get(), SoundSource.AMBIENT, 0.15F,
                            0.8F + this.level.random.nextFloat() * 0.4F, false);
                    this.lastsigh = System.currentTimeMillis() + 5000L + this.level.random.nextInt(25000);
                }
            }
            if (entity != null) {
                double d = entity.getX() - (pos.getX() + 0.5F);
                double d1 = entity.getZ() - (pos.getZ() + 0.5F);
                this.field_40066_q = (float) Math.atan2(d1, d);
                this.field_40059_f += 0.1F;
                if (this.field_40059_f < 0.5F || rand.nextInt(40) == 0) {
                    float f3 = this.field_40061_d;
                    do {
                        this.field_40061_d = this.field_40061_d + (rand.nextInt(4) - rand.nextInt(4));
                    } while (f3 == this.field_40061_d);
                }
            } else {
                this.field_40066_q += 0.01F;
            }
            while (this.rota >= 3.141593F) {
                this.rota -= 6.283185F;
            }
            while (this.rota < -3.141593F) {
                this.rota += 6.283185F;
            }
            while (this.field_40066_q >= 3.141593F) {
                this.field_40066_q -= 6.283185F;
            }
            while (this.field_40066_q < -3.141593F) {
                this.field_40066_q += 6.283185F;
            }
            float f = this.field_40066_q - this.rota;
            while (f >= 3.141593F) {
                f -= 6.283185F;
            }
            while (f < -3.141593F) {
                f += 6.283185F;
            }
            this.rota += f * 0.04F;
        }
        if (this.eatDelay > 0) {
            this.eatDelay--;
        } else if (this.xp < this.xpMax) {
            List<ExperienceOrb> ents = this.level.getEntitiesOfClass(ExperienceOrb.class,
                    new AABB(pos.getX() - 0.1, pos.getY() - 0.1, pos.getZ() - 0.1, pos.getX() + 1.1, pos.getY() + 1.1, pos.getZ() + 1.1));
            if (!ents.isEmpty()) {
                for (ExperienceOrb eo : ents) {
                    this.xp = this.xp + orbValue(eo);
                    eo.playSound(SoundEvents.GENERIC_EAT, 0.1F, (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F + 1.0F);
                    eo.discard();
                }
                this.syncTile(false);
                this.setChanged();
            }
        }
    }

    public Entity getClosestXPOrb() {
        double cdist = Double.MAX_VALUE;
        Entity orb = null;
        List<ExperienceOrb> ents = this.level.getEntitiesOfClass(ExperienceOrb.class, new AABB(this.worldPosition).inflate(8.0));
        for (ExperienceOrb eo : ents) {
            double d = eo.distanceToSqr(Vec3.atCenterOf(this.worldPosition));
            if (d < cdist) {
                orb = eo;
                cdist = d;
            }
        }
        return orb;
    }
}
