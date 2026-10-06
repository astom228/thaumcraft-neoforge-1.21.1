package thaumcraft.api.research.theorycraft;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;

/**
 * Теория на столе исследований (порт ResearchTableData): вдохновение, набранные очки по категориям, заблокированные категории,
 * карточки помощников, текущий выбор из трёх карточек и последняя разыгранная.
 */
public class ResearchTableData {
    public BlockEntity table;
    public String player;
    public int inspiration;
    public int inspirationStart;
    public int bonusDraws;
    public int placedCards;
    public int aidsChosen;
    public int penaltyStart;
    public ArrayList<Long> savedCards = new ArrayList<>();
    public ArrayList<String> aidCards = new ArrayList<>();
    public TreeMap<String, Integer> categoryTotals = new TreeMap<>();
    public ArrayList<String> categoriesBlocked = new ArrayList<>();
    public ArrayList<CardChoice> cardChoices = new ArrayList<>();
    public CardChoice lastDraw;

    public ResearchTableData(BlockEntity tileResearchTable) {
        this.table = tileResearchTable;
    }

    public ResearchTableData(Player player2, BlockEntity tileResearchTable) {
        this.player = player2.getName().getString();
        this.table = tileResearchTable;
    }

    public boolean isComplete() {
        return this.inspiration <= 0;
    }

    public boolean hasTotal(String cat) {
        return this.categoryTotals.containsKey(cat);
    }

    public int getTotal(String cat) {
        return this.categoryTotals.getOrDefault(cat, 0);
    }

    public void addTotal(String cat, int amt) {
        int current = this.categoryTotals.getOrDefault(cat, 0);
        current += amt;
        if (current <= 0) {
            this.categoryTotals.remove(cat);
        } else {
            this.categoryTotals.put(cat, current);
        }
    }

    public void addInspiration(int amt) {
        this.inspiration += amt;
        if (this.inspiration > this.inspirationStart) {
            this.inspiration = this.inspirationStart;
        }
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("player", this.player == null ? "" : this.player);
        nbt.putInt("inspiration", this.inspiration);
        nbt.putInt("inspirationStart", this.inspirationStart);
        nbt.putInt("placedCards", this.placedCards);
        nbt.putInt("bonusDraws", this.bonusDraws);
        nbt.putInt("aidsChosen", this.aidsChosen);
        nbt.putInt("penaltyStart", this.penaltyStart);
        ListTag savedTag = new ListTag();
        for (Long card : this.savedCards) {
            CompoundTag gt = new CompoundTag();
            gt.putLong("card", card);
            savedTag.add(gt);
        }
        nbt.put("savedCards", savedTag);
        ListTag categoriesBlockedTag = new ListTag();
        for (String category : this.categoriesBlocked) {
            CompoundTag gt = new CompoundTag();
            gt.putString("category", category);
            categoriesBlockedTag.add(gt);
        }
        nbt.put("categoriesBlocked", categoriesBlockedTag);
        ListTag categoryTotalsTag = new ListTag();
        for (String category : this.categoryTotals.keySet()) {
            CompoundTag gt = new CompoundTag();
            gt.putString("category", category);
            gt.putInt("total", this.categoryTotals.get(category));
            categoryTotalsTag.add(gt);
        }
        nbt.put("categoryTotals", categoryTotalsTag);
        ListTag aidCardsTag = new ListTag();
        for (String mc : this.aidCards) {
            CompoundTag gt = new CompoundTag();
            gt.putString("aidCard", mc);
            aidCardsTag.add(gt);
        }
        nbt.put("aidCards", aidCardsTag);
        ListTag cardChoicesTag = new ListTag();
        for (CardChoice mc : this.cardChoices) {
            cardChoicesTag.add(this.serializeCardChoice(mc, registries));
        }
        nbt.put("cardChoices", cardChoicesTag);
        if (this.lastDraw != null) {
            nbt.put("lastDraw", this.serializeCardChoice(this.lastDraw, registries));
        }
        return nbt;
    }

    public CompoundTag serializeCardChoice(CardChoice mc, HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("cardChoice", mc.key);
        nbt.putBoolean("aid", mc.fromAid);
        nbt.putBoolean("select", mc.selected);
        try {
            nbt.put("cardNBT", mc.card.serialize(registries));
        } catch (Exception ignored) {
        }
        return nbt;
    }

