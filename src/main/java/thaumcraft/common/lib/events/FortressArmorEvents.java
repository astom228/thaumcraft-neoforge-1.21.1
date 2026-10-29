package thaumcraft.common.lib.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.items.armor.ItemVoidRobeArmor;

/**
 * Замена ISpecialArmor для «Таумиумной крепости» и пустотной робы (DECISIONS 107) и эффекты масок крепости
 * (часть EntityEvents.entityHurt оригинала).
 * <p>
 * В 1.12 каждая часть поглощала долю урона: обычный — защита/25, магия — защита/35, у крепости огонь и взрыв — защита/20,
 * «неблокируемый» — 0; плюс к обычной броне +1 за часть (и ещё +1, если на шлеме маска), стойкость +1 за часть. В порте части —
 * обычная броня с теми же числами защиты (обычный урон режется ванильной формулой ≈ защита/25) и стойкостью 1, а здесь
 * добавляется недостающее: множитель для магии и для огня/взрыва до той же итоговой доли и бонус брони за маску/набор.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class FortressArmorEvents {
    private static final ResourceLocation SET_BONUS = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fortress_set");
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity ent = event.getEntity();
        if (ent.level().isClientSide) {
            return;
        }
        DamageSource src = event.getSource();
        masks(event, ent, src);
        int fortress = 0;
        int robe = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = ent.getItemBySlot(slot);
            if (s.getItem() instanceof ItemFortressArmor a) {
                fortress += a.getDefense();
            } else if (s.getItem() instanceof ItemVoidRobeArmor a) {
                robe += a.getDefense();
            }
        }
        if (fortress + robe == 0) {
            return;
        }
        float target;
        if (src.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            target = (fortress + robe) / 35.0F;
        } else if (src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypeTags.IS_EXPLOSION)) {
            target = fortress / 20.0F + robe / 25.0F;
        } else {
            return; // обычный урон — ванильная броня; «неблокируемый» — без поглощения, как в 1.12
        }
        target = Math.min(1.0F, target);
        // броня 1.21 уже режет неблокируемый урон примерно на защита/25 — добавляем только разницу
        float vanilla = src.is(DamageTypeTags.BYPASSES_ARMOR) ? 0.0F : Math.min(0.8F, (fortress + robe) / 25.0F);
        float factor = Math.min(1.0F, (1.0F - target) / (1.0F - vanilla));
        event.setAmount(event.getAmount() * factor);
    }

    /** Маски: «Пьющий изверг» (2) — атакующий игрок лечится на 1 с шансом урон/12; «Злой призрак» (1) — иссушение 4 с обидчику. */
    private static void masks(LivingIncomingDamageEvent event, LivingEntity ent, DamageSource src) {
        if (src.getEntity() instanceof Player leecher) {
            ItemStack helm = leecher.getItemBySlot(EquipmentSlot.HEAD);
            if (helm.getItem() instanceof ItemFortressArmor && ItemFortressArmor.getMask(helm) == 2
                    && leecher.getRandom().nextFloat() < event.getAmount() / 12.0F) {
                leecher.heal(1.0F);
            }
        }
        if (ent instanceof Player player && src.getEntity() instanceof LivingEntity attacker) {
            ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helm.getItem() instanceof ItemFortressArmor && ItemFortressArmor.getMask(helm) == 1
                    && player.getRandom().nextFloat() < event.getAmount() / 10.0F) {
                attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
            }
        }
    }

    /** Бонус брони набора крепости: +1 за каждую надетую часть и ещё +1 за часть, если на шлеме маска (ArmorProperties.Armor оригинала). */
    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        int n = 0;
        boolean mask = false;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.getItem() instanceof ItemFortressArmor) {
                n++;
                mask |= ItemFortressArmor.getMask(s) >= 0;
            }
        }
        double bonus = n * (mask ? 2 : 1);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        AttributeModifier cur = armor.getModifier(SET_BONUS);
        if (bonus <= 0) {
            if (cur != null) {
                armor.removeModifier(SET_BONUS);
            }
        } else if (cur == null || cur.amount() != bonus) {
            armor.addOrUpdateTransientModifier(new AttributeModifier(SET_BONUS, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
