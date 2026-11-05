package thaumcraft.common.lib.events;

import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.common.container.ContainerFocusPouch;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.casters.ItemFocusPouch;
import thaumcraft.common.items.casters.foci.FocusEffectFire;
import thaumcraft.common.items.casters.foci.FocusMediumBolt;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.tiles.crafting.FocusElementNode;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;
import thaumcraft.common.world.aura.AuraHandler;

/** Повседневный путь игрока: сканирование, изготовление фокуса, сумка, экипировка. */
final class KtEverydayChecks {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static HashMap<Integer,FocusElementNode> tree(){
        var map=new HashMap<Integer,FocusElementNode>();
        var nodes=new thaumcraft.api.casters.FocusNode[]{new FocusMediumRoot(),new FocusMediumBolt(),new FocusEffectFire()};
        for(int i=0;i<nodes.length;i++){nodes[i].initialize();var n=new FocusElementNode();n.node=nodes[i];n.id=i;n.parent=i-1;
            n.children=i<2?new int[]{i+1}:new int[0];map.put(i,n);}return map;
    }
    private static int observations(net.minecraft.world.entity.player.Player p){int count=0;
        for(var c:thaumcraft.api.research.ResearchCategories.researchCategories.values())count+=ThaumcraftCapabilities.getKnowledge(p).getKnowledgeRaw(thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType.OBSERVATION,c);return count;
    }
    static void run(net.minecraft.server.MinecraftServer server)throws java.io.IOException{
        var world=server.overworld();var pos=new BlockPos(136,122,138);require(world.hasChunkAt(pos),"Load isolated fixture first");
        // Default NeoForge FakePlayer is invulnerable to every damage source.
        var p=new net.neoforged.neoforge.common.util.FakePlayer(world,new GameProfile(UUID.fromString("e6ad9c26-9d1f-47df-9131-97c667587d9f"),"KtEveryday")){
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().selected=0;
        p.moveTo(136.5,121,134.5,0,0);p.setYHeadRot(0);p.yHeadRotO=0;p.yRotO=0;p.xRotO=0;p.tickCount=20;
        var knowledge=ThaumcraftCapabilities.getKnowledge(p);knowledge.clear();
        var checks=new ArrayList<java.util.Map<String,Object>>();
        java.util.function.BiConsumer<String,Runnable> check=(name,action)->{var r=new LinkedHashMap<String,Object>();r.put("name",name);
            try{action.run();r.put("passed",true);}catch(Exception|AssertionError e){r.put("passed",false);r.put("error",e.toString());}checks.add(r);};
        var aura=AuraHandler.getAuraChunk(world,pos.getX()>>4,pos.getZ()>>4);float oldVis=aura.getVis();
        var crafted=new ItemStack[]{ItemStack.EMPTY};
        try{
            world.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            check.accept("thaumometer real ray scan unlocks stone and grants observations",()->{
                int before=observations(p);((thaumcraft.common.items.tools.ItemThaumometer)ItemsTC.thaumometer.get()).doScan(world,p);
                require(knowledge.isResearchKnown("!minecraft:stone")&&observations(p)>before,"stone ray scan did not grant knowledge");
            });
            check.accept("repeat scanning does not farm observations",()->{
                int before=observations(p);((thaumcraft.common.items.tools.ItemThaumometer)ItemsTC.thaumometer.get()).doScan(world,p);
                require(observations(p)==before,"repeat scan awarded observations");
            });
            check.accept("container scan discovers contents without consuming them",()->{
                world.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)world.getBlockEntity(pos);
                chest.setItem(0,new ItemStack(Items.DIAMOND,3));ScanningManager.scanTheThing(p,pos);
                require(knowledge.isResearchKnown("!minecraft:diamond")&&chest.getItem(0).getCount()==3,"container scan lost items or missed contents");
                chest.clearContent();world.removeBlock(pos,false);
            });
            world.setBlockAndUpdate(pos,BlocksTC.wandWorkbench.get().defaultBlockState());var table=(TileFocalManipulator)world.getBlockEntity(pos);
            table.setInventorySlotContents(0,new ItemStack(ItemsTC.focus1.get()));table.data=tree();table.focusName="КТ Огонь";
            var crystals=new AspectList();for(var n:table.data.values())if(n.node.getAspect()!=null)crystals.add(n.node.getAspect(),1);
            Runnable supply=()->{p.getInventory().clearContent();int slot=1;for(var as:crystals.getAspects())p.getInventory().setItem(slot++,ThaumcraftApiHelper.makeCrystal(as,crystals.getAmount(as)*2));};
            supply.run();p.experienceLevel=8;
            check.accept("manipulator rejects missing research without payment",()->{
                require(!table.startCraft(0,p)&&p.experienceLevel==8&&table.vis==0,"missing research accepted or charged");
            });
            for(var n:table.data.values()){var key=n.node.getResearch();knowledge.addResearch(key);var entry=thaumcraft.api.research.ResearchCategories.getResearch(key);knowledge.setResearchStage(key,entry==null||entry.getStages()==null?100:entry.getStages().length+1);}
            check.accept("manipulator missing crystals preserves XP and blank focus",()->{
                p.getInventory().clearContent();require(!table.startCraft(0,p)&&p.experienceLevel==8&&ItemFocus.getPackage(table.getStackInSlot(0))==null,"missing crystals charged or crafted");supply.run();
            });
            check.accept("manipulator missing XP preserves crystals",()->{
                p.experienceLevel=0;int before=p.getInventory().countItem(ItemsTC.crystalEssence.get());require(!table.startCraft(0,p)&&p.getInventory().countItem(ItemsTC.crystalEssence.get())==before,"missing XP consumed crystals");p.experienceLevel=8;
            });
            check.accept("manipulator valid start pays exact XP and aspect crystals once",()->{
                int before=p.getInventory().countItem(ItemsTC.crystalEssence.get());require(table.startCraft(0,p),"valid focus did not start");
                require(p.experienceLevel==7&&p.getInventory().countItem(ItemsTC.crystalEssence.get())==before-crystals.visSize()&&table.vis>0,"wrong XP or crystal payment");
                int remaining=p.getInventory().countItem(ItemsTC.crystalEssence.get());require(!table.startCraft(0,p)&&p.experienceLevel==7&&p.getInventory().countItem(ItemsTC.crystalEssence.get())==remaining,"repeated start charged again");
            });
            check.accept("manipulator zero aura pauses without completing",()->{
                float cost=table.vis;require(cost>0,"start failed");aura.setVis(0);for(int i=0;i<20;i++)TileFocalManipulator.tick(world,pos,table.getBlockState(),table);
                require(table.vis==cost&&ItemFocus.getPackage(table.getStackInSlot(0))==null,"free progress or result without aura");
            });
            check.accept("manipulator real ticks finish named usable focus with exact vis payment",()->{
                float cost=table.vis;require(cost>0,"no craft pending");aura.setVis(1000);
                for(int i=0;i<2000&&table.vis>0;i++)TileFocalManipulator.tick(world,pos,table.getBlockState(),table);
                crafted[0]=table.getStackInSlot(0).copy();var pack=ItemFocus.getPackage(crafted[0]);
                require(table.vis==0&&pack!=null&&pack.nodes.size()==3&&crafted[0].getHoverName().getString().equals("КТ Огонь"),"wrong crafted focus");
                require(Math.abs((1000-aura.getVis())-cost)<0.001&&p.experienceLevel==7,"wrong vis cost or XP charged twice");
            });
            check.accept("crafted focus survives item serialization",()->{
                var restored=ItemStack.parseOptional(world.registryAccess(),(net.minecraft.nbt.CompoundTag)crafted[0].save(world.registryAccess()));require(ItemStack.matches(restored,crafted[0])&&ItemFocus.getPackage(restored).nodes.size()==3,"focus serialization changed spell");
            });
            check.accept("focus crafted by manipulator casts actual bolt-fire and pays aura",()->{
                p.getInventory().clearContent();p.moveTo(136.5,121,134.5,0,0);p.setYHeadRot(0);p.yHeadRotO=0;p.yRotO=0;p.xRotO=0;
                // The target must be before the manipulator at z138, which correctly stops the bolt.
                var target=new net.minecraft.world.entity.monster.Zombie(world);target.moveTo(136.5,121,136.5,0,0);target.setNoAi(true);world.addFreshEntity(target);
                try{var glove=new ItemStack(ItemsTC.casterBasic.get());var caster=(thaumcraft.common.items.casters.ItemCaster)glove.getItem();caster.setFocus(glove,crafted[0]);p.setItemInHand(InteractionHand.MAIN_HAND,glove);
                    CasterManager.setCooldown(p,0);aura.setVis(100);float cost=((ItemFocus)crafted[0].getItem()).getVisCost(crafted[0])*caster.getConsumptionModifier(glove,p,false);
                    caster.use(world,p,InteractionHand.MAIN_HAND);require(target.getHealth()<20&&Math.abs(aura.getVis()-(100-cost))<0.001,"crafted spell failed or incorrect payment");
                }finally{target.discard();CasterManager.setCooldown(p,0);}
            });
            check.accept("paid manipulator craft survives fresh block entity load and completes without second XP payment",()->{
                table.setInventorySlotContents(0,new ItemStack(ItemsTC.focus1.get()));table.data=tree();table.focusName="КТ Загрузка";supply.run();p.experienceLevel=8;
                require(table.startCraft(0,p),"second prepared craft refused");float remaining=table.vis;aura.setVis(0);
                for(int i=0;i<20;i++)TileFocalManipulator.tick(world,pos,table.getBlockState(),table);require(table.vis==remaining,"empty aura progressed");
                var saved=table.saveWithFullMetadata(world.registryAccess());var restored=new TileFocalManipulator(pos,table.getBlockState());restored.setLevel(world);restored.loadWithComponents(saved,world.registryAccess());world.setBlockEntity(restored);
                require(restored.vis==remaining&&restored.data.size()==3&&p.experienceLevel==7,"pending paid craft changed on load");aura.setVis(1000);
                for(int i=0;i<2000&&restored.vis>0;i++)TileFocalManipulator.tick(world,pos,restored.getBlockState(),restored);
                require(restored.vis==0&&ItemFocus.getPackage(restored.getStackInSlot(0))!=null&&restored.getStackInSlot(0).getHoverName().getString().equals("КТ Загрузка")&&p.experienceLevel==7&&Math.abs((1000-aura.getVis())-remaining)<0.001,"loaded craft failed or charged twice");
            });
            p.getInventory().clearContent();var pouch=new ItemStack(ItemsTC.focusPouch.get());p.getInventory().setItem(0,pouch);p.getInventory().setItem(1,crafted[0].copy());
            var menu=new ContainerFocusPouch(96,p.getInventory(),0);
            check.accept("pouch quick-move stores focus and immediately persists contents",()->{
                menu.clicked(46,0,ClickType.QUICK_MOVE,p);require(p.getInventory().getItem(1).isEmpty()&&((ItemFocusPouch)pouch.getItem()).getInventory(pouch).get(0).is(crafted[0].getItem()),"focus lost or not persisted");
            });
            check.accept("pouch refuses ordinary items and moving its own slot",()->{
                p.getInventory().setItem(2,new ItemStack(Items.DIAMOND,3));menu.clicked(47,0,ClickType.QUICK_MOVE,p);menu.clicked(45,0,ClickType.PICKUP,p);menu.clicked(0,0,ClickType.SWAP,p);
                require(p.getInventory().getItem(0)==pouch&&p.getInventory().getItem(2).getCount()==3&&menu.getCarried().isEmpty(),"pouch moved or ordinary item lost");
            });
            check.accept("pouch reopens with exact stored spell and quick-move returns it once",()->{
                menu.removed(p);var reopened=new ContainerFocusPouch(97,p.getInventory(),0);require(ItemStack.matches(reopened.input.getItem(0),crafted[0]),"stored spell changed");
                reopened.clicked(0,0,ClickType.QUICK_MOVE,p);require(reopened.input.isEmpty()&&((ItemFocusPouch)pouch.getItem()).getInventory(pouch).stream().allMatch(ItemStack::isEmpty)&&p.getInventory().countItem(crafted[0].getItem())==1,"extraction lost or duplicated focus");reopened.removed(p);
            });
            check.accept("caster equips swaps and removes foci from inventory pouch without loss",()->{
                p.getInventory().clearContent();var glove=new ItemStack(ItemsTC.casterBasic.get());p.setItemInHand(InteractionHand.MAIN_HAND,glove);
                var holder=(thaumcraft.common.items.casters.ItemCaster)glove.getItem();p.getInventory().setItem(1,pouch);var list=NonNullList.withSize(ItemFocusPouch.SIZE,ItemStack.EMPTY);
                var second=crafted[0].copy();second.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("КТ Второй"));list.set(0,crafted[0].copy());list.set(1,second);
                ((ItemFocusPouch)pouch.getItem()).setInventory(pouch,list);String firstKey=((ItemFocus)crafted[0].getItem()).getSortingHelper(crafted[0]);String secondKey=((ItemFocus)second.getItem()).getSortingHelper(second);
                CasterManager.changeFocus(glove,world,p,firstKey);require(ItemStack.matches(holder.getFocusStack(glove),crafted[0]),"first focus not equipped");
                CasterManager.changeFocus(glove,world,p,secondKey);require(ItemStack.matches(holder.getFocusStack(glove),second),"focus swap failed");
                CasterManager.changeFocus(glove,world,p,"REMOVE");require(holder.getFocus(glove)==null,"focus not removed");
                var stored=((ItemFocusPouch)pouch.getItem()).getInventory(pouch);require(stored.stream().filter(s->!s.isEmpty()).count()==2&&stored.stream().anyMatch(s->ItemStack.matches(s,crafted[0]))&&stored.stream().anyMatch(s->ItemStack.matches(s,second)),"swap lost or duplicated spells");
            });
            check.accept("goggles discount applies only while worn",()->{
                p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ItemsTC.goggles.get()));require(Math.abs(CasterManager.getTotalVisDiscount(p)-0.05)<0.001,"missing goggles discount");p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);require(CasterManager.getTotalVisDiscount(p)==0,"discount stuck after removal");
            });
            check.accept("Curios belt pouch supplies and receives focus without loss",()->{
                p.getInventory().clearContent();var curios=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow();
                var belt=curios.getStacksHandler("belt").orElseThrow().getStacks();require(belt.getSlots()>0,"belt slot absent");
                var equipped=new ItemStack(ItemsTC.focusPouch.get());var contents=NonNullList.withSize(ItemFocusPouch.SIZE,ItemStack.EMPTY);contents.set(0,crafted[0].copy());
                ((ItemFocusPouch)equipped.getItem()).setInventory(equipped,contents);belt.setStackInSlot(0,equipped);
                var glove=new ItemStack(ItemsTC.casterBasic.get());p.setItemInHand(InteractionHand.MAIN_HAND,glove);var caster=(thaumcraft.common.items.casters.ItemCaster)glove.getItem();
                CasterManager.changeFocus(glove,world,p,((ItemFocus)crafted[0].getItem()).getSortingHelper(crafted[0]));require(ItemStack.matches(caster.getFocusStack(glove),crafted[0]),"belt focus not equipped");
                CasterManager.changeFocus(glove,world,p,"REMOVE");var stored=((ItemFocusPouch)equipped.getItem()).getInventory(belt.getStackInSlot(0));
                require(caster.getFocus(glove)==null&&stored.stream().filter(s->!s.isEmpty()).count()==1&&ItemStack.matches(stored.get(0),crafted[0]),"belt focus lost or duplicated");belt.setStackInSlot(0,ItemStack.EMPTY);
            });
            check.accept("runic armor charges once for configured vis and absorbs real damage",()->{
                p.setHealth(20);p.setAbsorptionAmount(0);p.invulnerableTime=0;var armor=new ItemStack(Items.LEATHER_CHESTPLATE);armor.set(TCDataComponents.RUNIC_CHARGE.get(),3);p.setItemSlot(EquipmentSlot.CHEST,armor);p.tickCount=20;
                var shield=p.getData(thaumcraft.common.lib.capabilities.ThaumcraftAttachments.RUNIC);shield.nextCycle=-1;shield.lastCharge=-1;
                aura.setVis(100);PlayerEvents.livingTick(new PlayerTickEvent.Post(p));int cost=thaumcraft.common.config.ModConfig.CONFIG_MISC.shieldCost.get();
                require(p.getAbsorptionAmount()==1&&aura.getVis()==100-cost&&shield.max==3,"runic charge or payment failed");
                PlayerEvents.livingTick(new PlayerTickEvent.Post(p));require(p.getAbsorptionAmount()==1&&aura.getVis()==100-cost,"runic cooldown bypassed");
                // ServerPlayer also has a separate 60-tick spawn protection; disable only in this fixture.
                try{var field=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(p,0);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
                p.hurt(world.damageSources().magic(),1);require(p.getAbsorptionAmount()==0&&p.getHealth()==20,"runic shield failed to absorb damage: absorption="+p.getAbsorptionAmount()+", health="+p.getHealth());
            });
            check.accept("runic shield refuses empty aura and clears after armor removal",()->{
                aura.setVis(0);var shield=p.getData(thaumcraft.common.lib.capabilities.ThaumcraftAttachments.RUNIC);shield.nextCycle=-1;shield.lastCharge=0;
                PlayerEvents.livingTick(new PlayerTickEvent.Post(p));require(p.getAbsorptionAmount()==0,"shield charged for free");p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
                PlayerEvents.livingTick(new PlayerTickEvent.Post(p));require(!PlayerEvents.hasRunicShield(p)&&shield.max==0&&p.getAbsorptionAmount()==0,"shield stuck after armor removal");
            });
        }finally{aura.setVis(oldVis);p.getInventory().clearContent();for(var slot:EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);p.setAbsorptionAmount(0);world.removeBlock(pos,false);}
        Files.writeString(Path.of("engine-everyday.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(checks));
    }
}
