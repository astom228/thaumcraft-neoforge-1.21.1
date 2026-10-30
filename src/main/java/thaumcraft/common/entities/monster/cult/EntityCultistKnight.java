package thaumcraft.common.entities.monster.cult;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.ai.combat.AICultistHurtByTarget;

/**
 * Рыцарь Багрового культа (порт EntityCultistKnight): 30 здоровья, латы культа, железный меч; редко (1%, на сложной —
 * 5%) — тауминиевый или пустотный меч (с капюшоном вместо шлема).
 */
public class EntityCultistKnight extends EntityCultist {
    public EntityCultistKnight(EntityType<? extends EntityCultistKnight> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCultist.createAttributes().add(Attributes.MAX_HEALTH, 30.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        // EntityAIRestrictOpenDoor (деревни 1.12) в 1.21 нет
        this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new AICultistHurtByTarget(this, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void setLoot(DifficultyInstance diff) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemsTC.crimsonPlateHelm.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemsTC.crimsonPlateChest.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ItemsTC.crimsonPlateLegs.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemsTC.crimsonBoots.get()));
        if (this.random.nextFloat() < (this.level().getDifficulty() == Difficulty.HARD ? 0.05F : 0.01F)) {
            int i = this.random.nextInt(5);
            if (i == 0) {
                this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemsTC.voidSword.get()));
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemsTC.crimsonRobeHelm.get()));
            } else {
                this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemsTC.thaumiumSword.get()));
                if (this.random.nextBoolean()) {
                    this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            }
        } else {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    @Override
    protected void enchantGear(RandomSource rand, DifficultyInstance diff) {
        float f = diff.getSpecialMultiplier();
        if (!this.getMainHandItem().isEmpty() && rand.nextFloat() < 0.25F * f) {
            enchant(this, this.getMainHandItem(), (int) (5.0F + f * rand.nextInt(18)));
        }
    }

    /** addRandomEnchantment оригинала (без сокровищ — набор стола зачарования). */
    public static void enchant(LivingEntity e, ItemStack stack, int level) {
        var access = e.level().registryAccess();
        var set = access.registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        ItemStack res = EnchantmentHelper.enchantItem(e.getRandom(), stack.copy(), level, access, set);
        e.setItemInHand(InteractionHand.MAIN_HAND, res);
    }
}
