package thaumcraft.api.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Источники урона Thaumcraft (порт DamageSourceThaumcraft). В 1.21 типы урона — данные (data/thaumcraft/damage_type);
 * «скверна» — мимо брони и магический (теги minecraft:bypasses_armor и minecraft:witch_resistant_to).
 * Остальные (tentacle, swarm, dissolve) — с этапами своих существ.
 */
public final class DamageSourceThaumcraft {
    public static final ResourceKey<DamageType> TAINT = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("thaumcraft", "taint"));

    private DamageSourceThaumcraft() {
    }

    public static DamageSource taint(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TAINT));
    }
}
