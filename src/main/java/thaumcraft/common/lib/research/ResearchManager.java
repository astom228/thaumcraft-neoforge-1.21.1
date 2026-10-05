package thaumcraft.common.lib.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.ResearchAddendum;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Исследования: загрузка JSON, продвижение по стадиям, очки знаний. Порт ResearchManager оригинала.
 * Изменение знаний — только на сервере; syncList — игроки, чьи знания отправляются клиенту на ближайшем тике (раз в 20 тиков).
 */
public class ResearchManager {
    /** Игроки (UUID; в оригинале — имя), ожидающие синхронизации знаний. Только сервер. */
    public static final Set<UUID> syncList = ConcurrentHashMap.newKeySet();
    /** Флаг из PacketSyncProgressToServer: не ставить флаг RESEARCH при следующем продвижении. Только серверный поток. */
    public static boolean noFlags = false;
    /** Хеши предметов из required_craft всех исследований. */
    public static final Set<Integer> craftingReferences = ConcurrentHashMap.newKeySet();

    public static boolean addKnowledge(Player player, IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category, int amount) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (!type.hasFields()) {
            category = null;
        }
        int kp = knowledge.getKnowledge(type, category);
        knowledge.addKnowledge(type, category, amount);
        int kr = knowledge.getKnowledge(type, category) - kp;
        if (amount > 0) {
            for (int a = 0; a < kr; a++) {
                // TODO 7.2: PacketKnowledgeGain((byte) type.ordinal(), category == null ? null : category.key) игроку
            }
        }
        syncList.add(player.getUUID());
        return true;
    }

    public static boolean completeResearch(Player player, String researchkey, boolean sync) {
        boolean b = false;
        while (progressResearch(player, researchkey, sync)) {
            b = true;
        }
        return b;
    }

    public static boolean completeResearch(Player player, String researchkey) {
        return completeResearch(player, researchkey, true);
    }

    public static boolean startResearchWithPopup(Player player, String researchkey) {
        boolean b = progressResearch(player, researchkey, true);
        if (b) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            knowledge.setResearchFlag(researchkey, IPlayerKnowledge.EnumResearchFlag.POPUP);
            knowledge.setResearchFlag(researchkey, IPlayerKnowledge.EnumResearchFlag.RESEARCH);
        }
        return b;
    }

    public static boolean progressResearch(Player player, String researchkey) {
        return progressResearch(player, researchkey, true);
    }

    public static boolean progressResearch(Player player, String researchkey, boolean sync) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge.isResearchComplete(researchkey) || !doesPlayerHaveRequisites(player, researchkey)) {
            return false;
        }
        if (!knowledge.isResearchKnown(researchkey)) {
            knowledge.addResearch(researchkey);
        }
        ResearchEntry re = ResearchCategories.getResearch(researchkey);
        if (re != null) {
            boolean popups = true;
            if (re.getStages() != null) {
                int cs = knowledge.getResearchStage(researchkey);
                ResearchStage currentStage = null;
                if (cs > 0) {
                    currentStage = re.getStages()[cs - 1];
                }
                if (re.getStages().length == 1 && cs == 0 && isFree(re.getStages()[0])) {
                    cs++;
                } else if (re.getStages().length > 1 && re.getStages().length <= cs + 1 && cs < re.getStages().length && isFree(re.getStages()[cs])) {
                    cs++;
                }
                knowledge.setResearchStage(researchkey, Math.min(re.getStages().length + 1, cs + 1));
                popups = cs >= re.getStages().length;
                if (currentStage != null) {
                    int warp = currentStage.getWarp();
                    if (warp > 0 && !ModConfig.CONFIG_MISC.wussMode.get() && !player.level().isClientSide) {
                        if (warp > 1) {
                            int w2 = warp / 2;
                            if (warp - w2 > 0) {
                                ThaumcraftApi.internalMethods.addWarpToPlayer(player, warp - w2, IPlayerWarp.EnumWarpType.PERMANENT);
                            }
                            if (w2 > 0) {
                                ThaumcraftApi.internalMethods.addWarpToPlayer(player, w2, IPlayerWarp.EnumWarpType.NORMAL);
                            }
                        } else {
                            ThaumcraftApi.internalMethods.addWarpToPlayer(player, warp, IPlayerWarp.EnumWarpType.PERMANENT);
                        }
                    }
                }
            }
            if (sync && popups) {
                knowledge.setResearchFlag(researchkey, IPlayerKnowledge.EnumResearchFlag.POPUP);
                if (!noFlags) {
                    knowledge.setResearchFlag(researchkey, IPlayerKnowledge.EnumResearchFlag.RESEARCH);
                } else {
                    noFlags = false;
                }
                if (re.getRewardItem() != null) {
                    for (ItemStack rs : re.getRewardItem()) {
                        if (!player.getInventory().add(rs.copy())) {
                            player.spawnAtLocation(rs.copy(), 1.0F);
                        }
                    }
                }
                if (re.getRewardKnow() != null) {
                    for (ResearchStage.Knowledge rk : re.getRewardKnow()) {
                        addKnowledge(player, rk.type, rk.category, rk.type.getProgression() * rk.amount);
                    }
                }
            }
        }
        if (re != null && re.getSiblings() != null) {
            for (String sibling : re.getSiblings()) {
                if (!knowledge.isResearchComplete(sibling) && doesPlayerHaveRequisites(player, sibling)) {
                    completeResearch(player, sibling, sync);
                }
            }
        }
        for (ResearchCategory rc : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : rc.research.values()) {
                if (ri != null && ri.getAddenda() != null && knowledge.isResearchComplete(ri.getKey())) {
                    for (ResearchAddendum addendum : ri.getAddenda()) {
                        if (addendum.getResearch() != null && Arrays.asList(addendum.getResearch()).contains(researchkey)) {
                            player.sendSystemMessage(Component.translatable("tc.addaddendum", Component.translatable(ri.getName())));
                            knowledge.setResearchFlag(ri.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE);
                            break;
                        }
                    }
                }
            }
        }
        if (sync) {
            syncList.add(player.getUUID());
            if (re != null) {
                player.giveExperiencePoints(5);
            }
        }
        return true;
    }

    /** Стадия без требований (ни крафта, ни предметов, ни знаний, ни исследований). */
    private static boolean isFree(ResearchStage stage) {
        return stage.getCraft() == null && stage.getObtain() == null && stage.getKnow() == null && stage.getResearch() == null;
    }

    public static boolean doesPlayerHaveRequisites(Player player, String key) {
        ResearchEntry ri = ResearchCategories.getResearch(key);
        if (ri == null) {
            return true;
        }
        String[] parents = ri.getParentsStripped();
        return parents == null || ThaumcraftCapabilities.knowsResearchStrict(player, parents);
    }

    public static Aspect getCombinationResult(Aspect aspect1, Aspect aspect2) {
        for (Aspect aspect : Aspect.aspects.values()) {
            if (aspect.getComponents() != null
                    && (aspect.getComponents()[0] == aspect1 && aspect.getComponents()[1] == aspect2
                    || aspect.getComponents()[0] == aspect2 && aspect.getComponents()[1] == aspect1)) {
                return aspect;
            }
        }
        return null;
    }

    // --- загрузка JSON -------------------------------------------------------------------------------------------------

    /**
     * Загрузка всех зарегистрированных файлов исследований (postInit оригинала). Файлы читаются из ресурсов модов
     * (assets/&lt;namespace&gt;/&lt;path&gt;.json), одинаково на клиенте и сервере, поэтому дерево исследований не синхронизируется.
     * Записи собираются в новые таблицы и подменяются целиком — читатели не видят наполовину загруженное дерево.
     */
    public static synchronized void parseAllResearch() {
        Map<ResearchCategory, Map<String, ResearchEntry>> loaded = new LinkedHashMap<>();
        for (ResearchCategory rc : ResearchCategories.researchCategories.values()) {
            loaded.put(rc, new LinkedHashMap<>());
            rc.minDisplayColumn = rc.minDisplayRow = rc.maxDisplayColumn = rc.maxDisplayRow = 0;
        }
        craftingReferences.clear();
        List<ResourceLocation> locs;
        synchronized (CommonInternals.jsonLocs) {
            locs = new ArrayList<>(CommonInternals.jsonLocs.values());
        }
        for (ResourceLocation loc : locs) {
            String s = "assets/" + loc.getNamespace() + "/" + loc.getPath();
            if (!s.endsWith(".json")) {
                s = s + ".json";
            }
            InputStream stream = openModResource(loc.getNamespace(), s);
            if (stream == null) {
                Thaumcraft.log.warn("Research file not found: {}", loc);
                continue;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray entries = obj.get("entries").getAsJsonArray();
                int a = 0;
                for (JsonElement element : entries) {
                    a++;
                    try {
                        ResearchEntry researchEntry = parseResearchJson(element.getAsJsonObject());
                        addResearchToCategory(loaded, researchEntry);
                    } catch (Exception e) {
                        Thaumcraft.log.warn("Invalid research entry [{}] found in {}", a, loc, e);
                        a--;
                    }
                }
                Thaumcraft.log.info("Loaded {} research entries from {}", a, loc);
            } catch (Exception e) {
                Thaumcraft.log.warn("Invalid research file: {}", loc, e);
            }
        }
        for (Map.Entry<ResearchCategory, Map<String, ResearchEntry>> e : loaded.entrySet()) {
            e.getKey().research = new ConcurrentHashMap<>(e.getValue());
        }
    }

    /** Файл из ресурсов мода с modid = namespace, иначе из любого мода; null, если не найден. */
    private static InputStream openModResource(String namespace, String path) {
        try {
            IModFileInfo own = ModList.get().getModFileById(namespace);
            if (own != null) {
                Path p = own.getFile().findResource(path.split("/"));
                if (Files.exists(p)) {
                    return Files.newInputStream(p);
                }
            }
            for (IModFileInfo info : ModList.get().getModFiles()) {
                Path p = info.getFile().findResource(path.split("/"));
                if (Files.exists(p)) {
                    return Files.newInputStream(p);
                }
            }
        } catch (Exception e) {
            Thaumcraft.log.warn("Failed to open research file {}", path, e);
        }
        return null;
    }

    private static ResearchEntry parseResearchJson(JsonObject obj) throws Exception {
        ResearchEntry entry = new ResearchEntry();
        entry.setKey(obj.getAsJsonPrimitive("key").getAsString());
        if (entry.getKey() == null) {
            throw new Exception("Invalid key in research JSon");
        }
        entry.setName(obj.getAsJsonPrimitive("name").getAsString());
        entry.setCategory(obj.getAsJsonPrimitive("category").getAsString());
        if (entry.getCategory() == null) {
            throw new Exception("Invalid category in research JSon");
        }
        if (obj.has("icons")) {
            String[] icons = arrayJsonToString(obj.get("icons").getAsJsonArray());
            if (icons != null && icons.length > 0) {
                Object[] ir = new Object[icons.length];
                for (int a = 0; a < icons.length; a++) {
                    ItemStack stack = parseJSONtoItemStack(icons[a]);
                    if (stack != null && !stack.isEmpty()) {
                        ir[a] = stack;
                    } else if (icons[a].startsWith("focus")) {
                        ir[a] = icons[a];
                    } else {
                        ir[a] = legacyLocation(icons[a]);
                    }
                }
                entry.setIcons(ir);
            }
        }
        if (obj.has("parents")) {
            entry.setParents(arrayJsonToString(obj.get("parents").getAsJsonArray()));
        }
        if (obj.has("siblings")) {
            entry.setSiblings(arrayJsonToString(obj.get("siblings").getAsJsonArray()));
        }
        if (obj.has("meta")) {
            String[] meta = arrayJsonToString(obj.get("meta").getAsJsonArray());
            if (meta != null && meta.length > 0) {
                List<ResearchEntry.EnumResearchMeta> metas = new ArrayList<>();
                for (String s : meta) {
                    metas.add(ResearchEntry.EnumResearchMeta.valueOf(s.toUpperCase(Locale.ROOT)));
                }
                entry.setMeta(metas.toArray(new ResearchEntry.EnumResearchMeta[0]));
            }
        }
        if (obj.has("location")) {
            Integer[] location = arrayJsonToInt(obj.get("location").getAsJsonArray());
            if (location != null && location.length == 2) {
                entry.setDisplayColumn(location[0]);
                entry.setDisplayRow(location[1]);
            }
        }
        if (obj.has("reward_item")) {
            entry.setRewardItem(parseJsonItemList(arrayJsonToString(obj.get("reward_item").getAsJsonArray())));
        }
        if (obj.has("reward_knowledge")) {
            entry.setRewardKnow(parseKnowledgeList(arrayJsonToString(obj.get("reward_knowledge").getAsJsonArray())));
        }
        JsonArray stagesJson = obj.get("stages").getAsJsonArray();
        List<ResearchStage> stages = new ArrayList<>();
        for (JsonElement element : stagesJson) {
            JsonObject stageObj = element.getAsJsonObject();
            ResearchStage stage = new ResearchStage();
            stage.setText(stageObj.getAsJsonPrimitive("text").getAsString());
            if (stage.getText() == null) {
                throw new Exception("Illegal stage text in research JSon");
            }
            if (stageObj.has("recipes")) {
                stage.setRecipes(arrayJsonToResourceLocations(stageObj.get("recipes").getAsJsonArray()));
            }
            if (stageObj.has("required_item")) {
                stage.setObtain(parseJsonItemList(arrayJsonToString(stageObj.get("required_item").getAsJsonArray())));
            }
            if (stageObj.has("required_craft")) {
                stage.setCraft(parseJsonItemList(arrayJsonToString(stageObj.get("required_craft").getAsJsonArray())));
                if (stage.getCraft() != null && stage.getCraft().length > 0) {
                    int[] refs = new int[stage.getCraft().length];
                    int q = 0;
                    for (ItemStack stack : stage.getCraft()) {
                        int code = createItemStackHash(stack);
                        craftingReferences.add(code);
                        refs[q] = code;
                        q++;
                    }
                    stage.setCraftReference(refs);
                }
            }
            if (stageObj.has("required_knowledge")) {
                stage.setKnow(parseKnowledgeList(arrayJsonToString(stageObj.get("required_knowledge").getAsJsonArray())));
            }
            if (stageObj.has("required_research")) {
                stage.setResearch(arrayJsonToString(stageObj.get("required_research").getAsJsonArray()));
            }
            if (stageObj.has("warp")) {
                stage.setWarp(stageObj.getAsJsonPrimitive("warp").getAsInt());
            }
            stages.add(stage);
        }
        if (!stages.isEmpty()) {
            entry.setStages(stages.toArray(new ResearchStage[0]));
        }
        if (obj.get("addenda") != null) {
            JsonArray addendaJson = obj.get("addenda").getAsJsonArray();
            List<ResearchAddendum> addenda = new ArrayList<>();
            for (JsonElement element : addendaJson) {
                JsonObject addendumObj = element.getAsJsonObject();
                ResearchAddendum addendum = new ResearchAddendum();
                addendum.setText(addendumObj.getAsJsonPrimitive("text").getAsString());
                if (addendum.getText() == null) {
                    throw new Exception("Illegal addendum text in research JSon");
                }
                if (addendumObj.has("recipes")) {
                    addendum.setRecipes(arrayJsonToResourceLocations(addendumObj.get("recipes").getAsJsonArray()));
                }
                if (addendumObj.has("required_research")) {
                    addendum.setResearch(arrayJsonToString(addendumObj.get("required_research").getAsJsonArray()));
                }
                addenda.add(addendum);
            }
            if (!addenda.isEmpty()) {
                entry.setAddenda(addenda.toArray(new ResearchAddendum[0]));
            }
        }
        return entry;
    }

    private static ResearchStage.Knowledge[] parseKnowledgeList(String[] sl) {
        if (sl == null || sl.length == 0) {
            return null;
        }
        List<ResearchStage.Knowledge> kl = new ArrayList<>();
        for (String s : sl) {
            ResearchStage.Knowledge k = ResearchStage.Knowledge.parse(s);
            if (k != null) {
                kl.add(k);
            }
        }
        return kl.isEmpty() ? null : kl.toArray(new ResearchStage.Knowledge[0]);
    }

    /**
     * Хеш предмета для требований «создать предмет» (ключ "[#]хеш" в знаниях игрока). В оригинале — hashCode строки NBT стека
     * с количеством 1; в порте — ID предмета + компоненты, отличающиеся от стандартных. Количество стека сбрасывается в 1, как в оригинале.
     */
    public static int createItemStackHash(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        stack.setCount(1);
        String s = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!stack.getComponentsPatch().isEmpty()) {
            s = s + stack.getComponentsPatch();
        }
        return s.hashCode();
    }

    private static String[] arrayJsonToString(JsonArray jsonArray) {
        List<String> out = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            out.add(element.getAsString());
        }
        return out.isEmpty() ? null : out.toArray(new String[0]);
    }

    private static ResourceLocation[] arrayJsonToResourceLocations(JsonArray jsonArray) {
        List<ResourceLocation> out = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            out.add(legacyLocation(element.getAsString()));
        }
        return out.isEmpty() ? null : out.toArray(new ResourceLocation[0]);
    }

    /** ResourceLocation 1.12 приводил строку к нижнему регистру (ID рецептов в JSON записаны как "thaumcraft:IEBURROWINGFAKE"). */
    private static ResourceLocation legacyLocation(String s) {
        return ResourceLocation.parse(s.toLowerCase(Locale.ROOT));
    }

    private static Integer[] arrayJsonToInt(JsonArray jsonArray) {
        List<Integer> out = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            out.add(element.getAsInt());
        }
        return out.isEmpty() ? null : out.toArray(new Integer[0]);
    }

    private static ItemStack[] parseJsonItemList(String[] stacks) {
        if (stacks == null || stacks.length == 0) {
            return null;
        }
        List<ItemStack> out = new ArrayList<>();
        for (String s : stacks) {
            ItemStack stack = parseJSONtoItemStack(s.replace("'", "\""));
            if (stack != null && !stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out.isEmpty() ? null : out.toArray(new ItemStack[0]);
    }

    /**
     * Стек из строки "id[;количество][;{nbt}]" (формат оригинала; metadata в порте не используется — варианты стали отдельными ID).
     * NBT оригинала переводится в компоненты: "Aspects" → аспекты, "ench" → зачарования заполнителя, остальное — custom_data.
     */
    public static ItemStack parseJSONtoItemStack(String entry) {
        if (entry == null) {
            return null;
        }
        String[] split = entry.split(";");
        String name = split[0];
        int num = -1;
        String nbt = null;
        for (int a = 1; a < split.length; a++) {
            if (split[a].startsWith("{")) {
                nbt = split[a];
                break;
            }
            int q;
            try {
                q = Integer.parseInt(split[a]);
            } catch (NumberFormatException e) {
                continue;
            }
            if (q >= 0 && num < 0) {
                num = q;
            }
        }
        if (num < 0) {
            num = 1;
        }
        ItemStack stack = ItemStack.EMPTY;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(name);
            Item it = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (it != null) {
                stack = new ItemStack(it, num);
                if (nbt != null) {
                    applyLegacyTag(stack, TagParser.parseTag(nbt));
                }
            }
        } catch (Exception ignored) {
        }
        return stack;
    }

    private static void applyLegacyTag(ItemStack stack, CompoundTag tag) {
        if (tag.contains("Aspects", Tag.TAG_LIST)) {
            AspectList al = new AspectList();
            al.readFromNBT(tag);
            stack.set(TCDataComponents.ASPECTS.get(), al);
            tag.remove("Aspects");
        }
        if (tag.contains("ench", Tag.TAG_LIST) && stack.is(ItemsTC.enchantedPlaceholder.get())) {
            Map<ResourceKey<Enchantment>, Integer> ench = new LinkedHashMap<>();
            ListTag list = tag.getList("ench", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(e.getString("id"));
                if (id != null) {
                    ench.put(ResourceKey.create(Registries.ENCHANTMENT, id), (int) e.getShort("lvl"));
                }
            }
            stack.set(TCDataComponents.PLACEHOLDER_ENCHANTMENTS.get(), ench);
            tag.remove("ench");
        }
        if (!tag.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private static void addResearchToCategory(Map<ResearchCategory, Map<String, ResearchEntry>> loaded, ResearchEntry ri) {
        ResearchCategory rl = ResearchCategories.getResearchCategory(ri.getCategory());
        Map<String, ResearchEntry> research = rl == null ? null : loaded.get(rl);
        if (research != null && !research.containsKey(ri.getKey())) {
            for (ResearchEntry rr : research.values()) {
                if (rr.getDisplayColumn() == ri.getDisplayColumn() && rr.getDisplayRow() == ri.getDisplayRow()) {
                    Thaumcraft.log.warn("Research [{}] not added as it overlaps with existing research [{}] at {},{}",
                            ri.getKey(), rr.getKey(), ri.getDisplayColumn(), rr.getDisplayRow());
                    return;
                }
            }
            research.put(ri.getKey(), ri);
            if (ri.getDisplayColumn() < rl.minDisplayColumn) {
                rl.minDisplayColumn = ri.getDisplayColumn();
            }
            if (ri.getDisplayRow() < rl.minDisplayRow) {
                rl.minDisplayRow = ri.getDisplayRow();
            }
            if (ri.getDisplayColumn() > rl.maxDisplayColumn) {
                rl.maxDisplayColumn = ri.getDisplayColumn();
            }
            if (ri.getDisplayRow() > rl.maxDisplayRow) {
                rl.maxDisplayRow = ri.getDisplayRow();
            }
        } else {
            Thaumcraft.log.warn("Could not add invalid research entry {}", ri.getKey());
        }
    }
}
