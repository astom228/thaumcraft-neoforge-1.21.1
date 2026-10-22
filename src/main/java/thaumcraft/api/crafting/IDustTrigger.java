package thaumcraft.api.crafting;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Превращение, срабатывающее от соли мира: getValidFace проверяет блок (и исследование игрока), execute выполняет
 * (вызывается на обеих сторонах, как в оригинале; изменения мира — только на сервере), sparkle — блоки для искр на клиенте.
 * Регистрировать при загрузке (FMLCommonSetupEvent); список общий и после загрузки не меняется.
 */
public interface IDustTrigger {
    List<IDustTrigger> triggers = new CopyOnWriteArrayList<>();

    Placement getValidFace(Level world, Player player, BlockPos pos, Direction face);

    void execute(Level world, Player player, BlockPos pos, Placement placement, Direction side);

    default List<BlockPos> sparkle(Level world, Player player, BlockPos pos, Placement placement) {
        return List.of(pos);
    }

    static void registerDustTrigger(IDustTrigger trigger) {
        triggers.add(trigger);
    }

    class Placement {
        public int xOffset;
        public int yOffset;
        public int zOffset;
        public Direction facing;

        public Placement(int xOffset, int yOffset, int zOffset, Direction facing) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            this.facing = facing;
        }
    }
}