    public void deserialize(CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt == null) {
            return;
        }
        this.inspiration = nbt.getInt("inspiration");
        this.inspirationStart = nbt.getInt("inspirationStart");
        this.placedCards = nbt.getInt("placedCards");
        this.bonusDraws = nbt.getInt("bonusDraws");
        this.aidsChosen = nbt.getInt("aidsChosen");
        this.penaltyStart = nbt.getInt("penaltyStart");
        this.player = nbt.getString("player");
        ListTag savedTag = nbt.getList("savedCards", Tag.TAG_COMPOUND);
        this.savedCards = new ArrayList<>();
        for (int x = 0; x < savedTag.size(); x++) {
            this.savedCards.add(savedTag.getCompound(x).getLong("card"));
        }
        ListTag categoriesBlockedTag = nbt.getList("categoriesBlocked", Tag.TAG_COMPOUND);
        this.categoriesBlocked = new ArrayList<>();
        for (int x = 0; x < categoriesBlockedTag.size(); x++) {
            this.categoriesBlocked.add(categoriesBlockedTag.getCompound(x).getString("category"));
        }
        ListTag categoryTotalsTag = nbt.getList("categoryTotals", Tag.TAG_COMPOUND);
        this.categoryTotals = new TreeMap<>();
        for (int x = 0; x < categoryTotalsTag.size(); x++) {
            CompoundTag nbtdata = categoryTotalsTag.getCompound(x);
            this.categoryTotals.put(nbtdata.getString("category"), nbtdata.getInt("total"));
        }
        ListTag aidCardsTag = nbt.getList("aidCards", Tag.TAG_COMPOUND);
        this.aidCards = new ArrayList<>();
        for (int x = 0; x < aidCardsTag.size(); x++) {
            this.aidCards.add(aidCardsTag.getCompound(x).getString("aidCard"));
        }
        ListTag cardChoicesTag = nbt.getList("cardChoices", Tag.TAG_COMPOUND);
        this.cardChoices = new ArrayList<>();
        for (int x = 0; x < cardChoicesTag.size(); x++) {
            CardChoice cc = this.deserializeCardChoice(cardChoicesTag.getCompound(x), registries);
            if (cc != null) {
                this.cardChoices.add(cc);
            }
        }
        this.lastDraw = nbt.contains("lastDraw") ? this.deserializeCardChoice(nbt.getCompound("lastDraw"), registries) : null;
    }

    public CardChoice deserializeCardChoice(CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt == null) {
            return null;
        }
        String key = nbt.getString("cardChoice");
        TheorycraftCard tc = this.generateCardWithNBT(key, nbt.getCompound("cardNBT"), registries);
        return tc == null ? null : new CardChoice(key, tc, nbt.getBoolean("aid"), nbt.getBoolean("select"));
    }

    private boolean isCategoryBlocked(String cat) {
        return this.categoriesBlocked.contains(cat);
    }

    /** Вытянуть draw карточек (3 — с бонусным вытягиванием, если оно есть, иначе 2); с шансом 25% одна — из карточек помощников. */
    public void drawCards(int draw, Player pe) {
        if (draw == 3) {
            if (this.bonusDraws > 0) {
                this.bonusDraws--;
            } else {
                draw = 2;
            }
        }
        this.cardChoices.clear();
        this.player = pe.getName().getString();
        List<String> availCats = this.getAvailableCategories(pe);
        List<String> drawnCards = new ArrayList<>();
        boolean aidDrawn = false;
        int failsafe = 0;
        while (draw > 0 && failsafe < 10000) {
            failsafe++;
            if (!aidDrawn && !this.aidCards.isEmpty() && pe.getRandom().nextFloat() <= 0.25) {
                int idx = pe.getRandom().nextInt(this.aidCards.size());
                String key = this.aidCards.get(idx);
                TheorycraftCard card = this.generateCard(key, -1L, pe);
                if (card == null || card.getInspirationCost() > this.inspiration || this.isCategoryBlocked(card.getResearchCategory())
                        || drawnCards.contains(key)) {
                    continue;
                }
                drawnCards.add(key);
                this.cardChoices.add(new CardChoice(key, card, true, false));
                this.aidCards.remove(idx);
            } else {
                try {
                    String[] cards = TheorycraftManager.cards.keySet().toArray(new String[0]);
                    int idx = pe.getRandom().nextInt(cards.length);
                    TheorycraftCard card = this.generateCard(cards[idx], -1L, pe);
                    if (card == null || card.isAidOnly() || card.getInspirationCost() > this.inspiration) {
                        continue;
                    }
                    if (card.getResearchCategory() != null && !availCats.contains(card.getResearchCategory())) {
                        continue;
                    }
                    if (drawnCards.contains(cards[idx])) {
                        continue;
                    }
                    drawnCards.add(cards[idx]);
                    this.cardChoices.add(new CardChoice(cards[idx], card, false, false));
                } catch (Exception e) {
                    continue;
                }
            }
            draw--;
        }
    }

    private static TheorycraftCard newCard(String key) {
        Class<? extends TheorycraftCard> tcc = key == null ? null : TheorycraftManager.cards.get(key);
        if (tcc == null) {
            return null;
        }
        try {
            Constructor<? extends TheorycraftCard> c = tcc.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private TheorycraftCard generateCard(String key, long seed, Player pe) {
        TheorycraftCard tc = newCard(key);
        if (tc == null) {
            return null;
        }
        try {
            if (seed < 0L) {
                if (pe != null) {
                    tc.setSeed(pe.getRandom().nextLong());
                } else {
                    tc.setSeed(System.nanoTime());
                }
            } else {
                tc.setSeed(seed);
            }
            if (pe != null && !tc.initialize(pe, this)) {
                return null;
            }
        } catch (Exception ignored) {
        }
        return tc;
    }

    private TheorycraftCard generateCardWithNBT(String key, CompoundTag nbt, HolderLookup.Provider registries) {
        TheorycraftCard tc = newCard(key);
        if (tc == null) {
            return null;
        }
        try {
            tc.deserialize(nbt, registries);
        } catch (Exception ignored) {
        }
        return tc;
    }

    public void initialize(Player player1, Set<String> aids) {
        this.inspirationStart = getAvailableInspiration(player1);
        this.inspiration = this.inspirationStart - aids.size();
        for (String muk : aids) {
            ITheorycraftAid mu = TheorycraftManager.aids.get(muk);
            if (mu != null) {
                for (Class<? extends TheorycraftCard> clazz : mu.getCards()) {
                    this.aidCards.add(clazz.getName());
                }
            }
        }
    }

    public ArrayList<String> getAvailableCategories(Player player) {
        ArrayList<String> cats = new ArrayList<>();
        for (String rck : ResearchCategories.researchCategories.keySet()) {
            ResearchCategory rc = ResearchCategories.getResearchCategory(rck);
            if (rc != null && !this.isCategoryBlocked(rck) && (rc.researchKey == null || ThaumcraftCapabilities.knowsResearchStrict(player, rc.researchKey))) {
                cats.add(rck);
            }
        }
        return cats;
    }

    /** Вдохновение: 5 + 0,5 за каждое изученное «колючее» исследование + 0,1 за скрытое, не больше 15. */
    public static int getAvailableInspiration(Player player) {
        float tot = 5.0F;
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        for (String s : knowledge.getResearchList()) {
            if (ThaumcraftCapabilities.knowsResearchStrict(player, s)) {
                ResearchEntry re = ResearchCategories.getResearch(s);
                if (re != null) {
                    if (re.hasMeta(ResearchEntry.EnumResearchMeta.SPIKY)) {
                        tot += 0.5F;
                    }
                    if (re.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN)) {
                        tot += 0.1F;
                    }
                }
            }
        }
        return Math.min(15, Math.round(tot));
    }

    public class CardChoice {
        public TheorycraftCard card;
        public String key;
        public boolean fromAid;
        public boolean selected;

        public CardChoice(String key, TheorycraftCard card, boolean aid, boolean selected) {
            this.key = key;
            this.card = card;
            this.fromAid = aid;
            this.selected = selected;
        }

        @Override
        public String toString() {
            return "key:" + this.key + " card:" + this.card.getSeed() + " fromAid:" + this.fromAid + " selected:" + this.selected;
        }
    }
}
