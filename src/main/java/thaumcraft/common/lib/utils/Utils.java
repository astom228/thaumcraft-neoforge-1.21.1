package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCTags;

/** Порт части Utils: бревно/руда (по тегам вместо OreDictionary), особый результат добычи (самородные кластеры). */
public final class Utils {
    /** Особый результат добычи: предмет или тег предметов → результат, множитель шанса (addSpecialMiningResult). */
    private record SpecialMining(Item item, TagKey<Item> tag, ItemStack out, float chance) {
        boolean test(ItemStack is) {
            return this.item != null ? is.is(this.item) : is.is(this.tag);
        }
    }

    private static final List<SpecialMining> SPECIAL_MINING = new ArrayList<>();

    private Utils() {
    }

    public static synchronized void addSpecialMiningResult(Item in, ItemStack out, float chance) {
        SPECIAL_MINING.add(new SpecialMining(in, null, out, chance));
    }

    public static synchronized void addSpecialMiningResult(TagKey<Item> in, ItemStack out, float chance) {
        SPECIAL_MINING.add(new SpecialMining(null, in, out, chance));
    }

    /**
     * Самородные кластеры (ModConfig.modCompatibility и initMisc оригинала): в 1.21 руды роняют сырьё, поэтому кластер
     * дают и блоки руд (тег c:ores/…), и сырьё (c:raw_materials/…); киноварь — своя руда, кварц — сам кварц.
     */
    public static void initSpecialMining() {
        addSpecialMiningResult(Tags.Items.ORES_IRON, new ItemStack(ItemsTC.clusterIron.get()), 1.0F);
        addSpecialMiningResult(Tags.Items.RAW_MATERIALS_IRON, new ItemStack(ItemsTC.clusterIron.get()), 1.0F);
        addSpecialMiningResult(Tags.Items.ORES_GOLD, new ItemStack(ItemsTC.clusterGold.get()), 1.0F);
        addSpecialMiningResult(Tags.Items.RAW_MATERIALS_GOLD, new ItemStack(ItemsTC.clusterGold.get()), 1.0F);
        addSpecialMiningResult(Tags.Items.ORES_COPPER, new ItemStack(ItemsTC.clusterCopper.get()), 1.0F);
        addSpecialMiningResult(Tags.Items.RAW_MATERIALS_COPPER, new ItemStack(ItemsTC.clusterCopper.get()), 1.0F);
        for (String m : new String[] {"tin", "silver", "lead"}) {
            Item cluster = switch (m) {
                case "tin" -> ItemsTC.clusterTin.get();
                case "silver" -> ItemsTC.clusterSilver.get();
                default -> ItemsTC.clusterLead.get();
            };
            addSpecialMiningResult(itemTag("c", "ores/" + m), new ItemStack(cluster), 1.0F);
            addSpecialMiningResult(itemTag("c", "raw_materials/" + m), new ItemStack(cluster), 1.0F);
        }
        addSpecialMiningResult(TCTags.Items.ORES_CINNABAR, new ItemStack(ItemsTC.clusterCinnabar.get()), 1.0F);
        addSpecialMiningResult(Items.QUARTZ, new ItemStack(ItemsTC.clusterQuartz.get()), 1.0F);
    }

