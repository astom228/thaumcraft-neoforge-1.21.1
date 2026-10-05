package thaumcraft.common.lib.capabilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledgeDelta;

/**
 * Реализация знаний игрока (в оригинале — PlayerKnowledge.DefaultImpl). Формат NBT — как в оригинале:
 * "research" — список {key, stage, flags}, "knowledge" — список {key, amount} (ключ "T_BASICS", "O_ALCHEMY"…).
 */
public class PlayerKnowledge implements IPlayerKnowledge {
    private final HashSet<String> research = new HashSet<>();
    private final Map<String, Integer> stages = new HashMap<>();
    private final Map<String, HashSet<EnumResearchFlag>> flags = new HashMap<>();
    private final Map<String, Integer> knowledge = new HashMap<>();
    /** Раздел 7: изменения с последней синхронизации (на сервере). Полная синхронизация — после clear/загрузки и при входе. */
    private final Set<String> dirtyResearch = new HashSet<>();
    private final Set<String> dirtyKnowledge = new HashSet<>();
    private boolean fullSync = true;

    @Override
    public void clear() {
        this.research.clear();
        this.flags.clear();
        this.stages.clear();
        this.knowledge.clear();
        this.dirtyResearch.clear();
        this.dirtyKnowledge.clear();
        this.fullSync = true;
    }

    @Override
    public EnumResearchStatus getResearchStatus(@Nonnull String res) {
        if (!this.isResearchKnown(res)) {
            return EnumResearchStatus.UNKNOWN;
        }
        ResearchEntry entry = ResearchCategories.getResearch(res);
        return entry != null && entry.getStages() != null && this.getResearchStage(res) <= entry.getStages().length ? EnumResearchStatus.IN_PROGRESS : EnumResearchStatus.COMPLETE;
    }

    @Override
    public boolean isResearchKnown(String res) {
        if (res == null) {
            return false;
        }
        if (res.equals("")) {
            return true;
        }
        String[] ss = res.split("@");
        return ss.length > 1 && this.getResearchStage(ss[0]) < Mth.getInt(ss[1], 0) ? false : this.research.contains(ss[0]);
    }

    @Override
    public boolean isResearchComplete(String res) {
        return this.getResearchStatus(res) == EnumResearchStatus.COMPLETE;
    }

    @Override
    public int getResearchStage(String res) {
        if (res != null && this.research.contains(res)) {
            Integer stage = this.stages.get(res);
            return stage == null ? 0 : stage;
        }
        return -1;
    }

    @Override
    public boolean setResearchStage(String res, int stage) {
        if (res != null && this.research.contains(res) && stage > 0) {
            this.stages.put(res, stage);
            this.dirtyResearch.add(res);
            return true;
        }
        return false;
    }

