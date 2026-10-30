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

    /** Цвет робы на теле без краски — 6961280 оригинала (а не цвет кожи по умолчанию). */
    @SubscribeEvent
    public static void registerClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public int getDefaultDyeColor(net.minecraft.world.item.ItemStack stack) {
                return 0xFF000000 | thaumcraft.common.items.armor.ItemRobeArmor.getColor(stack);
            }
        }, thaumcraft.api.items.ItemsTC.clothChest.get(), thaumcraft.api.items.ItemsTC.clothLegs.get(), thaumcraft.api.items.ItemsTC.clothBoots.get());
        // Жидкости: очищающая — текстуры воды с цветом 0x77FFCCAA, жидкая смерть — animatedglow с цветом 0xF0440099 (getColor оригинала)
        event.registerFluidType(fluidExt(net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_still"),
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_flow"), 0x77FFCCAA), thaumcraft.common.config.ConfigFluids.PURIFYING_TYPE.get());
        net.minecraft.resources.ResourceLocation glow = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "blocks/animatedglow");
        event.registerFluidType(fluidExt(glow, glow, 0xF0440099), thaumcraft.common.config.ConfigFluids.DEATH_TYPE.get());
    }

    private static net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions fluidExt(net.minecraft.resources.ResourceLocation still,
            net.minecraft.resources.ResourceLocation flow, int color) {
        return new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            @Override
            public net.minecraft.resources.ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                return flow;
            }

            @Override
            public int getTintColor() {
                return color;
            }
        };
    }

    /** Клавиши перчатки (KeyHandler). */
    @SubscribeEvent
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(thaumcraft.client.lib.events.KeyHandler.keyF);
        event.register(thaumcraft.client.lib.events.KeyHandler.keyG);
    }

    /** Радиальное меню фокусов — слой поверх интерфейса. */
    @SubscribeEvent
    public static void registerGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CAMERA_OVERLAYS,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "warp_vignette"), thaumcraft.client.lib.events.WarpVignette::render);
        event.registerAboveAll(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "foci_radial"),
                thaumcraft.client.lib.events.WandRenderingHandler::renderRadialLayer);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TCMenus.RESEARCH_TABLE.get(), GuiResearchTable::new);
        event.register(TCMenus.ARCANE_WORKBENCH.get(), GuiArcaneWorkbench::new);
        event.register(TCMenus.SMELTER.get(), thaumcraft.client.gui.GuiSmelter::new);
        event.register(TCMenus.SPA.get(), thaumcraft.client.gui.GuiSpa::new);
        event.register(TCMenus.POTION_SPRAYER.get(), thaumcraft.client.gui.GuiPotionSprayer::new);
        event.register(TCMenus.HAND_MIRROR.get(), thaumcraft.client.gui.GuiHandMirror::new);
        event.register(TCMenus.THAUMATORIUM.get(), thaumcraft.client.gui.GuiThaumatorium::new);
        event.register(TCMenus.FOCAL_MANIPULATOR.get(), thaumcraft.client.gui.GuiFocalManipulator::new);
        event.register(TCMenus.FOCUS_POUCH.get(), thaumcraft.client.gui.GuiFocusPouch::new);
    }

    /** Магические рецепты не входят в ванильную книгу рецептов; без категории клиент пишет предупреждение на каждый рецепт. */
    @SubscribeEvent
    public static void registerRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        event.registerRecipeCategoryFinder(ConfigRecipes.ARCANE_CRAFTING.get(), recipe -> RecipeBookCategories.UNKNOWN);
    }

    /** Слой волокон заражения (заражённые чемпионы) — всем рендерам живых существ. */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var type : event.getEntityTypes()) {
            if (event.getRenderer(type) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer lr) {
                lr.addLayer(new thaumcraft.client.renderers.entity.layers.LayerTainted(lr));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ConfigEntities.SPECIAL_ITEM.get(), ItemEntityRenderer::new);
        event.registerEntityRenderer(ConfigEntities.FOLLOWING_ITEM.get(), ItemEntityRenderer::new);
        event.registerEntityRenderer(ConfigEntities.FOCUS_PROJECTILE.get(), thaumcraft.client.renderers.entity.projectile.RenderNoProjectile::new);
        event.registerEntityRenderer(ConfigEntities.FOCUS_CLOUD.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ConfigEntities.FOCUS_MINE.get(), thaumcraft.client.renderers.entity.projectile.RenderFocusMine::new);
        event.registerEntityRenderer(ConfigEntities.GRAPPLE.get(), thaumcraft.client.renderers.entity.projectile.RenderGrapple::new);
        // алюментум — без модели (RenderNoProjectile оригинала; след — частицы сущности)
        event.registerEntityRenderer(ConfigEntities.ALUMENTUM.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ConfigEntities.SPELL_BAT.get(), thaumcraft.client.renderers.entity.mob.RenderSpellBat::new);
        event.registerEntityRenderer(ConfigEntities.BOTTLE_TAINT.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ConfigEntities.FIRE_BAT.get(), thaumcraft.client.renderers.entity.mob.RenderFireBat::new);
        event.registerEntityRenderer(ConfigEntities.THAUMIC_SLIME.get(), thaumcraft.client.renderers.entity.mob.RenderThaumicSlime::new);
        event.registerEntityRenderer(ConfigEntities.MIND_SPIDER.get(), thaumcraft.client.renderers.entity.mob.RenderMindSpider::new);
        event.registerEntityRenderer(ConfigEntities.BRAINY_ZOMBIE.get(), thaumcraft.client.renderers.entity.mob.RenderBrainyZombie::new);
        event.registerEntityRenderer(ConfigEntities.GIANT_BRAINY_ZOMBIE.get(), thaumcraft.client.renderers.entity.mob.RenderBrainyZombie::new);
        event.registerEntityRenderer(ConfigEntities.TAINT_CRAWLER.get(), thaumcraft.client.renderers.entity.mob.RenderTaintCrawler::new);
        event.registerEntityRenderer(ConfigEntities.TAINT_SWARM.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ConfigEntities.TAINTACLE.get(), thaumcraft.client.renderers.entity.mob.RenderTaintacle::normal);
        event.registerEntityRenderer(ConfigEntities.TAINTACLE_SMALL.get(), thaumcraft.client.renderers.entity.mob.RenderTaintacle::small);
        event.registerEntityRenderer(ConfigEntities.ELDRITCH_CRAB.get(), thaumcraft.client.renderers.entity.mob.RenderEldritchCrab::new);
        event.registerEntityRenderer(ConfigEntities.INHABITED_ZOMBIE.get(), thaumcraft.client.renderers.entity.mob.RenderBrainyZombie::inhabited);
        event.registerEntityRenderer(ConfigEntities.ELDRITCH_GUARDIAN.get(), thaumcraft.client.renderers.entity.mob.RenderEldritchGuardian::new);
        event.registerEntityRenderer(ConfigEntities.ELDRITCH_ORB.get(), thaumcraft.client.renderers.entity.projectile.RenderEldritchOrb::new);
        event.registerEntityRenderer(ConfigEntities.ELDRITCH_GOLEM.get(), thaumcraft.client.renderers.entity.mob.RenderEldritchGolem::new);
        event.registerEntityRenderer(ConfigEntities.GOLEM_ORB.get(), thaumcraft.client.renderers.entity.projectile.RenderElectricOrb::new);
        event.registerEntityRenderer(ConfigEntities.WISP.get(), thaumcraft.client.renderers.entity.mob.RenderWisp::new);
        event.registerEntityRenderer(ConfigEntities.FLUX_RIFT.get(), thaumcraft.client.renderers.entity.RenderFluxRift::new);
        event.registerEntityRenderer(ConfigEntities.TAINT_SEED.get(), thaumcraft.client.renderers.entity.mob.RenderTaintSeed::new);
        event.registerEntityRenderer(ConfigEntities.TAINT_SEED_PRIME.get(), thaumcraft.client.renderers.entity.mob.RenderTaintSeed::new);
        event.registerBlockEntityRenderer(ConfigBlocks.CRUCIBLE_TILE.get(), TileCrucibleRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.BELLOWS_TILE.get(), thaumcraft.client.renderers.tile.TileBellowsRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.RECHARGE_PEDESTAL_TILE.get(), thaumcraft.client.renderers.tile.TileRechargePedestalRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.DIOPTRA_TILE.get(), thaumcraft.client.renderers.tile.TileDioptraRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.PATTERN_CRAFTER_TILE.get(), thaumcraft.client.renderers.tile.TilePatternCrafterRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.HUNGRY_CHEST_TILE.get(), thaumcraft.client.renderers.tile.TileHungryChestRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.BANNER_TILE.get(), thaumcraft.client.renderers.tile.TileBannerRenderer::new);
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
        event.registerBlockEntityRenderer(ConfigBlocks.HOLE_TILE.get(), thaumcraft.client.renderers.tile.TileHoleRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.FOCAL_MANIPULATOR_TILE.get(), thaumcraft.client.renderers.tile.TileFocalManipulatorRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.PEDESTAL_TILE.get(), thaumcraft.client.renderers.tile.TilePedestalRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.MIRROR_TILE.get(), thaumcraft.client.renderers.tile.TileMirrorRenderer::new);
        event.registerBlockEntityRenderer(ConfigBlocks.MIRROR_ESSENTIA_TILE.get(), thaumcraft.client.renderers.tile.TileMirrorRenderer::new);
    }

    /** Предмет-зеркало со связью — модель «открытого» зеркала (metadata 1 оригинала). */
    @SubscribeEvent
    public static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        // очки откровения в слоте головы Curios
        top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(thaumcraft.api.items.ItemsTC.goggles.get(),
                () -> new thaumcraft.client.renderers.item.GogglesCurioRenderer(thaumcraft.client.renderers.item.GogglesCurioRenderer.GOGGLES));
        top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(thaumcraft.api.items.ItemsTC.bandCuriosity.get(),
                () -> new thaumcraft.client.renderers.item.GogglesCurioRenderer(thaumcraft.client.renderers.item.GogglesCurioRenderer.CURIOSITY_BAND));
        event.enqueueWork(() -> {
            for (net.minecraft.world.level.material.Fluid f : new net.minecraft.world.level.material.Fluid[] {thaumcraft.common.config.ConfigFluids.PURIFYING.get(),
                    thaumcraft.common.config.ConfigFluids.PURIFYING_FLOWING.get(), thaumcraft.common.config.ConfigFluids.DEATH.get(), thaumcraft.common.config.ConfigFluids.DEATH_FLOWING.get()}) {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(f, net.minecraft.client.renderer.RenderType.translucent());
            }
            net.minecraft.resources.ResourceLocation linked = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "linked");
            // перчатка с фокусом (свойство focus оригинала); фокусы — этап 12
            net.minecraft.client.renderer.item.ItemProperties.register(thaumcraft.api.items.ItemsTC.casterBasic.get(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus"),
                    (stack, level, entity, seed) -> stack.getItem() instanceof thaumcraft.api.casters.ICaster c && c.getFocus(stack) != null ? 1.0F : 0.0F);
            // крюкомёт: type 1 — заряжен (крюк в полёте или держит)
            net.minecraft.client.renderer.item.ItemProperties.register(thaumcraft.api.items.ItemsTC.grappleGun.get(),
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("type"),
                    (stack, level, entity, seed) -> thaumcraft.common.items.tools.ItemGrappleGun.isLoaded(stack) ? 1.0F : 0.0F);
            // изначальная жемчужина: type 0 — жемчужина, 1 — узелок, 2 — пылинка
            net.minecraft.client.renderer.item.ItemProperties.register(thaumcraft.api.items.ItemsTC.primordialPearl.get(),
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("type"),
                    (stack, level, entity, seed) -> thaumcraft.common.items.curios.ItemPrimordialPearl.getStage(stack));
            // оберег зелёного сердца: свойство type (0–2) выбирает модель
            net.minecraft.client.renderer.item.ItemProperties.register(thaumcraft.api.items.ItemsTC.charmVerdant.get(),
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("type"),
                    (stack, level, entity, seed) -> thaumcraft.common.items.baubles.ItemVerdantCharm.getType(stack));
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
