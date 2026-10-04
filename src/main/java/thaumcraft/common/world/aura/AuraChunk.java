package thaumcraft.common.world.aura;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Аура одного чанка (порт AuraChunk): база (0..500), Вис и флюкс (0..32766).
 * Хранится как Data Attachment чанка thaumcraft:aura; формат NBT — как в оригинале (base: short, vis/flux: float).
 */
public class AuraChunk implements INBTSerializable<CompoundTag> {
    ChunkPos loc;
    short base;
    float vis;
    float flux;

    public AuraChunk() {
    }

    public AuraChunk(ChunkPos loc, short base, float vis, float flux) {
        this.loc = loc;
        this.base = base;
        this.vis = vis;
        this.flux = flux;
    }

    public short getBase() {
        return this.base;
    }

    public void setBase(short base) {
        this.base = base;
    }

    public float getVis() {
        return this.vis;
    }

    public void setVis(float vis) {
        this.vis = Math.min(32766.0F, Math.max(0.0F, vis));
    }

    public float getFlux() {
        return this.flux;
    }

    public void setFlux(float flux) {
        this.flux = Math.min(32766.0F, Math.max(0.0F, flux));
    }

    public ChunkPos getLoc() {
        return this.loc;
    }

    public void setLoc(ChunkPos loc) {
        this.loc = loc;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putShort("base", this.base);
        nbt.putFloat("flux", this.flux);
        nbt.putFloat("vis", this.vis);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.base = nbt.getShort("base");
        this.flux = nbt.getFloat("flux");
        this.vis = nbt.getFloat("vis");
    }
}