    private static TagKey<Item> itemTag(String ns, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path));
    }

    /** С вероятностью chance · множитель — особый результат (количество умножается на размер стака). */
    public static ItemStack findSpecialMiningResult(ItemStack is, float chance, RandomSource rand) {
        ItemStack dropped = is.copy();
        float r = rand.nextFloat();
        SpecialMining found = null;
        synchronized (Utils.class) {
            for (SpecialMining sm : SPECIAL_MINING) {
                if (sm.test(is)) {
                    found = sm;
                    break;
                }
            }
        }
        if (found != null && r <= chance * found.chance()) {
            dropped = found.out().copy();
            dropped.setCount(dropped.getCount() * is.getCount());
        }
        return dropped;
    }

    /** Бревно: isWood / canSustainLeaves / oreDict «logWood» оригинала → тег minecraft:logs. */
    public static boolean isWoodLog(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos).is(BlockTags.LOGS);
    }

    /** Руда: имя OreDictionary содержит «ORE» → тег c:ores. */
    public static boolean isOreBlock(BlockGetter world, BlockPos pos) {
        BlockState bi = world.getBlockState(pos);
        return !bi.isAir() && !bi.is(Blocks.BEDROCK) && bi.is(Tags.Blocks.ORES);
    }

    /** Точка x внутри конуса с вершиной t, осью к b и углом раствора aperture (радианы), не дальше b. */
    public static boolean isLyingInCone(double[] x, double[] t, double[] b, float aperture) {
        double halfAperture = aperture / 2.0F;
        double[] apexToXVect = dif(t, x);
        double[] axisVect = dif(t, b);
        boolean isInInfiniteCone = dotProd(apexToXVect, axisVect) / magn(apexToXVect) / magn(axisVect) > Math.cos(halfAperture);
        return isInInfiniteCone && dotProd(apexToXVect, axisVect) / magn(axisVect) < magn(axisVect);
    }

    public static double dotProd(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static double[] dif(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    public static double magn(double[] a) {
        return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    }

    /** Предмет из мешка с добычей (generateLoot): с шансом 2.5%·редкость — снаряжение, иначе — по таблице редкости; книги зачаровываются. */
    public static ItemStack generateLoot(net.minecraft.world.level.Level world, int rarity, RandomSource rand) {
        ItemStack is;
        if (rarity > 0 && rand.nextFloat() < 0.025F * rarity) {
            is = genGear(world, rarity, rand);
            if (is.isEmpty()) {
                is = generateLoot(world, rarity, rand);
            }
        } else {
            is = WeightedRandomLoot.pick(switch (rarity) {
                case 1 -> WeightedRandomLoot.lootBagUncommon;
                case 2 -> WeightedRandomLoot.lootBagRare;
                default -> WeightedRandomLoot.lootBagCommon;
            }, rand);
        }
        is = is.copy();
        if (is.is(Items.BOOK)) {
            is = net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(rand, is, (int) (5.0F + rarity * 0.75F * rand.nextInt(18)),
                    world.registryAccess(), java.util.Optional.empty());
        }
        return is;
    }

    private static ItemStack genGear(net.minecraft.world.level.Level world, int rarity, RandomSource rand) {
        int quality = rand.nextInt(2);
        if (rand.nextFloat() < 0.2F) {
            quality++;
        }
        if (rand.nextFloat() < 0.15F) {
            quality++;
        }
        if (rand.nextFloat() < 0.1F) {
            quality++;
        }
        if (rand.nextFloat() < 0.095F) {
            quality++;
        }
        Item item = getGearItemForSlot(rand.nextInt(5), quality);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack is = new ItemStack(item);
        is.setDamageValue(rand.nextInt(1 + is.getMaxDamage() / 6));
        if (rand.nextInt(4) < rarity) {
            is = net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(rand, is, (int) (5.0F + rarity * 0.75F * rand.nextInt(18)),
                    world.registryAccess(), java.util.Optional.empty());
        }
        return is;
    }

    /** Снаряжение по слоту (0 — оружие, 1–4 — броня) и качеству 0–6; в оригинале — switch без break: при отсутствии — следующий слот. */
    private static Item getGearItemForSlot(int slot, int quality) {
        Item[][] table = {
                {Items.IRON_AXE, Items.IRON_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_SWORD, ItemsTC.thaumiumSword.get(), Items.DIAMOND_SWORD, ItemsTC.voidSword.get()},
                {Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, ItemsTC.thaumiumBoots.get(), Items.DIAMOND_BOOTS,
                        ItemsTC.voidBoots.get()},
                {Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, ItemsTC.thaumiumLegs.get(),
                        Items.DIAMOND_LEGGINGS, ItemsTC.voidLegs.get()},
                {Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, ItemsTC.thaumiumChest.get(),
                        Items.DIAMOND_CHESTPLATE, ItemsTC.voidChest.get()},
                {Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, ItemsTC.thaumiumHelm.get(), Items.DIAMOND_HELMET,
                        ItemsTC.voidHelm.get()}};
        return quality >= 0 && quality < 7 ? table[slot][quality] : null;
    }
}
