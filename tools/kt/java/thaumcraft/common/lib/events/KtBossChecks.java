package thaumcraft.common.lib.events;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.entities.monster.boss.*;

/** Ускоренные реальные тики/урон боссов, без изменения production-кода. */
final class KtBossChecks {
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static java.lang.reflect.Field field(Class<?> cls,String name){try{var f=cls.getDeclaredField(name);f.setAccessible(true);return f;}catch(Exception e){throw new RuntimeException(e);}}
    private static void set(Object obj,Class<?> cls,String name,Object value){try{field(cls,name).set(obj,value);}catch(Exception e){throw new RuntimeException(e);}}
    private static int value(Object obj,Class<?> cls,String name){try{return field(cls,name).getInt(obj);}catch(Exception e){throw new RuntimeException(e);}}
    static void run(net.minecraft.server.MinecraftServer server)throws java.io.IOException{
        var world=server.overworld();var pos=new BlockPos(168,121,168);require(world.hasChunkAt(pos),"Load boss arena first");
        var area=new AABB(150,115,150,190,145,190);var initial=world.getEntitiesOfClass(Entity.class,area).stream().map(Entity::getId).collect(java.util.stream.Collectors.toSet());
        var checks=new ArrayList<java.util.Map<String,Object>>();java.util.function.BiConsumer<String,Runnable> check=(name,action)->{var r=new LinkedHashMap<String,Object>();r.put("name",name);try{action.run();r.put("passed",true);}catch(Exception|AssertionError e){r.put("passed",false);r.put("error",e.toString());}checks.add(r);};
        var warden=ConfigEntities.ELDRITCH_WARDEN.get().create(world);var golem=ConfigEntities.ELDRITCH_GOLEM.get().create(world);
        require(warden!=null&&golem!=null,"Boss registration missing");
        try{
            for(var boss:new EntityThaumcraftBoss[]{warden,golem}){boss.moveTo(pos.getX()+0.5,121,pos.getZ()+0.5,0,0);boss.finalizeSpawn(world,world.getCurrentDifficultyAt(pos),MobSpawnType.COMMAND,null);boss.setNoAi(true);world.addFreshEntity(boss);}
            check.accept("warden spawns with health shield and temporary invulnerability",()->{
                require(warden.getMaxHealth()==400&&warden.getAbsorptionAmount()==264&&warden.getSpawnTimer()==150,"wrong initial stats");float hp=warden.getHealth();require(!warden.hurt(world.damageSources().magic(),10)&&warden.getHealth()==hp,"spawn invulnerability failed");
            });
            check.accept("warden spawn timer ends and shield break triggers bounded frenzy",()->{
                for(int i=0;i<150;i++)world.tickNonPassenger(warden);require(warden.getSpawnTimer()==0,"spawn timer stuck");warden.setAbsorptionAmount(3);warden.invulnerableTime=0;
                require(warden.hurt(world.damageSources().magic(),5)&&warden.getAbsorptionAmount()==0&&value(warden,EntityEldritchWarden.class,"fieldFrenzyCounter")==150,"shield break did not trigger frenzy");
                float hp=warden.getHealth();require(!warden.hurt(world.damageSources().magic(),5)&&warden.getHealth()==hp,"frenzy invulnerability failed");
                for(int i=0;i<150;i++)world.tickNonPassenger(warden);require(value(warden,EntityEldritchWarden.class,"fieldFrenzyCounter")==0,"frenzy never ends");
            });
            check.accept("warden ranged attacks create actual projectiles",()->{
                var target=new net.minecraft.world.entity.monster.Zombie(world);target.moveTo(168.5,121,178.5,0,0);world.addFreshEntity(target);
                try{for(int i=0;i<12;i++)warden.performRangedAttack(target,1);require(!world.getEntitiesOfClass(thaumcraft.common.entities.projectile.EntityEldritchOrb.class,area).isEmpty(),"no warden projectiles");}finally{target.discard();}
            });
            check.accept("warden excessive hit is capped and enrages boss",()->{
                warden.setAbsorptionAmount(0);warden.setHealth(400);warden.invulnerableTime=0;warden.setAnger(0);warden.hurt(world.damageSources().magic(),200);
                require(warden.getHealth()>=365&&warden.getHealth()<400&&warden.getAnger()==200,"damage cap or anger failed");
            });
            check.accept("warden health shield title and home survive entity serialization",()->{
                warden.setAbsorptionAmount(12);var saved=new CompoundTag();warden.saveWithoutId(saved);var copy=ConfigEntities.ELDRITCH_WARDEN.get().create(world);copy.load(saved);
                require(copy.getHealth()==warden.getHealth()&&copy.getAbsorptionAmount()==12&&copy.getRestrictCenter().equals(warden.getRestrictCenter())&&copy.getName().getString().equals(warden.getName().getString()),"warden state changed on load");copy.discard();
            });
            check.accept("warden death drops one pearl and one rare bag",()->{
                long before=world.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ItemsTC.primordialPearl.get())).mapToLong(e->e.getItem().getCount()).sum();
                for(int i=0;i<100&&warden.isAlive();i++){warden.invulnerableTime=0;warden.setAbsorptionAmount(0);warden.hurt(world.damageSources().magic(),30);}
                require(!warden.isAlive(),"warden cannot be killed");long pearls=world.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ItemsTC.primordialPearl.get())).mapToLong(e->e.getItem().getCount()).sum();
                require(pearls-before==1&&world.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ItemsTC.lootBagRare.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"wrong boss rewards");
            });
            check.accept("golem lethal first phase triggers headless resurrection",()->{
                for(int i=0;i<100;i++)world.tickNonPassenger(golem);require(golem.getSpawnTimer()==0,"golem spawn timer stuck");golem.setHealth(10);golem.invulnerableTime=0;
                golem.hurt(world.damageSources().magic(),20);require(golem.isAlive()&&golem.isHeadless()&&golem.getSpawnTimer()==100,"headless phase missing");
                for(int i=0;i<100;i++)world.tickNonPassenger(golem);require(golem.getSpawnTimer()==0&&golem.getHealth()>10,"golem did not revive");
            });
            check.accept("headless golem state survives entity serialization",()->{
                var tag=new CompoundTag();golem.saveWithoutId(tag);var copy=ConfigEntities.ELDRITCH_GOLEM.get().create(world);copy.load(tag);require(copy.isHeadless()&&copy.getHealth()==golem.getHealth(),"headless phase lost");copy.discard();
            });
            check.accept("headless golem launches projectiles",()->{
                var target=new net.minecraft.world.entity.monster.Zombie(world);target.moveTo(168.5,121,178.5,0,0);world.addFreshEntity(target);
                try{set(golem,EntityEldritchGolem.class,"chargingBeam",false);set(golem,EntityEldritchGolem.class,"beamCharge",150);golem.performRangedAttack(target,1);
                    require(!world.getEntitiesOfClass(thaumcraft.common.entities.projectile.EntityGolemOrb.class,area).isEmpty(),"no golem projectile");}finally{target.discard();}
            });
            check.accept("golem second phase can die and grants second pearl",()->{
                for(int i=0;i<100&&golem.isAlive();i++){golem.invulnerableTime=0;golem.hurt(world.damageSources().magic(),30);}require(!golem.isAlive(),"golem cannot finish second phase");
                require(world.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ItemsTC.primordialPearl.get())).mapToInt(e->e.getItem().getCount()).sum()==2,"second boss reward missing");
            });
            check.accept("greater cult portal boss wave creates actual cultist leader",()->{
                var portal=ConfigEntities.CULTIST_PORTAL_GREATER.get().create(world);portal.moveTo(180.5,121,168.5,0,0);world.addFreshEntity(portal);
                try{var method=EntityCultistPortalGreater.class.getDeclaredMethod("spawnBoss");method.setAccessible(true);method.invoke(portal);
                    require(!world.getEntitiesOfClass(EntityCultistLeader.class,area).isEmpty(),"portal boss not spawned");var tag=new CompoundTag();portal.saveWithoutId(tag);var copy=ConfigEntities.CULTIST_PORTAL_GREATER.get().create(world);copy.load(tag);require(copy.getHealth()==portal.getHealth(),"portal save failed");copy.discard();
                }catch(ReflectiveOperationException e){throw new RuntimeException(e);}finally{portal.discard();}
            });
            check.accept("Outer Lands dimension is not registered in BETA17 port",()->{
                require(server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","outer_lands")))==null,"unexpected Outer Lands dimension");
            });
        }finally{for(var entity:world.getEntitiesOfClass(Entity.class,area))if(!initial.contains(entity.getId()))entity.discard();}
        Files.writeString(Path.of("engine-bosses.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(checks));
    }
}
