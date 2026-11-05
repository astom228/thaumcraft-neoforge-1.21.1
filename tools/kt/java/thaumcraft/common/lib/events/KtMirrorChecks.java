package thaumcraft.common.lib.events;

import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.devices.TileMirror;

/** Реальные предметы, меню и разрушение зеркал; без графического клиента. */
final class KtMirrorChecks {
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static int queued(TileMirror mirror, net.minecraft.server.level.ServerLevel world){
        var list=mirror.saveWithFullMetadata(world.registryAccess()).getList("Items",Tag.TAG_COMPOUND);int count=0;
        for(var entry:list) count+=ItemStack.parseOptional(world.registryAccess(),(net.minecraft.nbt.CompoundTag)entry).getCount();
        return count;
    }
    private static void clearQueue(TileMirror mirror, net.minecraft.server.level.ServerLevel world){
        var data=mirror.saveWithFullMetadata(world.registryAccess());data.put("Items",new ListTag());mirror.loadWithComponents(data,world.registryAccess());
    }
    static void run(net.minecraft.server.MinecraftServer server)throws java.io.IOException{
        var world=server.overworld();var aPos=new BlockPos(140,122,140);var bPos=aPos.east(2);
        require(world.hasChunkAt(aPos)&&world.hasChunkAt(bPos),"Mirror fixtures must be loaded explicitly");
        var player=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("d99154de-9fb0-49c1-b7f9-e3eddb986735"),"KtMirrors"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getInventory().clearContent();player.getInventory().selected=0;
        var area=new AABB(138,120,138,147,128,147);
        var initial=world.getEntitiesOfClass(ItemEntity.class,area).stream().map(ItemEntity::getId).collect(java.util.stream.Collectors.toSet());
        var checks=new ArrayList<java.util.Map<String,Object>>();
        java.util.function.BiConsumer<String,Runnable> check=(name,action)->{var r=new LinkedHashMap<String,Object>();r.put("name",name);
            try{action.run();r.put("passed",true);}catch(Exception|AssertionError error){r.put("passed",false);r.put("error",error.toString());}checks.add(r);};
        java.util.function.IntSupplier droppedDiamonds=()->world.getEntitiesOfClass(ItemEntity.class,area).stream()
                .filter(e->!initial.contains(e.getId())&&e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        try{
            for(var pos:new BlockPos[]{aPos,bPos})world.setBlockAndUpdate(pos.north(),Blocks.STONE.defaultBlockState());
            world.setBlockAndUpdate(aPos,BlocksTC.mirror.get().defaultBlockState().setValue(thaumcraft.common.blocks.devices.BlockMirror.FACING,Direction.SOUTH));
            var hit=new BlockHitResult(Vec3.atCenterOf(aPos),Direction.SOUTH,aPos,false);
            var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit);
            check.accept("mirror rejects linking a different mirror kind",()->{
                var wrong=new ItemStack(BlocksTC.mirrorEssentia.get());player.setItemInHand(InteractionHand.MAIN_HAND,wrong);
                wrong.getItem().onItemUseFirst(wrong,context);require(wrong.getCount()==1&&!wrong.has(TCDataComponents.MIRROR_LINK.get()),"foreign mirror linked or consumed");
            });
            var bound=new ItemStack[1];
            check.accept("mirror item binding conserves count and actual placement restores pair",()->{
                player.getInventory().clearContent();var mirrors=new ItemStack(BlocksTC.mirror.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,mirrors);
                mirrors.getItem().onItemUseFirst(mirrors,context);
                require(player.getInventory().countItem(BlocksTC.mirror.get().asItem())==2,"binding duplicated or consumed total count");
                bound[0]=player.getInventory().items.stream().filter(s->s.has(TCDataComponents.MIRROR_LINK.get())).findFirst().orElseThrow().copy();
                require(bound[0].get(TCDataComponents.MIRROR_LINK.get()).equals(GlobalPos.of(world.dimension(),aPos)),"wrong binding destination");
                player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,bound[0]);
                bound[0].getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(bPos.north()).add(0,0,0.5),Direction.SOUTH,bPos.north(),false)));
                require(world.getBlockEntity(bPos) instanceof TileMirror,"bound item did not place");
                var a=(TileMirror)world.getBlockEntity(aPos);var b=(TileMirror)world.getBlockEntity(bPos);b.serverTick();
                require(a.linked&&b.linked&&a.linkPos.equals(bPos)&&b.linkPos.equals(aPos),"pair not reciprocal");
            });
            check.accept("breaking linked mirror drops bound item and closes partner",()->{
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
                player.moveTo(bPos.getX()+0.5,bPos.getY(),bPos.getZ()+2,180,0);
                player.gameMode.destroyBlock(bPos);
                require(!((TileMirror)world.getBlockEntity(aPos)).linked,"remaining partner stayed linked");
                require(world.getEntitiesOfClass(ItemEntity.class,area).stream().anyMatch(e->!initial.contains(e.getId())
                        &&e.getItem().is(BlocksTC.mirror.get().asItem())&&GlobalPos.of(world.dimension(),aPos).equals(e.getItem().get(TCDataComponents.MIRROR_LINK.get()))),"bound mirror loot lost link");
            });
            var hand=new ItemStack(ItemsTC.handMirror.get());player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,hand);
            hand.getItem().onItemUseFirst(hand,context);
            var menu=new thaumcraft.common.container.ContainerHandMirror(110,player.getInventory());
            check.accept("hand mirror links by actual item use and quick-move transfers once",()->{
                require(GlobalPos.of(world.dimension(),aPos).equals(hand.get(TCDataComponents.MIRROR_LINK.get())),"hand mirror not linked");
                player.getInventory().setItem(9,new ItemStack(Items.DIAMOND,8));menu.clicked(1,0,ClickType.QUICK_MOVE,player);
                require(player.getInventory().getItem(9).isEmpty()&&menu.getSlot(0).getItem().isEmpty(),"input retained");
                require(queued((TileMirror)world.getBlockEntity(aPos),world)==8,"transfer not exactly eight");
                menu.clicked(1,0,ClickType.QUICK_MOVE,player);require(queued((TileMirror)world.getBlockEntity(aPos),world)==8,"empty second click duplicated");
            });
            check.accept("hand mirror cannot move itself and rejects malformed swap button",()->{
                menu.clicked(28,0,ClickType.PICKUP,player);menu.clicked(0,0,ClickType.SWAP,player);
                require(player.getMainHandItem()==hand&&menu.getCarried().isEmpty(),"hand mirror moved into its own menu");
                menu.clicked(0,-1,ClickType.SWAP,player);menu.clicked(0,9999,ClickType.SWAP,player);
                require(player.getMainHandItem()==hand,"malformed button changed mirror");
            });
            check.accept("breaking receiving mirror preserves pending items",()->{
                int before=droppedDiamonds.getAsInt();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
                player.gameMode.destroyBlock(aPos);require(droppedDiamonds.getAsInt()-before==8,"queued diamonds lost when receiver broke");
                player.setItemInHand(InteractionHand.MAIN_HAND,hand);
            });
            check.accept("failed hand mirror transfer survives closing menu",()->{
                int before=droppedDiamonds.getAsInt();menu.getSlot(0).set(new ItemStack(Items.DIAMOND,5));
                require(menu.getSlot(0).getItem().getCount()==5&&!hand.has(TCDataComponents.MIRROR_LINK.get()),"broken link consumed input or stayed linked");
                menu.removed(player);
                require(droppedDiamonds.getAsInt()-before+player.getInventory().countItem(Items.DIAMOND)==5,"closing failed transfer lost items");
            });
            check.accept("hand mirror menu transfers across dimensions exactly once",()->{
                var nether=server.getLevel(net.minecraft.world.level.Level.NETHER);var remotePos=new BlockPos(12,122,-10);
                require(nether!=null&&nether.hasChunkAt(remotePos),"Nether fixture must be explicitly loaded");
                var old=nether.getBlockState(remotePos);var support=nether.getBlockState(remotePos.north());
                nether.setBlockAndUpdate(remotePos.north(),Blocks.STONE.defaultBlockState());
                nether.setBlockAndUpdate(remotePos,BlocksTC.mirror.get().defaultBlockState().setValue(thaumcraft.common.blocks.devices.BlockMirror.FACING,Direction.SOUTH));
                var remotePlayer=FakePlayerFactory.get(nether,new GameProfile(UUID.fromString("9a468d57-615e-49f3-817a-3a2620fd13e6"),"KtMirrorNether"));
                try{
                    var remoteHand=new ItemStack(ItemsTC.handMirror.get());remotePlayer.setItemInHand(InteractionHand.MAIN_HAND,remoteHand);
                    remoteHand.getItem().onItemUseFirst(remoteHand,new UseOnContext(remotePlayer,InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(remotePos),Direction.SOUTH,remotePos,false)));
                    require(GlobalPos.of(nether.dimension(),remotePos).equals(remoteHand.get(TCDataComponents.MIRROR_LINK.get())),"remote binding wrong");
                    player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,remoteHand.copy());remotePlayer.getInventory().clearContent();
                    var cross=new thaumcraft.common.container.ContainerHandMirror(111,player.getInventory());
                    player.getInventory().setItem(9,new ItemStack(Items.EMERALD,5));cross.clicked(1,0,ClickType.QUICK_MOVE,player);
                    require(player.getInventory().countItem(Items.EMERALD)==0&&queued((TileMirror)nether.getBlockEntity(remotePos),nether)==5,"cross-dimensional menu quantity wrong");
                    cross.removed(player);require(queued((TileMirror)nether.getBlockEntity(remotePos),nether)==5,"closing menu resent items");
                }finally{
                    clearQueue((TileMirror)nether.getBlockEntity(remotePos),nether);nether.setBlockAndUpdate(remotePos,old);
                    nether.setBlockAndUpdate(remotePos.north(),support);remotePlayer.getInventory().clearContent();player.getInventory().clearContent();
                }
            });
            world.setBlockAndUpdate(aPos,BlocksTC.mirror.get().defaultBlockState().setValue(thaumcraft.common.blocks.devices.BlockMirror.FACING,Direction.SOUTH));
            var receiver=(TileMirror)world.getBlockEntity(aPos);
            check.accept("mirror merges equivalent pending stackable items",()->{
                for(int i=0;i<64;i++)require(receiver.transportDirect(new ItemStack(Items.DIRT)),"small queue rejected");
                require(queued(receiver,world)==64&&receiver.saveWithFullMetadata(world.registryAccess()).getList("Items",Tag.TAG_COMPOUND).size()==1,"pending items not merged");
            });
            clearQueue(receiver,world);
            check.accept("mirror queue backpressure keeps overflow with sender",()->{
                for(int i=0;i<64;i++)require(receiver.transportDirect(new ItemStack(Items.DIRT,64)),"queue rejected before capacity");
                require(queued(receiver,world)==4096,"wrong queue capacity fixture");
                require(!receiver.transportDirect(new ItemStack(Items.DIRT))&&queued(receiver,world)==4096,"overflow accepted beyond bounded queue");
            });
            world.setBlockAndUpdate(bPos,BlocksTC.mirror.get().defaultBlockState().setValue(thaumcraft.common.blocks.devices.BlockMirror.FACING,Direction.SOUTH));
            var sender=(TileMirror)world.getBlockEntity(bPos);sender.linked=receiver.linked=true;sender.linkPos=aPos;receiver.linkPos=bPos;
            sender.linkDim=receiver.linkDim=world.dimension();
            check.accept("full mirror handler simulation and insertion preserve overflow",()->{
                var incoming=new ItemStack(Items.DIRT);int before=queued(receiver,world);
                require(ItemStack.matches(sender.getItemHandler().insertItem(0,incoming,true),incoming),"simulation accepted overflow");
                require(ItemStack.matches(sender.getItemHandler().insertItem(0,incoming,false),incoming),"insertion consumed overflow");
                require(queued(receiver,world)==before&&incoming.getCount()==1,"overflow mutated quantity");
            });
            check.accept("full mirror refuses entity transfer without discarding entity",()->{
                var entity=new ItemEntity(world,bPos.getX()+0.5,bPos.getY(),bPos.getZ()+0.5,new ItemStack(Items.DIRT));world.addFreshEntity(entity);
                int before=queued(receiver,world);require(!sender.transport(entity)&&entity.isAlive()&&entity.getItem().getCount()==1,"overflow entity consumed");
                require(queued(receiver,world)==before,"overflow changed receiver");
            });
        }finally{
            for(var pos:new BlockPos[]{aPos,bPos}){
                if(world.getBlockEntity(pos)instanceof TileMirror mirror)clearQueue(mirror,world);
                world.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());world.setBlockAndUpdate(pos.north(),Blocks.AIR.defaultBlockState());
            }
            for(var entity:world.getEntitiesOfClass(ItemEntity.class,area))if(!initial.contains(entity.getId()))entity.discard();
            player.getInventory().clearContent();
        }
        Files.writeString(Path.of("engine-mirrors.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(checks));
    }
}
