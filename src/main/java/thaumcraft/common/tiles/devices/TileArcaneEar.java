package thaumcraft.common.tiles.devices;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import thaumcraft.common.blocks.devices.BlockArcaneEar;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.events.ArcaneEarEvents;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Магическое ухо (порт TileArcaneEar): настроено на ноту (0–24) и «тон» (инструмент блока, к которому прикреплено). Услышав ту же
 * ноту того же инструмента от нотного блока в радиусе 64 за этот тик, даёт сигнал 15 на 10 тиков (переключающее — меняет
 * состояние). В 1.12 тон — по материалу (камень, песок, стекло, дерево); в 1.21 инструмент нотного блока задаёт сам блок
 * (NoteBlockInstrument), ухо берёт его же — совпадает с нотными блоками на том же основании (DECISIONS 111).
 */
public class TileArcaneEar extends TileThaumcraft {
    public byte note = 0;
    public byte tone = 0;
    public int redstoneSignal = 0;

    public TileArcaneEar(BlockPos pos, BlockState state) {
        super(ConfigBlocks.ARCANE_EAR_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.note = (byte) Math.max(0, Math.min(24, nbt.getByte("note")));
        this.tone = nbt.getByte("tone");
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putByte("note", this.note);
        nbt.putByte("tone", this.tone);
        return nbt;
    }

    private Direction attached() {
        return this.getBlockState().getValue(BlockTCDeviceFacing.FACING).getOpposite();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileArcaneEar tile) {
        if (tile.redstoneSignal > 0) {
            tile.redstoneSignal--;
            if (tile.redstoneSignal == 0) {
                level.setBlock(pos, level.getBlockState(pos).setValue(BlockTCDeviceFacing.ENABLED, false), 3);
                BlockArcaneEar.notifyAround(level, pos);
            }
        }
        List<int[]> nbe = ArcaneEarEvents.getNotes(level);
        for (int[] dat : nbe) {
            if (dat[3] == tile.tone && dat[4] == tile.note && tile.worldPosition.distToCenterSqr(dat[0] + 0.5, dat[1] + 0.5, dat[2] + 0.5) <= 4096.0) {
                tile.triggerNote(level, pos, false);
                BlockState st = level.getBlockState(pos);
                if (st.getBlock() instanceof BlockArcaneEar ear && ear.toggle) {
                    level.setBlock(pos, st.setValue(BlockTCDeviceFacing.ENABLED, !st.getValue(BlockTCDeviceFacing.ENABLED)), 3);
                } else {
                    tile.redstoneSignal = 10;
                    level.setBlock(pos, st.setValue(BlockTCDeviceFacing.ENABLED, true), 3);
                }
                BlockArcaneEar.notifyAround(level, pos);
                break;
            }
        }
    }

    /** Инструмент блока, к которому прикреплено ухо (головы мобов сверху не учитываются — арфа). */
    public static int instrumentOf(Level level, BlockPos pos) {
        NoteBlockInstrument ins = level.getBlockState(pos).instrument();
        return ins.worksAboveNoteBlock() ? NoteBlockInstrument.HARP.ordinal() : ins.ordinal();
    }

    public void updateTone() {
        if (this.level != null) {
            this.tone = (byte) instrumentOf(this.level, this.worldPosition.relative(this.attached()));
            this.setChanged();
        }
    }

    public void changePitch() {
        this.note = (byte) ((this.note + 1) % 25);
        this.setChanged();
        this.syncTile(false);
    }

    /** Звук ноты (событие блока): со звуком — инструмент основания, без — «−1» (арфа), как в оригинале. */
    public void triggerNote(Level world, BlockPos pos, boolean sound) {
        int ins = -1;
        if (sound) {
            ins = instrumentOf(world, pos.relative(this.attached()));
        }
        world.blockEvent(pos, this.getBlockState().getBlock(), ins, this.note);
    }
}
