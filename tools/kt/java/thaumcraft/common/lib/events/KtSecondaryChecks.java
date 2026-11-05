package thaumcraft.common.lib.events;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.blocks.devices.BlockTCDeviceFacing;
import thaumcraft.common.items.casters.foci.FocusEffectRift;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.devices.TileHungryChest;
import thaumcraft.common.tiles.devices.TileRechargePedestal;
import thaumcraft.common.tiles.devices.TileVisGenerator;
import thaumcraft.common.tiles.misc.TileHole;
import thaumcraft.common.world.aura.AuraHandler;

/** Выбранные второстепенные устройства; только изолированный сервер приёмки. */
final class KtSecondaryChecks {
    private static final BlockPos BASE = new BlockPos(136, 130, 136);
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void run(net.minecraft.server.MinecraftServer server, boolean resume) throws java.io.IOException {
        var world = server.overworld();
        require(world.hasChunkAt(BASE), "Explicitly load the acceptance fixture first");
        var checks = new ArrayList<java.util.Map<String, Object>>();
        java.util.function.BiConsumer<String,Runnable> check = (name, action) -> {
            var result = new LinkedHashMap<String,Object>(); result.put("name", name);
            try { action.run(); result.put("passed", true); }
            catch (Exception | AssertionError error) { result.put("passed", false); result.put("error", error.toString()); }
            checks.add(result);
        };
        var pedestalPos = BASE;
        var generatorPos = BASE.east(2);
        var chestPos = BASE.east(4);
        var holePos = BASE.south(3);
        if (resume) {
            check.accept("restart preserves rechargeable item and exact charge", () -> {
                var tile = (TileRechargePedestal)world.getBlockEntity(pedestalPos);
                var stack=tile.getStackInSlot(0);
                require(stack.is(ItemsTC.travellerBoots.get()) && RechargeHelper.getCharge(stack) == ((thaumcraft.api.items.IRechargable)stack.getItem()).getMaxCharge(stack,null), "charge or item changed");
            });
            check.accept("restart preserves generator FE without redstone spending", () -> {
                var tile = (TileVisGenerator)world.getBlockEntity(generatorPos);
                require(tile.getEnergyStored() == 1000, "energy changed while disabled");
            });
            check.accept("restart preserves hungry chest exact contents", () -> {
                var chest = (TileHungryChest)world.getBlockEntity(chestPos);
                require(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 13, "contents changed");
            });
            check.accept("restart preserves temporary hole memory and restores original state", () -> {
                var tile = (TileHole)world.getBlockEntity(holePos);
                var original = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
                require(tile != null && tile.oldblock.equals(original) && tile.countdownmax == 12000, "hole memory lost");
                tile.countdown = (short)(tile.countdownmax - 1);
                TileHole.tick(world, holePos, world.getBlockState(holePos), tile);
                require(world.getBlockState(holePos).equals(original), "hole did not restore block properties");
            });
            // These blocks belong exclusively to this fixture. Remove the redstone block last.
            for (var pos : new BlockPos[]{pedestalPos,generatorPos,chestPos,holePos,generatorPos.north()}) world.removeBlock(pos, false);
        } else {
            var aura = AuraHandler.getAuraChunk(world, BASE.getX() >> 4, BASE.getZ() >> 4);
            require(aura != null, "Fixture aura missing");
            float oldVis = aura.getVis();
            try {
                world.setBlockAndUpdate(pedestalPos, BlocksTC.rechargePedestal.get().defaultBlockState());
                var pedestal = (TileRechargePedestal)world.getBlockEntity(pedestalPos);
                var boots = new ItemStack(ItemsTC.travellerBoots.get());
                check.accept("pedestal charges boots with exact aura payment", () -> {
                    aura.setVis(100); pedestal.setInventorySlotContents(0, boots);
                    TileRechargePedestal.tick(world,pedestalPos,pedestal.getBlockState(),pedestal);
                    require(RechargeHelper.getCharge(boots)==5 && Math.abs(aura.getVis()-95)<0.001, "wrong charge or payment");
                });
                check.accept("charge cap spends only remaining capacity", () -> {
                    int max=((thaumcraft.api.items.IRechargable)boots.getItem()).getMaxCharge(boots,null);
                    boots.set(TCDataComponents.CHARGE.get(),max-1);aura.setVis(100);
                    require(RechargeHelper.rechargeItem(world,boots,BASE,null,5)==1 && RechargeHelper.getCharge(boots)==max && aura.getVis()==99, "overcharge or overpayment");
                    require(RechargeHelper.rechargeItem(world,boots,BASE,null,5)==0 && aura.getVis()==99,"full item charged again");
                });
                check.accept("empty aura and insufficient charge refuse safely", () -> {
                    boots.set(TCDataComponents.CHARGE.get(),0);aura.setVis(0);
                    require(RechargeHelper.rechargeItem(world,boots,BASE,null,5)==0 && RechargeHelper.getCharge(boots)==0,"free charge");
                    require(!RechargeHelper.consumeCharge(boots,null,1) && RechargeHelper.getCharge(boots)==0,"overdraw allowed");
                    boots.set(TCDataComponents.CHARGE.get(),10);
                    require(RechargeHelper.consumeCharge(boots,null,3) && RechargeHelper.getCharge(boots)==7,"wrong consumption");
                });
                check.accept("pedestal item handler rejects ordinary items without consumption", () -> {
                    pedestal.setInventorySlotContents(0,ItemStack.EMPTY);
                    var result=pedestal.getItemHandler().insertItem(0,new ItemStack(Items.DIAMOND,4),false);
                    require(result.getCount()==4 && pedestal.getStackInSlot(0).isEmpty(),"ordinary item accepted or lost");
                });
                world.setBlockAndUpdate(generatorPos,BlocksTC.visGenerator.get().defaultBlockState().setValue(BlockTCDeviceFacing.FACING,Direction.EAST).setValue(BlockTCDeviceFacing.ENABLED,true));
                var generator=(TileVisGenerator)world.getBlockEntity(generatorPos);
                check.accept("generator converts one vis into 1000 FE and does not refill nonempty buffer", () -> {
                    aura.setVis(100);TileVisGenerator.tick(world,generatorPos,generator.getBlockState(),generator);
                    require(generator.getEnergyStored()==1000 && aura.getVis()==99,"incorrect conversion");
                    TileVisGenerator.tick(world,generatorPos,generator.getBlockState(),generator);
                    require(generator.getEnergyStored()==1000 && aura.getVis()==99,"buffer charged twice");
                });
                check.accept("generator energy capability exists only at output and rejects input", () -> {
                    for(var side:Direction.values()) require((world.getCapability(Capabilities.EnergyStorage.BLOCK,generatorPos,side)!=null)==(side==Direction.EAST),"wrong energy side "+side);
                    require(generator.receiveEnergy(100,false)==0 && generator.getEnergyStored()==1000,"accepted foreign energy");
                });
                check.accept("redstone disables generator and prevents aura consumption", () -> {
                    world.setBlockAndUpdate(generatorPos.north(),Blocks.REDSTONE_BLOCK.defaultBlockState());
                    require(!world.getBlockState(generatorPos).getValue(BlockTCDeviceFacing.ENABLED),"redstone did not disable");
                    var data=generator.saveWithFullMetadata(world.registryAccess());data.putInt("energy",0);generator.loadWithComponents(data,world.registryAccess());aura.setVis(100);
                    TileVisGenerator.tick(world,generatorPos,generator.getBlockState(),generator);
                    require(generator.getEnergyStored()==0 && aura.getVis()==100,"disabled generator spent aura");
                    data.putInt("energy",1000);generator.loadWithComponents(data,world.registryAccess());
                });
                world.setBlockAndUpdate(chestPos,BlocksTC.hungryChest.get().defaultBlockState());
                var chest=(TileHungryChest)world.getBlockEntity(chestPos);
                check.accept("hungry chest absorbs dropped stack exactly once", () -> {
                    var item=new ItemEntity(world,chestPos.getX()+0.5,chestPos.getY()+0.5,chestPos.getZ()+0.5,new ItemStack(Items.DIAMOND,8));world.addFreshEntity(item);
                    world.getBlockState(chestPos).entityInside(world,chestPos,item);
                    require(!item.isAlive() && chest.countItem(Items.DIAMOND)==8,"item not absorbed exactly");
                    world.getBlockState(chestPos).entityInside(world,chestPos,item);
                    require(chest.countItem(Items.DIAMOND)==8,"dead entity duplicated");
                });
                check.accept("full hungry chest keeps dropped items alive and unchanged", () -> {
                    for(int i=0;i<chest.getContainerSize();i++)chest.setItem(i,new ItemStack(Items.COBBLESTONE,64));
                    var item=new ItemEntity(world,chestPos.getX()+0.5,chestPos.getY()+0.5,chestPos.getZ()+0.5,new ItemStack(Items.DIAMOND,5));world.addFreshEntity(item);
                    world.getBlockState(chestPos).entityInside(world,chestPos,item);
                    require(item.isAlive()&&item.getItem().getCount()==5,"overflow lost");item.discard();chest.clearContent();
                });
                check.accept("hungry chest capability supports simulated insert and exact extraction", () -> {
                    var handler=world.getCapability(Capabilities.ItemHandler.BLOCK,chestPos,Direction.UP);require(handler!=null,"handler missing");
                    require(handler.insertItem(0,new ItemStack(Items.DIAMOND,13),true).isEmpty()&&chest.isEmpty(),"simulation changed inventory");
                    require(handler.insertItem(0,new ItemStack(Items.DIAMOND,13),false).isEmpty(),"insert refused");
                    require(handler.extractItem(0,3,false).getCount()==3&&chest.countItem(Items.DIAMOND)==10,"extraction duplicated or lost");
                    handler.insertItem(0,new ItemStack(Items.DIAMOND,3),false);
                });
                check.accept("rift refuses block entities and bedrock without damage", () -> {
                    require(!FocusEffectRift.createHole(world,chestPos,null,(byte)1,40)&&chest.countItem(Items.DIAMOND)==13,"chest replaced");
                    world.setBlockAndUpdate(holePos,Blocks.BEDROCK.defaultBlockState());
                    require(!FocusEffectRift.createHole(world,holePos,null,(byte)1,40)&&world.getBlockState(holePos).is(Blocks.BEDROCK),"bedrock replaced");
                });
                check.accept("rift timeout restores exact original block orientation", () -> {
                    var state=Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST);world.setBlockAndUpdate(holePos,state);
                    require(FocusEffectRift.createHole(world,holePos,null,(byte)1,2),"hole refused stairs");var tile=(TileHole)world.getBlockEntity(holePos);
                    TileHole.tick(world,holePos,tile.getBlockState(),tile);TileHole.tick(world,holePos,tile.getBlockState(),tile);
                    require(world.getBlockState(holePos).equals(state),"wrong restored state");
                    require(FocusEffectRift.createHole(world,holePos,null,(byte)1,12000),"persistent hole refused");
                });
                boots.set(TCDataComponents.CHARGE.get(),((thaumcraft.api.items.IRechargable)boots.getItem()).getMaxCharge(boots,null));pedestal.setInventorySlotContents(0,boots);
                // A full item remains stable while the server starts and commands are dispatched.
                Files.writeString(Path.of("secondary-ready"),"restart fixture prepared");
            } finally { aura.setVis(oldVis); }
        }
        Files.writeString(Path.of(resume?"engine-secondary-resume.json":"engine-secondary.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(checks));
    }
}
