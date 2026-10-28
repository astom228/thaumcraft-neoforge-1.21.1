package thaumcraft.api.casters;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Настройка узла фокуса (порт NodeSetting): значение из списка или из диапазона. */
public class NodeSetting {
    int value;
    public String key;
    String description;
    INodeSettingType type;
    String research;

    public NodeSetting(String key, String description, INodeSettingType setting, String research) {
        this.key = key;
        this.type = setting;
        this.value = setting.getDefault();
        this.description = description;
        this.research = research;
    }

    public NodeSetting(String key, String description, INodeSettingType setting) {
        this(key, description, setting, null);
    }

    public int getValue() {
        return this.type.getValue(this.value);
    }

    /** Подобрать внутренний индекс так, чтобы getValue() дал truevalue (или ближайшее по шагам). */
    public void setValue(int truevalue) {
        int lv = -1;
        this.value = 0;
        while (this.getValue() != truevalue && lv != this.value) {
            lv = this.value;
            this.increment();
        }
    }

    public String getResearch() {
        return this.research;
    }

    public String getValueText() {
        return Component.translatable(this.type.getValueText(this.value)).getString();
    }

    public void increment() {
        this.value++;
        this.value = this.getType().clamp(this.value);
    }

    public void decrement() {
        this.value--;
        this.value = this.getType().clamp(this.value);
    }

    public INodeSettingType getType() {
        return this.type;
    }

    public String getLocalizedName() {
        return Component.translatable(this.description).getString();
    }

    public interface INodeSettingType {
        int getDefault();

        int clamp(int old);

        int getValue(int value);

        String getValueText(int value);
    }

    public static class NodeSettingIntList implements INodeSettingType {
        int[] values;
        String[] descriptions;

        public NodeSettingIntList(int[] values, String[] descriptions) {
            this.values = values;
            this.descriptions = descriptions;
        }

        @Override
        public int getDefault() {
            return 0;
        }

        @Override
        public int clamp(int old) {
            return Mth.clamp(old, 0, this.values.length - 1);
        }

        @Override
        public int getValue(int value) {
            return this.values[this.clamp(value)];
        }

        @Override
        public String getValueText(int value) {
            return this.descriptions[value];
        }
    }

    public static class NodeSettingIntRange implements INodeSettingType {
        int min;
        int max;

        public NodeSettingIntRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public int getDefault() {
            return this.min;
        }

        @Override
        public int clamp(int old) {
            return Mth.clamp(old, this.min, this.max);
        }

        @Override
        public int getValue(int value) {
            return this.clamp(value);
        }

        @Override
        public String getValueText(int value) {
            return "" + this.getValue(value);
        }
    }
}
