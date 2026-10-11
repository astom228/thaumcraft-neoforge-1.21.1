package thaumcraft.common.tiles.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.ArcaneCraftingInput;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Магический верстак (порт TileArcaneWorkbench): 15 слотов (сетка 3×3 и 6 кристаллов), Вис ауры. С зарядником сверху Вис
 * берётся из 3×3 чанков вокруг. Как в оригинале, инвентарь не доступен воронкам и не синхронизируется с клиентом
 * блоком — содержимое видно только через меню.
 */
public class TileArcaneWorkbench extends TileThaumcraftInventory {
    public int auraVisServer = 0;
    public int auraVisClient = 0;
    /** Открытые меню (только сервер): в оригинале InventoryCrafting.eventHandler — пересчёт результата при изменении сетки. */
    private final List<ContainerArcaneWorkbench> openContainers = new ArrayList<>();

    public TileArcaneWorkbench(BlockPos pos, BlockState state) {
        super(ConfigBlocks.ARCANE_WORKBENCH_TILE.get(), pos, state, ArcaneCraftingInput.SIZE);
    }

    public void addContainer(ContainerArcaneWorkbench c) {
        this.openContainers.add(c);
    }

    public void removeContainer(ContainerArcaneWorkbench c) {
        this.openContainers.remove(c);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            for (ContainerArcaneWorkbench c : List.copyOf(this.openContainers)) {
                c.onCraftMatrixChanged();
            }
        }
    }

    public ArcaneCraftingInput getCraftingInput() {
        List<ItemStack> list = new ArrayList<>(ArcaneCraftingInput.SIZE);
        for (int i = 0; i < ArcaneCraftingInput.SIZE; i++) {
            list.add(this.inventory.getStackInSlot(i));
        }
        return ArcaneCraftingInput.of(list);
    }

    private boolean hasCharger() {
        return this.level.getBlockState(this.worldPosition.above()).is(BlocksTC.arcaneWorkbenchCharger.get());
    }

    public void getAura() {
        if (this.level != null && !this.level.isClientSide) {
            int t = 0;
            if (!this.hasCharger()) {
                t = (int) AuraHandler.getVis(this.level, this.worldPosition);
            } else {
                int sx = this.worldPosition.getX() >> 4;
                int sz = this.worldPosition.getZ() >> 4;
                for (int xx = -1; xx <= 1; xx++) {
                    for (int zz = -1; zz <= 1; zz++) {
                        AuraChunk ac = AuraHandler.getAuraChunk(this.level, sx + xx, sz + zz);
                        if (ac != null) {
                            t = (int) (t + ac.getVis());
                        }
                    }
                }
            }
            this.auraVisServer = t;
        }
    }

    public void spendAura(int vis) {
        if (this.level != null && !this.level.isClientSide) {
            if (this.hasCharger()) {
                int q = vis;
                int z = Math.max(1, vis / 9);
                int attempts = 0;
                while (q > 0) {
                    attempts++;
                    for (int xx = -1; xx <= 1; xx++) {
                        for (int zz = -1; zz <= 1; zz++) {
                            if (z > q) {
                                z = q;
                            }
                            q = (int) (q - AuraHandler.drainVis(this.level, this.worldPosition.offset(xx * 16, 0, zz * 16), z, false));
                            if (q <= 0 || attempts > 1000) {
                                return;
                            }
                        }
                    }
                }
            } else {
                AuraHandler.drainVis(this.level, this.worldPosition, vis, false);
            }
        }
    }
}
