package thaumcraft.common.lib;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

public class SoundsTC {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Thaumcraft.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> zap = register("zap");
    public static final DeferredHolder<SoundEvent, SoundEvent> heartbeat = register("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> spill = register("spill");
    public static final DeferredHolder<SoundEvent, SoundEvent> bubble = register("bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> creak = register("creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> squeek = register("squeek");
    public static final DeferredHolder<SoundEvent, SoundEvent> jar = register("jar");
    public static final DeferredHolder<SoundEvent, SoundEvent> swarm = register("swarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> swarmattack = register("swarmattack");
    public static final DeferredHolder<SoundEvent, SoundEvent> fly = register("fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> key = register("key");
    public static final DeferredHolder<SoundEvent, SoundEvent> ticks = register("ticks");
    public static final DeferredHolder<SoundEvent, SoundEvent> clack = register("clack");
    public static final DeferredHolder<SoundEvent, SoundEvent> pump = register("pump");
    public static final DeferredHolder<SoundEvent, SoundEvent> poof = register("poof");
    public static final DeferredHolder<SoundEvent, SoundEvent> page = register("page");
    public static final DeferredHolder<SoundEvent, SoundEvent> pageturn = register("pageturn");
    public static final DeferredHolder<SoundEvent, SoundEvent> learn = register("learn");
    public static final DeferredHolder<SoundEvent, SoundEvent> write = register("write");
    public static final DeferredHolder<SoundEvent, SoundEvent> erase = register("erase");
    public static final DeferredHolder<SoundEvent, SoundEvent> brain = register("brain");
    public static final DeferredHolder<SoundEvent, SoundEvent> crystal = register("crystal");
    public static final DeferredHolder<SoundEvent, SoundEvent> wispdead = register("wispdead");
    public static final DeferredHolder<SoundEvent, SoundEvent> wisplive = register("wisplive");
    public static final DeferredHolder<SoundEvent, SoundEvent> wand = register("wand");
    public static final DeferredHolder<SoundEvent, SoundEvent> wandfail = register("wandfail");
    public static final DeferredHolder<SoundEvent, SoundEvent> rumble = register("rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> ice = register("ice");
    public static final DeferredHolder<SoundEvent, SoundEvent> jacobs = register("jacobs");
    public static final DeferredHolder<SoundEvent, SoundEvent> hhoff = register("hhoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> hhon = register("hhon");
    public static final DeferredHolder<SoundEvent, SoundEvent> pech_idle = register("pech_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> pech_trade = register("pech_trade");
    public static final DeferredHolder<SoundEvent, SoundEvent> pech_dice = register("pech_dice");
    public static final DeferredHolder<SoundEvent, SoundEvent> pech_hit = register("pech_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> pech_death = register("pech_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> pech_charge = register("pech_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> shock = register("shock");
    public static final DeferredHolder<SoundEvent, SoundEvent> craftfail = register("craftfail");
    public static final DeferredHolder<SoundEvent, SoundEvent> scan = register("scan");
    public static final DeferredHolder<SoundEvent, SoundEvent> craftstart = register("craftstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> runicShieldEffect = register("runicshieldeffect");
    public static final DeferredHolder<SoundEvent, SoundEvent> runicShieldCharge = register("runicshieldcharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> wind = register("wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> tool = register("tool");
    public static final DeferredHolder<SoundEvent, SoundEvent> gore = register("gore");
    public static final DeferredHolder<SoundEvent, SoundEvent> tentacle = register("tentacle");
    public static final DeferredHolder<SoundEvent, SoundEvent> upgrade = register("upgrade");
    public static final DeferredHolder<SoundEvent, SoundEvent> whispers = register("whispers");
    public static final DeferredHolder<SoundEvent, SoundEvent> monolith = register("monolith");
    public static final DeferredHolder<SoundEvent, SoundEvent> infuser = register("infuser");
    public static final DeferredHolder<SoundEvent, SoundEvent> infuserstart = register("infuserstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> egidle = register("egidle");
    public static final DeferredHolder<SoundEvent, SoundEvent> egattack = register("egattack");
    public static final DeferredHolder<SoundEvent, SoundEvent> egdeath = register("egdeath");
    public static final DeferredHolder<SoundEvent, SoundEvent> egscreech = register("egscreech");
    public static final DeferredHolder<SoundEvent, SoundEvent> crabclaw = register("crabclaw");
    public static final DeferredHolder<SoundEvent, SoundEvent> crabdeath = register("crabdeath");
    public static final DeferredHolder<SoundEvent, SoundEvent> crabtalk = register("crabtalk");
    public static final DeferredHolder<SoundEvent, SoundEvent> chant = register("chant");
    public static final DeferredHolder<SoundEvent, SoundEvent> coins = register("coins");
    public static final DeferredHolder<SoundEvent, SoundEvent> urnbreak = register("urnbreak");
    public static final DeferredHolder<SoundEvent, SoundEvent> evilportal = register("evilportal");
    public static final DeferredHolder<SoundEvent, SoundEvent> grind = register("grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> dust = register("dust");

    public static final DeferredSoundType GORE = new DeferredSoundType(0.5F, 1.0F, gore, gore, gore, gore, gore);
    public static final DeferredSoundType CRYSTAL = new DeferredSoundType(0.5F, 1.0F, crystal, crystal, crystal, crystal, crystal);
    public static final DeferredSoundType JAR = new DeferredSoundType(0.5F, 1.0F, jar, jar, jar, jar, jar);
    public static final DeferredSoundType URN = new DeferredSoundType(0.5F, 1.5F, urnbreak, urnbreak, urnbreak, urnbreak, urnbreak);

    private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, id)));
    }
}
