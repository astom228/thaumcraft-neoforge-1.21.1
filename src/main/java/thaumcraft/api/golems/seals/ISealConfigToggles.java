package thaumcraft.api.golems.seals;

/** Печать с переключателями (порт ISealConfigToggles). */
public interface ISealConfigToggles {
    SealToggle[] getToggles();

    void setToggle(int index, boolean value);

    class SealToggle {
        public boolean value;
        public String key;
        public String name;

        public SealToggle(boolean value, String key, String name) {
            this.value = value;
            this.key = key;
            this.name = name;
        }

        public boolean getValue() {
            return this.value;
        }

        public void setValue(boolean value) {
            this.value = value;
        }

        public String getKey() {
            return this.key;
        }

        public String getName() {
            return this.name;
        }
    }
}
