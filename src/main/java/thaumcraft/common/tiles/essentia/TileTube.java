package thaumcraft.common.tiles.essentia;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.casters.IInteractWithCaster;
import thaumcraft.common.blocks.essentia.BlockTube;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Трубка эссенции (порт TileTube): держит не больше 1 эссенции; всасывание берётся от соседа с наибольшим всасыванием минус 1,
 * эссенция перетекает от меньшего всасывания к большему. Если с соседом конфликт по типу при равном всасывании —
 * трубка «стравливает» (пар и шипение). Стороны открываются/закрываются кастером (этап 12), facing — для клапана и односторонней.
 */
public class TileTube extends TileThaumcraft implements IEssentiaTransport, IInteractWithCaster {
    public static final int freq = 5;
    public Direction facing = Direction.NORTH;
    public boolean[] openSides = new boolean[] {true, true, true, true, true, true};
    Aspect essentiaType = null;
    int essentiaAmount = 0;
    Aspect suctionType = null;
    int suction = 0;
    int venting = 0;
    int count = 0;
    int ventColor = 0;

    public TileTube(BlockPos pos, BlockState state) {
        this(ConfigBlocks.TUBE_TILE.get(), pos, state);
    }

    protected TileTube(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        this.essentiaType = Aspect.getAspect(nbttagcompound.getString("type"));
        this.essentiaAmount = nbttagcompound.getInt("amount");
        this.facing = Direction.values()[Mth.clamp(nbttagcompound.getInt("side"), 0, 5)];
        byte[] sides = nbttagcompound.getByteArray("open");
        if (sides.length == 6) {
            for (int a = 0; a < 6; a++) {
                this.openSides[a] = sides[a] == 1;
            }
        }
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbttagcompound, HolderLookup.Provider registries) {
        if (this.essentiaType != null) {
            nbttagcompound.putString("type", this.essentiaType.getTag());
        }
        nbttagcompound.putInt("amount", this.essentiaAmount);
        byte[] sides = new byte[6];
        for (int a = 0; a < 6; a++) {
            sides[a] = (byte) (this.openSides[a] ? 1 : 0);
        }
        nbttagcompound.putInt("side", this.facing.ordinal());
        nbttagcompound.putByteArray("open", sides);
        return nbttagcompound;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.suctionType = Aspect.getAspect(nbt.getString("stype"));
        this.suction = nbt.getInt("samount");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        if (this.suctionType != null) {
            nbt.putString("stype", this.suctionType.getTag());
        }
        nbt.putInt("samount", this.suction);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileTube te) {
        te.update();
    }

    public void update() {
        if (this.venting > 0) {
            this.venting--;
        }
        if (this.count == 0) {
            this.count = this.level.random.nextInt(10);
        }
        if (!this.level.isClientSide) {
            if (this.venting <= 0) {
                if (++this.count % 2 == 0) {
                    this.calculateSuction(null, false, false);
                    this.checkVenting();
                    if (this.essentiaType != null && this.essentiaAmount == 0) {
                        this.essentiaType = null;
                    }
                }
                if (this.count % 5 == 0 && this.suction > 0) {
                    this.equalizeWithNeighbours(false);
                }
            }
        } else if (this.venting > 0) {
            Random r = new Random(this.hashCode() * 4L);
            float rp = r.nextFloat() * 360.0F;
            float ry = r.nextFloat() * 360.0F;
            double fx = -Mth.sin(ry / 180.0F * (float) Math.PI) * Mth.cos(rp / 180.0F * (float) Math.PI);
            double fz = Mth.cos(ry / 180.0F * (float) Math.PI) * Mth.cos(rp / 180.0F * (float) Math.PI);
            double fy = -Mth.sin(rp / 180.0F * (float) Math.PI);
            IFXDispatcher.get().drawVentParticles(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5,
                    fx / 5.0, fy / 5.0, fz / 5.0, this.ventColor);
        }
    }

    protected BlockEntity connectable(Direction loc) {
        return ThaumcraftApiHelper.getConnectableTile(this.level, this.worldPosition, loc);
    }

