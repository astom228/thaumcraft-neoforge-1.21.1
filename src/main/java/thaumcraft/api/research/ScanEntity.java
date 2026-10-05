package thaumcraft.api.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.ThaumcraftApi;

/** Сущности класса entityClass (inheritedClasses — и подклассы), при необходимости с заданными значениями NBT. */
public class ScanEntity implements IScanThing {
    String research;
    Class<?> entityClass;
    ThaumcraftApi.EntityTagsNBT[] NBTData;
    boolean inheritedClasses = false;

    public ScanEntity(String research, Class<?> entityClass, boolean inheritedClasses) {
        this.research = research;
        this.entityClass = entityClass;
        this.inheritedClasses = inheritedClasses;
    }

    public ScanEntity(String research, Class<?> entityClass, boolean inheritedClasses, ThaumcraftApi.EntityTagsNBT... nbt) {
        this.research = research;
        this.entityClass = entityClass;
        this.inheritedClasses = inheritedClasses;
        this.NBTData = nbt;
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj != null && (!this.inheritedClasses && this.entityClass == obj.getClass() || this.inheritedClasses && this.entityClass.isInstance(obj))) {
            if (this.NBTData != null && this.NBTData.length > 0 && obj instanceof Entity entity) {
                CompoundTag tc = new CompoundTag();
                entity.saveWithoutId(tc);
                for (ThaumcraftApi.EntityTagsNBT nbt : this.NBTData) {
                    if (!tc.contains(nbt.name) || !java.util.Objects.equals(ThaumcraftApi.getNBTDataFromId(tc, nbt.name), nbt.value)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String getResearchKey(Player player, Object object) {
        return this.research;
    }
}
