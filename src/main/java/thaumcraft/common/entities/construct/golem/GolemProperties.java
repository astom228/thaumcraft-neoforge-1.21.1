package thaumcraft.common.entities.construct.golem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.api.golems.parts.PartModel;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.renderers.entity.construct.PartModelBreakers;
import thaumcraft.client.renderers.entity.construct.PartModelClaws;
import thaumcraft.client.renderers.entity.construct.PartModelDarts;
import thaumcraft.client.renderers.entity.construct.PartModelHauler;
import thaumcraft.client.renderers.entity.construct.PartModelWheel;
import thaumcraft.common.entities.construct.golem.parts.GolemArmDart;
import thaumcraft.common.entities.construct.golem.parts.GolemLegLevitator;
import thaumcraft.common.entities.construct.golem.parts.GolemLegWheels;

/**
 * Состав голема (порт GolemProperties): байты long — материал, голова, руки, ноги, дополнение, ранг. Черты складываются
 * из частей; противоположные гасят друг друга. Детали регистрируются в init() (в оригинале — статический блок; в 1.21
 * предметы-компоненты существуют только после регистрации).
 */
public class GolemProperties implements IGolemProperties {
    private long data = 0L;
    private Set<EnumGolemTrait> traitCache = null;

    private static byte getByte(long data, int index) {
        return (byte) (data >>> (8 * (7 - index)));
    }

    private static long setByte(long data, byte b, int index) {
        int shift = 8 * (7 - index);
        return data & ~(0xFFL << shift) | (b & 0xFFL) << shift;
    }

    @Override
    public Set<EnumGolemTrait> getTraits() {
        if (this.traitCache == null) {
            this.traitCache = new HashSet<>();
            for (EnumGolemTrait t : this.getMaterial().traits) {
                this.addTraitSmart(t);
            }
            for (EnumGolemTrait t : this.getHead().traits) {
                this.addTraitSmart(t);
            }
            for (EnumGolemTrait t : this.getArms().traits) {
                this.addTraitSmart(t);
            }
            for (EnumGolemTrait t : this.getLegs().traits) {
                this.addTraitSmart(t);
            }
            for (EnumGolemTrait t : this.getAddon().traits) {
                this.addTraitSmart(t);
            }
        }
        return this.traitCache;
    }

    private void addTraitSmart(EnumGolemTrait trait) {
        if (trait.opposite != null && this.traitCache.contains(trait.opposite)) {
            this.traitCache.remove(trait.opposite);
        } else {
            this.traitCache.add(trait);
        }
    }

    @Override
    public boolean hasTrait(EnumGolemTrait trait) {
        return this.getTraits().contains(trait);
    }

    private static <T> T safe(T[] arr, int i) {
        return arr[i >= 0 && i < arr.length ? i : 0];
    }

    @Override
    public void setMaterial(GolemMaterial mat) {
        this.data = setByte(this.data, mat.id, 0);
        this.traitCache = null;
    }

    @Override
    public GolemMaterial getMaterial() {
        return safe(GolemMaterial.getMaterials(), getByte(this.data, 0));
    }

    @Override
    public void setHead(GolemHead mat) {
        this.data = setByte(this.data, mat.id, 1);
        this.traitCache = null;
    }

    @Override
    public GolemHead getHead() {
        return safe(GolemHead.getHeads(), getByte(this.data, 1));
    }

    @Override
    public void setArms(GolemArm mat) {
        this.data = setByte(this.data, mat.id, 2);
        this.traitCache = null;
    }

    @Override
    public GolemArm getArms() {
        return safe(GolemArm.getArms(), getByte(this.data, 2));
    }

    @Override
    public void setLegs(GolemLeg mat) {
        this.data = setByte(this.data, mat.id, 3);
        this.traitCache = null;
    }

    @Override
    public GolemLeg getLegs() {
        return safe(GolemLeg.getLegs(), getByte(this.data, 3));
    }

    @Override
    public void setAddon(GolemAddon mat) {
        this.data = setByte(this.data, mat.id, 4);
        this.traitCache = null;
    }

    @Override
    public GolemAddon getAddon() {
        return safe(GolemAddon.getAddons(), getByte(this.data, 4));
    }

    @Override
    public void setRank(int rank) {
        this.data = setByte(this.data, (byte) rank, 5);
    }

    @Override
    public int getRank() {
        return getByte(this.data, 5);
    }

    public static IGolemProperties fromLong(long d) {
        GolemProperties out = new GolemProperties();
        out.data = d;
        return out;
    }

