package thaumcraft.common.entities.construct.golem;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.items.ICreativeVariants;

/**
 * Голем в предмете (порт ItemGolemPlacer): в данных — состав ("props", long) и опыт ранга ("xp"). ПКМ по твёрдому блоку
 * ставит голема на соседнюю клетку; хозяин — поставивший.
 */
public class ItemGolemPlacer extends Item implements ISealDisplayer, ICreativeVariants {
    public ItemGolemPlacer(Properties props) {
        super(props);
    }

    public static ItemStack createStack(long props, int xp) {
        ItemStack stack = new ItemStack(ItemsTC.golemPlacer.get());
        CompoundTag tag = new CompoundTag();
        tag.putLong("props", props);
        if (xp != 0) {
            tag.putInt("xp", xp);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd == null ? null : cd.copyTag();
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        output.accept(createStack(0L, 0));
        IGolemProperties props = new GolemProperties();
        props.setHead(GolemHead.getHeads()[1]);
        props.setArms(GolemArm.getArms()[1]);
        output.accept(createStack(props.toLong(), 0));
        props = new GolemProperties();
        props.setMaterial(GolemMaterial.getMaterials()[1]);
        props.setHead(GolemHead.getHeads()[1]);
        props.setArms(GolemArm.getArms()[2]);
        output.accept(createStack(props.toLong(), 0));
        props = new GolemProperties();
        props.setMaterial(GolemMaterial.getMaterials()[4]);
        props.setHead(GolemHead.getHeads()[1]);
        props.setArms(GolemArm.getArms()[3]);
        output.accept(createStack(props.toLong(), 0));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        if (tag != null && tag.contains("props")) {
            IGolemProperties props = GolemProperties.fromLong(tag.getLong("props"));
            if (props.hasTrait(EnumGolemTrait.SMART)) {
                if (props.getRank() >= 10) {
                    tooltip.add(Component.translatable("golem.rank").append(" " + props.getRank()).withStyle(ChatFormatting.GOLD));
                } else {
                    int rx = tag.getInt("xp");
                    int xn = (props.getRank() + 1) * (props.getRank() + 1) * EntityThaumcraftGolem.XPM;
                    tooltip.add(Component.translatable("golem.rank").append(" " + props.getRank()).withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(" (" + rx + "/" + xn + ")").withStyle(ChatFormatting.DARK_GREEN)));
                }
            }
            tooltip.add(props.getMaterial().getLocalizedName().copy().withStyle(ChatFormatting.GREEN));
            for (EnumGolemTrait t : props.getTraits()) {
                tooltip.add(Component.literal("-").append(t.getLocalizedName()).withStyle(ChatFormatting.BLUE));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (!world.getBlockState(pos).isSolid()) {
            return InteractionResult.FAIL;
        }
        if (world.isClientSide) {
            return InteractionResult.PASS;
        }
        pos = pos.relative(ctx.getClickedFace());
        if (player == null || !player.mayUseItemAt(pos, ctx.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        EntityThaumcraftGolem golem = ConfigEntities.GOLEM.get().create(world);
        if (golem == null) {
            return InteractionResult.FAIL;
        }
        golem.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        golem.setOwned(true);
        golem.setValidSpawn();
        golem.setOwnerId(player.getUUID());
        CompoundTag tag = data(stack);
        if (tag != null && tag.contains("props")) {
            golem.setProperties(GolemProperties.fromLong(tag.getLong("props")));
        }
        if (tag != null && tag.contains("xp")) {
            golem.rankXp = tag.getInt("xp");
        }
        golem.finalizeSpawn((ServerLevel) world, world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
        golem.setHealth(golem.getMaxHealth());
        if (world.addFreshEntity(golem) && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
