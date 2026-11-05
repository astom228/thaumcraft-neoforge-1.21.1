package thaumcraft.common.lib.events;

import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.common.tiles.essentia.TileJarFillable;

/** Проверки настоящего серверного движка с отдельным FakePlayer, не управление GUI клиентов. */
final class KtEngineChecks {
    private static final ArrayList<java.util.Map<String, Object>> checks = new ArrayList<>();
    private static void check(String name, Runnable action) {
        var result = new LinkedHashMap<String, Object>();
        result.put("name", name);
        try { action.run(); result.put("passed", true); }
        catch (Exception | AssertionError error) { result.put("passed", false); result.put("error", error.toString()); }
        checks.add(result);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static ItemStack crystal(Aspect aspect, int count) {
        var stack = new ItemStack(ItemsTC.crystalEssence.get(), count);
        stack.set(TCDataComponents.ASPECTS.get(), new AspectList().add(aspect, 1));
        return stack;
    }

    static void runInfusions(MinecraftServer server) throws java.io.IOException {
        checks.clear();
        var world = server.overworld();
        var center = new BlockPos(128, 121, 128);
        require(world.hasChunkAt(center), "Infusion fixture chunk must be loaded explicitly");
        var player = FakePlayerFactory.get(world, new GameProfile(UUID.fromString("924ac16d-7361-46cf-bbad-928bce2f9c98"), "KtEngine"));
        var knowledge = thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player);
        var pedestals = new BlockPos[]{center.offset(-3,0,0), center.offset(3,0,0), center.offset(0,0,-3), center.offset(0,0,3),
                center.offset(-3,0,-3), center.offset(3,0,3), center.offset(-3,0,3), center.offset(3,0,-3)};
        var jars = new BlockPos[]{center.offset(-2,0,7), center.offset(0,0,7), center.offset(2,0,7)};
        world.setBlockAndUpdate(center, BlocksTC.pedestalArcane.get().defaultBlockState());
        for (int x : new int[]{-1,1}) for (int z : new int[]{-1,1}) world.setBlockAndUpdate(center.offset(x,0,z), BlocksTC.pillarArcane.get().defaultBlockState());
        for (var pos : pedestals) world.setBlockAndUpdate(pos, BlocksTC.pedestalArcane.get().defaultBlockState());
        for (var pos : jars) world.setBlockAndUpdate(pos, BlocksTC.jarNormal.get().defaultBlockState());
        var outputs = java.util.Set.of(BlocksTC.jarBrain.get().asItem(), BlocksTC.mirror.get().asItem(),
                BlocksTC.mirrorEssentia.get().asItem(), ItemsTC.handMirror.get());
        int tested = 0;
        for (var entry : thaumcraft.api.ThaumcraftApi.getCraftingRecipes().entrySet()) {
            if (!(entry.getValue() instanceof thaumcraft.api.crafting.InfusionRecipe recipe)) continue;
            var enchantment = recipe instanceof thaumcraft.common.lib.crafting.InfusionEnchantmentRecipe er ? er.getEnchantment() : null;
            boolean selectedEnchantment = enchantment != null && java.util.Set.of(
                    thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.COLLECTOR,
                    thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.DESTRUCTIVE,
                    thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.BURROWING,
                    thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.SOUNDING,
                    thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.REFINING,
                    thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.ARCING,
                    thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.ESSENCE).contains(enchantment);
            boolean runic = recipe instanceof thaumcraft.common.lib.crafting.InfusionRunicAugmentRecipe;
            if (!(recipe.getRecipeOutput() instanceof ItemStack output && outputs.contains(output.getItem())) && !selectedEnchantment && !runic) continue;
            tested++;
            check("infusion complete recipe, payment and remainders: " + entry.getKey(), () -> {
                knowledge.clear();
                world.setBlockAndUpdate(center.above(2), Blocks.AIR.defaultBlockState());
                world.setBlockAndUpdate(center.above(2), BlocksTC.infusionMatrix.get().defaultBlockState());
                var matrix = (thaumcraft.common.tiles.crafting.TileInfusionMatrix) world.getBlockEntity(center.above(2));
                var main = (thaumcraft.common.tiles.crafting.TilePedestal) world.getBlockEntity(center);
                var input = selectedEnchantment ? new ItemStack(enchantment == thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.ARCING
                        || enchantment == thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.ESSENCE ? Items.DIAMOND_SWORD : Items.DIAMOND_PICKAXE)
                        : runic ? new ItemStack(Items.DIAMOND_CHESTPLATE) : recipe.getRecipeInput().getItems()[0].copyWithCount(1);
                main.setInventorySlotContents(0, input);
                require(recipe.getComponents().size() <= pedestals.length, "fixture too few pedestals");
                var remainders = new ArrayList<ItemStack>();
                var components = new ArrayList<ItemStack>();
                for (int i = 0; i < pedestals.length; i++) {
                    var ingredient = i < recipe.getComponents().size() ? recipe.getComponents().get(i).getItems()[0].copyWithCount(1) : ItemStack.EMPTY;
                    remainders.add(ingredient.getCraftingRemainingItem());
                    if (!ingredient.isEmpty()) components.add(ingredient.copy());
                    ((thaumcraft.common.tiles.crafting.TilePedestal) world.getBlockEntity(pedestals[i])).setInventorySlotContents(0, ingredient);
                }
                var expected = (ItemStack) recipe.getRecipeOutput(player, input, components);
                var recipeCost = recipe.getAspects(player, input, components);
                var cost = recipeCost.getAspects();
                require(cost.length <= jars.length, "fixture too few jars");
                for (int i = 0; i < jars.length; i++) {
                    var jar = (TileJarFillable) world.getBlockEntity(jars[i]);
                    jar.amount = i < cost.length ? recipeCost.getAmount(cost[i]) : 0;
                    jar.aspect = jar.aspectFilter = i < cost.length ? cost[i] : null;
                }
                matrix.active = true;
                matrix.craftingStart(player);
                require(!matrix.crafting && ItemStack.matches(main.getStackInSlot(0), input), "recipe started without research");
                knowledge.addResearch(recipe.getResearch());
                if (selectedEnchantment) {
                    require(!recipe.matches(components, new ItemStack(Items.DIAMOND), world, player), "enchantment accepts wrong item type");
                    var maxed = input.copy();
                    thaumcraft.common.lib.enchantment.EnumInfusionEnchantment.addInfusionEnchantment(maxed, enchantment, enchantment.maxLevel);
                    require(!recipe.matches(components, maxed, world, player), "enchantment exceeds maximum level");
                }
                var first = (thaumcraft.common.tiles.crafting.TilePedestal) world.getBlockEntity(pedestals[0]);
                var firstIngredient = first.getStackInSlot(0).copy();
                first.setInventorySlotContents(0, ItemStack.EMPTY);
                matrix.craftingStart(player);
                require(!matrix.crafting && ItemStack.matches(main.getStackInSlot(0), input), "recipe started without ingredient");
                first.setInventorySlotContents(0, firstIngredient);
                matrix.craftingStart(player);
                require(matrix.crafting, "recipe did not start");
                // Ускоренный прогон реальных craftCycle; стабильность контролируется для изоляции рецепта.
                for (int cycle = 0; matrix.crafting && cycle < 2000; cycle++) { matrix.stability = 25; matrix.craftCycle(); }
                require(!matrix.crafting, "recipe stalled");
                require(ItemStack.matches(main.getStackInSlot(0), expected), "wrong central result");
                for (var pos : jars) require(((TileJarFillable) world.getBlockEntity(pos)).amount == 0, "wrong essentia payment");
                for (int i = 0; i < pedestals.length; i++) require(ItemStack.matches(
                        ((thaumcraft.common.tiles.crafting.TilePedestal) world.getBlockEntity(pedestals[i])).getStackInSlot(0), remainders.get(i)), "ingredient/remainder mismatch " + i);
                matrix.onCasterRightClick(world, new ItemStack(ItemsTC.casterBasic.get()), player, center.above(2),
                        net.minecraft.core.Direction.SOUTH, net.minecraft.world.InteractionHand.MAIN_HAND);
                require(!matrix.crafting, "repeated click restarted completed recipe");
                require(ItemStack.matches(main.getStackInSlot(0), expected), "extra cycle duplicated result");
            });
        }
        require(tested == 12, "expected twelve selected recipes, got " + tested);
        var finalMatrix = (thaumcraft.common.tiles.crafting.TileInfusionMatrix) world.getBlockEntity(center.above(2));
        finalMatrix.crafting = false; finalMatrix.active = false;
        knowledge.clear(); player.getInventory().clearContent();
        Files.writeString(Path.of("engine-infusion.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(checks));
    }

    static void runStructures(MinecraftServer server) {
        checks.clear();
        var world = server.overworld();
        var base = new BlockPos(-4, 121, -10);
        var lower = base.above();
        var upper = base.above(2);
        var player = FakePlayerFactory.get(world, new GameProfile(UUID.fromString("924ac16d-7361-46cf-bbad-928bce2f9c98"), "KtEngine"));
        var knowledge = thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player);
        knowledge.clear(); player.getInventory().clearContent();
        world.setBlockAndUpdate(base, BlocksTC.crucible.get().defaultBlockState());
        world.setBlockAndUpdate(lower, BlocksTC.metalAlchemical.get().defaultBlockState());
        world.setBlockAndUpdate(upper, BlocksTC.metalAlchemical.get().defaultBlockState());
        var dust = new ItemStack(ItemsTC.salisMundus.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dust);
        var context = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(lower), net.minecraft.core.Direction.SOUTH, lower, false));
        check("thaumatorium dust rejected without research", () -> {
            dust.getItem().onItemUseFirst(dust, context);
            require(dust.getCount() == 1, "dust consumed without research");
            require(world.getBlockState(lower).is(BlocksTC.metalAlchemical.get()), "structure changed without research");
        });
        knowledge.addResearch("THAUMATORIUM");
        dust.getItem().onItemUseFirst(dust, context);
        check("thaumatorium consumes one dust and protects both pending blocks", () -> {
            require(dust.isEmpty(), "dust not consumed");
            for (var pos : new BlockPos[]{lower, upper}) {
                var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(world, pos, world.getBlockState(pos), player);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
                require(event.isCanceled(), "pending structure can be broken: " + pos);
            }
        });
        ServerEvents.addRunnableServer(world, () -> {
            check("thaumatorium delayed conversion and lock release", () -> {
                require(world.getBlockState(lower).is(BlocksTC.thaumatorium.get()), "lower block did not convert");
                require(world.getBlockState(upper).is(BlocksTC.thaumatoriumTop.get()), "upper block did not convert");
                require(world.getBlockState(lower).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                        == net.minecraft.core.Direction.SOUTH, "wrong front face");
                var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(world, lower, world.getBlockState(lower), player);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
                require(!event.isCanceled(), "conversion left a permanent block lock");
            });
            var machine = (thaumcraft.common.tiles.crafting.TileThaumatorium) world.getBlockEntity(lower);
            world.setBlockAndUpdate(base.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
            world.setBlockAndUpdate(lower.south(), Blocks.CHEST.defaultBlockState());
            world.setBlockAndUpdate(lower.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            machine.setInventorySlotContents(0, new ItemStack(ItemsTC.nuggetQuartz.get(), 2));
            player.moveTo(lower.getX() + 0.5, lower.getY() - 1, lower.getZ() + 3.5, 180, 0);
            var menu = new thaumcraft.common.container.ContainerThaumatorium(101, player.getInventory(), machine);
            check("thaumatorium hides unknown catalyst recipes", () -> require(menu.recipes.isEmpty(), "unknown recipe exposed"));
            knowledge.addResearch("BASEALCHEMY"); menu.updateRecipes();
            check("thaumatorium hides unfinished research recipes", () -> require(menu.recipes.isEmpty(), "unfinished research exposed recipe"));
            var research = thaumcraft.api.research.ResearchCategories.getResearch("BASEALCHEMY");
            knowledge.setResearchStage("BASEALCHEMY", research.getStages().length + 1);
            menu.updateRecipes();
            check("thaumatorium rejects invalid buttons and enforces recipe limit", () -> {
                require(!menu.clickMenuButton(player, -1) && !menu.clickMenuButton(player, 9999), "invalid button accepted");
                int chosen = -1;
                for (int i = 0; i < menu.recipes.size(); i++) if (menu.recipes.get(i).getAspects().getAmount(Aspect.AIR) == 2) { chosen = i; break; }
                require(chosen >= 0 && menu.clickMenuButton(player, chosen), "Aer recipe not selected");
                int alternative = chosen == 0 ? 1 : 0;
                require(!menu.clickMenuButton(player, alternative) && machine.recipeHash.size() == 1, "one-slot limit bypassed");
            });
            machine.essentia = new AspectList().add(Aspect.AIR, 2);
            ServerEvents.addRunnableServer(world, () -> {
                check("thaumatorium redstone preserves catalyst and essentia", () -> {
                    require(machine.getStackInSlot(0).getCount() == 2, "powered machine consumed catalyst");
                    require(machine.essentia.getAmount(Aspect.AIR) == 2, "powered machine consumed essentia");
                });
                world.setBlockAndUpdate(lower.west(), Blocks.AIR.defaultBlockState());
                ServerEvents.addRunnableServer(world, () -> {
                    check("thaumatorium pays once and inserts one result into chest", () -> {
                        require(machine.getStackInSlot(0).getCount() == 1, "wrong catalyst payment");
                        require(machine.essentia.visSize() == 0, "essentia cost not consumed");
                        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) world.getBlockEntity(lower.south());
                        int total = 0;
                        for (int i = 0; i < chest.getContainerSize(); i++) {
                            var result = chest.getItem(i);
                            if (result.isEmpty()) continue;
                            require(result.is(ItemsTC.crystalEssence.get()) && result.get(TCDataComponents.ASPECTS.get()).getAmount(Aspect.AIR) == 1, "wrong output");
                            total += result.getCount();
                        }
                        require(total == 1, "output duplicated or missing");
                    });
                    machine.setInventorySlotContents(0, ItemStack.EMPTY);
                    var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) world.getBlockEntity(lower.south());
                    chest.clearContent();
                    for (var pos : new BlockPos[]{upper, lower, base, lower.south()}) world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    world.setBlockAndUpdate(base.below(), Blocks.STONE.defaultBlockState());
                    player.getInventory().clearContent(); knowledge.clear();
                    try { Files.writeString(Path.of("engine-structures.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(checks)); }
                    catch (java.io.IOException error) { throw new RuntimeException(error); }
                }, 25);
            }, 45);
        }, 65);
    }

    static void run(MinecraftServer server) throws java.io.IOException {
        checks.clear();
        var world = server.overworld();
        var origin = new BlockPos(-16, 121, 20);
        require(world.hasChunkAt(origin), "Fixture chunk must already be loaded");
        var player = FakePlayerFactory.get(world, new GameProfile(UUID.fromString("924ac16d-7361-46cf-bbad-928bce2f9c98"), "KtEngine"));
        player.getInventory().clearContent();
        world.setBlockAndUpdate(origin, BlocksTC.arcaneWorkbench.get().defaultBlockState());
        var tile = (TileArcaneWorkbench) world.getBlockEntity(origin);
        var inventory = (net.neoforged.neoforge.items.IItemHandlerModifiable) tile.getItemHandler();
        var menu = new ContainerArcaneWorkbench(99, player.getInventory(), tile);
        var aura = thaumcraft.common.world.aura.AuraHandler.getAuraChunk(world, origin.getX() >> 4, origin.getZ() >> 4);
        float previousVis = aura.getVis();
        try {
            Aspect[] primals = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};
            for (int i = 0; i < primals.length; i++) {
                final int index = i;
                check("crystal slot and quick-move " + primals[i].getTag(), () -> {
                    player.getInventory().clearContent();
                    for (int j = 0; j < 15; j++) inventory.setStackInSlot(j, ItemStack.EMPTY);
                    var stack = crystal(primals[index], 16);
                    for (int j = 0; j < 6; j++) require(menu.getSlot(10 + j).mayPlace(stack) == (j == index), "wrong slot acceptance " + j);
                    player.getInventory().setItem(9, stack);
                    require(!menu.quickMoveStack(player, 16).isEmpty(), "quick move rejected");
                    require(tile.getItemHandler().getStackInSlot(9 + index).getCount() == 16, "crystals not routed to aspect slot");
                    require(player.getInventory().isEmpty(), "inventory duplicates crystals");
                    menu.quickMoveStack(player, 10 + index);
                    require(tile.getItemHandler().getStackInSlot(9 + index).isEmpty(), "crystal slot not emptied");
                    require(player.getInventory().countItem(ItemsTC.crystalEssence.get()) == 16, "crystal round-trip changed count");
                });
            }
            check("non-crystal rejected by every crystal slot", () -> {
                for (int j = 10; j < 16; j++) require(!menu.getSlot(j).mayPlace(new ItemStack(Items.DIAMOND)), "diamond accepted");
            });
            var filledPhial = new ItemStack(ItemsTC.phialFilled.get());
            filledPhial.set(TCDataComponents.ASPECTS.get(), new AspectList().add(Aspect.AIR, 10));
            var markedLabel = new ItemStack(ItemsTC.labelFilled.get());
            markedLabel.set(TCDataComponents.ASPECTS.get(), new AspectList().add(Aspect.AIR, 1));
            record Craft(String name, ItemStack[] grid, ItemStack result) {}
            for (var craft : new Craft[]{
                    new Craft("eight phials", new ItemStack[]{ItemStack.EMPTY,new ItemStack(Items.CLAY_BALL),ItemStack.EMPTY,
                            new ItemStack(Items.GLASS),ItemStack.EMPTY,new ItemStack(Items.GLASS),ItemStack.EMPTY,new ItemStack(Items.GLASS)}, new ItemStack(ItemsTC.phial.get(),8)),
                    new Craft("four labels", new ItemStack[]{new ItemStack(Items.PAPER),new ItemStack(Items.PAPER),new ItemStack(Items.PAPER),
                            new ItemStack(Items.PAPER),new ItemStack(Items.BLACK_DYE),new ItemStack(Items.SLIME_BALL)}, new ItemStack(ItemsTC.label.get(),4)),
                    new Craft("aspect label", new ItemStack[]{new ItemStack(ItemsTC.label.get()),filledPhial},markedLabel),
                    new Craft("clear label", new ItemStack[]{markedLabel},new ItemStack(ItemsTC.label.get())),
                    new Craft("scribing tools", new ItemStack[]{new ItemStack(ItemsTC.phial.get()),new ItemStack(Items.FEATHER),new ItemStack(Items.BLACK_DYE)},new ItemStack(ItemsTC.scribingTools.get()))}) {
                check("real crafting menu: " + craft.name(), () -> {
                    player.getInventory().clearContent();
                    for(int j=0;j<15;j++) inventory.setStackInSlot(j,ItemStack.EMPTY);
                    for(int j=0;j<craft.grid().length;j++) inventory.setStackInSlot(j,craft.grid()[j].copy());
                    require(ItemStack.matches(menu.getSlot(0).getItem(),craft.result()),"wrong crafting result");
                    menu.clicked(0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,player);
                    int count=0;
                    for(var stack:player.getInventory().items) if(!stack.isEmpty()) {
                        require(ItemStack.isSameItemSameComponents(stack,craft.result()),"unexpected output or remainder");count+=stack.getCount();
                    }
                    require(count==craft.result().getCount(),"wrong result count");
                    for(int j=0;j<9;j++) require(inventory.getStackInSlot(j).isEmpty(),"ingredient not consumed");
                });
            }
            check("aspect label rejects malformed phial without consuming ingredients", () -> {
                for(var aspects:new AspectList[]{new AspectList().add(Aspect.AIR,1),new AspectList().add(Aspect.AIR,5),
                        new AspectList().add(Aspect.AIR,10).add(Aspect.FIRE,10)}) {
                    player.getInventory().clearContent();
                    for(int j=0;j<15;j++) inventory.setStackInSlot(j,ItemStack.EMPTY);
                    var invalid=new ItemStack(ItemsTC.phialFilled.get()); invalid.set(TCDataComponents.ASPECTS.get(),aspects);
                    inventory.setStackInSlot(0,new ItemStack(ItemsTC.label.get())); inventory.setStackInSlot(1,invalid);
                    require(menu.getSlot(0).getItem().isEmpty()&&menu.quickMoveStack(player,0).isEmpty(),"invalid phial produced label");
                    require(inventory.getStackInSlot(0).getCount()==1&&ItemStack.matches(inventory.getStackInSlot(1),invalid),"refusal consumed ingredients");
                }
            });
            check("vanilla result quick-move consumes exactly one log", () -> {
                player.getInventory().clearContent();
                for (int j = 0; j < 15; j++) inventory.setStackInSlot(j, ItemStack.EMPTY);
                inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
                require(menu.getSlot(0).getItem().is(Items.OAK_PLANKS), "missing planks result");
                menu.quickMoveStack(player, 0);
                require(player.getInventory().countItem(Items.OAK_PLANKS) == 4, "not exactly four planks");
                require(tile.getItemHandler().getStackInSlot(0).isEmpty(), "log retained");
                require(menu.quickMoveStack(player, 0).isEmpty(), "second result duplicated");
            });
            check("salis quick-move preserves bowl and flint", () -> {
                player.getInventory().clearContent();
                for (int j = 0; j < 15; j++) inventory.setStackInSlot(j, ItemStack.EMPTY);
                var inputs = new ItemStack[]{new ItemStack(Items.BOWL), new ItemStack(Items.FLINT), new ItemStack(Items.REDSTONE),
                    crystal(Aspect.AIR, 1), crystal(Aspect.FIRE, 1), crystal(Aspect.WATER, 1)};
                for (int j = 0; j < inputs.length; j++) inventory.setStackInSlot(j, inputs[j]);
                require(menu.getSlot(0).getItem().is(ItemsTC.salisMundus.get()), "missing salis result");
                menu.quickMoveStack(player, 0);
                require(player.getInventory().countItem(ItemsTC.salisMundus.get()) == 1, "wrong salis count");
                require(tile.getItemHandler().getStackInSlot(0).is(Items.BOWL), "bowl consumed");
                require(tile.getItemHandler().getStackInSlot(1).is(Items.FLINT), "flint consumed");
                for (int j = 2; j < 9; j++) require(tile.getItemHandler().getStackInSlot(j).isEmpty(), "ingredient retained " + j);
            });
            Runnable prepareArcane = () -> {
                player.getInventory().clearContent();
                var knowledge = thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player);
                knowledge.clear(); knowledge.addResearch("FIRSTSTEPS"); knowledge.setResearchStage("FIRSTSTEPS", 4);
                for (int j = 0; j < 15; j++) inventory.setStackInSlot(j, ItemStack.EMPTY);
                aura.setVis(200);
                for (int slot : new int[]{1, 3, 5, 7}) inventory.setStackInSlot(slot, new ItemStack(Items.GOLD_INGOT));
                inventory.setStackInSlot(4, new ItemStack(Items.GLASS_PANE));
                Aspect[] order = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};
                for (int j = 0; j < order.length; j++) inventory.setStackInSlot(9 + j, crystal(order[j], 4));
                menu.onCraftMatrixChanged();
                require(menu.getSlot(0).getItem().is(ItemsTC.thaumometer.get()), "arcane fixture has no result");
            };
            check("arcane quick-move pays 20 vis and all six crystals", () -> {
                prepareArcane.run();
                menu.quickMoveStack(player, 0);
                require(player.getInventory().countItem(ItemsTC.thaumometer.get()) == 1, "wrong arcane result count");
                require(Math.abs(aura.getVis() - 180) < 0.001F, "wrong vis payment: " + aura.getVis());
                for (int j = 0; j < 9; j++) require(inventory.getStackInSlot(j).isEmpty(), "grid ingredient retained");
                for (int j = 9; j < 15; j++) require(inventory.getStackInSlot(j).getCount() == 3, "wrong crystal payment");
            });
            check("full QUICK_MOVE crafts until vis is exhausted without free output", () -> {
                prepareArcane.run();
                for (int j : new int[]{1, 3, 4, 5, 7, 9, 10, 11, 12, 13, 14}) {
                    inventory.setStackInSlot(j, inventory.getStackInSlot(j).copyWithCount(16));
                }
                menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
                require(player.getInventory().countItem(ItemsTC.thaumometer.get()) == 10, "bulk craft must stop at ten results");
                require(Math.abs(aura.getVis()) < 0.001F, "bulk craft has wrong vis balance");
                for (int j : new int[]{1, 3, 4, 5, 7, 9, 10, 11, 12, 13, 14}) {
                    require(inventory.getStackInSlot(j).getCount() == 6, "bulk craft consumed wrong quantity in slot " + j);
                }
                require(menu.getSlot(0).getItem().isEmpty(), "bulk craft leaves an available result without vis");
                menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
                require(player.getInventory().countItem(ItemsTC.thaumometer.get()) == 10, "repeat bulk click created free output");
            });
            for (boolean quickMove : new boolean[]{false, true}) {
                for (boolean revokeKnowledge : new boolean[]{false, true}) {
                    check("stale arcane output rejected: " + (revokeKnowledge ? "research revoked" : "aura exhausted")
                            + (quickMove ? "; quick-move" : "; pickup"), () -> {
                        prepareArcane.run();
                        if (revokeKnowledge) thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player).clear();
                        else aura.setVis(0);
                        require(menu.getSlot(0).hasItem(), "fixture must retain a stale output before validation");
                        if (quickMove) require(menu.quickMoveStack(player, 0).isEmpty(), "stale quick-move succeeded");
                        else require(!menu.getSlot(0).mayPickup(player), "stale pickup accepted");
                        require(menu.getSlot(0).getItem().isEmpty(), "stale result not cleared");
                        require(player.getInventory().isEmpty(), "stale result appeared in inventory");
                        for (int j : new int[]{1, 3, 5, 7}) require(inventory.getStackInSlot(j).getCount() == 1, "gold changed on refusal");
                        require(inventory.getStackInSlot(4).is(Items.GLASS_PANE), "glass changed on refusal");
                        for (int j = 9; j < 15; j++) require(inventory.getStackInSlot(j).getCount() == 4, "crystals changed on refusal");
                        require(aura.getVis() == (revokeKnowledge ? 200 : 0), "vis changed on refusal");
                    });
                }
            }
        } finally {
            menu.removed(player);
            player.getInventory().clearContent();
            for (int j = 0; j < 15; j++) inventory.setStackInSlot(j, ItemStack.EMPTY);
            world.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
            aura.setVis(previousVis);
            thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player).clear();
        }
        var cruciblePos = origin.offset(2, 0, 0);
        world.setBlockAndUpdate(cruciblePos, BlocksTC.crucible.get().defaultBlockState());
        var crucible = (TileCrucible) world.getBlockEntity(cruciblePos);
        try {
            check("crucible water simulate leaves tank empty", () -> {
                require(crucible.tank.fill(new FluidStack(Fluids.WATER, 1500), FluidAction.SIMULATE) == 1000, "wrong simulated capacity");
                require(crucible.tank.isEmpty(), "simulation mutated tank");
            });
            check("crucible pipe rejects lava", () -> require(crucible.tank.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE) == 0, "lava accepted"));
            check("crucible fill is capped at 1000", () -> {
                require(crucible.tank.fill(new FluidStack(Fluids.WATER, 1500), FluidAction.EXECUTE) == 1000, "wrong fill");
                require(crucible.tank.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE) == 0, "overfilled");
            });
            for (int amount : new int[]{0, 1, 250, 500, 550, 5000}) {
                check("crucible comparator range at " + amount, () -> {
                    crucible.aspects = new AspectList().add(Aspect.AIR, amount);
                    int signal = world.getBlockState(cruciblePos).getAnalogOutputSignal(world, cruciblePos);
                    require(signal >= 0 && signal <= 15, "signal out of range: " + signal);
                    require(signal == (amount == 0 ? 0 : Math.min(15, (int)Math.floor(amount / 500.0F * 14) + 1)), "wrong signal " + signal);
                });
            }
            check("crucible spill removes contents and pollutes aura", () -> {
                crucible.aspects = new AspectList().add(Aspect.AIR, 20);
                float before = thaumcraft.common.world.aura.AuraHandler.getFlux(world, cruciblePos);
                crucible.spillRemnants();
                require(crucible.tank.isEmpty() && crucible.aspects.visSize() == 0, "contents retained");
                require(Math.abs(thaumcraft.common.world.aura.AuraHandler.getFlux(world, cruciblePos) - before - 15) < 0.001F, "wrong pollution");
            });
        } finally {
            crucible.aspects = new AspectList(); crucible.tank.setFluid(FluidStack.EMPTY);
            world.setBlockAndUpdate(cruciblePos, Blocks.AIR.defaultBlockState());
        }
        var jarPos = origin.offset(4, 0, 0);
        world.setBlockAndUpdate(jarPos, BlocksTC.jarNormal.get().defaultBlockState());
        var jar = (TileJarFillable) world.getBlockEntity(jarPos);
        try {
            check("jar capacity and aspect isolation", () -> {
                require(jar.addToContainer(Aspect.AIR, 300) == 50 && jar.amount == 250, "wrong jar capacity");
                require(jar.addToContainer(Aspect.FIRE, 10) == 10 && jar.amount == 250 && jar.aspect == Aspect.AIR, "mixed aspects");
                require(jar.takeFromContainer(Aspect.AIR, 10) && jar.amount == 240, "phial amount extraction failed");
            });
            check("jar label rejects foreign aspect while empty", () -> {
                jar.amount = 0; jar.aspect = null; jar.aspectFilter = Aspect.FIRE;
                require(jar.addToContainer(Aspect.AIR, 10) == 10 && jar.amount == 0, "filter bypassed");
                require(jar.addToContainer(Aspect.FIRE, 10) == 0 && jar.amount == 10, "matching filter rejected");
            });
            check("jar invalid quantities cannot create or destroy essentia", () -> {
                jar.amount = 10; jar.aspect = Aspect.FIRE;
                require(jar.addToContainer(Aspect.FIRE, -5) == -5 && jar.amount == 10, "negative insertion changed amount");
                require(!jar.takeFromContainer(Aspect.FIRE, -5) && jar.amount == 10, "negative extraction created essentia");
                require(!jar.takeFromContainer(null, 1) && jar.amount == 10, "null extraction accepted");
                require(jar.addToContainer(null, 1) == 1 && jar.amount == 10, "null insertion accepted");
            });
        } finally { jar.amount = 0; world.setBlockAndUpdate(jarPos, Blocks.AIR.defaultBlockState()); }
        world.setBlockAndUpdate(jarPos, BlocksTC.jarVoid.get().defaultBlockState());
        var voidJar = (TileJarFillable) world.getBlockEntity(jarPos);
        try {
            check("void jar discards overflow, retains filter and rejects negative quantities", () -> {
                voidJar.aspectFilter = Aspect.AIR;
                require(voidJar.addToContainer(Aspect.FIRE, 300) == 300 && voidJar.amount == 0, "void label bypassed");
                require(voidJar.addToContainer(Aspect.AIR, 300) == 0 && voidJar.amount == 250, "void jar overflow failed");
                require(voidJar.addToContainer(Aspect.AIR, -10) == -10 && voidJar.amount == 250, "negative insertion changed void jar");
                require(!voidJar.takeFromContainer(Aspect.AIR, -10) && voidJar.amount == 250, "negative extraction increased void jar");
            });
        } finally { voidJar.amount = 0; world.setBlockAndUpdate(jarPos, Blocks.AIR.defaultBlockState()); }
        var urnPos = origin.offset(6, 0, 0);
        world.setBlockAndUpdate(urnPos, BlocksTC.everfullUrn.get().defaultBlockState());
        var urn = (thaumcraft.common.tiles.devices.TileWaterJug) world.getBlockEntity(urnPos);
        try {
            for (int water : new int[]{249, 250, 1000}) {
                check("urn bottle water conservation at " + water, () -> {
                    player.getInventory().clearContent();
                    player.getInventory().selected = 0;
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
                    urn.tank.setFluid(new FluidStack(Fluids.WATER, water));
                    world.getBlockState(urnPos).useItemOn(player.getMainHandItem(), world, player,
                            net.minecraft.world.InteractionHand.MAIN_HAND,
                            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(urnPos),
                                    net.minecraft.core.Direction.SOUTH, urnPos, false));
                    require(urn.tank.getFluidAmount() == (water < 250 ? water : water - 250), "wrong bottle water cost");
                    var held = player.getMainHandItem();
                    if (water < 250) require(held.is(Items.GLASS_BOTTLE) && held.getCount() == 1, "insufficient water consumed bottle");
                    else require(held.is(Items.POTION) && held.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS)
                            .is(net.minecraft.world.item.alchemy.Potions.WATER), "not a water bottle");
                });
            }
        } finally {
            player.getInventory().clearContent();
            urn.tank.setFluid(FluidStack.EMPTY);
            world.setBlockAndUpdate(urnPos, Blocks.AIR.defaultBlockState());
        }
        var tubePos = origin.offset(8, 1, 0);
        world.setBlockAndUpdate(tubePos, BlocksTC.tube.get().defaultBlockState());
        world.setBlockAndUpdate(tubePos.west(), BlocksTC.tube.get().defaultBlockState());
        try {
            check("caster closes and reopens the same pipe through real outline ray", () -> {
                player.moveTo(tubePos.getX() + 0.2, tubePos.getY() - 1, tubePos.getZ() - 2.5, 0, 2);
                player.setYHeadRot(0);
                player.yHeadRotO = 0;
                var caster = new ItemStack(ItemsTC.casterBasic.get());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, caster);
                var tube = (thaumcraft.common.tiles.essentia.TileTube) world.getBlockEntity(tubePos);
                var neighbor = (thaumcraft.common.tiles.essentia.TileTube) world.getBlockEntity(tubePos.west());
                for (boolean open : new boolean[]{false, true}) {
                    var start = player.getEyePosition();
                    var end = start.add(player.getViewVector(1).scale(player.blockInteractionRange()));
                    var hit = world.clip(new net.minecraft.world.level.ClipContext(start, end,
                            net.minecraft.world.level.ClipContext.Block.OUTLINE,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, player));
                    require(hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(tubePos),
                            "closed pipe cannot be targeted: " + hit.getType() + " " + hit.getBlockPos() + " expected " + tubePos
                                    + " eye=" + start + " end=" + end + " pose=" + player.getPose());
                    require(tube.onCasterRightClick(world, caster, player, tubePos, hit.getDirection(),
                            net.minecraft.world.InteractionHand.MAIN_HAND), "caster interaction refused");
                    require(tube.isConnectable(net.minecraft.core.Direction.WEST) == open, "wrong local side");
                    require(neighbor.isConnectable(net.minecraft.core.Direction.EAST) == open, "wrong neighbor side");
                }
            });
        } finally {
            player.getInventory().clearContent();
            world.setBlockAndUpdate(tubePos, Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(tubePos.west(), Blocks.AIR.defaultBlockState());
        }
        var mirrorA = origin.offset(0, 0, 10);
        var mirrorB = origin.offset(2, 0, 10);
        var mirrorJar = mirrorB.south(2);
        for (var at : new BlockPos[]{mirrorA, mirrorB}) {
            world.setBlockAndUpdate(at.north(), Blocks.STONE.defaultBlockState());
            world.setBlockAndUpdate(at, BlocksTC.mirrorEssentia.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.devices.BlockMirror.FACING, net.minecraft.core.Direction.SOUTH));
        }
        world.setBlockAndUpdate(mirrorJar, BlocksTC.jarNormal.get().defaultBlockState());
        try {
            var a = (thaumcraft.common.tiles.devices.TileMirrorEssentia) world.getBlockEntity(mirrorA);
            var b = (thaumcraft.common.tiles.devices.TileMirrorEssentia) world.getBlockEntity(mirrorB);
            var source = (TileJarFillable) world.getBlockEntity(mirrorJar);
            a.linked = b.linked = true; a.linkPos = mirrorB; b.linkPos = mirrorA;
            a.linkDim = b.linkDim = world.dimension();
            for (int quantity : new int[]{-1, 0, 2}) {
                check("essentia mirror rejects invalid quantity " + quantity, () -> {
                    source.amount = 10; source.aspect = source.aspectFilter = Aspect.SENSES;
                    int instability = a.instability;
                    require(!a.takeFromContainer(Aspect.SENSES, quantity), "invalid extraction succeeded");
                    require(a.addToContainer(Aspect.SENSES, quantity) == quantity, "invalid insertion consumed");
                    require(!a.doesContainerContainAmount(Aspect.SENSES, quantity), "invalid availability accepted");
                    require(source.amount == 10 && a.instability == instability, "invalid call changed source or instability");
                });
            }
            check("essentia mirror null aspect is harmless", () -> {
                require(!a.takeFromContainer(null, 1), "null extraction accepted");
                require(a.addToContainer(null, 1) == 1, "null insertion accepted");
                require(!a.doesContainerAccept(null), "null filter accepted");
            });
            check("essentia mirror valid transfer conserves one unit", () -> {
                source.amount = 10; source.aspect = source.aspectFilter = Aspect.SENSES;
                require(a.takeFromContainer(Aspect.SENSES, 1) && source.amount == 9, "valid extraction failed");
                require(a.addToContainer(Aspect.SENSES, 1) == 0 && source.amount == 10, "valid insertion failed");
            });
        } finally {
            for (var at : new BlockPos[]{mirrorA, mirrorB, mirrorJar}) world.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            for (var at : new BlockPos[]{mirrorA, mirrorB}) world.setBlockAndUpdate(at.north(), Blocks.AIR.defaultBlockState());
        }
        var storagePos = origin.offset(10, 0, 0);
        for (var storageBlock : new net.minecraft.world.level.block.Block[]{BlocksTC.alembic.get(), BlocksTC.tubeBuffer.get(),
                BlocksTC.thaumatorium.get(), BlocksTC.centrifuge.get()}) {
            world.setBlockAndUpdate(storagePos, storageBlock.defaultBlockState());
            var storage = (thaumcraft.api.aspects.IAspectContainer) world.getBlockEntity(storagePos);
            try {
                check("storage refuses negative extraction: " + storageBlock, () -> {
                    if (storage instanceof thaumcraft.common.tiles.crafting.TileThaumatorium th) th.essentia.add(Aspect.AIR, 1);
                    else require(storage.addToContainer(Aspect.AIR, 1) == 0, "fixture insertion failed");
                    require(!storage.takeFromContainer(Aspect.AIR, -1), "negative extraction accepted");
                    require(storage.getAspects().getAmount(Aspect.AIR) == 1, "negative extraction changed stored quantity");
                    require(!storage.takeFromContainer(Aspect.AIR, 0), "zero extraction accepted");
                    require(storage.getAspects().getAmount(Aspect.AIR) == 1, "zero extraction changed stored quantity");
                    require(!storage.takeFromContainer(Aspect.AIR, 2), "extraction exceeded contents");
                    require(storage.takeFromContainer(Aspect.AIR, 1), "ordinary extraction failed");
                    require(storage.getAspects().getAmount(Aspect.AIR) == 0, "ordinary extraction not conserved");
                    require(storage.addToContainer(Aspect.AIR, -1) == -1, "negative insertion consumed");
                    require(storage.addToContainer(null, 1) == 1, "null insertion consumed");
                    require(storage.getAspects().getAmount(Aspect.AIR) == 0, "invalid insertion created essentia");
                });
            } finally { world.setBlockAndUpdate(storagePos, Blocks.AIR.defaultBlockState()); }
        }
        var pedestalPos = origin.offset(12, 0, 0);
        world.setBlockAndUpdate(pedestalPos, BlocksTC.pedestalArcane.get().defaultBlockState());
        var pedestal = (thaumcraft.common.tiles.crafting.TilePedestal) world.getBlockEntity(pedestalPos);
        player.moveTo(pedestalPos.getX() + 0.5, pedestalPos.getY(), pedestalPos.getZ() + 2, 180, 20);
        var oldEntities = world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(3))
                .stream().map(net.minecraft.world.entity.Entity::getId).collect(java.util.stream.Collectors.toSet());
        try {
            check("pedestal real use places one and empty-hand use returns one", () -> {
                player.getInventory().clearContent();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 8));
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pedestalPos), net.minecraft.core.Direction.SOUTH, pedestalPos, false);
                world.getBlockState(pedestalPos).useItemOn(player.getMainHandItem(), world, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                require(pedestal.getStackInSlot(0).is(Items.DIAMOND) && pedestal.getStackInSlot(0).getCount() == 1, "pedestal did not take one");
                require(player.getMainHandItem().getCount() == 7, "wrong held remainder");
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, player.getMainHandItem());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                world.getBlockState(pedestalPos).useWithoutItem(world, player, hit);
                require(pedestal.getStackInSlot(0).isEmpty(), "pedestal retained item");
                int returned = world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(3)).stream()
                        .filter(e -> !oldEntities.contains(e.getId()) && e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
                require(returned == 1 && player.getOffhandItem().getCount() == 7, "diamond round-trip not conserved");
            });
        } finally {
            pedestal.setInventorySlotContents(0, ItemStack.EMPTY);
            world.setBlockAndUpdate(pedestalPos, Blocks.AIR.defaultBlockState());
            for (var entity : world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(3)))
                if (!oldEntities.contains(entity.getId()) && entity.getItem().is(Items.DIAMOND)) entity.discard();
            player.getInventory().clearContent();
        }
        check("rift focus rejects configured Outer Lands without changing blocks", () -> {
            var configured = thaumcraft.common.config.ModConfig.CONFIG_WORLD.dimensionOuterId;
            String original = configured.get();
            var state = world.getBlockState(origin);
            try {
                configured.set(world.dimension().location().toString());
                configured.clearCache();
                var pack = new thaumcraft.api.casters.FocusPackage(); pack.world = world;
                var effect = new thaumcraft.common.items.casters.foci.FocusEffectRift(); effect.setPackage(pack);
                require(!effect.execute(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(origin),
                        net.minecraft.core.Direction.UP, origin, false), null, 1, 0), "rift allowed in protected dimension");
                require(world.getBlockState(origin) == state, "protected block changed");
            } finally { configured.set(original); configured.clearCache(); }
        });
        var report = new LinkedHashMap<String, Object>();
        report.put("kind", "server-engine integration; FakePlayer; no client UI input");
        report.put("tick", server.getTickCount()); report.put("checks", checks);
        Files.writeString(Path.of("engine-basic.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
