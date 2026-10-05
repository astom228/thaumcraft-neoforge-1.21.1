package thaumcraft.api.capabilities;

import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import thaumcraft.api.research.ResearchCategory;

/**
 * Знания игрока: исследования (стадии, флаги) и очки знаний (теория/наблюдение по категориям).
 * Порт IPlayerKnowledge оригинала.
 */
public interface IPlayerKnowledge extends INBTSerializable<CompoundTag> {
    void clear();

    EnumResearchStatus getResearchStatus(@Nonnull String res);

    boolean isResearchComplete(String res);

    boolean isResearchKnown(String res);

    int getResearchStage(@Nonnull String res);

    boolean addResearch(@Nonnull String res);

    boolean setResearchStage(@Nonnull String res, int stage);

    boolean removeResearch(@Nonnull String res);

    @Nonnull
    Set<String> getResearchList();

    boolean setResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag);

    boolean clearResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag);

    boolean hasResearchFlag(@Nonnull String res, @Nonnull EnumResearchFlag flag);

    boolean addKnowledge(@Nonnull EnumKnowledgeType type, ResearchCategory category, int amount);

    int getKnowledge(@Nonnull EnumKnowledgeType type, ResearchCategory category);

    int getKnowledgeRaw(@Nonnull EnumKnowledgeType type, ResearchCategory category);

    void sync(ServerPlayer player);

    enum EnumKnowledgeType {
        THEORY(32, true, "T"),
        OBSERVATION(16, true, "O");

        private final short progression;
        private final boolean hasFields;
        private final String abbr;

        EnumKnowledgeType(int progression, boolean hasFields, String abbr) {
            this.progression = (short) progression;
            this.hasFields = hasFields;
            this.abbr = abbr;
        }

        public int getProgression() {
            return this.progression;
        }

        public boolean hasFields() {
            return this.hasFields;
        }

        public String getAbbreviation() {
            return this.abbr;
        }
    }

    enum EnumResearchFlag {
        PAGE,
        RESEARCH,
        POPUP
    }

    enum EnumResearchStatus {
        UNKNOWN,
        COMPLETE,
        IN_PROGRESS
    }
}