    @Override
    public boolean addResearch(@Nonnull String res) {
        if (!this.isResearchKnown(res)) {
            this.research.add(res);
            this.dirtyResearch.add(res);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeResearch(@Nonnull String res) {
        if (this.isResearchKnown(res)) {
            this.research.remove(res);
            this.dirtyResearch.add(res);
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public Set<String> getResearchList() {
        return Collections.unmodifiableSet(this.research);
    }

    @Override
    public boolean setResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag) {
        HashSet<EnumResearchFlag> list = this.flags.computeIfAbsent(res, k -> new HashSet<>());
        boolean b = list.add(flag);
        if (b) {
            this.dirtyResearch.add(res);
        }
        return b;
    }

    @Override
    public boolean clearResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag) {
        HashSet<EnumResearchFlag> list = this.flags.get(res);
        if (list != null) {
            boolean b = list.remove(flag);
            if (b) {
                this.dirtyResearch.add(res);
            }
            // В оригинале здесь flags.remove(this.research) — удаление по неверному ключу, т.е. ничего не происходит
            return b;
        }
        return false;
    }

    @Override
    public boolean hasResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag) {
        return this.flags.get(res) != null && this.flags.get(res).contains(flag);
    }

    private String getKey(EnumKnowledgeType type, ResearchCategory category) {
        return type.getAbbreviation() + "_" + (category == null ? "" : category.key);
    }

    @Override
    public boolean addKnowledge(EnumKnowledgeType type, ResearchCategory category, int amount) {
        String key = this.getKey(type, category);
        int c = this.getKnowledgeRaw(type, category);
        if (c + amount < 0) {
            return false;
        }
        this.knowledge.put(key, c + amount);
        this.dirtyKnowledge.add(key);
        return true;
    }

    @Override
    public int getKnowledge(EnumKnowledgeType type, ResearchCategory category) {
        String key = this.getKey(type, category);
        int c = this.knowledge.getOrDefault(key, 0);
        return (int) Math.floor((double) c / type.getProgression());
    }

    @Override
    public int getKnowledgeRaw(EnumKnowledgeType type, ResearchCategory category) {
        return this.knowledge.getOrDefault(this.getKey(type, category), 0);
    }

    @Override
    public void sync(@Nonnull ServerPlayer player) {
        if (this.fullSync) {
            PacketDistributor.sendToPlayer(player, PacketSyncKnowledge.create(player, this));
        } else if (!this.dirtyResearch.isEmpty() || !this.dirtyKnowledge.isEmpty()) {
            PacketDistributor.sendToPlayer(player, PacketSyncKnowledgeDelta.create(this));
        }
        this.dirtyResearch.clear();
        this.dirtyKnowledge.clear();
        this.fullSync = false;
    }

    /** Следующая синхронизация — полная (вход в мир, возрождение, смена измерения: клиент мог создать нового игрока). */
    public void markFullSync() {
        this.fullSync = true;
    }

    // --- дельты для PacketSyncKnowledgeDelta ---------------------------------------------------------------------------

    public Set<String> getDirtyResearch() {
        return this.dirtyResearch;
    }

    public Set<String> getDirtyKnowledge() {
        return this.dirtyKnowledge;
    }

    /** Флаги исследования битовой маской (бит = ordinal). */
    public int getFlagBits(String res) {
        int bits = 0;
        HashSet<EnumResearchFlag> list = this.flags.get(res);
        if (list != null) {
            for (EnumResearchFlag flag : list) {
                bits |= 1 << flag.ordinal();
            }
        }
        return bits;
    }

    public int getKnowledgeRawByKey(String key) {
        return this.knowledge.getOrDefault(key, 0);
    }

    /** Клиент: применить изменение одного исследования (как запись в NBT полной синхронизации). */
    public void applyResearchDelta(String res, boolean present, int stage, int flagBits) {
        if (!present) {
            this.research.remove(res);
            this.stages.remove(res);
            this.flags.remove(res);
            return;
        }
        this.research.add(res);
        if (stage > 0) {
            this.stages.put(res, stage);
        } else {
            this.stages.remove(res);
        }
        HashSet<EnumResearchFlag> list = new HashSet<>();
        for (EnumResearchFlag flag : EnumResearchFlag.values()) {
            if ((flagBits & 1 << flag.ordinal()) != 0) {
                list.add(flag);
            }
        }
        if (list.isEmpty()) {
            this.flags.remove(res);
        } else {
            this.flags.put(res, list);
        }
    }

    /** Клиент: применить изменение очков знаний (ключ "T_BASICS" и т.п.). */
    public void applyKnowledgeDelta(String key, int amount) {
        if (amount > 0) {
            this.knowledge.put(key, amount);
        } else {
            this.knowledge.remove(key);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag rootTag = new CompoundTag();
        ListTag researchList = new ListTag();
        for (String resKey : this.research) {
            CompoundTag tag = new CompoundTag();
            tag.putString("key", resKey);
            if (this.stages.containsKey(resKey)) {
                tag.putInt("stage", this.stages.get(resKey));
            }
            HashSet<EnumResearchFlag> list = this.flags.get(resKey);
            if (list != null) {
                StringBuilder fs = new StringBuilder();
                for (EnumResearchFlag flag : list) {
                    if (fs.length() > 0) {
                        fs.append(",");
                    }
                    fs.append(flag.name());
                }
                tag.putString("flags", fs.toString());
            }
            researchList.add(tag);
        }
        rootTag.put("research", researchList);
        ListTag knowledgeList = new ListTag();
        for (Map.Entry<String, Integer> e : this.knowledge.entrySet()) {
            Integer c = e.getValue();
            if (c != null && c > 0 && e.getKey() != null && !e.getKey().isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putString("key", e.getKey());
                tag.putInt("amount", c);
                knowledgeList.add(tag);
            }
        }
        rootTag.put("knowledge", knowledgeList);
        return rootTag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag rootTag) {
        if (rootTag == null) {
            return;
        }
        this.clear();
        ListTag researchList = rootTag.getList("research", Tag.TAG_COMPOUND);
        for (int i = 0; i < researchList.size(); i++) {
            CompoundTag tag = researchList.getCompound(i);
            String know = tag.getString("key");
            if (!this.isResearchKnown(know)) {
                this.research.add(know);
                int stage = tag.getInt("stage");
                if (stage > 0) {
                    this.stages.put(know, stage);
                }
                String fs = tag.getString("flags");
                if (!fs.isEmpty()) {
                    for (String s : fs.split(",")) {
                        try {
                            this.setResearchFlag(know, EnumResearchFlag.valueOf(s));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }
        ListTag knowledgeList = rootTag.getList("knowledge", Tag.TAG_COMPOUND);
        for (int i = 0; i < knowledgeList.size(); i++) {
            CompoundTag tag = knowledgeList.getCompound(i);
            this.knowledge.put(tag.getString("key"), tag.getInt("amount"));
        }
        this.addAutoUnlockResearch();
    }

    private void addAutoUnlockResearch() {
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                if (ri.hasMeta(ResearchEntry.EnumResearchMeta.AUTOUNLOCK)) {
                    this.addResearch(ri.getKey());
                }
            }
        }
    }
}
