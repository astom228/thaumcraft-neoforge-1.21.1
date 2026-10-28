package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Сапоги странника (порт ItemBootsTraveller). Заряд Вис до 240; пока надеты и заряжены — бонус к скорости при ходьбе вперёд,
 * шаг на полный блок (без Shift), повышенное управление в прыжке и прыжок выше (PlayerEvents.playerJumps).
 * Расход: 1 заряд на 60 секунд ходьбы-ношения ("energy" оригинала).
 * Высота шага: в 1.12 — поле stepHeight и статическая карта прежних значений на клиенте; в порте — временный модификатор
 * атрибута STEP_HEIGHT у клиентского игрока (без общего статического состояния).
 */
public class ItemBootsTraveller extends ArmorItem implements IRechargable {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/bootstraveler.png");
    private static final ResourceLocation STEP_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "traveller_step");
    /** stepHeight 1.0 при базовой 0.6. */
    private static final AttributeModifier STEP = new AttributeModifier(STEP_ID, 0.4, AttributeModifier.Operation.ADD_VALUE);

    public ItemBootsTraveller(Holder<ArmorMaterial> material, Properties props) {
        super(material, Type.BOOTS, props);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEX;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.LEATHER) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player) || !ItemVoidArmor.isWorn(stack, entity, slot)) {
            return;
        }
        boolean hasCharge = RechargeHelper.getCharge(stack) > 0;
        if (!level.isClientSide && player.tickCount % 20 == 0) {
            int e = stack.getOrDefault(TCDataComponents.TRAVELLER_ENERGY.get(), 0);
            if (e > 0) {
                e--;
            } else if (RechargeHelper.consumeCharge(stack, player, 1)) {
                e = 60;
            }
            stack.set(TCDataComponents.TRAVELLER_ENERGY.get(), e);
        }
        if (hasCharge && !player.getAbilities().flying && player.zza > 0.0F) {
            if (level.isClientSide && !player.isShiftKeyDown()) {
                AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
                if (step != null && !step.hasModifier(STEP_ID)) {
                    step.addTransientModifier(STEP);
                }
            }
            if (player.onGround()) {
                float bonus = 0.05F;
                if (player.isInWater()) {
                    bonus /= 4.0F;
                }
                player.moveRelative(bonus, new Vec3(0.0, 0.0, 1.0));
            } else {
                if (player.isInWater()) {
                    player.moveRelative(0.025F, new Vec3(0.0, 0.0, 1.0));
                }
                // jumpMovementFactor = 0.05 в 1.12: в 1.21 скорость в воздухе — getFlyingSpeed (0.02 / 0.026 бегом), добавляем разницу
                float air = 0.05F - (player.isSprinting() ? 0.026F : 0.02F);
                player.moveRelative(air, new Vec3(player.xxa, 0.0, player.zza));
            }
        }
    }

    /** handleSpeedMods оригинала (клиент, каждый тик игрока): шаг возвращается, если присел или снял сапоги. */
    public static void handleSpeedMods(Player player) {
        if (player.level().isClientSide && (player.isShiftKeyDown() || !player.getItemBySlot(EquipmentSlot.FEET).is(ItemsTC.travellerBoots.get()))) {
            AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
            if (step != null && step.hasModifier(STEP_ID)) {
                step.removeModifier(STEP_ID);
            }
        }
    }

    @Override
    public int getMaxCharge(ItemStack stack, LivingEntity player) {
        return 240;
    }

    @Override
    public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) {
        return EnumChargeDisplay.PERIODIC;
    }
}
