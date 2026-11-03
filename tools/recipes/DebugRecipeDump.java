package thaumcraft.common.lib.events;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.GsonBuilder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionRecipe;

/** Шаблон временного дампа для scripts/check-recipes.ps1; в основной код и итоговый jar не включается. */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class DebugRecipeDump {
    @SubscribeEvent
    public static void started(ServerStartedEvent event) throws java.io.IOException {
        if (!Files.exists(Path.of("tc-recipe-dump"))) return;
        Map<String, Object> result = new LinkedHashMap<>();
        ThaumcraftApi.getCraftingRecipes().forEach((id, recipe) -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("research", recipe.getResearch());
            if (recipe instanceof CrucibleRecipe crucible) {
                data.put("kind", "crucible");
                data.put("aspects", aspects(crucible.getAspects()));
                data.put("count", crucible.getRecipeOutput().getCount());
                data.put("resultItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crucible.getRecipeOutput().getItem()).toString());
                data.put("catalyst", ingredient(crucible.getCatalyst(), event.getServer().registryAccess()));
                var resultAspects = crucible.getRecipeOutput().get(thaumcraft.common.lib.TCDataComponents.ASPECTS.get());
                if (resultAspects != null) data.put("resultAspects", aspects(resultAspects));
            } else if (recipe instanceof thaumcraft.common.lib.crafting.InfusionEnchantmentRecipe enchantment) {
                data.put("kind", "enchantment");
                data.put("enchantment", enchantment.getEnchantment().name());
                data.put("aspects", aspects(enchantment.getAspects()));
                data.put("instability", enchantment.getInstability(null, ItemStack.EMPTY, java.util.List.of()));
                data.put("components", enchantment.getComponents().size());
                data.put("ingredients", enchantment.getComponents().stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
            } else if (recipe instanceof thaumcraft.common.lib.crafting.InfusionRunicAugmentRecipe runic) {
                data.put("kind", "runic");
                var samples = new java.util.ArrayList<Map<String, Object>>();
                for (int charge = 0; charge <= 6; charge++) {
                    var input = new ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE);
                    input.set(thaumcraft.common.lib.TCDataComponents.RUNIC_CHARGE.get(), charge);
                    Map<String, Object> sample = new LinkedHashMap<>();
                    sample.put("charge", charge);
                    sample.put("instability", runic.getInstability(null, input, java.util.List.of()));
                    sample.put("aspects", aspects(runic.getAspects(null, input, java.util.List.of())));
                    sample.put("components", runic.getComponents(input).size());
                    sample.put("ingredients", runic.getComponents(input).stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
                    var output = (ItemStack) runic.getRecipeOutput(null, input, java.util.List.of());
                    sample.put("outputCharge", output.get(thaumcraft.common.lib.TCDataComponents.RUNIC_CHARGE.get()));
                    samples.add(sample);
                }
                data.put("samples", samples);
            } else if (recipe.getClass() == InfusionRecipe.class) {
                var infusion = (InfusionRecipe) recipe;
                data.put("kind", "infusion");
                data.put("aspects", aspects(infusion.getAspects()));
                data.put("instability", infusion.getInstability(null, ItemStack.EMPTY, java.util.List.of()));
                data.put("components", infusion.getComponents().size());
                data.put("central", ingredient(infusion.getRecipeInput(), event.getServer().registryAccess()));
                data.put("ingredients", infusion.getComponents().stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
                if (infusion.getRecipeOutput() instanceof ItemStack stack) {
                    data.put("count", stack.getCount());
                    data.put("resultItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                } else if (infusion.getRecipeOutput() instanceof Object[] change
                        && change[0] instanceof net.minecraft.core.component.DataComponentType<?> type) {
                    data.put("resultMutation", Map.of(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString(), change[1]));
                }
            } else return;
            result.put(id.toString(), data);
        });
        for (var holder : event.getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof IArcaneRecipe arcane) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("kind", "arcane");
                data.put("research", arcane.getResearch());
                data.put("aspects", aspects(arcane.getCrystals()));
                data.put("vis", arcane.getVis());
                data.put("count", arcane.getResultItem(event.getServer().registryAccess()).getCount());
                data.put("resultItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(arcane.getResultItem(event.getServer().registryAccess()).getItem()).toString());
                data.put("ingredients", arcane.getIngredients().stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
                if (arcane instanceof thaumcraft.api.crafting.ShapedArcaneRecipe) {
                    data.put("ingredientMode", "shaped");
                    for (int width = 1; width <= 3; width++) if (arcane.canCraftInDimensions(width, 3)) { data.put("width", width); break; }
                    for (int height = 1; height <= 3; height++) if (arcane.canCraftInDimensions(3, height)) { data.put("height", height); break; }
                } else data.put("ingredientMode", "unordered");
                result.put(holder.id().toString(), data);
            } else if (holder.id().getNamespace().equals("thaumcraft") &&
                    (holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe
                     || holder.value() instanceof net.minecraft.world.item.crafting.ShapelessRecipe
                     || holder.value() instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe)) {
                var recipe = holder.value();
                Map<String, Object> data = new LinkedHashMap<>();
                var output = recipe.getResultItem(event.getServer().registryAccess());
                data.put("resultItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(output.getItem()).toString());
                data.put("count", output.getCount());
                var resultAspects = output.get(thaumcraft.common.lib.TCDataComponents.ASPECTS.get());
                if (resultAspects != null) data.put("resultAspects", aspects(resultAspects));
                if (recipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe cooking) {
                    data.put("kind", "smelting");
                    data.put("catalyst", ingredient(cooking.getIngredients().getFirst(), event.getServer().registryAccess()));
                    data.put("experience", cooking.getExperience());
                    data.put("cookingTime", cooking.getCookingTime());
                } else {
                    data.put("kind", "crafting");
                    data.put("ingredients", recipe.getIngredients().stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
                    if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
                        data.put("ingredientMode", "shaped");
                        data.put("width", shaped.getWidth());
                        data.put("height", shaped.getHeight());
                    } else data.put("ingredientMode", "unordered");
                }
                result.put(holder.id().toString(), data);
            }
        }
        var sample = thaumcraft.api.ThaumcraftApiHelper.makeCrystal(thaumcraft.api.aspects.Aspect.AIR);
        var componentIngredient = thaumcraft.api.ThaumcraftApiHelper.getIngredient(sample);
        var named = sample.copy();
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Recipe audit"));
        named.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(net.minecraft.network.chat.Component.literal("Extra lore"))));
        if (!componentIngredient.test(named) || componentIngredient.test(thaumcraft.api.ThaumcraftApiHelper.makeCrystal(thaumcraft.api.aspects.Aspect.FIRE))) {
            throw new IllegalStateException("[TC-RECIPE-DUMP] component ingredient regression");
        }
        var wrongAmount = sample.copy();
        wrongAmount.set(thaumcraft.common.lib.TCDataComponents.ASPECTS.get(), new AspectList().add(thaumcraft.api.aspects.Aspect.AIR, 2));
        if (componentIngredient.test(wrongAmount)) throw new IllegalStateException("[TC-RECIPE-DUMP] aspect amount regression");
        Thaumcraft.log.info("[TC-RECIPE-DUMP] PASS: named/lore crystal accepted, wrong aspect and amount rejected");
        var phial = (net.minecraft.world.item.crafting.ShapedRecipe) event.getServer().getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:phial")).orElseThrow().value();
        for (var glass : java.util.List.of(net.minecraft.world.item.Items.GLASS, net.minecraft.world.item.Items.WHITE_STAINED_GLASS,
                net.minecraft.world.item.Items.BLACK_STAINED_GLASS, net.minecraft.world.item.Items.TINTED_GLASS)) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(ItemStack.EMPTY,
                    new ItemStack(net.minecraft.world.item.Items.CLAY_BALL), ItemStack.EMPTY, new ItemStack(glass), ItemStack.EMPTY,
                    new ItemStack(glass), ItemStack.EMPTY, new ItemStack(glass), ItemStack.EMPTY));
            boolean wanted = glass != net.minecraft.world.item.Items.TINTED_GLASS;
            if (phial.matches(input, event.getServer().overworld()) != wanted) throw new IllegalStateException("[TC-RECIPE-DUMP] phial glass regression: " + glass);
        }
        Thaumcraft.log.info("[TC-RECIPE-DUMP] PASS: phials accept original glass and reject tinted glass");
        auditAspects(event.getServer());
        auditServerGuards(event.getServer());
        auditResearchPayments(event.getServer());
        auditLifecycle(event.getServer());
        Files.writeString(Path.of("../build/recipe-audit-actual.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
        Thaumcraft.log.info("[TC-RECIPE-DUMP] PASS: all functional audits");
        Thaumcraft.log.info("[TC-RECIPE-DUMP] {} recipes exported", result.size());
    }

    private static Map<String, Integer> aspects(AspectList list) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (list != null) for (var aspect : list.getAspects()) result.put(aspect.getTag(), list.getAmount(aspect));
        return result;
    }

    private static com.google.gson.JsonElement ingredient(net.minecraft.world.item.crafting.Ingredient value, net.minecraft.core.RegistryAccess registries) {
        return net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), value).getOrThrow();
    }

    private static void auditAspects(net.minecraft.server.MinecraftServer server) {
        var world = server.overworld();
        var guardian = net.minecraft.world.entity.EntityType.GUARDIAN.create(world);
        var elder = net.minecraft.world.entity.EntityType.ELDER_GUARDIAN.create(world);
        requireAspect(thaumcraft.api.aspects.AspectHelper.getEntityAspects(guardian), thaumcraft.api.aspects.Aspect.ELDRITCH, 10, "guardian");
        requireAspect(thaumcraft.api.aspects.AspectHelper.getEntityAspects(elder), thaumcraft.api.aspects.Aspect.ELDRITCH, 15, "elder guardian");
        var creeper = net.minecraft.world.entity.EntityType.CREEPER.create(world);
        var nbt = new net.minecraft.nbt.CompoundTag();
        creeper.saveWithoutId(nbt);
        nbt.putBoolean("powered", true);
        creeper.load(nbt);
        requireAspect(thaumcraft.api.aspects.AspectHelper.getEntityAspects(creeper), thaumcraft.api.aspects.Aspect.ENERGY, 15, "charged creeper");
        var pech = thaumcraft.common.config.ConfigEntities.PECH.get().create(world);
        var pechAspects = new thaumcraft.api.aspects.Aspect[] {thaumcraft.api.aspects.Aspect.DESIRE, thaumcraft.api.aspects.Aspect.AVERSION, thaumcraft.api.aspects.Aspect.MAGIC};
        for (int type = 0; type < 3; type++) {
            pech.setPechType(type);
            requireAspect(thaumcraft.api.aspects.AspectHelper.getEntityAspects(pech), pechAspects[type], 5, "pech " + type);
        }
        var wisp = thaumcraft.common.config.ConfigEntities.WISP.get().create(world);
        for (var aspect : thaumcraft.api.aspects.Aspect.aspects.values()) {
            wisp.setWispType(aspect.getTag());
            var expected = new AspectList().add(aspect, 5).add(thaumcraft.api.aspects.Aspect.AURA, 5).add(thaumcraft.api.aspects.Aspect.FLIGHT, 5);
            if (!thaumcraft.api.aspects.AspectHelper.getEntityAspects(wisp).aspects.equals(expected.aspects)) throw new IllegalStateException("Wisp aspects: " + aspect.getTag());
        }
        var types = new net.minecraft.world.item.Item[] {net.minecraft.world.item.Items.POTION, net.minecraft.world.item.Items.TIPPED_ARROW,
                net.minecraft.world.item.Items.SPLASH_POTION, net.minecraft.world.item.Items.LINGERING_POTION};
        var additions = new thaumcraft.api.aspects.Aspect[] {thaumcraft.api.aspects.Aspect.WATER, thaumcraft.api.aspects.Aspect.AVERSION,
                thaumcraft.api.aspects.Aspect.ENERGY, thaumcraft.api.aspects.Aspect.TRAP};
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < types.length; i++) {
                var potion = net.minecraft.world.item.alchemy.PotionContents.createItemStack(types[i], net.minecraft.world.item.alchemy.Potions.WATER);
                var expected = new AspectList().add(thaumcraft.api.aspects.Aspect.WATER, 5).add(additions[i], 5);
                if (!thaumcraft.common.lib.crafting.ThaumcraftCraftingManager.getObjectTags(potion).aspects.equals(expected.aspects)) throw new IllegalStateException("Water potion aspects: " + i);
            }
            for (var color : net.minecraft.world.item.DyeColor.values()) {
                var shield = new ItemStack(net.minecraft.world.item.Items.SHIELD);
                shield.set(net.minecraft.core.component.DataComponents.BASE_COLOR, color);
                var aspects = thaumcraft.common.lib.crafting.ThaumcraftCraftingManager.getObjectTags(shield);
                if (aspects == null || aspects.getAmount(thaumcraft.api.aspects.Aspect.PROTECT) < 20) throw new IllegalStateException("Shield aspects: " + color);
            }
            if (pass == 0) AspectRegistryEvents.build(net.neoforged.fml.LogicalSide.SERVER, server.getRecipeManager(), server.registryAccess());
        }
        if (thaumcraft.api.internal.CommonInternals.state(net.neoforged.fml.LogicalSide.SERVER).objectTags
                == thaumcraft.api.internal.CommonInternals.state(net.neoforged.fml.LogicalSide.CLIENT).objectTags) throw new IllegalStateException("Aspect side isolation");
        Thaumcraft.log.info("[TC-ASPECT-AUDIT] PASS: entity NBT, 37 wisp types, 4 water potion forms, 16 shield colors, rebuild and side isolation");
    }

    private static void auditServerGuards(net.minecraft.server.MinecraftServer server) {
        var world = server.overworld();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(world);
        class CountingLogistics extends thaumcraft.common.container.ContainerLogistics {
            int scans;
            CountingLogistics() { super(0, player.getInventory(), null, null); }
            @Override public void refreshItemList(boolean full) { if (full) scans++; }
        }
        var menu = new CountingLogistics();
        menu.scans = 0;
        menu.setSearchText("a");
        menu.setSearchText("latest");
        if (menu.scans != 0) throw new IllegalStateException("Search scans before menu tick");
        menu.broadcastChanges();
        if (menu.scans != 1 || !menu.searchText.equals("latest")) throw new IllegalStateException("Search coalescing");
        menu.setSearchText("latest");
        menu.setSearchText("pending");
        for (int i = 0; i < 100; i++) {
            menu.clickMenuButton(player, 22);
            menu.broadcastChanges();
        }
        if (menu.scans != 1) throw new IllegalStateException("Repeated refresh packets bypass throttle");
        var remote = new net.minecraft.core.BlockPos(29_000_000, 64, 29_000_000);
        if (world.isLoaded(remote)) throw new IllegalStateException("Mirror audit requires unloaded target");
        var mirror = new thaumcraft.common.tiles.devices.TileMirrorEssentia(net.minecraft.core.BlockPos.ZERO,
                thaumcraft.api.blocks.BlocksTC.mirrorEssentia.get().defaultBlockState());
        mirror.setLevel(world);
        mirror.linked = true;
        mirror.linkPos = remote;
        mirror.linkDim = world.dimension();
        if (mirror.doesContainerContainAmount(thaumcraft.api.aspects.Aspect.AIR, 1)) throw new IllegalStateException("Unloaded mirror reports available essentia");
        mirror.doesContainerAccept(thaumcraft.api.aspects.Aspect.AIR);
        if (world.isLoaded(remote) || !mirror.linked) throw new IllegalStateException("Mirror probe loads chunk or breaks pending link");
        Thaumcraft.log.info("[TC-SERVER-AUDIT] PASS: logistics search/refresh coalescing, mirror probes leave remote chunk unloaded and link intact");
    }

    private static void auditResearchPayments(net.minecraft.server.MinecraftServer server) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ResearchAudit"));
        var category = thaumcraft.api.research.ResearchCategories.getResearchCategory("BASICS");
        String key = "TC_AUDIT_PAYMENT";
        var entry = new thaumcraft.api.research.ResearchEntry();
        entry.setKey(key);
        entry.setCategory("BASICS");
        var stage = new thaumcraft.api.research.ResearchStage();
        stage.setObtain(new ItemStack[] {new ItemStack(net.minecraft.world.item.Items.DIAMOND, 2)});
        var second = new thaumcraft.api.research.ResearchStage();
        second.setObtain(new ItemStack[] {new ItemStack(net.minecraft.world.item.Items.APPLE)});
        entry.setStages(new thaumcraft.api.research.ResearchStage[] {stage, second});
        if (category.research.putIfAbsent(key, entry) != null) throw new IllegalStateException("Audit research key exists");
        try {
            var knowledge = thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player);
            knowledge.addResearch(key);
            knowledge.setResearchStage(key, 1);
            player.getInventory().items.set(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 4));
            var packet = new thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer(key, false);
            stage.setCraft(new ItemStack[] {new ItemStack(net.minecraft.world.item.Items.STICK)});
            stage.setCraftReference(new int[] {Integer.MAX_VALUE});
            thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer.handle(packet, player);
            requirePaymentUnchanged(player, key);
            stage.setCraft(null);
            stage.setResearch(new String[] {"TC_AUDIT_MISSING"});
            thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer.handle(packet, player);
            requirePaymentUnchanged(player, key);
            stage.setResearch(null);
            stage.setKnow(new thaumcraft.api.research.ResearchStage.Knowledge[] {
                    new thaumcraft.api.research.ResearchStage.Knowledge(thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category, 1)});
            thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer.handle(packet, player);
            requirePaymentUnchanged(player, key);
            stage.setKnow(null);
            entry.setParents(new String[] {"TC_AUDIT_MISSING"});
            thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer.handle(packet, player);
            requirePaymentUnchanged(player, key);
            entry.setParents(null);
            thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer.handle(packet, player);
            if (player.getInventory().items.get(0).getCount() != 2 || knowledge.getResearchStage(key) != 2) throw new IllegalStateException("Valid research payment/progress");
            thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer.handle(packet, player);
            if (player.getInventory().items.get(0).getCount() != 2 || knowledge.getResearchStage(key) != 2) throw new IllegalStateException("Repeated packet bypasses next-stage requirements");
            if (thaumcraft.common.lib.research.ResearchManager.noFlags) throw new IllegalStateException("Research flag leaked");
        } finally {
            category.research.remove(key);
            thaumcraft.common.lib.research.ResearchManager.syncList.remove(player.getUUID());
        }
        Thaumcraft.log.info("[TC-SERVER-AUDIT] PASS: failed craft/research/knowledge/parent checks preserve items; valid payment once; replay rejected");
    }

    private static void requirePaymentUnchanged(net.minecraft.server.level.ServerPlayer player, String key) {
        if (player.getInventory().items.get(0).getCount() != 4
                || thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player).getResearchStage(key) != 1) {
            throw new IllegalStateException("Rejected research request changed inventory or stage");
        }
    }

    private static void auditLifecycle(net.minecraft.server.MinecraftServer server) {
        try {
            var ticker = thaumcraft.common.world.aura.AuraTicker.class;
            var stateClass = Class.forName(ticker.getName() + "$State");
            var constructor = stateClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            var state = constructor.newInstance();
            var phaseMax = stateClass.getDeclaredField("phaseMax");
            var phaseVis = stateClass.getDeclaredField("phaseVis");
            phaseMax.setAccessible(true);
            phaseVis.setAccessible(true);
            phaseMax.setFloat(state, 1.0F);
            phaseVis.setFloat(state, 0.25F);
            var snapshot = new thaumcraft.common.world.aura.AuraChunk(new net.minecraft.world.level.ChunkPos(100000, 100000), (short) 500, 0, 0);
            var world = new thaumcraft.common.world.aura.AuraWorld();
            var process = ticker.getDeclaredMethod("processAuraChunk", net.minecraft.server.level.ServerLevel.class,
                    thaumcraft.common.world.aura.AuraWorld.class, thaumcraft.common.world.aura.AuraChunk.class, stateClass);
            process.setAccessible(true);
            process.invoke(null, server.overworld(), world, snapshot, state);
            if (snapshot.getVis() != 0 || snapshot.getFlux() != 0) throw new IllegalStateException("Unloaded aura snapshot was processed");
            var replacement = new thaumcraft.common.world.aura.AuraChunk(snapshot.getLoc(), (short) 500, 0, 0);
            world.getAuraChunks().put(snapshot.getLoc().toLong(), replacement);
            process.invoke(null, server.overworld(), world, snapshot, state);
            if (snapshot.getVis() != 0 || replacement.getVis() != 0) throw new IllegalStateException("Replaced aura snapshot was processed");
            var dimension = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse("thaumcraft:audit_lifecycle"));
            var statesField = ticker.getDeclaredField("STATES");
            statesField.setAccessible(true);
            @SuppressWarnings("unchecked") var states = (Map<Object, Object>) statesField.get(null);
            states.put(dimension, state);
            var remove = ticker.getDeclaredMethod("removeWorld", net.minecraft.resources.ResourceKey.class);
            remove.setAccessible(true);
            try {
                remove.invoke(null, dimension);
                if (states.containsKey(dimension)) throw new IllegalStateException("Aura cycle survived world unload");
            } finally { states.remove(dimension); }
            var client = thaumcraft.api.internal.CommonInternals.state(net.neoforged.fml.LogicalSide.CLIENT);
            var clientTags = new LinkedHashMap<>(client.objectTags);
            var clientCosts = client.derivedCrucibleCosts;
            var clientRecipes = client.recipes;
            var clientRegistries = client.registries;
            int serverTags = thaumcraft.api.internal.CommonInternals.state(net.neoforged.fml.LogicalSide.SERVER).objectTags.size();
            try {
                client.objectTags.put(thaumcraft.api.internal.CommonInternals.generateUniqueItemstackId(new ItemStack(net.minecraft.world.item.Items.DIAMOND)), new AspectList());
                client.recipes = server.getRecipeManager();
                client.registries = server.registryAccess();
                thaumcraft.api.internal.CommonInternals.clearSide(net.neoforged.fml.LogicalSide.CLIENT);
                if (!client.objectTags.isEmpty() || !client.derivedCrucibleCosts.isEmpty() || client.recipes != null || client.registries != null
                        || thaumcraft.api.internal.CommonInternals.state(net.neoforged.fml.LogicalSide.SERVER).objectTags.size() != serverTags) throw new IllegalStateException("Side cleanup isolation");
            } finally {
                client.objectTags.putAll(clientTags);
                client.derivedCrucibleCosts = clientCosts;
                client.recipes = clientRecipes;
                client.registries = clientRegistries;
            }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Lifecycle audit", e); }
        Thaumcraft.log.info("[TC-SERVER-AUDIT] PASS: unloaded/replaced aura snapshots skipped, dimension cycle removed, side cleanup isolated");
    }

    private static void requireAspect(AspectList list, thaumcraft.api.aspects.Aspect aspect, int amount, String label) {
        if (list == null || list.getAmount(aspect) != amount) throw new IllegalStateException("Aspects for " + label);
    }
}
