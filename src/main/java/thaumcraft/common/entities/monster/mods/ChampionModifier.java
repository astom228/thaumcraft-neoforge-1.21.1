package thaumcraft.common.entities.monster.mods;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import thaumcraft.Thaumcraft;

/**
 * Модификатор чемпиона (порт ChampionModifier). Номер хранится в атрибуте CHAMPION_MOD: базовое −2 плюс модификатор
 * (id + 2). type: 0 — действует каждый тик, 1 — при ударе чемпиона, 2 — при ударе по нему, −1 — только при появлении.
 */
public class ChampionModifier {
    public final String name;
    public final int id;
    public final int type;
    public final IChampionModifierEffect effect;
    public final AttributeModifier attributeMod;

    public static final ChampionModifier[] mods = new ChampionModifier[] {
            new ChampionModifier(0, "bold", -1, new ChampionModBold()),
            new ChampionModifier(1, "spine", 2, new ChampionModSpined()),
            new ChampionModifier(2, "armor", 2, new ChampionModArmored()),
            new ChampionModifier(3, "mighty", -1, new ChampionModMighty()),
            new ChampionModifier(4, "grim", 1, new ChampionModGrim()),
            new ChampionModifier(5, "warded", 0, new ChampionModWarded()),
            new ChampionModifier(6, "warp", 1, new ChampionModWarp()),
            new ChampionModifier(7, "undying", 0, new ChampionModUndying()),
            new ChampionModifier(8, "fiery", 1, new ChampionModFire()),
            new ChampionModifier(9, "sickly", 1, new ChampionModSickly()),
            new ChampionModifier(10, "venomous", 1, new ChampionModPoison()),
            new ChampionModifier(11, "vampiric", 1, new ChampionModVampire()),
            new ChampionModifier(12, "infested", 2, new ChampionModInfested()),
            new ChampionModifier(13, "tainted", 0, new ChampionModTainted())
    };
    /** Проверен, не чемпион: −2 + 1 = −1. */
    public static final AttributeModifier ATTRIBUTE_MOD_NONE = new AttributeModifier(id("champion_normal"), 1.0, AttributeModifier.Operation.ADD_VALUE);
    public static final AttributeModifier ATTRIBUTE_MINUS_ONE = new AttributeModifier(id("champion_minus1"), -1.0, AttributeModifier.Operation.ADD_VALUE);

    public ChampionModifier(int id, String name, int type, IChampionModifierEffect effect) {
        this.name = name;
        this.id = id;
        this.type = type;
        this.effect = effect;
        this.attributeMod = new AttributeModifier(id("champion_" + name), id + 2, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Название модификатора («Смелый», «Шипастый»…) — переводится на стороне клиента. */
    public Component getModName() {
        return Component.translatable("champion.mod." + this.name);
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }
}
