package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.config.ConfigBlocks;

/** Пустотная банка (порт TileJarFillableVoid): принимает эссенцию сверх 250, излишек исчезает (иногда — Выброс). */
public class TileJarFillableVoid extends TileJarFillable {
    int count = 0;

    public TileJarFillableVoid(BlockPos pos, BlockState state) {
        super(ConfigBlocks.VOID_JAR_TILE.get(), pos, state);
    }

    @Override
    public int addToContainer(Aspect tt, int am) {
        boolean up = this.amount < CAPACITY;
        if (am <= 0 || !this.doesContainerAccept(tt)) {
            return am;
        }
        if (tt == this.aspect || this.amount == 0) {
            this.aspect = tt;
            this.amount += am;
            am = 0;
            if (this.amount > CAPACITY) {
                if (this.level.random.nextInt(250) == 0) {
                    AuraHelper.polluteAura(this.level, this.getBlockPos(), 1.0F, true);
                }
                this.amount = CAPACITY;
            }
        }
        if (up) {
            this.syncTile(false);
            this.setChanged();
        }
        return am;
    }

    @Override
    public int getMinimumSuction() {
        return this.aspectFilter != null ? 48 : 32;
    }

    @Override
    public int getSuctionAmount(Direction loc) {
        return this.aspectFilter != null && this.amount < CAPACITY ? 48 : 32;
    }

    @Override
    public void update() {
        if (!this.level.isClientSide && ++this.count % 5 == 0) {
            this.fillJar();
        }
    }
}
