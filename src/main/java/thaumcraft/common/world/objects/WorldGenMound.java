package thaumcraft.common.world.objects;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import thaumcraft.api.blocks.BlocksTC;

/**
 * Курган (порт WorldGenMound): холм 19×19 из земли, травы и булыжника с погребальной камерой — сундук простого
 * подземелья (каждый третий — с ловушкой и динамитом под ним), два ящика/урны с добычей, рассадники скелетов и зомби.
 * Блоки — из файла thaumcraft/structures/mound.txt (tools/structures/convert-mound.js), порядок как в оригинале.
 */
public class WorldGenMound {
    private static List<int[]> data;

    /** Палитра файла: 0–8 — блоки, 10–17 — ступени из булыжника с метой 0–7. */
    private static BlockState palette(int p) {
        return switch (p) {
            case 0 -> Blocks.AIR.defaultBlockState();
            case 1 -> Blocks.DIRT.defaultBlockState();
            case 2 -> Blocks.COBBLESTONE.defaultBlockState();
            case 3 -> Blocks.GRASS_BLOCK.defaultBlockState();
            case 4 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 5 -> Blocks.IRON_BARS.defaultBlockState();
            case 6 -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
            case 7 -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            case 8 -> Blocks.SHORT_GRASS.defaultBlockState();
            default -> {
                int meta = p - 10;
                Direction facing = switch (meta & 3) {
                    case 0 -> Direction.EAST;
                    case 1 -> Direction.WEST;
                    case 2 -> Direction.SOUTH;
                    default -> Direction.NORTH;
                };
                yield Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing)
                        .setValue(StairBlock.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM);
            }
        };
    }

    private static synchronized List<int[]> data() {
        if (data == null) {
            List<int[]> list = new ArrayList<>();
            try (InputStream in = WorldGenMound.class.getResourceAsStream("/thaumcraft/structures/mound.txt")) {
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    while ((line = r.readLine()) != null) {
                        String[] s = line.trim().split(" ");
                        if (s.length == 4) {
                            list.add(new int[] {Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3])});
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("thaumcraft: не прочитан курган", e);
            }
            data = List.copyOf(list);
        }
        return data;
    }

    /** LocationIsValidSpawn: до воздуха не больше 2 блоков вверх, сверху — камень, трава или земля (или снег/трава на них). */
    private static boolean locationIsValidSpawn(TreeWriter w, BlockPos pos) {
        int distanceToAir = 0;
        while (!w.isAir(pos.above(distanceToAir)) && distanceToAir < 3) {
            distanceToAir++;
        }
        if (distanceToAir > 2) {
            return false;
        }
        BlockPos pos2 = pos.above(distanceToAir - 1);
        if (!w.isAir(pos2.above())) {
            return false;
        }
        BlockState s = w.get(pos2);
        BlockState below = w.get(pos2.below());
        if (valid(s)) {
            return true;
        }
        return (s.getBlock() instanceof SnowLayerBlock || s.is(Blocks.SHORT_GRASS)) && valid(below);
    }

    private static boolean valid(BlockState s) {
        return s.is(Blocks.STONE) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT);
    }

    private static Block loot(RandomSource rand) {
        float rr = rand.nextFloat();
        int md = rr < 0.1F ? 2 : (rr < 0.33F ? 1 : 0);
        boolean crate = rand.nextFloat() < 0.3F;
        return switch (md) {
            case 1 -> crate ? BlocksTC.lootCrateUncommon.get() : BlocksTC.lootUrnUncommon.get();
            case 2 -> crate ? BlocksTC.lootCrateRare.get() : BlocksTC.lootUrnRare.get();
            default -> crate ? BlocksTC.lootCrateCommon.get() : BlocksTC.lootUrnCommon.get();
        };
    }

    public boolean generate(TreeWriter w, RandomSource rand, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        if (!locationIsValidSpawn(w, new BlockPos(i + 9, j + 9, k + 9)) || !locationIsValidSpawn(w, new BlockPos(i, j + 9, k))
                || !locationIsValidSpawn(w, new BlockPos(i + 18, j + 9, k)) || !locationIsValidSpawn(w, new BlockPos(i + 18, j + 9, k + 18))
                || !locationIsValidSpawn(w, new BlockPos(i, j + 9, k + 18))) {
            return false;
        }
        List<BlockPos> shaped = new ArrayList<>();
        for (int[] d : data()) {
            BlockPos p = new BlockPos(i + d[0], j + d[1], k + d[2]);
            w.set(p, palette(d[3]));
            if (d[3] == 5 || d[3] >= 10) {
                shaped.add(p);
            }
        }
        w.set(new BlockPos(i + 9, j + 1, k + 7), loot(rand).defaultBlockState());
        w.set(new BlockPos(i + 9, j + 1, k + 11), loot(rand).defaultBlockState());
        BlockPos chestPos = new BlockPos(i + 10, j + 1, k + 9);
        if (rand.nextInt(3) == 0) {
            w.set(chestPos, Blocks.TRAPPED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
            w.set(new BlockPos(i + 10, j - 1, k + 9), Blocks.TNT.defaultBlockState());
        } else {
            w.set(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        }
        if (w.canAccess(chestPos)) {
            RandomizableContainer.setBlockEntityLootTable(w.level(), rand, chestPos, BuiltInLootTables.SIMPLE_DUNGEON);
        }
        spawner(w, rand, new BlockPos(i + 4, j + 5, k + 4), EntityType.SKELETON);
        spawner(w, rand, new BlockPos(i + 4, j + 5, k + 14), EntityType.ZOMBIE);
        // решётки и ступени соединяются с соседями (в 1.12 — getActualState при отрисовке)
        for (BlockPos p : shaped) {
            if (w.canAccess(p)) {
                BlockState s = w.level().getBlockState(p);
                w.set(p, Block.updateFromNeighbourShapes(s, w.level(), p));
            }
        }
        return true;
    }

    private static void spawner(TreeWriter w, RandomSource rand, BlockPos p, EntityType<?> type) {
        w.set(p, Blocks.SPAWNER.defaultBlockState());
        if (w.canAccess(p) && w.level().getBlockEntity(p) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(type, rand);
        }
    }
}
