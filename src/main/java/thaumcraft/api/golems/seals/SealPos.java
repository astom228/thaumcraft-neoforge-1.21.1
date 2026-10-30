package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Место печати: блок и грань (порт SealPos). */
public class SealPos {
    public BlockPos pos;
    public Direction face;

    public SealPos(BlockPos pos, Direction face) {
        this.pos = pos;
        this.face = face;
    }

    @Override
    public int hashCode() {
        byte b0 = (byte) (this.face.ordinal() + 1);
        int i = 31 * b0 + this.pos.getX();
        i = 31 * i + this.pos.getY();
        return 31 * i + this.pos.getZ();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SealPos sp && this.pos.equals(sp.pos) && this.face == sp.face;
    }
}
