package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;
import thaumcraft.client.renderers.tile.TileCrucibleRenderer;
import thaumcraft.client.renderers.tile.TileJarRenderer;
import thaumcraft.client.renderers.tile.TileTubeRenderers;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.crafting.ConfigRecipes;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.lib.TCMenus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
        event.register(TCMenus.ARCANE_WORKBENCH.get(), GuiArcaneWorkbench::new);
        event.register(TCMenus.SMELTER.get(), thaumcraft.client.gui.GuiSmelter::new);
        event.register(TCMenus.HAND_MIRROR.get(), thaumcraft.client.gui.GuiHandMirror::new);
        event.register(TCMenus.THAUMATORIUM.get(), thaumcraft.client.gui.GuiThaumatorium::new);
    }

    /** Магические рецепты не входят в ванильную книгу рецептов; без категории клиент пишет предупреждение на каждый рецепт. */
    @SubscribeEvent
    public static void registerRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        event.registerRecipeCategoryFinder(ConfigRecipes.ARCANE_CRAFTING.get(), recipe -> RecipeBookCategories.UNKNOWN);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ConfigEntities.SPECIAL_ITEM.get(), ItemEntityRenderer::new);
        event.registerEntityRenderer(ConfigEntities.FOLLOWING_ITEM.get(), ItemEntityRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.CRUCIBLE_TILE.get(), TileCrucibleRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.WARDED_JAR_TILE.get(), TileJarRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.VOID_JAR_TILE.get(), TileJarRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.BRAIN_JAR_TILE.get(), TileJarRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.TUBE_VALVE_TILE.get(), TileTubeRenderers.Valve::new);
        event.registerBlockEntityRenderer(ConfigBlocks.TUBE_ONEWAY_TILE.get(), TileTubeRenderers.Oneway::new);
        event.registerBlockEntityRenderer(ConfigBlocks.TUBE_BUFFER_TILE.get(), TileTubeRenderers.Buffer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.ALEMBIC_TILE.get(), thaumcraft.client.renderers.tile.TileAlembicRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.CENTRIFUGE_TILE.get(), thaumcraft.client.renderers.tile.TileCentrifugeRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.THAUMATORIUM_TILE.get(), thaumcraft.client.renderers.tile.TileThaumatoriumRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.INFUSION_MATRIX_TILE.get(), thaumcraft.client.renderers.tile.TileInfusionMatrixRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.PEDESTAL_TILE.get(), thaumcraft.client.renderers.tile.TilePedestalRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.MIRROR_TILE.get(), thaumcraft.client.renderers.tile.TileMirrorRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.MIRROR_ESSENTIA_TILE.get(), thaumcraft.client.renderers.tile.TileMirrorRenderer::new);
    }

    /** Предмет-зеркало со связью — модель «открытого» зеркала (metadata 1 оригинала). */
    @SubscribeEvent
    public static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.resources.ResourceLocation linked = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "linked");
            // перчатка с фокусом (свойство focus оригинала); фокусы — этап 12
            net.minecraft.client.renderer.item.ItemProperties.register(thaumcraft.api.items.ItemsTC.casterBasic.get(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus"),
                    (stack, level, entity, seed) -> stack.getItem() instanceof thaumcraft.api.casters.ICaster c && c.getFocus(stack) != null ? 1.0F : 0.0F);
            for (var block : new net.minecraft.world.level.block.Block[]{thaumcraft.api.blocks.BlocksTC.mirror.get(), thaumcraft.api.blocks.BlocksTC.mirrorEssentia.get()}) {
                net.minecraft.client.renderer.item.ItemProperties.register(block.asItem(), linked,
                        (stack, level, entity, seed) -> stack.has(thaumcraft.common.lib.TCDataComponents.MIRROR_LINK.get()) ? 1.0F : 0.0F);
            }
        });
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(CrystalGeometry.LOADER_ID, CrystalGeometry.Loader.INSTANCE);
    }
}
