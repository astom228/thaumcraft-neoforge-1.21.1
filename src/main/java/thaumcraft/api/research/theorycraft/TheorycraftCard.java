package thaumcraft.api.research.theorycraft;

import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Карточка теоретизирования (порт TheorycraftCard). initialize — подготовка при вытягивании (false — карточка не выпадает),
 * activate — розыгрыш на сервере. Название и текст — на языке клиента (Component.translatable(...).getString()).
 * В отличие от оригинала, serialize/deserialize получают HolderLookup.Provider (нужен для сохранения предметов в 1.21).
 */
public abstract class TheorycraftCard {
    private long seed = -1L;

    public long getSeed() {
        if (this.seed < 0L) {
            this.setSeed(System.nanoTime());
        }
        return this.seed;
    }

    public boolean initialize(Player player, ResearchTableData data) {
        return true;
    }

    /** Только из помощников (в общей колоде не выпадает). */
    public boolean isAidOnly() {
        return false;
    }

    public abstract int getInspirationCost();

    public String getResearchCategory() {
        return null;
    }

    public abstract String getLocalizedName();

    public abstract String getLocalizedText();

    public ItemStack[] getRequiredItems() {
        return null;
    }

    public boolean[] getRequiredItemsConsumed() {
        if (this.getRequiredItems() != null) {
            boolean[] b = new boolean[this.getRequiredItems().length];
            Arrays.fill(b, false);
            return b;
        }
        return null;
    }

    public abstract boolean activate(Player player, ResearchTableData data);

    public void setSeed(long seed) {
        this.seed = Math.abs(seed);
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("seed", this.seed);
        return nbt;
    }

    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt != null) {
            this.seed = nbt.getLong("seed");
        }
    }

    /** Перевод строки с аргументами (TextComponentTranslation(...).getFormattedText() оригинала). */
    protected static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    /** Название категории жирным (как в текстах карточек оригинала). */
    protected static String boldCategory(String cat) {
        return ChatFormatting.BOLD + tr("tc.research_category." + cat) + ChatFormatting.RESET;
    }

    /** MathHelper.getInt 1.12: случайное от min до max включительно (при min >= max — min). */
    protected static int randInt(net.minecraft.util.RandomSource rand, int min, int max) {
        return min >= max ? min : rand.nextInt(max - min + 1) + min;
    }

    protected static int randInt(java.util.Random rand, int min, int max) {
        return min >= max ? min : rand.nextInt(max - min + 1) + min;
    }
}
