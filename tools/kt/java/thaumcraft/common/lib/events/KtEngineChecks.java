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
        var report = new LinkedHashMap<String, Object>();
        report.put("kind", "server-engine integration; FakePlayer; no client UI input");
        report.put("tick", server.getTickCount()); report.put("checks", checks);
        Files.writeString(Path.of("engine-basic.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
