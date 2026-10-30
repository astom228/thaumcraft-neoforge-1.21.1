package thaumcraft.common.items;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ConfigEntities;

/**
 * Яйца призыва существ Thaumcraft (в 1.12 — цвета яиц в EntityRegistry.registerModEntity). Цвета — RGB из оригинала
 * (альфа-байт отброшен). Лежат во ванильной вкладке яиц призыва, как и в оригинале — не во вкладке мода.
 */
public final class SpawnEggs {
    public static final DeferredRegister<Item> EGGS = DeferredRegister.create(Registries.ITEM, Thaumcraft.MODID);

    private static DeferredHolder<Item, Item> egg(String id, Supplier<? extends EntityType<? extends Mob>> type, int bg, int fg) {
        return EGGS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg & 0xFFFFFF, fg & 0xFFFFFF, new Item.Properties()));
    }

    static {
        egg("cultistportalgreater", ConfigEntities.CULTIST_PORTAL_GREATER, 6842578, 32896);
        egg("cultistportallesser", ConfigEntities.CULTIST_PORTAL_LESSER, 9438728, 6316242);
        egg("eldritchwarden", ConfigEntities.ELDRITCH_WARDEN, 6842578, 8421504);
        egg("eldritchgolem", ConfigEntities.ELDRITCH_GOLEM, 6842578, 8947848);
        egg("cultistleader", ConfigEntities.CULTIST_LEADER, 6842578, 9438728);
        egg("taintaclegiant", ConfigEntities.TAINTACLE_GIANT, 6842578, 10618530);
        egg("brainyzombie", ConfigEntities.BRAINY_ZOMBIE, -16129, -16744448);
        egg("giantbrainyzombie", ConfigEntities.GIANT_BRAINY_ZOMBIE, -16129, -16760832);
        egg("wisp", ConfigEntities.WISP, -16129, -1);
        egg("firebat", ConfigEntities.FIRE_BAT, -16129, -806354944);
        egg("spellbat", ConfigEntities.SPELL_BAT, -16129, -806354944);
        egg("pech", ConfigEntities.PECH, -16129, -12582848);
        egg("mindspider", ConfigEntities.MIND_SPIDER, 4996656, 4473924);
        egg("eldritchguardian", ConfigEntities.ELDRITCH_GUARDIAN, 8421504, 0);
        egg("cultistknight", ConfigEntities.CULTIST_KNIGHT, 9438728, 128);
        egg("cultistcleric", ConfigEntities.CULTIST_CLERIC, 9438728, 8388608);
        egg("eldritchcrab", ConfigEntities.ELDRITCH_CRAB, 8421504, 5570560);
        egg("inhabitedzombie", ConfigEntities.INHABITED_ZOMBIE, 8421504, 5570560);
        egg("thaumslime", ConfigEntities.THAUMIC_SLIME, 10618530, -32513);
        egg("taintcrawler", ConfigEntities.TAINT_CRAWLER, 10618530, 3158064);
        egg("taintacle", ConfigEntities.TAINTACLE, 10618530, 4469572);
        egg("taintswarm", ConfigEntities.TAINT_SWARM, 10618530, 16744576);
        egg("taint_seed", ConfigEntities.TAINT_SEED, 10618530, 4465237);
        egg("taint_seed_prime", ConfigEntities.TAINT_SEED_PRIME, 10618530, 5583718);
    }

    private SpawnEggs() {
    }

    public static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            EGGS.getEntries().forEach(e -> event.accept(e.get()));
        }
    }
}
