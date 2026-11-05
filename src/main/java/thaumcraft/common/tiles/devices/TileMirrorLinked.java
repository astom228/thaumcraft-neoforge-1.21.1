package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Общая часть магического зеркала и зеркала эссенции (в оригинале продублирована в TileMirror / TileMirrorEssentia):
 * связь с парным зеркалом (в т.ч. в другом измерении), её восстановление и проверка, нестабильность.
 * Раздел 7: периодическая проверка связи не загружает чанк парного зеркала (пока он не загружен, связь не рвётся);
 * сам перенос предметов и эссенции, как в оригинале, загружает его (load = true) — DECISIONS 90.
 */
public abstract class TileMirrorLinked extends TileThaumcraft {
    /** FULL (distance 0), без тиков блоков/сущностей; срок обновляется только при запросе переноса. */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TRANSFER_TICKET =
            net.minecraft.server.level.TicketType.create("thaumcraft_mirror_transfer",
                    java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 80);
    /** Кратко сохраняет только данные чанка между связанными запросами, не включая симуляцию. */
    public static void retainTransferChunk(ServerLevel world, BlockPos pos) {
        var chunk = new net.minecraft.world.level.ChunkPos(pos);
        world.getChunkSource().addRegionTicket(TRANSFER_TICKET, chunk, 0, chunk);
    }
    public boolean linked = false;
    public BlockPos linkPos = BlockPos.ZERO;
    public ResourceKey<Level> linkDim = Level.OVERWORLD;
    public int instability;
    int count = 0;
    int inc = 40;

    protected TileMirrorLinked(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Порог нестабильности, после которого 1 Выброс уходит в ауру (128 у магического, 64 у зеркала эссенции). */
    protected abstract int instabilityLimit();

    protected abstract boolean isPartner(BlockEntity te);

    /** Связь восстановлена (зеркало эссенции запоминает сторону парного). */
    protected void onLinkRestored(ServerLevel targetWorld) {
    }

    /** Связь разорвана со стороны парного зеркала. */
    protected void onUnlinked() {
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.linked = nbt.getBoolean("linked");
        this.linkPos = new BlockPos(nbt.getInt("linkX"), nbt.getInt("linkY"), nbt.getInt("linkZ"));
        ResourceLocation dim = ResourceLocation.tryParse(nbt.getString("linkDim"));
        this.linkDim = dim != null ? ResourceKey.create(Registries.DIMENSION, dim) : Level.OVERWORLD;
        this.instability = nbt.getInt("instability");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putBoolean("linked", this.linked);
        nbt.putInt("linkX", this.linkPos.getX());
        nbt.putInt("linkY", this.linkPos.getY());
        nbt.putInt("linkZ", this.linkPos.getZ());
        nbt.putString("linkDim", this.linkDim.location().toString());
        nbt.putInt("instability", this.instability);
        return nbt;
    }

    /** Зеркало-предмет со связью → блок (placeBlockAt BlockMirrorItem): запомнить цель и сразу восстановить связь. */
    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        GlobalPos link = input.get(TCDataComponents.MIRROR_LINK.get());
        if (link != null) {
            this.linkPos = link.pos();
            this.linkDim = link.dimension();
        }
    }

    /** Блок → предмет (dropMirror): связанное зеркало выпадает со связью. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.linked) {
            components.set(TCDataComponents.MIRROR_LINK.get(), GlobalPos.of(this.linkDim, this.linkPos));
        }
    }

    protected ServerLevel targetWorld() {
        return this.level != null && this.level.getServer() != null ? this.level.getServer().getLevel(this.linkDim) : null;
    }

    /** Мир цели есть и её чанк загружен (или его можно загрузить — load). */
    protected boolean targetLoaded(boolean load) {
        ServerLevel tw = this.targetWorld();
        if (tw == null) return false;
        if (load) {
            retainTransferChunk(tw, this.linkPos);
        }
        return load || tw.isLoaded(this.linkPos);
    }

    /** Парное зеркало (null, если его нет или чанк не загружен и load = false). */
    protected BlockEntity targetTile(boolean load) {
        if (!this.targetLoaded(load)) {
            return null;
        }
        BlockEntity te = this.targetWorld().getBlockEntity(this.linkPos);
        return this.isPartner(te) ? te : null;
    }

    private boolean pointsAtMe(TileMirrorLinked tm) {
        return tm.linkPos.equals(this.worldPosition) && tm.linkDim == this.level.dimension();
    }

    private void unlink() {
        this.linked = false;
        this.setChanged();
        this.syncTile(false);
    }

    public void restoreLink() {
        if (this.isDestinationValid()) {
            ServerLevel tw = this.targetWorld();
            if (tw.getBlockEntity(this.linkPos) instanceof TileMirrorLinked tm && this.isPartner(tm)) {
                tm.linked = true;
                tm.linkPos = this.worldPosition;
                tm.linkDim = this.level.dimension();
                tm.syncTile(false);
                this.onLinkRestored(tw);
                this.linked = true;
                this.setChanged();
                tm.setChanged();
                this.syncTile(false);
            }
        }
    }

    /** Разорвать связь у парного зеркала (при снятии этого зеркала). */
    public void invalidateLink() {
        if (this.targetLoaded(false) && this.targetWorld().getBlockEntity(this.linkPos) instanceof TileMirrorLinked tm && this.isPartner(tm)) {
            tm.linked = false;
            tm.onUnlinked();
            this.setChanged();
            tm.setChanged();
            tm.syncTile(false);
        }
    }

    public boolean isLinkValid(boolean load) {
        if (!this.linked || !this.targetLoaded(load)) {
            return false;
        }
        BlockEntity te = this.targetWorld().getBlockEntity(this.linkPos);
        if (te instanceof TileMirrorLinked tm && this.isPartner(tm)) {
            if (!tm.linked) {
                this.unlink();
                return false;
            }
            if (this.pointsAtMe(tm)) {
                return true;
            }
        }
        this.unlink();
        return false;
    }

    public boolean isLinkValidSimple() {
        if (!this.linked || !this.targetLoaded(false)) {
            return false;
        }
        return this.targetWorld().getBlockEntity(this.linkPos) instanceof TileMirrorLinked tm && this.isPartner(tm) && tm.linked && this.pointsAtMe(tm);
    }

    public boolean isDestinationValid() {
        if (!this.targetLoaded(false)) {
            return false;
        }
        BlockEntity te = this.targetWorld().getBlockEntity(this.linkPos);
        if (te instanceof TileMirrorLinked tm && this.isPartner(tm)) {
            return !tm.isLinkValid(false);
        }
        this.unlink();
        return false;
    }

    protected void addInstability(int amt) {
        this.instability += amt;
        this.setChanged();
    }

    /** Серверный тик (update оригинала): связь проверяется раз в inc тиков, при неудаче — всё реже (до 600). */
    public void serverTick() {
        this.checkInstability();
        if (this.count++ % this.inc == 0) {
            if (!this.isLinkValidSimple()) {
                if (this.inc < 600) {
                    this.inc += 20;
                }
                this.restoreLink();
            } else {
                this.inc = 40;
            }
        }
    }

    public void checkInstability() {
        int limit = this.instabilityLimit();
        if (this.instability > limit) {
            AuraHelper.polluteAura(this.level, this.worldPosition, 1.0F, true);
            this.instability -= limit;
            this.setChanged();
        }
        if (this.instability > 0 && this.count % 100 == 0) {
            this.instability--;
        }
    }
}
