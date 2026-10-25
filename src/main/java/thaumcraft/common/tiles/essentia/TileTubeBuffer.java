package thaumcraft.common.tiles.essentia;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.essentia.BlockTube;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.SoundsTC;

/**
 * Буфер эссенции (порт TileTubeBuffer): хранит до 10 эссенции любых аспектов, всасывание 1 (с мехами — 32 за каждые).
 * Стороны можно «придушить» кастером с Shift: 1 — без усиления мехами, 2 — всасывание 0.
 */
public class TileTubeBuffer extends TileTube implements IAspectContainer {
    public AspectList aspects = new AspectList();
    public final int MAXAMOUNT = 10;
    public byte[] chokedSides = new byte[] {0, 0, 0, 0, 0, 0};
    int count = 0;
    int bellows = -1;

    public TileTubeBuffer(BlockPos pos, BlockState state) {
        super(ConfigBlocks.TUBE_BUFFER_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        this.aspects.readFromNBT(nbttagcompound);
        byte[] sides = nbttagcompound.getByteArray("open");
        if (sides.length == 6) {
            for (int a = 0; a < 6; a++) {
                this.openSides[a] = sides[a] == 1;
            }
        }
        this.chokedSides = nbttagcompound.getByteArray("choke");
        if (this.chokedSides.length < 6) {
            this.chokedSides = new byte[] {0, 0, 0, 0, 0, 0};
        }
        this.facing = Direction.values()[Mth.clamp(nbttagcompound.getInt("side"), 0, 5)];
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        this.aspects.writeToNBT(nbttagcompound);
        byte[] sides = new byte[6];
        for (int a = 0; a < 6; a++) {
            sides[a] = (byte) (this.openSides[a] ? 1 : 0);
        }
        nbttagcompound.putByteArray("open", sides);
        nbttagcompound.putByteArray("choke", this.chokedSides);
        nbttagcompound.putInt("side", this.facing.ordinal());
        return nbttagcompound;
    }

    @Override
    public AspectList getAspects() {
        return this.aspects;
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public int addToContainer(Aspect tt, int am) {
        if (am != 1) {
            return am;
        }
        if (this.aspects.visSize() < 10) {
            this.aspects.add(tt, am);
            this.setChanged();
            this.syncTile(false);
            return 0;
        }
        return am;
    }

    @Override
    public boolean takeFromContainer(Aspect tt, int am) {
        if (this.aspects.getAmount(tt) >= am) {
            this.aspects.remove(tt, am);
            this.setChanged();
            this.syncTile(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amt) {
        return this.aspects.getAmount(tag) >= amt;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return this.aspects.getAmount(tag);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face != null && this.openSides[face.ordinal()];
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != null && this.openSides[face.ordinal()];
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face != null && this.openSides[face.ordinal()];
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public Aspect getSuctionType(Direction loc) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        if (loc == null) {
            return 1;
        }
        return this.chokedSides[loc.ordinal()] == 2 ? 0 : (this.bellows > 0 && this.chokedSides[loc.ordinal()] != 1 ? this.bellows * 32 : 1);
    }

    @Override
    public Aspect getEssentiaType(Direction loc) {
        return this.aspects.size() > 0 ? this.aspects.getAspects()[this.level.random.nextInt(this.aspects.getAspects().length)] : null;
    }

    @Override
    public int getEssentiaAmount(Direction loc) {
        return this.aspects.visSize();
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (!this.canOutputTo(face)) {
            return 0;
        }
        int suction = 0;
        BlockEntity te = this.connectable(face);
        if (te != null) {
            suction = ((IEssentiaTransport) te).getSuctionAmount(face.getOpposite());
        }
        for (Direction dir : Direction.values()) {
            if (this.canOutputTo(dir) && dir != face) {
                te = this.connectable(dir);
                if (te != null) {
                    IEssentiaTransport ic = (IEssentiaTransport) te;
                    int sa = ic.getSuctionAmount(dir.getOpposite());
                    Aspect su = ic.getSuctionType(dir.getOpposite());
                    if ((su == aspect || su == null) && suction < sa && this.getSuctionAmount(dir) < sa) {
                        return 0;
                    }
                }
            }
        }
        if (amount > this.aspects.getAmount(aspect)) {
            amount = this.aspects.getAmount(aspect);
        }
        return this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return this.canInputFrom(face) ? amount - this.addToContainer(aspect, amount) : 0;
    }

    @Override
    public void update() {
        this.count++;
        if (this.bellows < 0 || this.count % 20 == 0) {
            this.getBellows();
        }
        if (!this.level.isClientSide && this.count % 5 == 0 && this.aspects.visSize() < 10) {
            this.fillBuffer();
        }
    }

    void fillBuffer() {
        for (Direction dir : Direction.values()) {
            BlockEntity te = this.connectable(dir);
            if (te != null) {
                IEssentiaTransport ic = (IEssentiaTransport) te;
                if (ic.getEssentiaAmount(dir.getOpposite()) > 0 && ic.getSuctionAmount(dir.getOpposite()) < this.getSuctionAmount(dir)
                        && this.getSuctionAmount(dir) >= ic.getMinimumSuction()) {
                    Aspect ta = ic.getEssentiaType(dir.getOpposite());
                    this.addToContainer(ta, ic.takeEssentia(ta, 1, dir.getOpposite()));
                    return;
                }
            }
        }
    }

    /** TileBellows.getBellows — мехи со всех сторон; мехи — этап 14. */
    public void getBellows() {
        // TODO этап 14: this.bellows = TileBellows.getBellows(this.level, this.worldPosition, Direction.values());
        this.bellows = 0;
    }

    @Override
    public boolean onCasterRightClick(Level world, ItemStack wandstack, Player player, BlockPos bp, Direction side, InteractionHand hand) {
        int hit = this.getSubHit(player);
        if (hit < 0 || hit >= 6) {
            return false;
        }
        player.swing(hand);
        if (player.isShiftKeyDown()) {
            world.playLocalSound(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, SoundsTC.squeek.get(), SoundSource.BLOCKS, 0.6F,
                    2.0F + world.random.nextFloat() * 0.2F, false);
            if (!world.isClientSide) {
                this.chokedSides[hit]++;
                if (this.chokedSides[hit] > 2) {
                    this.chokedSides[hit] = 0;
                }
                this.setChanged();
                this.syncTile(true);
            }
        } else {
            world.playLocalSound(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, SoundsTC.tool.get(), SoundSource.BLOCKS, 0.5F,
                    0.9F + world.random.nextFloat() * 0.2F, false);
            this.openSides[hit] = !this.openSides[hit];
            Direction dir = Direction.values()[hit];
            if (world.getBlockEntity(this.worldPosition.relative(dir)) instanceof TileTube tile) {
                tile.openSides[dir.getOpposite().ordinal()] = this.openSides[hit];
                tile.syncTile(true);
                tile.setChanged();
                BlockTube.refreshConnections(world, tile.getBlockPos());
            }
            this.setChanged();
            this.syncTile(true);
            BlockTube.refreshConnections(world, this.worldPosition);
        }
        return true;
    }

    @Override
    public void addTraceableCuboids(List<IndexedAABB> cuboids) {
        float min = 0.375F;
        float max = 0.625F;
        int x = this.worldPosition.getX();
        int y = this.worldPosition.getY();
        int z = this.worldPosition.getZ();
        if (this.canConnectSide(Direction.DOWN)) {
            cuboids.add(new IndexedAABB(0, new AABB(x + min, y, z + min, x + max, y + 0.5, z + max)));
        }
        if (this.canConnectSide(Direction.UP)) {
            cuboids.add(new IndexedAABB(1, new AABB(x + min, y + 0.5, z + min, x + max, y + 1, z + max)));
        }
        if (this.canConnectSide(Direction.NORTH)) {
            cuboids.add(new IndexedAABB(2, new AABB(x + min, y + min, z, x + max, y + max, z + 0.5)));
        }
        if (this.canConnectSide(Direction.SOUTH)) {
            cuboids.add(new IndexedAABB(3, new AABB(x + min, y + min, z + 0.5, x + max, y + max, z + 1)));
        }
        if (this.canConnectSide(Direction.WEST)) {
            cuboids.add(new IndexedAABB(4, new AABB(x, y + min, z + min, x + 0.5, y + max, z + max)));
        }
        if (this.canConnectSide(Direction.EAST)) {
            cuboids.add(new IndexedAABB(5, new AABB(x + 0.5, y + min, z + min, x + 1, y + max, z + max)));
        }
        cuboids.add(new IndexedAABB(6, new AABB(x + 0.25F, y + 0.25F, z + 0.25F, x + 0.75F, y + 0.75F, z + 0.75F)));
    }
}
