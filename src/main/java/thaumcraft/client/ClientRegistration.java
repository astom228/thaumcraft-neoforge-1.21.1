package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;
import thaumcraft.common.lib.crafting.ConfigRecipes;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.lib.TCMenus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.renderers.block.CrystalGeometry;

/** Клиентские регистрации (шейдеры, рендереры…). */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ClientRegistration {
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "particle"), DefaultVertexFormat.PARTICLE),
                shader -> ParticleEngine.particleShader = shader);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TCMenus.RESEARCH_TABLE.get(), GuiResearchTable::new);
    }

    /** Магические рецепты не входят в ванильную книгу рецептов; без категории клиент пишет предупреждение на каждый рецепт. */
    @SubscribeEvent
    public static void registerRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        event.registerRecipeCategoryFinder(ConfigRecipes.ARCANE_CRAFTING.get(), recipe -> RecipeBookCategories.UNKNOWN);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(CrystalGeometry.LOADER_ID, CrystalGeometry.Loader.INSTANCE);
    }
}
