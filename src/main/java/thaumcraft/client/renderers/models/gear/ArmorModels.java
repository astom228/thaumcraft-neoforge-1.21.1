package thaumcraft.client.renderers.models.gear;

import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.items.armor.ItemVoidRobeArmor;

/**
 * Свои модели брони (getArmorModel оригинала + CustomArmorHelper): вариант «поножей» (ModelBiped 0.5F) — у шлема и поножей,
 * вариант 1.0F — у нагрудника и ботинок. Позы и видимость основных частей NeoForge копирует из ванильной модели брони
 * (как делал CustomArmorHelper); здесь — скрытие частей по набору и анимация ткани (render оригинала).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ArmorModels {
    private static final ModelLayerLocation ROBE_CHEST = layer("robe", "chest");
    private static final ModelLayerLocation ROBE_LEGS = layer("robe", "legs");
    private static final ModelLayerLocation FORTRESS_CHEST = layer("fortress_armor", "chest");
    private static final ModelLayerLocation FORTRESS_LEGS = layer("fortress_armor", "legs");
    private static final ModelLayerLocation KNIGHT_CHEST = layer("knight_armor", "chest");
    private static final ModelLayerLocation KNIGHT_LEGS = layer("knight_armor", "legs");
    private static final ModelLayerLocation LEADER_CHEST = layer("leader_armor", "chest");
    private static final ModelLayerLocation LEADER_LEGS = layer("leader_armor", "legs");

    private ArmorModels() {
    }

    private static ModelLayerLocation layer(String name, String variant) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name), variant);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(thaumcraft.client.renderers.entity.mob.RenderTaintacle.LAYER_10, () -> thaumcraft.client.renderers.models.entity.ModelTaintacle.create(10));
        event.registerLayerDefinition(thaumcraft.client.renderers.entity.mob.RenderTaintacle.LAYER_6, () -> thaumcraft.client.renderers.models.entity.ModelTaintacle.create(6));
        event.registerLayerDefinition(thaumcraft.client.renderers.entity.mob.RenderTaintacle.LAYER_14, () -> thaumcraft.client.renderers.models.entity.ModelTaintacle.create(14));
        event.registerLayerDefinition(thaumcraft.client.renderers.entity.mob.RenderTaintSeed.LAYER, thaumcraft.client.renderers.models.entity.ModelTaintSeed::create);
        event.registerLayerDefinition(ROBE_CHEST, () -> ModelRobe.createLayer(false));
        event.registerLayerDefinition(ROBE_LEGS, () -> ModelRobe.createLayer(true));
        event.registerLayerDefinition(FORTRESS_CHEST, () -> ModelFortressArmor.createLayer(false));
        event.registerLayerDefinition(FORTRESS_LEGS, () -> ModelFortressArmor.createLayer(true));
        event.registerLayerDefinition(KNIGHT_CHEST, () -> ModelKnightArmor.createLayer(false));
        event.registerLayerDefinition(KNIGHT_LEGS, () -> ModelKnightArmor.createLayer(true));
        event.registerLayerDefinition(LEADER_CHEST, () -> ModelLeaderArmor.createLayer(false));
        event.registerLayerDefinition(LEADER_LEGS, () -> ModelLeaderArmor.createLayer(true));
    }

    /** Пара моделей (1.0F / 0.5F), запекаются при первом показе (после загрузки слоёв). */
    private static final class Pair<M extends HumanoidModel<LivingEntity>> {
        private final ModelLayerLocation chest;
        private final ModelLayerLocation legs;
        private final Function<ModelPart, M> factory;
        private M model1;
        private M model2;

        Pair(ModelLayerLocation chest, ModelLayerLocation legs, Function<ModelPart, M> factory) {
            this.chest = chest;
            this.legs = legs;
            this.factory = factory;
        }

        M get(EquipmentSlot slot) {
            if (this.model1 == null) {
                this.model1 = this.factory.apply(Minecraft.getInstance().getEntityModels().bakeLayer(this.chest));
                this.model2 = this.factory.apply(Minecraft.getInstance().getEntityModels().bakeLayer(this.legs));
            }
            return slot == EquipmentSlot.CHEST || slot == EquipmentSlot.FEET ? this.model1 : this.model2;
        }
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        Pair<ModelRobe> robe = new Pair<>(ROBE_CHEST, ROBE_LEGS, ModelRobe::new);
        Pair<ModelFortressArmor> fortress = new Pair<>(FORTRESS_CHEST, FORTRESS_LEGS, ModelFortressArmor::new);
        Pair<ModelKnightArmor> knight = new Pair<>(KNIGHT_CHEST, KNIGHT_LEGS, ModelKnightArmor::new);
        Pair<ModelLeaderArmor> leader = new Pair<>(LEADER_CHEST, LEADER_LEGS, ModelLeaderArmor::new);

        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                ModelRobe m = robe.get(slot);
                animateRobe(m, entity);
                return m;
            }

            @Override
            public int getDefaultDyeColor(ItemStack stack) {
                return 0xFF000000 | ItemVoidRobeArmor.getColor(stack);
            }
        }, ItemsTC.voidRobeHelm.get(), ItemsTC.voidRobeChest.get(), ItemsTC.voidRobeLegs.get());

        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                ModelRobe m = robe.get(slot);
                animateRobe(m, entity);
                return m;
            }
        }, ItemsTC.crimsonRobeHelm.get(), ItemsTC.crimsonRobeChest.get(), ItemsTC.crimsonRobeLegs.get());

        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                ModelFortressArmor m = fortress.get(slot);
                fortressParts(m, entity);
                return m;
            }
        }, ItemsTC.fortressHelm.get(), ItemsTC.fortressChest.get(), ItemsTC.fortressLegs.get());

        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return knight.get(slot);
            }
        }, ItemsTC.crimsonPlateHelm.get(), ItemsTC.crimsonPlateChest.get(), ItemsTC.crimsonPlateLegs.get());

        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return leader.get(slot);
            }
        }, ItemsTC.crimsonPraetorHelm.get(), ItemsTC.crimsonPraetorChest.get(), ItemsTC.crimsonPraetorLegs.get());
    }

    private static float partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    /** ModelRobe.render: полы ткани следуют шагу (меньший из углов ног). */
    private static void animateRobe(ModelRobe m, LivingEntity entity) {
        float pt = partialTick();
        float f = entity.walkAnimation.position(pt);
        float f1 = entity.walkAnimation.speed(pt);
        float a = Mth.cos(f * 0.6662F) * 1.4F * f1;
        float b = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * f1;
        float c = Math.min(a, b);
        setX(m.FrontclothR1, c - 0.1047198F);
        setX(m.FrontclothL1, c - 0.1047198F);
        setX(m.FrontclothR2, c - 0.3316126F);
        setX(m.FrontclothL2, c - 0.3316126F);
        setX(m.ClothBackR1, -c + 0.1047198F);
        setX(m.ClothBackL1, -c + 0.1047198F);
        setX(m.ClothBackR2, -c + 0.2268928F);
        setX(m.ClothBackL2, -c + 0.2268928F);
        setX(m.ClothBackR3, -c + 0.2268928F);
        setX(m.ClothBackL3, -c + 0.2268928F);
    }

    private static void setX(ModelPart p, float x) {
        if (p != null) {
            p.xRot = x;
        }
    }

    private static void show(ModelPart p, boolean visible) {
        if (p != null) {
            p.visible = visible;
        }
    }

    /**
     * ModelFortressArmor.render/checkSet: части набора видны при 2–3 надетых частях крепости, маска — по шлему, очки — если в шлеме
     * очки (в 1.12 — статические карты по id сущности; здесь считается по броне сущности при каждом показе).
     */
    private static void fortressParts(ModelFortressArmor m, LivingEntity entity) {
        int set = 0;
        int mask = -1;
        boolean goggles = false;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            ItemStack s = entity.getItemBySlot(slot);
            if (s.getItem() instanceof ItemFortressArmor) {
                set++;
                if (slot == EquipmentSlot.HEAD) {
                    mask = ItemFortressArmor.getMask(s);
                    goggles = ItemFortressArmor.hasGoggles(s);
                }
            }
        }
        show(m.Goggles, goggles);
        show(m.Mask0, mask == 0);
        show(m.Mask1, mask == 1);
        show(m.Mask2, mask == 2);
        show(m.Scroll, set >= 3);
        show(m.Book, set >= 2);
        show(m.OrnamentL, set >= 3);
        show(m.OrnamentL2, set >= 3);
        show(m.OrnamentR, set >= 3);
        show(m.OrnamentR2, set >= 3);
        show(m.Gemornament, set >= 3);
        show(m.Gem, set >= 3);
        show(m.flapL, set >= 2);
        show(m.flapR, set >= 2);
        show(m.ShoulderplateLtop, set >= 2);
        show(m.ShoulderplateL1, set >= 2);
        show(m.ShoulderplateL2, set >= 3);
        show(m.ShoulderplateL3, set >= 3);
        show(m.ShoulderplateRtop, set >= 2);
        show(m.ShoulderplateR1, set >= 2);
        show(m.ShoulderplateR2, set >= 3);
        show(m.ShoulderplateR3, set >= 3);
        show(m.SidepanelR2, set >= 2);
        show(m.SidepanelL2, set >= 2);
        show(m.SidepanelR3, set >= 3);
        show(m.SidepanelL3, set >= 3);
    }
}
