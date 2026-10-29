package thaumcraft.common.config;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;

import java.util.function.Supplier;

/**
 * Жидкости Thaumcraft (в оригинале — ConfigBlocks.FluidPure/FluidDeath + BlockFluidClassic). Жидкости 1.12 с материалом
 * MaterialLiquid не давали плавать, тонуть, тушить огонь и сбивать падение — так же и здесь; замедление сущностей —
 * в блоках жидкостей. Скорость растекания: tickRate = вязкость / 200; «квант» жидкой смерти 4 → уровень убывает на 2 за блок.
 * Жидкость Выброса (flux_goo) — этап 15.
 */
public class ConfigFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Thaumcraft.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, Thaumcraft.MODID);

    public static final Supplier<FluidType> PURIFYING_TYPE = FLUID_TYPES.register("purifying_fluid", () -> new FluidType(base("fluid.purifying_fluid")
            .lightLevel(5).rarity(Rarity.RARE).viscosity(1000)));
    public static final Supplier<FluidType> DEATH_TYPE = FLUID_TYPES.register("liquid_death", () -> new FluidType(base("fluid.liquid_death")
            .rarity(Rarity.RARE).viscosity(1500)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PURIFYING = FLUIDS.register("purifying_fluid",
            () -> new BaseFlowingFluid.Source(purifyingProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> PURIFYING_FLOWING = FLUIDS.register("flowing_purifying_fluid",
            () -> new BaseFlowingFluid.Flowing(purifyingProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> DEATH = FLUIDS.register("liquid_death",
            () -> new BaseFlowingFluid.Source(deathProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> DEATH_FLOWING = FLUIDS.register("flowing_liquid_death",
            () -> new BaseFlowingFluid.Flowing(deathProps()));

    private static FluidType.Properties base(String descriptionId) {
        return FluidType.Properties.create().descriptionId(descriptionId).canSwim(false).canDrown(false).canExtinguish(false)
                .canHydrate(false).supportsBoating(false).canPushEntity(false).canConvertToSource(false).fallDistanceModifier(1.0F)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
    }

    private static BaseFlowingFluid.Properties purifyingProps() {
        return new BaseFlowingFluid.Properties(PURIFYING_TYPE, PURIFYING, PURIFYING_FLOWING).block(() -> (net.minecraft.world.level.block.LiquidBlock) BlocksTC.purifyingFluid.get())
                .bucket(ItemsTC.bucketPure).tickRate(5).levelDecreasePerBlock(1).slopeFindDistance(4);
    }

    private static BaseFlowingFluid.Properties deathProps() {
        return new BaseFlowingFluid.Properties(DEATH_TYPE, DEATH, DEATH_FLOWING).block(() -> (net.minecraft.world.level.block.LiquidBlock) BlocksTC.liquidDeath.get())
                .bucket(ItemsTC.bucketDeath).tickRate(7).levelDecreasePerBlock(2).slopeFindDistance(4);
    }
}
