package thaumcraft.common.config;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.potions.PotionFluxTaint;
import thaumcraft.api.potions.PotionVisExhaust;

/** Эффекты Thaumcraft (в оригинале — Registrar.registerPotions). Все 9 эффектов оригинала. */
public class ConfigPotions {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Thaumcraft.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> FLUX_TAINT = EFFECTS.register("flux_taint", PotionFluxTaint::new);
    public static final DeferredHolder<MobEffect, MobEffect> VIS_EXHAUST = EFFECTS.register("vis_exhaust", PotionVisExhaust::new);
    public static final DeferredHolder<MobEffect, MobEffect> INFECTIOUS_VIS_EXHAUST = EFFECTS.register("infectious_vis_exhaust",
            thaumcraft.common.lib.potions.PotionInfectiousVisExhaust::new);
    public static final DeferredHolder<MobEffect, MobEffect> UNNATURAL_HUNGER = EFFECTS.register("unnatural_hunger",
            thaumcraft.common.lib.potions.PotionUnnaturalHunger::new);
    public static final DeferredHolder<MobEffect, MobEffect> DEATH_GAZE = EFFECTS.register("death_gaze", thaumcraft.common.lib.potions.PotionDeathGaze::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLURRED_VISION = EFFECTS.register("blurred_vision",
            thaumcraft.common.lib.potions.PotionBlurredVision::new);
    public static final DeferredHolder<MobEffect, MobEffect> SUN_SCORNED = EFFECTS.register("sun_scorned", thaumcraft.common.lib.potions.PotionSunScorned::new);
    public static final DeferredHolder<MobEffect, MobEffect> THAUMARHIA = EFFECTS.register("thaumarhia", thaumcraft.common.lib.potions.PotionThaumarhia::new);
    public static final DeferredHolder<MobEffect, MobEffect> WARP_WARD = EFFECTS.register("warp_ward", thaumcraft.common.lib.potions.PotionWarpWard::new);
}
