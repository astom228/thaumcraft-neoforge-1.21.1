package thaumcraft.api.aspects;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.internal.CommonInternals;

/** Порт AspectHelper. */
public class AspectHelper {
    public static AspectList cullTags(AspectList temp) {
        return cullTags(temp, 7);
    }

    public static AspectList cullTags(AspectList temp, int cap) {
        AspectList temp2 = new AspectList();
        for (Aspect tag : temp.getAspects()) {
            if (tag != null) {
                temp2.add(tag, temp.getAmount(tag));
            }
        }
        while (temp2.size() > cap) {
            Aspect lowest = null;
            float low = 32767.0F;
            for (Aspect tag : temp2.getAspects()) {
                if (tag != null) {
                    float ta = temp2.getAmount(tag);
                    if (tag.isPrimal()) {
                        ta *= 0.9F;
                    } else {
                        if (!tag.getComponents()[0].isPrimal()) {
                            ta *= 1.1F;
                            if (!tag.getComponents()[0].getComponents()[0].isPrimal()) {
                                ta *= 1.05F;
                            }
                            if (!tag.getComponents()[0].getComponents()[1].isPrimal()) {
                                ta *= 1.05F;
                            }
                        }
                        if (!tag.getComponents()[1].isPrimal()) {
                            ta *= 1.1F;
                            if (!tag.getComponents()[1].getComponents()[0].isPrimal()) {
                                ta *= 1.05F;
                            }
                            if (!tag.getComponents()[1].getComponents()[1].isPrimal()) {
                                ta *= 1.05F;
                            }
                        }
                    }
                    if (ta < low) {
                        low = ta;
                        lowest = tag;
                    }
                }
            }
            temp2.aspects.remove(lowest);
        }
        return temp2;
    }

    public static AspectList getObjectAspects(ItemStack is) {
        return ThaumcraftApi.internalMethods.getObjectAspects(is);
    }

    public static AspectList generateTags(ItemStack is) {
        return ThaumcraftApi.internalMethods.generateTags(is);
    }

    /**
     * Аспекты существа. В оригинале существа задавались строковым именем EntityList (например "Zombie"),
     * в порте — id типа сущности 1.21 (minecraft:zombie). NBT-условия сравниваются по тому же ключу.
     */
    public static AspectList getEntityAspects(Entity entity) {
        AspectList tags = null;
        String entityString = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (entity instanceof Player player) {
            tags = new AspectList();
            tags.add(Aspect.MAN, 4);
            Random rand = new Random(player.getName().getString().hashCode());
            Aspect[] posa = Aspect.aspects.values().toArray(new Aspect[0]);
            tags.add(posa[rand.nextInt(posa.length)], 15);
            tags.add(posa[rand.nextInt(posa.length)], 15);
            tags.add(posa[rand.nextInt(posa.length)], 15);
        } else {
            outer:
            for (ThaumcraftApi.EntityTags et : CommonInternals.scanEntities) {
                if (et.entityName.equals(entityString)) {
                    if (et.nbts != null && et.nbts.length != 0) {
                        CompoundTag tc = new CompoundTag();
                        entity.saveWithoutId(tc);
                        for (ThaumcraftApi.EntityTagsNBT nbt : et.nbts) {
                            if (!tc.contains(nbt.name) || !String.valueOf(ThaumcraftApi.getNBTDataFromId(tc, nbt.name)).equals(String.valueOf(nbt.value))) {
                                continue outer;
                            }
                        }
                    }
                    tags = et.aspects;
                }
            }
        }
        return tags;
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

    public static Aspect getRandomPrimal(Random rand, Aspect aspect) {
        ArrayList<Aspect> list = new ArrayList<>();
        if (aspect != null) {
            AspectList temp = new AspectList();
            temp.add(aspect, 1);
            AspectList temp2 = reduceToPrimals(temp);
            for (Aspect a : temp2.getAspects()) {
                for (int b = 0; b < temp2.getAmount(a); b++) {
                    list.add(a);
                }
            }
        }
        return !list.isEmpty() ? list.get(rand.nextInt(list.size())) : null;
    }

    public static AspectList reduceToPrimals(AspectList in) {
        AspectList out = new AspectList();
        for (Aspect aspect : in.getAspects()) {
            if (aspect != null) {
                if (aspect.isPrimal()) {
                    out.add(aspect, in.getAmount(aspect));
                } else {
                    AspectList temp = new AspectList();
                    temp.add(aspect.getComponents()[0], in.getAmount(aspect));
                    temp.add(aspect.getComponents()[1], in.getAmount(aspect));
                    AspectList temp2 = reduceToPrimals(temp);
                    for (Aspect a : temp2.getAspects()) {
                        out.add(a, temp2.getAmount(a));
                    }
                }
            }
        }
        return out;
    }

    public static AspectList getPrimalAspects(AspectList in) {
        AspectList t = new AspectList();
        t.add(Aspect.AIR, in.getAmount(Aspect.AIR));
        t.add(Aspect.FIRE, in.getAmount(Aspect.FIRE));
        t.add(Aspect.WATER, in.getAmount(Aspect.WATER));
        t.add(Aspect.EARTH, in.getAmount(Aspect.EARTH));
        t.add(Aspect.ORDER, in.getAmount(Aspect.ORDER));
        t.add(Aspect.ENTROPY, in.getAmount(Aspect.ENTROPY));
        return t;
    }

    public static AspectList getAuraAspects(AspectList in) {
        AspectList t = getPrimalAspects(in);
        t.add(Aspect.FLUX, in.getAmount(Aspect.FLUX));
        return t;
    }
}
