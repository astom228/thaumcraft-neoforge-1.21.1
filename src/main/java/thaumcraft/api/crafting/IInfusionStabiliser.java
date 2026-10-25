package thaumcraft.api.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Блок, стабилизирующий наполнение, если стоит симметрично вокруг матрицы (свечи; черепа — сами по себе). */
public interface IInfusionStabiliser {
    boolean canStabaliseInfusion(Level world, BlockPos pos);
}