    @Override
    public long toLong() {
        return this.data;
    }

    /** Компоненты для сборки: 2 основы, механизм и детали частей («base»/«mech» — по материалу). */
    @Override
    public ItemStack[] generateComponents() {
        List<ItemStack> comps = new ArrayList<>();
        addToList(comps, this.getMaterial().componentBase, 2);
        addToList(comps, this.getMaterial().componentMechanism, 1);
        addToListFromComps(comps, this.getArms().components, this.getMaterial());
        addToListFromComps(comps, this.getLegs().components, this.getMaterial());
        addToListFromComps(comps, this.getHead().components, this.getMaterial());
        addToListFromComps(comps, this.getAddon().components, this.getMaterial());
        return comps.toArray(new ItemStack[0]);
    }

    private static void addToListFromComps(List<ItemStack> comps, Object[] objs, GolemMaterial mat) {
        for (Object o : objs) {
            if (o instanceof ItemStack s) {
                addToList(comps, s, 1);
            } else if (o instanceof String s) {
                if (s.equalsIgnoreCase("base")) {
                    addToList(comps, mat.componentBase, 1);
                } else if (s.equalsIgnoreCase("mech")) {
                    addToList(comps, mat.componentMechanism, 1);
                }
            }
        }
    }

    private static void addToList(List<ItemStack> comps, ItemStack newItem, int mult) {
        for (ItemStack stack : comps) {
            if (ItemStack.isSameItemSameComponents(stack, newItem)) {
                stack.grow(newItem.getCount() * mult);
                return;
            }
        }
        ItemStack stack = newItem.copy();
        stack.setCount(stack.getCount() * mult);
        comps.add(stack);
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }

    private static boolean initialized = false;

    /** Регистрация материалов и частей (статический блок оригинала); вызывается один раз при запуске. */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ItemStack mech = new ItemStack(ItemsTC.mechanismSimple.get());
        GolemMaterial.register(new GolemMaterial("WOOD", new String[] {"MATSTUDWOOD"}, rl("textures/entity/golems/mat_wood.png"), 5059370, 6, 2, 1,
                new ItemStack(BlocksTC.plankGreatwood.get()), mech, new EnumGolemTrait[] {EnumGolemTrait.LIGHT}));
        GolemMaterial.register(new GolemMaterial("IRON", new String[] {"MATSTUDIRON"}, rl("textures/entity/golems/mat_iron.png"), 16777215, 20, 8, 3,
                new ItemStack(ItemsTC.plateIron.get()), mech, new EnumGolemTrait[] {EnumGolemTrait.HEAVY, EnumGolemTrait.FIREPROOF, EnumGolemTrait.BLASTPROOF}));
        GolemMaterial.register(new GolemMaterial("CLAY", new String[] {"MATSTUDCLAY"}, rl("textures/entity/golems/mat_clay.png"), 13071447, 10, 4, 2,
                new ItemStack(Blocks.TERRACOTTA), mech, new EnumGolemTrait[] {EnumGolemTrait.FIREPROOF}));
        GolemMaterial.register(new GolemMaterial("BRASS", new String[] {"MATSTUDBRASS"}, rl("textures/entity/golems/mat_brass.png"), 15638812, 16, 6, 3,
                new ItemStack(ItemsTC.plateBrass.get()), mech, new EnumGolemTrait[] {EnumGolemTrait.LIGHT}));
        GolemMaterial.register(new GolemMaterial("THAUMIUM", new String[] {"MATSTUDTHAUMIUM"}, rl("textures/entity/golems/mat_thaumium.png"), 5257074, 24, 10, 4,
                new ItemStack(ItemsTC.plateThaumium.get()), mech, new EnumGolemTrait[] {EnumGolemTrait.HEAVY, EnumGolemTrait.FIREPROOF, EnumGolemTrait.BLASTPROOF}));
        GolemMaterial.register(new GolemMaterial("VOID", new String[] {"MATSTUDVOID"}, rl("textures/entity/golems/mat_void.png"), 1445161, 20, 6, 4,
                new ItemStack(ItemsTC.plateVoid.get()), mech, new EnumGolemTrait[] {EnumGolemTrait.REPAIR}));

