package thaumcraft.api.golems.parts;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import thaumcraft.api.golems.IGolemAPI;

/** Руки голема (порт GolemArm). Реестр заполняется при запуске (до загрузки миров) и дальше не меняется. */
public class GolemArm {
    protected static GolemArm[] arms = new GolemArm[0];
    public byte id;
    public String key;
    public String[] research;
    public ResourceLocation icon;
    public Object[] components;
    public EnumGolemTrait[] traits;
    public GolemArm.IArmFunction function;
    public PartModel model;
    private static byte lastID = 0;

    public GolemArm(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, EnumGolemTrait[] tags) {
        this.key = key;
        this.research = research;
        this.icon = icon;
        this.components = comp;
        this.traits = tags;
        this.model = model;
        this.function = null;
    }

    public GolemArm(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, GolemArm.IArmFunction function, EnumGolemTrait[] tags) {
        this(key, research, icon, model, comp, tags);
        this.function = function;
    }

    public static void register(GolemArm thing) {
        thing.id = lastID++;
        if (thing.id >= arms.length) {
            GolemArm[] temp = new GolemArm[thing.id + 1];
            System.arraycopy(arms, 0, temp, 0, arms.length);
            arms = temp;
        }
        arms[thing.id] = thing;
    }

    public Component getLocalizedName() {
        return Component.translatable("golem.arm." + this.key.toLowerCase());
    }

    public Component getLocalizedDescription() {
        return Component.translatable("golem.arm.text." + this.key.toLowerCase());
    }

    public static GolemArm[] getArms() {
        return arms;
    }

    public interface IArmFunction extends IGenericFunction {
        void onMeleeAttack(IGolemAPI golem, Entity target);

        void onRangedAttack(IGolemAPI golem, LivingEntity target, float range);

        RangedAttackGoal getRangedAttackAI(RangedAttackMob golem);
    }
}
