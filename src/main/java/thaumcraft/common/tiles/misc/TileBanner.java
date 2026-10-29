package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.basic.BlockBannerTC;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Знамя (порт TileBanner): поворот (0–15, как у ванильного знамени), на стене ли, нарисованный аспект. Аспект переносится в
 * предмет и обратно компонентом thaumcraft:banner_aspect (NBT "aspect" оригинала).
 */
public class TileBanner extends TileThaumcraft {
    private byte facing = 0;
    private Aspect aspect = null;
    private boolean onWall = false;

    public TileBanner(BlockPos pos, BlockState state) {
        super(ConfigBlocks.BANNER_TILE.get(), pos, state);
    }

    public byte getBannerFacing() {
        return this.facing;
    }

    public void setBannerFacing(byte face) {
        this.facing = face;
        this.setChanged();
    }

    public boolean getWall() {
        return this.onWall;
    }

    public void setWall(boolean b) {
        this.onWall = b;
        this.setChanged();
    }

    public Aspect getAspect() {
        return this.aspect;
    }

    public void setAspect(Aspect aspect) {
        this.aspect = aspect;
    }

    /** Цвет ткани (RGB) или −1 — знамя Багрового культа. */
    public int getColor() {
        return this.getBlockState().getBlock() instanceof BlockBannerTC b && b.dye != null ? b.dye.getTextureDiffuseColor() & 0xFFFFFF : -1;
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.facing = nbt.getByte("facing");
        String as = nbt.getString("aspect");
        this.aspect = as.isEmpty() ? null : Aspect.getAspect(as);
        this.onWall = nbt.getBoolean("wall");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putByte("facing", this.facing);
        nbt.putString("aspect", this.aspect == null ? "" : this.aspect.getTag());
        nbt.putBoolean("wall", this.onWall);
        return nbt;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        String as = input.get(TCDataComponents.BANNER_ASPECT.get());
        if (as != null) {
            this.aspect = Aspect.getAspect(as);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        if (this.aspect != null) {
            builder.set(TCDataComponents.BANNER_ASPECT.get(), this.aspect.getTag());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("aspect");
    }
}