        ResourceLocation headOther = rl("textures/entity/golems/golem_head_other.png");
        GolemHead.register(new GolemHead("BASIC", new String[] {"MINDCLOCKWORK"}, rl("textures/misc/golem/head_basic.png"),
                new PartModel(rl("models/obj/golem_head_basic.obj"), null, PartModel.EnumAttachPoint.HEAD), new Object[] {new ItemStack(ItemsTC.mindClockwork.get())},
                new EnumGolemTrait[0]));
        GolemHead.register(new GolemHead("SMART", new String[] {"MINDBIOTHAUMIC"}, rl("textures/misc/golem/head_smart.png"),
                new PartModel(rl("models/obj/golem_head_smart.obj"), headOther, PartModel.EnumAttachPoint.HEAD),
                new Object[] {new ItemStack(ItemsTC.mindBiothaumic.get())}, new EnumGolemTrait[] {EnumGolemTrait.SMART, EnumGolemTrait.FRAGILE}));
        GolemHead.register(new GolemHead("SMART_ARMORED", new String[] {"MINDBIOTHAUMIC", "GOLEMCOMBATADV"}, rl("textures/misc/golem/head_smartarmor.png"),
                new PartModel(rl("models/obj/golem_head_smart_armor.obj"), null, PartModel.EnumAttachPoint.HEAD),
                new Object[] {new ItemStack(ItemsTC.mindBiothaumic.get()), new ItemStack(ItemsTC.plateBrass.get()), "base", new ItemStack(Blocks.WHITE_WOOL)},
                new EnumGolemTrait[] {EnumGolemTrait.SMART}));
        GolemHead.register(new GolemHead("SCOUT", new String[] {"GOLEMVISION"}, rl("textures/misc/golem/head_scout.png"),
                new PartModel(rl("models/obj/golem_head_scout.obj"), headOther, PartModel.EnumAttachPoint.HEAD),
                new Object[] {new ItemStack(ItemsTC.mindClockwork.get()), new ItemStack(ItemsTC.moduleVision.get())},
                new EnumGolemTrait[] {EnumGolemTrait.SCOUT, EnumGolemTrait.FRAGILE}));
        GolemHead.register(new GolemHead("SMART_SCOUT", new String[] {"GOLEMVISION", "MINDBIOTHAUMIC"}, rl("textures/misc/golem/head_smartscout.png"),
                new PartModel(rl("models/obj/golem_head_scout_smart.obj"), headOther, PartModel.EnumAttachPoint.HEAD),
                new Object[] {new ItemStack(ItemsTC.mindBiothaumic.get()), new ItemStack(ItemsTC.moduleVision.get())},
                new EnumGolemTrait[] {EnumGolemTrait.SCOUT, EnumGolemTrait.SMART, EnumGolemTrait.FRAGILE}));

        GolemArm.register(new GolemArm("BASIC", new String[] {"MINDCLOCKWORK"}, rl("textures/misc/golem/arms_basic.png"),
                new PartModel(rl("models/obj/golem_arms_basic.obj"), null, PartModel.EnumAttachPoint.ARMS), new Object[0], new EnumGolemTrait[0]));
        GolemArm.register(new GolemArm("FINE", new String[] {"MATSTUDBRASS"}, rl("textures/misc/golem/arms_fine.png"),
                new PartModel(rl("models/obj/golem_arms_fine.obj"), null, PartModel.EnumAttachPoint.ARMS), new Object[] {mech.copy(), "base"},
                new EnumGolemTrait[] {EnumGolemTrait.DEFT, EnumGolemTrait.FRAGILE}));
        GolemArm.register(new GolemArm("CLAWS", new String[] {"GOLEMCOMBATADV"}, rl("textures/misc/golem/arms_claws.png"),
                new PartModelClaws(rl("models/obj/golem_arms_claws.obj"), rl("textures/entity/golems/golem_arms_claws.png"), PartModel.EnumAttachPoint.ARMS),
                new Object[] {new ItemStack(ItemsTC.moduleAggression.get()), new ItemStack(Items.SHEARS, 2), "base"},
                new EnumGolemTrait[] {EnumGolemTrait.FIGHTER, EnumGolemTrait.CLUMSY, EnumGolemTrait.BRUTAL}));
        GolemArm.register(new GolemArm("BREAKERS", new String[] {"GOLEMBREAKER"}, rl("textures/misc/golem/arms_breakers.png"),
                new PartModelBreakers(rl("models/obj/golem_arms_breakers.obj"), rl("textures/entity/golems/golem_arms_breakers.png"), PartModel.EnumAttachPoint.ARMS),
                new Object[] {new ItemStack(Items.DIAMOND, 2), "base", new ItemStack(Blocks.PISTON, 2)},
                new EnumGolemTrait[] {EnumGolemTrait.BREAKER, EnumGolemTrait.CLUMSY, EnumGolemTrait.BRUTAL}));
        GolemArm.register(new GolemArm("DARTS", new String[] {"GOLEMCOMBATADV"}, rl("textures/misc/golem/arms_darts.png"),
                new PartModelDarts(rl("models/obj/golem_arms_darter.obj"), rl("textures/entity/golems/golem_arms_darter.png"), PartModel.EnumAttachPoint.ARMS),
                new Object[] {new ItemStack(ItemsTC.moduleAggression.get()), new ItemStack(Blocks.DISPENSER, 2), new ItemStack(Items.ARROW, 32), "mech"},
                new GolemArmDart(), new EnumGolemTrait[] {EnumGolemTrait.FIGHTER, EnumGolemTrait.CLUMSY, EnumGolemTrait.RANGED, EnumGolemTrait.FRAGILE}));

