package thaumcraft.common.lib;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.container.ContainerSmelter;
import thaumcraft.common.container.ContainerHandMirror;
import thaumcraft.common.container.ContainerThaumatorium;

/** Типы меню (в оригинале — GUI handler с номерами экранов). */
public class TCMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Thaumcraft.MODID);

    public static final Supplier<MenuType<ContainerResearchTable>> RESEARCH_TABLE = MENUS.register("research_table",
            () -> IMenuTypeExtension.create(ContainerResearchTable::new));
    public static final Supplier<MenuType<ContainerArcaneWorkbench>> ARCANE_WORKBENCH = MENUS.register("arcane_workbench",
            () -> IMenuTypeExtension.create(ContainerArcaneWorkbench::new));
    public static final Supplier<MenuType<ContainerSmelter>> SMELTER = MENUS.register("essentia_smeltery",
            () -> IMenuTypeExtension.create(ContainerSmelter::new));
    public static final Supplier<MenuType<thaumcraft.common.container.ContainerSpa>> SPA = MENUS.register("spa",
            () -> IMenuTypeExtension.create(thaumcraft.common.container.ContainerSpa::new));
    public static final Supplier<MenuType<thaumcraft.common.container.ContainerPotionSprayer>> POTION_SPRAYER = MENUS.register("potion_sprayer",
            () -> IMenuTypeExtension.create(thaumcraft.common.container.ContainerPotionSprayer::new));
    public static final Supplier<MenuType<ContainerHandMirror>> HAND_MIRROR = MENUS.register("hand_mirror",
            () -> IMenuTypeExtension.create(ContainerHandMirror::new));
    public static final Supplier<MenuType<ContainerThaumatorium>> THAUMATORIUM = MENUS.register("thaumatorium",
            () -> IMenuTypeExtension.create(ContainerThaumatorium::new));
    public static final Supplier<MenuType<thaumcraft.common.container.ContainerFocalManipulator>> FOCAL_MANIPULATOR = MENUS.register("focal_manipulator",
            () -> IMenuTypeExtension.create(thaumcraft.common.container.ContainerFocalManipulator::new));
    public static final Supplier<MenuType<thaumcraft.common.container.ContainerFocusPouch>> FOCUS_POUCH = MENUS.register("focus_pouch",
            () -> IMenuTypeExtension.create(thaumcraft.common.container.ContainerFocusPouch::new));
    public static final Supplier<MenuType<thaumcraft.common.container.ContainerGolemBuilder>> GOLEM_BUILDER = MENUS.register("golem_press",
            () -> IMenuTypeExtension.create(thaumcraft.common.container.ContainerGolemBuilder::new));
    public static final Supplier<MenuType<thaumcraft.common.container.ContainerPech>> PECH = MENUS.register("pech",
            () -> IMenuTypeExtension.create(thaumcraft.common.container.ContainerPech::new));
}
