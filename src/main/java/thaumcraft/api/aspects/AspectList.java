package thaumcraft.api.aspects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Набор аспектов с количествами (порт AspectList; порядок вставки сохраняется).
 * Сериализация — как NBT оригинала: список {key, amount}.
 */
public class AspectList {
    public LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();

    private record Entry(String key, int amount) {
    }

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(Entry::key),
            Codec.INT.fieldOf("amount").forGetter(Entry::amount)).apply(i, Entry::new));
    private static final StreamCodec<ByteBuf, Entry> ENTRY_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Entry::key, ByteBufCodecs.VAR_INT, Entry::amount, Entry::new);

    public static final Codec<AspectList> CODEC = ENTRY_CODEC.listOf().xmap(AspectList::fromEntries, AspectList::toEntries);
    public static final StreamCodec<ByteBuf, AspectList> STREAM_CODEC =
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()).map(AspectList::fromEntries, AspectList::toEntries);

    private static AspectList fromEntries(List<Entry> entries) {
        AspectList out = new AspectList();
        for (Entry e : entries) {
            Aspect a = Aspect.getAspect(e.key());
            if (a != null) {
                out.add(a, e.amount());
            }
        }
        return out;
    }

    private List<Entry> toEntries() {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<Aspect, Integer> e : this.aspects.entrySet()) {
            if (e.getKey() != null) {
                out.add(new Entry(e.getKey().getTag(), e.getValue()));
            }
        }
        return out;
    }

    /** Аспекты предмета (как в оригинале: копия результата AspectHelper.getObjectAspects). */
    public AspectList(ItemStack stack) {
        try {
            AspectList temp = AspectHelper.getObjectAspects(stack);
            if (temp != null) {
                for (Aspect tag : temp.getAspects()) {
                    this.add(tag, temp.getAmount(tag));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public AspectList() {
    }

    public AspectList copy() {
        AspectList out = new AspectList();
        for (Aspect a : this.getAspects()) {
            out.add(a, this.getAmount(a));
        }
        return out;
    }

    public int size() {
        return this.aspects.size();
    }

    public int visSize() {
        int q = 0;
        for (Aspect as : this.aspects.keySet()) {
            q += this.getAmount(as);
        }
        return q;
    }

    public Aspect[] getAspects() {
        return this.aspects.keySet().toArray(new Aspect[0]);
    }

    public Aspect[] getAspectsSortedByName() {
        try {
            Aspect[] out = this.aspects.keySet().toArray(new Aspect[0]);
            boolean change;
            do {
                change = false;
                for (int a = 0; a < out.length - 1; a++) {
                    Aspect e1 = out[a];
                    Aspect e2 = out[a + 1];
                    if (e1 != null && e2 != null && e1.getTag().compareTo(e2.getTag()) > 0) {
                        out[a] = e2;
                        out[a + 1] = e1;
                        change = true;
                        break;
                    }
                }
            } while (change);
            return out;
        } catch (Exception e) {
            return this.getAspects();
        }
    }

    public Aspect[] getAspectsSortedByAmount() {
        try {
            Aspect[] out = this.aspects.keySet().toArray(new Aspect[0]);
            boolean change;
            do {
                change = false;
                for (int a = 0; a < out.length - 1; a++) {
                    int e1 = this.getAmount(out[a]);
                    int e2 = this.getAmount(out[a + 1]);
                    if (e1 > 0 && e2 > 0 && e2 > e1) {
                        Aspect ea = out[a];
                        out[a] = out[a + 1];
                        out[a + 1] = ea;
                        change = true;
                        break;
                    }
                }
            } while (change);
            return out;
        } catch (Exception e) {
            return this.getAspects();
        }
    }

    public int getAmount(Aspect key) {
        return this.aspects.get(key) == null ? 0 : this.aspects.get(key);
    }

    public boolean reduce(Aspect key, int amount) {
        if (this.getAmount(key) >= amount) {
            this.aspects.put(key, this.getAmount(key) - amount);
            return true;
        }
        return false;
    }

    public AspectList remove(Aspect key, int amount) {
        int am = this.getAmount(key) - amount;
        if (am <= 0) {
            this.aspects.remove(key);
        } else {
            this.aspects.put(key, am);
        }
        return this;
    }

    public AspectList remove(Aspect key) {
        this.aspects.remove(key);
        return this;
    }

    public AspectList add(Aspect aspect, int amount) {
        if (this.aspects.containsKey(aspect)) {
            amount += this.aspects.get(aspect);
        }
        this.aspects.put(aspect, amount);
        return this;
    }

    public AspectList merge(Aspect aspect, int amount) {
        if (this.aspects.containsKey(aspect)) {
            int oldamount = this.aspects.get(aspect);
            if (amount < oldamount) {
                amount = oldamount;
            }
        }
        this.aspects.put(aspect, amount);
        return this;
    }

    public AspectList add(AspectList in) {
        for (Aspect a : in.getAspects()) {
            this.add(a, in.getAmount(a));
        }
        return this;
    }

    public AspectList remove(AspectList in) {
        for (Aspect a : in.getAspects()) {
            this.remove(a, in.getAmount(a));
        }
        return this;
    }

    public AspectList merge(AspectList in) {
        for (Aspect a : in.getAspects()) {
            this.merge(a, in.getAmount(a));
        }
        return this;
    }

    public void readFromNBT(CompoundTag tag) {
        this.readFromNBT(tag, "Aspects");
    }

    public void readFromNBT(CompoundTag tag, String label) {
        this.aspects.clear();
        ListTag tlist = tag.getList(label, Tag.TAG_COMPOUND);
        for (int j = 0; j < tlist.size(); j++) {
            CompoundTag rs = tlist.getCompound(j);
            if (rs.contains("key")) {
                this.add(Aspect.getAspect(rs.getString("key")), rs.getInt("amount"));
            }
        }
    }

    public void writeToNBT(CompoundTag tag) {
        this.writeToNBT(tag, "Aspects");
    }

    public void writeToNBT(CompoundTag tag, String label) {
        ListTag tlist = new ListTag();
        tag.put(label, tlist);
        for (Aspect aspect : this.getAspects()) {
            if (aspect != null) {
                CompoundTag f = new CompoundTag();
                f.putString("key", aspect.getTag());
                f.putInt("amount", this.getAmount(aspect));
                tlist.add(f);
            }
        }
    }

    // Нужны для Data Components (сравнение стеков)
    @Override
    public boolean equals(Object o) {
        return o instanceof AspectList other && Objects.equals(this.aspects, other.aspects);
    }

    @Override
    public int hashCode() {
        return this.aspects.hashCode();
    }
}
