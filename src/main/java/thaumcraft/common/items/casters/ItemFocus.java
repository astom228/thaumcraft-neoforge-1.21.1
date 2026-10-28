package thaumcraft.common.items.casters;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Фокус (порт ItemFocus): простой / продвинутый / великий — различаются предельной сложностью. Заклинание — пакет фокуса
 * в компоненте данных. Цвет и ключ сортировки вычисляются из пакета (в оригинале кэшировались записью в NBT при чтении).
 */
public class ItemFocus extends Item {
    static final DecimalFormat DECIMALFORMAT = new DecimalFormat("#,##0.##");
    private final int maxComplexity;

    public ItemFocus(int complexity, Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        this.maxComplexity = complexity;
    }

    /** Средний цвет эффектов пакета; без пакета — белый. */
    public int getFocusColor(ItemStack focusstack) {
        if (focusstack == null || focusstack.isEmpty()) {
            return 0xFFFFFF;
        }
        int color = 0xFFFFFF;
        FocusPackage core = getPackage(focusstack);
        if (core != null) {
            FocusEffect[] fe = core.getFocusEffects();
            int r = 0;
            int g = 0;
            int b = 0;
            for (FocusEffect ef : fe) {
                Color c = new Color(FocusEngine.getElementColor(ef.getKey()));
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
            }
            // в оригинале пакет без эффектов давал деление на ноль (исключение) — здесь белый
            if (fe.length > 0) {
                color = new Color(r / fe.length, g / fe.length, b / fe.length).getRGB();
            }
        }
        return color;
    }

    /** Имя + хэш узлов пакета: ключ сортировки фокусов при смене (CasterManager.changeFocus). */
    public String getSortingHelper(ItemStack focusstack) {
        if (focusstack == null || focusstack.isEmpty() || !focusstack.has(TCDataComponents.FOCUS_PACKAGE.get())) {
            return null;
        }
        FocusPackage p = getPackage(focusstack);
        int sh = p == null ? 0 : p.getSortingHelper();
        return focusstack.getHoverName().getString() + sh;
    }

    public static void setPackage(ItemStack focusstack, FocusPackage core) {
        focusstack.set(TCDataComponents.FOCUS_PACKAGE.get(), core.serialize());
    }

    public static FocusPackage getPackage(ItemStack focusstack) {
        if (focusstack == null || focusstack.isEmpty()) {
            return null;
        }
        net.minecraft.nbt.CompoundTag tag = focusstack.get(TCDataComponents.FOCUS_PACKAGE.get());
        if (tag == null) {
            return null;
        }
        FocusPackage p = new FocusPackage();
        p.deserialize(tag);
        return p;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        this.addFocusInformation(stack, tooltip);
    }

    public void addFocusInformation(ItemStack stack, List<Component> tooltip) {
        FocusPackage p = getPackage(stack);
        if (p != null) {
            float al = this.getVisCost(stack);
            tooltip.add(Component.literal(DECIMALFORMAT.format(al) + " ").append(Component.translatable("item.Focus.cost1")));
            for (IFocusElement fe : p.nodes) {
                if (fe instanceof FocusNode fn && !(fe instanceof FocusMediumRoot)) {
                    this.buildInfo(tooltip, fn, 0);
                }
            }
        }
    }

    private void buildInfo(List<Component> list, FocusNode node, int depth) {
        if (node instanceof FocusMediumRoot) {
            return;
        }
        StringBuilder t0 = new StringBuilder();
        t0.append("  ".repeat(depth));
        t0.append(ChatFormatting.DARK_PURPLE).append(Component.translatable(node.getUnlocalizedName()).getString());
        if (!node.getSettingList().isEmpty()) {
            t0.append(ChatFormatting.DARK_AQUA).append(" [");
            boolean q = false;
            for (String st : node.getSettingList()) {
                NodeSetting ns = node.getSetting(st);
                t0.append(q ? ", " : "").append(ns.getLocalizedName()).append(" ").append(ns.getValueText());
                q = true;
            }
            t0.append("]");
        }
        list.add(Component.literal(t0.toString()));
        if (node instanceof FocusModSplit split) {
            for (FocusPackage p : split.getSplitPackages()) {
                for (IFocusElement fe : p.nodes) {
                    if (fe instanceof FocusNode fn && !(fe instanceof FocusMediumRoot)) {
                        this.buildInfo(list, fn, depth + 1);
                    }
                }
            }
        }
    }

    public float getVisCost(ItemStack focusstack) {
        FocusPackage p = getPackage(focusstack);
        return p == null ? 0.0F : p.getComplexity() / 5.0F;
    }

    public int getActivationTime(ItemStack focusstack) {
        FocusPackage p = getPackage(focusstack);
        return p == null ? 0 : Math.max(5, p.getComplexity() / 5 * (p.getComplexity() / 4));
    }

    public int getMaxComplexity() {
        return this.maxComplexity;
    }
}
