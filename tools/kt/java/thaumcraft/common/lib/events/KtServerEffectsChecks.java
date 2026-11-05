package thaumcraft.common.lib.events;

import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

/** Только -PtcAcceptance: реальные серверные добыча, события атаки и loot tables. */
final class KtServerEffectsChecks {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void run(net.minecraft.server.MinecraftServer server) throws java.io.IOException {
        var world = server.overworld();
        var pos = new BlockPos(136, 123, 136);
        require(world.hasChunkAt(pos), "Effects fixture must already be loaded");
        var player = FakePlayerFactory.get(world, new GameProfile(UUID.fromString("d6ad58a9-097f-49c5-9344-87ffea579fea"), "KtEffects"));
        player.setGameMode(GameType.SURVIVAL);
        var area = new AABB(132, 120, 132, 142, 132, 143);
        var initial = world.getEntitiesOfClass(Entity.class, area).stream().map(Entity::getId).collect(java.util.stream.Collectors.toSet());
        var checks = new ArrayList<java.util.Map<String,Object>>();
        var counts = new LinkedHashMap<String,Integer>();
        java.util.function.BiConsumer<String,Runnable> check = (name, action) -> {
            var result = new LinkedHashMap<String,Object>(); result.put("name", name);
            try { action.run(); result.put("passed", true); }
            catch (Exception | AssertionError error) { result.put("passed", false); result.put("error", error.toString()); }
            checks.add(result);
        };
        java.util.function.Supplier<java.util.List<ItemEntity>> drops = () -> world.getEntitiesOfClass(ItemEntity.class, area).stream()
                .filter(entity -> !initial.contains(entity.getId())).toList();
        Runnable cleanup = () -> {
            player.setShiftKeyDown(false); player.getInventory().clearContent();
            for (var entity : world.getEntitiesOfClass(Entity.class, area)) if (!initial.contains(entity.getId())
                    && (entity instanceof ItemEntity || entity instanceof Zombie || entity instanceof net.minecraft.world.entity.ExperienceOrb)) entity.discard();
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 4; y++) for (int z = -1; z <= 1; z++)
                world.setBlockAndUpdate(pos.offset(x,y,z), Blocks.AIR.defaultBlockState());
        };
        try {
            check.accept("caster actual bolt-fire cast pays vis, cooldown and empty aura reject",()->{
                cleanup.run();player.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);player.setYHeadRot(0);player.yHeadRotO=0;
                var target=new Zombie(world);target.setNoAi(true);target.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+5.5,180,0);world.addFreshEntity(target);
                var aura=thaumcraft.common.world.aura.AuraHandler.getAuraChunk(world,pos.getX()>>4,pos.getZ()>>4);
                float original=aura.getVis();
                try{
                    var pack=new thaumcraft.api.casters.FocusPackage(player);
                    var root=new thaumcraft.api.casters.FocusMediumRoot();var bolt=new thaumcraft.common.items.casters.foci.FocusMediumBolt();
                    var fire=new thaumcraft.common.items.casters.foci.FocusEffectFire();
                    pack.nodes.add(root);pack.nodes.add(bolt);pack.nodes.add(fire);pack.setComplexity(bolt.getComplexity()+fire.getComplexity());
                    var focus=new ItemStack(ItemsTC.focus1.get());thaumcraft.common.items.casters.ItemFocus.setPackage(focus,pack);
                    var glove=new ItemStack(ItemsTC.casterBasic.get());var caster=(thaumcraft.common.items.casters.ItemCaster)glove.getItem();caster.setFocus(glove,focus);
                    player.setItemInHand(InteractionHand.MAIN_HAND,glove);aura.setVis(100);
                    thaumcraft.common.items.casters.CasterManager.setCooldown(player,0);
                    float cost=((thaumcraft.common.items.casters.ItemFocus)focus.getItem()).getVisCost(focus)*caster.getConsumptionModifier(glove,player,false);
                    require(cost>0,"empty spell cost");caster.use(world,player,InteractionHand.MAIN_HAND);
                    require(target.getHealth()<20,"spell did not reach target");
                    require(Math.abs(aura.getVis()-(100-cost))<0.001,"wrong spell payment");
                    float paid=aura.getVis();caster.use(world,player,InteractionHand.MAIN_HAND);
                    require(aura.getVis()==paid,"cooldown allowed second payment");
                    thaumcraft.common.items.casters.CasterManager.setCooldown(player,0);aura.setVis(0);
                    require(caster.use(world,player,InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL,"empty aura did not reject spell");
                    require(aura.getVis()==0,"failed cast changed aura");
                }finally{aura.setVis(original);thaumcraft.common.items.casters.CasterManager.setCooldown(player,0);target.discard();}
            });
            check.accept("buffer pressure modes independent on all six sides with bellows",()->{
                cleanup.run();
                world.setBlockAndUpdate(pos,BlocksTC.tubeBuffer.get().defaultBlockState());
                world.setBlockAndUpdate(pos.below(),BlocksTC.bellows.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.UP));
                var buffer=(thaumcraft.common.tiles.essentia.TileTubeBuffer)world.getBlockEntity(pos);
                buffer.getBellows();
                for(var face:Direction.values()) {
                    require(buffer.getSuctionAmount(face)==32,"bellows pressure absent");
                    buffer.chokedSides[face.ordinal()]=1;
                    require(buffer.getSuctionAmount(face)==1,"restricted pressure wrong");
                    for(var other:Direction.values()) if(other!=face) require(buffer.getSuctionAmount(other)==32,"mode leaked to other side");
                    buffer.chokedSides[face.ordinal()]=2;
                    require(buffer.getSuctionAmount(face)==0,"closed pressure wrong");
                    buffer.chokedSides[face.ordinal()]=0;
                }
            });
            check.accept("valve closes under redstone and reopens after power removed",()->{
                cleanup.run();world.setBlockAndUpdate(pos,BlocksTC.tubeValve.get().defaultBlockState());
                var valve=(thaumcraft.common.tiles.essentia.TileTubeValve)world.getBlockEntity(pos);
                world.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
                for(int tick=0;tick<6;tick++)valve.update();
                require(!valve.allowFlow,"powered valve still open");
                world.setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
                for(int tick=0;tick<6;tick++)valve.update();
                require(valve.allowFlow,"unpowered valve stayed closed");
            });
            check.accept("smelter sided capabilities and lava bucket remainder",()->{
                cleanup.run();world.setBlockAndUpdate(pos,BlocksTC.smelterBasic.get().defaultBlockState());
                var smelter=(thaumcraft.common.tiles.essentia.TileSmelter)world.getBlockEntity(pos);
                require(smelter.getItemHandler(Direction.UP).getSlots()==0,"top accepts inventory");
                require(smelter.getItemHandler(Direction.DOWN).insertItem(0,new ItemStack(Items.COAL),false).isEmpty(),"bottom rejects fuel");
                require(smelter.getItemHandler(Direction.EAST).insertItem(0,new ItemStack(Items.OAK_LOG),false).isEmpty(),"side rejects input");
                require(smelter.getStackInSlot(1).is(Items.COAL)&&smelter.getStackInSlot(0).is(Items.OAK_LOG),"sides routed to wrong slots");
                smelter.setInventorySlotContents(1,new ItemStack(Items.LAVA_BUCKET));
                thaumcraft.common.tiles.essentia.TileSmelter.tick(world,pos,world.getBlockState(pos),smelter);
                require(smelter.furnaceBurnTime>0&&smelter.getStackInSlot(1).is(Items.BUCKET),"lava bucket remainder lost");
                smelter.setInventorySlotContents(0,ItemStack.EMPTY);smelter.setInventorySlotContents(1,ItemStack.EMPTY);
            });
            for (boolean ore : new boolean[]{false,true}) {
                check.accept("burrowing removes farthest " + (ore ? "ore without experience" : "log") + " and respects shift", () -> {
                    cleanup.run();
                    var block = ore ? Blocks.DIAMOND_ORE : Blocks.OAK_LOG;
                    var tool = new ItemStack(ore ? Items.DIAMOND_PICKAXE : Items.DIAMOND_AXE);
                    EnumInfusionEnchantment.addInfusionEnchantment(tool, EnumInfusionEnchantment.BURROWING, 1);
                    player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                    player.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+3,180,20);
                    for (int y=0;y<4;y++) world.setBlockAndUpdate(pos.above(y),block.defaultBlockState());
                    player.gameMode.destroyBlock(pos);
                    require(world.getBlockState(pos).is(block) && world.getBlockState(pos.above(3)).isAir(), "wrong end of chain mined");
                    require(tool.getDamageValue()==1,"wrong chain durability cost");
                    if (ore) require(world.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,area).stream()
                            .noneMatch(entity -> !initial.contains(entity.getId())),"farthest ore gave experience");
                    player.setShiftKeyDown(true); player.gameMode.destroyBlock(pos);
                    require(world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).is(block),"shift did not mine normally");
                });
            }
            for (int plane=0;plane<3;plane++) {
                final int mode=plane;
                check.accept("destructive plane " + mode + (mode==2 ? " shift single block" : " nine blocks"), () -> {
                    cleanup.run();
                    var tool=new ItemStack(Items.DIAMOND_PICKAXE);
                    EnumInfusionEnchantment.addInfusionEnchantment(tool,EnumInfusionEnchantment.DESTRUCTIVE,1);
                    player.setItemInHand(InteractionHand.MAIN_HAND,tool);
                    player.moveTo(pos.getX()+0.5,pos.getY()+(mode==1?0:1),pos.getZ()+(mode==1?3:0.5),180,30);
                    player.setShiftKeyDown(mode==2);
                    for(int a=-1;a<=1;a++) for(int b=-1;b<=1;b++) world.setBlockAndUpdate(pos.offset(a,mode==1?b:0,mode==1?0:b),Blocks.STONE.defaultBlockState());
                    player.gameMode.destroyBlock(pos);
                    int removed=0;
                    for(int a=-1;a<=1;a++) for(int b=-1;b<=1;b++) if(world.getBlockState(pos.offset(a,mode==1?b:0,mode==1?0:b)).isAir()) removed++;
                    require(removed==(mode==2?1:9),"wrong number of mined blocks: "+removed);
                    require(tool.getDamageValue()==removed,"durability not paid per block: "+tool.getDamageValue());
                });
            }
            check.accept("sounding real event costs five durability and shift suppresses it", () -> {
                cleanup.run();
                var tool=new ItemStack(Items.DIAMOND_PICKAXE);
                EnumInfusionEnchantment.addInfusionEnchantment(tool,EnumInfusionEnchantment.SOUNDING,1);
                player.setItemInHand(InteractionHand.MAIN_HAND,tool);
                var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,hit));
                require(tool.getDamageValue()==5,"wrong sounding cost");
                player.setShiftKeyDown(true);
                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,hit));
                require(tool.getDamageValue()==5,"shift sounding spent durability");
            });
            for(boolean shift:new boolean[]{false,true}) check.accept("collector mining destination; shift="+shift,()->{
                cleanup.run();
                var tool=new ItemStack(Items.DIAMOND_PICKAXE);
                EnumInfusionEnchantment.addInfusionEnchantment(tool,EnumInfusionEnchantment.COLLECTOR,1);
                player.setItemInHand(InteractionHand.MAIN_HAND,tool); player.setShiftKeyDown(shift);
                player.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+3,180,20);
                world.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState()); player.gameMode.destroyBlock(pos);
                var items=drops.get();
                require(items.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"wrong mined drop count");
                if(shift) require(items.stream().noneMatch(e->e instanceof thaumcraft.common.entities.EntityFollowingItem),"shift created following drop");
                else for(var item:items) {
                    require(item instanceof thaumcraft.common.entities.EntityFollowingItem following && following.target==player,"drop does not follow owner");
                    double before=item.distanceToSqr(player);
                    for(int tick=0;tick<10;tick++) world.tickNonPassenger(item);
                    require(item.distanceToSqr(player)<before,"following item did not approach owner");
                }
            });
            check.accept("arcing actual attack damages one neighboring hostile at level one",()->{
                cleanup.run();
                var sword=new ItemStack(Items.DIAMOND_SWORD);
                EnumInfusionEnchantment.addInfusionEnchantment(sword,EnumInfusionEnchantment.ARCING,1);
                player.setItemInHand(InteractionHand.MAIN_HAND,sword);
                player.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+3,180,0);
                var mobs=new ArrayList<Zombie>();
                for(int i=0;i<3;i++) {var mob=new Zombie(world);mob.setNoAi(true);mob.moveTo(pos.getX()+0.5+(i==1?1:0),pos.getY(),pos.getZ()+0.5+(i==2?1:0),0,0);world.addFreshEntity(mob);mobs.add(mob);}
                player.attack(mobs.get(0));
                require(mobs.get(0).getHealth()<20,"primary attack failed");
                require(mobs.subList(1,3).stream().filter(m->m.getHealth()<20).count()==1,"wrong arcing target count");
            });
            for(boolean collector:new boolean[]{false,true}) check.accept("essence level five actual kill; collector="+collector,()->{
                cleanup.run();
                var sword=new ItemStack(Items.DIAMOND_SWORD);
                EnumInfusionEnchantment.addInfusionEnchantment(sword,EnumInfusionEnchantment.ESSENCE,5);
                if(collector) EnumInfusionEnchantment.addInfusionEnchantment(sword,EnumInfusionEnchantment.COLLECTOR,1);
                player.setItemInHand(InteractionHand.MAIN_HAND,sword);
                var mob=new Zombie(world);mob.setNoAi(true);mob.moveTo(Vec3.atCenterOf(pos));world.addFreshEntity(mob);
                var aspects=AspectHelper.getEntityAspects(mob);
                require(aspects!=null&&aspects.size()>0,"zombie aspect fixture absent");
                mob.hurt(world.damageSources().playerAttack(player),100);
                var crystals=drops.get().stream().filter(e->e.getItem().is(ItemsTC.crystalEssence.get())).toList();
                int count=crystals.stream().mapToInt(e->e.getItem().getCount()).sum();
                require(count>0&&count<=5,"wrong essence crystal count: "+count);
                for(var crystal:crystals) {
                    var tags=crystal.getItem().get(TCDataComponents.ASPECTS.get());
                    require(tags!=null&&tags.size()==1&&aspects.getAmount(tags.getAspects()[0])>0,"foreign crystal aspect");
                    if(collector) require(crystal instanceof thaumcraft.common.entities.EntityFollowingItem,"collector crystal not following");
                }
            });
            for(boolean silk:new boolean[]{false,true}) check.accept("refining actual iron mining; silk="+silk,()->{
                cleanup.run();
                var tool=new ItemStack(Items.DIAMOND_PICKAXE);
                EnumInfusionEnchantment.addInfusionEnchantment(tool,EnumInfusionEnchantment.REFINING,4);
                if(silk) tool.enchant(world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH),1);
                player.setItemInHand(InteractionHand.MAIN_HAND,tool);
                for(int i=0;i<256;i++){world.setBlockAndUpdate(pos,Blocks.IRON_ORE.defaultBlockState());player.gameMode.destroyBlock(pos);}
                int clusters=drops.get().stream().filter(e->e.getItem().is(ItemsTC.clusterIron.get())).mapToInt(e->e.getItem().getCount()).sum();
                counts.put("refining_silk_"+silk,clusters);
                require(clusters>0&&clusters<256,"refining produced no variation: "+clusters);
            });
            for(var ore:new Block[]{Blocks.DIAMOND_ORE,Blocks.EMERALD_ORE,BlocksTC.oreAmber.get(),BlocksTC.oreQuartz.get(),Blocks.NETHER_QUARTZ_ORE}) {
                check.accept("rare earths registered loot and silk exclusion: "+ore,()->{
                    var tool=new ItemStack(Items.DIAMOND_PICKAXE);
                    int samples=ore==Blocks.NETHER_QUARTZ_ORE?2048:512;
                    int rare=0;
                    for(int i=0;i<samples;i++) for(var stack:Block.getDrops(ore.defaultBlockState(),world,pos,null,player,tool)) if(stack.is(ItemsTC.nuggetRareEarth.get())) rare+=stack.getCount();
                    counts.put(ore.toString(),rare);
                    require(rare>0&&rare<samples/4,"rare earth modifier absent or excessive: "+rare);
                    tool.enchant(world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH),1);
                    for(int i=0;i<128;i++) for(var stack:Block.getDrops(ore.defaultBlockState(),world,pos,null,player,tool)) require(!stack.is(ItemsTC.nuggetRareEarth.get()),"silk produced rare earth");
                });
            }
            check.accept("rare earth explosion loot context",()->{
                int rare=0;
                for(int i=0;i<512;i++) for(var stack:Blocks.DIAMOND_ORE.defaultBlockState().getDrops(new net.minecraft.world.level.storage.loot.LootParams.Builder(world)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,Vec3.atCenterOf(pos))
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,ItemStack.EMPTY)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.EXPLOSION_RADIUS,2f))) if(stack.is(ItemsTC.nuggetRareEarth.get())) rare+=stack.getCount();
                counts.put("explosion",rare);require(rare>0&&rare<128,"explosion context failed: "+rare);
            });
        } finally {cleanup.run();}
        var report=new LinkedHashMap<String,Object>(); report.put("kind","server game mode, event bus, loot tables; no client graphics");report.put("checks",checks);report.put("counts",counts);
        Files.writeString(Path.of("engine-effects.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
