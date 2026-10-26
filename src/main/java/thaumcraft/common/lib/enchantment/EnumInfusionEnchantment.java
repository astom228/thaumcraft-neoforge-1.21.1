package thaumcraft.common.lib.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Наполняющие зачарования (порт EnumInfusionEnchantment). В 1.12 — NBT «infench»: [{id: номер, lvl}]; здесь — компонент
 * thaumcraft:infusion_enchantments, список {имя, уровень} в порядке добавления. toolClasses — для каких предметов:
 * axe/pickaxe/shovel — инструменты, weapon — урон в основной руке, helm/chest/legs/boots — доспехи, chargable — заряжаемые.
 */
public enum EnumInfusionEnchantment {
    COLLECTOR(Set.of("axe", "pickaxe", "shovel", "weapon"), 1, "INFUSIONENCHANTMENT"),
    DESTRUCTIVE(Set.of("axe", "pickaxe", "shovel"), 1, "INFUSIONENCHANTMENT"),
    BURROWING(Set.of("axe", "pickaxe"), 1, "INFUSIONENCHANTMENT"),
    SOUNDING(Set.of("pickaxe"), 4, "INFUSIONENCHANTMENT"),
    REFINING(Set.of("pickaxe"), 4, "INFUSIONENCHANTMENT"),
    ARCING(Set.of("weapon"), 4, "INFUSIONENCHANTMENT"),
    ESSENCE(Set.of("weapon"), 5, "INFUSIONENCHANTMENT"),
    VISBATTERY(Set.of("chargable"), 3, "?"),
    VISCHARGE(Set.of("chargable"), 1, "?"),
    SWIFT(Set.of("boots"), 4, "IEARMOR"),
    AGILE(Set.of("legs"), 1, "IEARMOR"),
    INFESTED(Set.of("chest"), 1, "IETAINT");

    public final Set<String> toolClasses;
    public final int maxLevel;
    public final String research;

    EnumInfusionEnchantment(Set<String> toolClasses, int ml, String research) {
        this.toolClasses = toolClasses;
        this.maxLevel = ml;
        this.research = research;
    }

    /** Запись компонента: зачарование и уровень. */
    public record Entry(String id, int lvl) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Entry::id),
                Codec.INT.fieldOf("lvl").forGetter(Entry::lvl)).apply(i, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::id, ByteBufCodecs.VAR_INT, Entry::lvl, Entry::new);

        public EnumInfusionEnchantment enchantment() {
            try {
                return EnumInfusionEnchantment.valueOf(this.id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static List<Entry> getInfusionEnchantmentTagList(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.get(TCDataComponents.INFUSION_ENCHANTMENTS.get());
    }

    public static List<EnumInfusionEnchantment> getInfusionEnchantments(ItemStack stack) {
        List<EnumInfusionEnchantment> list = new ArrayList<>();
        List<Entry> tags = getInfusionEnchantmentTagList(stack);
        if (tags != null) {
            for (Entry e : tags) {
                EnumInfusionEnchantment ie = e.enchantment();
                if (ie != null) {
                    list.add(ie);
                }
            }
        }
        return list;
    }

    public static int getInfusionEnchantmentLevel(ItemStack stack, EnumInfusionEnchantment enchantment) {
        List<Entry> tags = getInfusionEnchantmentTagList(stack);
        if (tags != null) {
            for (Entry e : tags) {
                if (e.enchantment() == enchantment) {
                    return e.lvl();
                }
            }
        }
        return 0;
    }

    /** Добавить или поднять уровень (не выше максимального и не ниже текущего). */
    public static void addInfusionEnchantment(ItemStack stack, EnumInfusionEnchantment ie, int level) {
        if (stack == null || stack.isEmpty() || level > ie.maxLevel) {
            return;
        }
        List<Entry> tags = getInfusionEnchantmentTagList(stack);
        List<Entry> out = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        for (int j = 0; j < out.size(); j++) {
            if (out.get(j).enchantment() == ie) {
                if (level <= out.get(j).lvl()) {
                    return;
                }
                out.set(j, new Entry(ie.name(), level));
                stack.set(TCDataComponents.INFUSION_ENCHANTMENTS.get(), List.copyOf(out));
                return;
            }
        }
        out.add(new Entry(ie.name(), level));
        stack.set(TCDataComponents.INFUSION_ENCHANTMENTS.get(), List.copyOf(out));
    }
}
