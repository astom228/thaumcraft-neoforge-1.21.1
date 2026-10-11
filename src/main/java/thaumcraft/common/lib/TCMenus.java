package thaumcraft.common.lib;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.container.ContainerResearchTable;

/** Типы меню (в оригинале — GUI handler с номерами экранов). */
public class TCMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Thaumcraft.MODID);

    public static final Supplier<MenuType<ContainerResearchTable>> RESEARCH_TABLE = MENUS.register("research_table",
            () -> IMenuTypeExtension.create(ContainerResearchTable::new));
    public static final Supplier<MenuType<ContainerArcaneWorkbench>> ARCANE_WORKBENCH = MENUS.register("arcane_workbench",
            () -> IMenuTypeExtension.create(ContainerArcaneWorkbench::new));
}