        GolemLeg.register(new GolemLeg("WALKER", new String[] {"MINDCLOCKWORK"}, rl("textures/misc/golem/legs_walker.png"),
                new PartModel(rl("models/obj/golem_legs_walker.obj"), null, PartModel.EnumAttachPoint.LEGS), new Object[] {"base", "mech"}, new EnumGolemTrait[0]));
        GolemLeg.register(new GolemLeg("ROLLER", new String[] {"MINDCLOCKWORK"}, rl("textures/misc/golem/legs_roller.png"),
                new PartModelWheel(rl("models/obj/golem_legs_wheel.obj"), rl("textures/entity/golems/golem_legs_wheel.png"), PartModel.EnumAttachPoint.BODY),
                new Object[] {new ItemStack(Items.BOWL, 2), new ItemStack(Items.LEATHER), "mech"}, new GolemLegWheels(),
                new EnumGolemTrait[] {EnumGolemTrait.WHEELED}));
        GolemLeg.register(new GolemLeg("CLIMBER", new String[] {"GOLEMCLIMBER"}, rl("textures/misc/golem/legs_climber.png"),
                new PartModel(rl("models/obj/golem_legs_climber.obj"), rl("textures/blocks/base_metal.png"), PartModel.EnumAttachPoint.LEGS),
                new Object[] {new ItemStack(Items.FLINT, 4), "base", "mech", "mech"}, new EnumGolemTrait[] {EnumGolemTrait.CLIMBER}));
        GolemLeg.register(new GolemLeg("FLYER", new String[] {"GOLEMFLYER"}, rl("textures/misc/golem/legs_flyer.png"),
                new PartModel(rl("models/obj/golem_legs_floater.obj"), rl("textures/entity/golems/golem_legs_floater.png"), PartModel.EnumAttachPoint.BODY),
                new Object[] {new ItemStack(BlocksTC.levitator.get()), new ItemStack(ItemsTC.plateBrass.get(), 4), new ItemStack(Items.SLIME_BALL), "mech"},
                new GolemLegLevitator(), new EnumGolemTrait[] {EnumGolemTrait.FLYER, EnumGolemTrait.FRAGILE}));

        GolemAddon.register(new GolemAddon("NONE", new String[] {"MINDCLOCKWORK"}, rl("textures/blocks/blank.png"), null, new Object[0], new EnumGolemTrait[0]));
        GolemAddon.register(new GolemAddon("ARMORED", new String[] {"GOLEMCOMBATADV"}, rl("textures/misc/golem/addon_armored.png"),
                new PartModel(rl("models/obj/golem_armor.obj"), null, PartModel.EnumAttachPoint.BODY), new Object[] {"base", "base", "base", "base"},
                new EnumGolemTrait[] {EnumGolemTrait.ARMORED, EnumGolemTrait.HEAVY}));
        GolemAddon.register(new GolemAddon("FIGHTER", new String[] {"SEALGUARD"}, rl("textures/misc/golem/addon_fighter.png"), null,
                new Object[] {new ItemStack(ItemsTC.moduleAggression.get()), "mech"}, new EnumGolemTrait[] {EnumGolemTrait.FIGHTER}));
        GolemAddon.register(new GolemAddon("HAULER", new String[] {"MINDCLOCKWORK"}, rl("textures/misc/golem/addon_hauler.png"),
                new PartModelHauler(rl("models/obj/golem_hauler.obj"), rl("textures/entity/golems/golem_hauler.png"), PartModel.EnumAttachPoint.BODY),
                new Object[] {new ItemStack(Items.LEATHER), new ItemStack(Blocks.CHEST)}, new EnumGolemTrait[] {EnumGolemTrait.HAULER}));
    }
}