    void calculateSuction(Aspect filter, boolean restrict, boolean directional) {
        this.suction = 0;
        this.suctionType = null;
        for (Direction loc : Direction.values()) {
            try {
                if ((!directional || this.facing == loc.getOpposite()) && this.isConnectable(loc)) {
                    BlockEntity te = this.connectable(loc);
                    if (te != null) {
                        IEssentiaTransport ic = (IEssentiaTransport) te;
                        Aspect ist = ic.getSuctionType(loc.getOpposite());
                        if ((filter == null || ist == null || ist == filter)
                                && (filter != null || this.getEssentiaAmount(loc) <= 0 || ist == null || this.getEssentiaType(loc) == ist)
                                && (filter == null || this.getEssentiaAmount(loc) <= 0 || this.getEssentiaType(loc) == null || ist == null
                                || this.getEssentiaType(loc) == ist)) {
                            int suck = ic.getSuctionAmount(loc.getOpposite());
                            if (suck > 0 && suck > this.suction + 1) {
                                Aspect st = ist;
                                if (st == null) {
                                    st = filter;
                                }
                                this.setSuction(st, restrict ? suck / 2 : suck - 1);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    void checkVenting() {
        for (Direction loc : Direction.values()) {
            try {
                if (this.isConnectable(loc)) {
                    BlockEntity te = this.connectable(loc);
                    if (te != null) {
                        IEssentiaTransport ic = (IEssentiaTransport) te;
                        int suck = ic.getSuctionAmount(loc.getOpposite());
                        if (this.suction > 0 && (suck == this.suction || suck == this.suction - 1) && this.suctionType != ic.getSuctionType(loc.getOpposite())
                                && !(te instanceof TileTubeFilter)) {
                            int c = -1;
                            if (this.suctionType != null) {
                                // ModConfig.aspectOrder оригинала — порядок регистрации аспектов
                                c = new ArrayList<>(Aspect.aspects.values()).indexOf(this.suctionType);
                            }
                            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, c);
                            this.venting = 40;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    void equalizeWithNeighbours(boolean directional) {
        if (this.essentiaAmount > 0) {
            return;
        }
        for (Direction loc : Direction.values()) {
            try {
                if ((!directional || this.facing != loc.getOpposite()) && this.isConnectable(loc)) {
                    BlockEntity te = this.connectable(loc);
                    if (te != null) {
                        IEssentiaTransport ic = (IEssentiaTransport) te;
                        if (ic.canOutputTo(loc.getOpposite())
                                && (this.getSuctionType(null) == null || this.getSuctionType(null) == ic.getEssentiaType(loc.getOpposite())
                                || ic.getEssentiaType(loc.getOpposite()) == null)
                                && this.getSuctionAmount(null) > ic.getSuctionAmount(loc.getOpposite())
                                && this.getSuctionAmount(null) >= ic.getMinimumSuction()) {
                            Aspect a = this.getSuctionType(null);
                            if (a == null) {
                                a = ic.getEssentiaType(loc.getOpposite());
                                if (a == null) {
                                    a = ic.getEssentiaType(null);
                                }
                            }
                            int am = this.addEssentia(a, ic.takeEssentia(a, 1, loc.getOpposite()), loc);
                            if (am > 0) {
                                if (this.level.random.nextInt(100) == 0) {
                                    this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 0, 0);
                                }
                                return;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
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
        this.suctionType = aspect;
        this.suction = amount;
    }

    @Override
    public Aspect getSuctionType(Direction loc) {
        return this.suctionType;
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        return this.suction;
    }

    @Override
    public Aspect getEssentiaType(Direction loc) {
        return this.essentiaType;
    }

    @Override
    public int getEssentiaAmount(Direction loc) {
        return this.essentiaAmount;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (aspect != null && this.canOutputTo(face) && this.essentiaType == aspect && this.essentiaAmount > 0 && amount > 0) {
            this.essentiaAmount--;
            if (this.essentiaAmount <= 0) {
                this.essentiaType = null;
            }
            this.setChanged();
            return 1;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (aspect != null && this.canInputFrom(face) && this.essentiaAmount == 0 && amount > 0) {
            this.essentiaType = aspect;
            this.essentiaAmount++;
            this.setChanged();
            return 1;
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 0) {
            if (this.level.isClientSide) {
                this.level.playLocalSound(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5,
                        SoundsTC.creak.get(), SoundSource.AMBIENT, 1.0F, 1.3F + this.level.random.nextFloat() * 0.2F, false);
            }
            return true;
        }
        if (i != 1) {
            return super.triggerEvent(i, j);
        }
        if (this.level.isClientSide) {
            if (this.venting <= 0) {
                this.level.playLocalSound(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5,
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.1F, 1.0F + this.level.random.nextFloat() * 0.1F, false);
            }
            this.venting = 50;
            List<Aspect> order = new ArrayList<>(Aspect.aspects.values());
            if (j != -1 && j < order.size()) {
                this.ventColor = order.get(j).getColor();
            } else {
                this.ventColor = 11184810;
            }
        }
        return true;
    }

    /** Переключение стороны (subHit 0–5) или поворот facing (6) кастером; кастеры — этап 12. */
    @Override
    public boolean onCasterRightClick(Level world, ItemStack wandstack, Player player, BlockPos bp, Direction side, InteractionHand hand) {
        int hit = this.getSubHit(player);
        if (hit < 0) {
            return false;
        }
        if (hit < 6) {
            world.playLocalSound(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, SoundsTC.tool.get(), SoundSource.BLOCKS, 0.5F,
                    0.9F + world.random.nextFloat() * 0.2F, false);
            player.swing(hand);
            this.setChanged();
            this.openSides[hit] = !this.openSides[hit];
            this.syncTile(true);
            Direction dir = Direction.values()[hit];
            if (world.getBlockEntity(this.worldPosition.relative(dir)) instanceof TileTube tile) {
                tile.openSides[dir.getOpposite().ordinal()] = this.openSides[hit];
                tile.syncTile(true);
                tile.setChanged();
                BlockTube.refreshConnections(world, tile.getBlockPos());
            }
            BlockTube.refreshConnections(world, this.worldPosition);
            return true;
        }
        world.playLocalSound(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, SoundsTC.tool.get(), SoundSource.BLOCKS, 0.5F,
                0.9F + world.random.nextFloat() * 0.2F, false);
        player.swing(hand);
        int a = this.facing.ordinal();
        this.setChanged();
        while (++a < 20) {
            Direction d = Direction.values()[a % 6].getOpposite();
            if (this.canConnectSide(d) && this.isConnectable(d)) {
                a %= 6;
                this.facing = Direction.values()[a];
                this.syncTile(true);
                this.setChanged();
                break;
            }
        }
        return true;
    }

    public boolean canConnectSide(Direction side) {
        BlockPos p = this.worldPosition.relative(side);
        return this.level.isLoaded(p) && this.level.getBlockEntity(p) instanceof IEssentiaTransport;
    }

    /** Кубоид выделения: 0–5 — патрубки к соседям, 6 — середина (IndexedCuboid6 оригинала), в мировых координатах. */
    public record IndexedAABB(int index, AABB box) {
    }

    public void addTraceableCuboids(List<IndexedAABB> cuboids) {
        float min = 0.375F;
        float max = 0.625F;
        int x = this.worldPosition.getX();
        int y = this.worldPosition.getY();
        int z = this.worldPosition.getZ();
        if (this.canConnectSide(Direction.DOWN)) {
            cuboids.add(new IndexedAABB(0, new AABB(x + min, y, z + min, x + max, y + 0.375, z + max)));
        }
        if (this.canConnectSide(Direction.UP)) {
            cuboids.add(new IndexedAABB(1, new AABB(x + min, y + 0.625, z + min, x + max, y + 1, z + max)));
        }
        if (this.canConnectSide(Direction.NORTH)) {
            cuboids.add(new IndexedAABB(2, new AABB(x + min, y + min, z, x + max, y + max, z + 0.375)));
        }
        if (this.canConnectSide(Direction.SOUTH)) {
            cuboids.add(new IndexedAABB(3, new AABB(x + min, y + min, z + 0.625, x + max, y + max, z + 1)));
        }
        if (this.canConnectSide(Direction.WEST)) {
            cuboids.add(new IndexedAABB(4, new AABB(x, y + min, z + min, x + 0.375, y + max, z + max)));
        }
        if (this.canConnectSide(Direction.EAST)) {
            cuboids.add(new IndexedAABB(5, new AABB(x + 0.625, y + min, z + min, x + 1, y + max, z + max)));
        }
        cuboids.add(new IndexedAABB(6, new AABB(x + 0.375, y + 0.375, z + 0.375, x + 0.625, y + 0.625, z + 0.625)));
    }

    /** RayTracer.retraceBlock оригинала: какой кубоид под взглядом игрока (−1 — никакой). */
    public int getSubHit(Player player) {
        IndexedAABB c = this.getHitCuboid(player);
        return c == null ? -1 : c.index();
    }

    public IndexedAABB getHitCuboid(Player player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        List<IndexedAABB> cuboids = new ArrayList<>();
        this.addTraceableCuboids(cuboids);
        IndexedAABB best = null;
        double bestDist = Double.MAX_VALUE;
        for (IndexedAABB c : cuboids) {
            var hit = c.box().clip(start, end);
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(start);
                if (d < bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
        }
        return best;
    }
}
