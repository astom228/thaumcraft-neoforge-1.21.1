package thaumcraft.api.crafting;

/** Рецепт каталога Thaumcraft (тигель, наполнение, многоблочные схемы) — не ванильный рецепт. */
public interface IThaumcraftRecipe {
    String getResearch();

    String getGroup();
}
