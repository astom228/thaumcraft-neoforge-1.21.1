package thaumcraft.common.config;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.potions.PotionFluxTaint;
import thaumcraft.api.potions.PotionVisExhaust;

/** Эффекты Thaumcraft (в оригинале — Registrar.registerPotions). Остальные 6 — этап 15. */
public class ConfigPotions {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Thaumcraft.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> FLUX_TAINT = EFFECTS.register("flux_taint", PotionFluxTaint::new);
    public static final DeferredHolder<MobEffect, MobEffect> VIS_EXHAUST = EFFECTS.register("vis_exhaust", PotionVisExhaust::new);
    public static final DeferredHolder<MobEffect, MobEffect> WARP_WARD = EFFECTS.register("warp_ward", thaumcraft.common.lib.potions.PotionWarpWard::new);
}
