package thaumcraft.api.casters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Пакет фокуса (порт FocusPackage): цепочка узлов, сила, сложность, заклинатель. Сериализуется в NBT как в оригинале
 * (в предмете фокуса — в компоненте данных). Измерение — ключ мира (в 1.12 — номер).
 */
public class FocusPackage implements IFocusElement {
    private static final UUID NIL = new UUID(0L, 0L);
    public Level world;
    private LivingEntity caster;
    private UUID casterUUID;
    private float power = 1.0F;
    private int complexity = 0;
    int index;
    UUID uid;
    public List<IFocusElement> nodes = Collections.synchronizedList(new ArrayList<>());

    @Override
    public String getResearch() {
        return null;
    }

    public FocusPackage() {
    }

    public FocusPackage(LivingEntity caster) {
        this.world = caster.level();
        this.caster = caster;
        this.casterUUID = caster.getUUID();
    }

    @Override
    public String getKey() {
        return "thaumcraft.PACKAGE";
    }

    @Override
    public EnumUnitType getType() {
        return EnumUnitType.PACKAGE;
    }

    public int getComplexity() {
        return this.complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public UUID getUniqueID() {
        return this.uid;
    }

    public void setUniqueID(UUID id) {
        this.uid = id;
    }

    public int getExecutionIndex() {
        return this.index;
    }

    public void setExecutionIndex(int idx) {
        this.index = idx;
    }

    public void addNode(IFocusElement e) {
        this.nodes.add(e);
    }

    public UUID getCasterUUID() {
        if (this.caster != null) {
            this.casterUUID = this.caster.getUUID();
        }
        return this.casterUUID;
    }

    public void setCasterUUID(UUID casterUUID) {
        this.casterUUID = casterUUID;
    }

    /** Заклинатель: игрок по UUID, иначе любое живое существо мира с этим UUID (только загруженные). */
    public LivingEntity getCaster() {
        try {
            if (this.caster == null && this.world != null) {
                this.caster = this.world.getPlayerByUUID(this.getCasterUUID());
            }
            if (this.caster == null && this.world instanceof ServerLevel sl && sl.getEntity(this.getCasterUUID()) instanceof LivingEntity e
                    && e.isAlive()) {
                this.caster = e;
            }
        } catch (Exception ignored) {
        }
        return this.caster;
    }

    public FocusEffect[] getFocusEffects() {
        return this.getFocusEffectsPackage(this);
    }

    private FocusEffect[] getFocusEffectsPackage(FocusPackage fp) {
        ArrayList<FocusEffect> out = new ArrayList<>();
        for (IFocusElement el : fp.nodes) {
            if (el instanceof FocusEffect fe) {
                out.add(fe);
            } else if (el instanceof FocusPackage p) {
                Collections.addAll(out, this.getFocusEffectsPackage(p));
            } else if (el instanceof FocusModSplit split) {
                for (FocusPackage fsp : split.getSplitPackages()) {
                    Collections.addAll(out, this.getFocusEffectsPackage(fsp));
                }
            }
        }
        return out.toArray(new FocusEffect[0]);
    }

    public void deserialize(CompoundTag nbt) {
        this.uid = nbt.hasUUID("uid") ? nbt.getUUID("uid") : NIL;
        this.index = nbt.getInt("index");
        // в 1.12 без "dim" getInteger давал 0 — обычный мир; на клиенте мира сервера нет (null)
        this.world = null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ResourceLocation dim = nbt.contains("dim", Tag.TAG_STRING) ? ResourceLocation.tryParse(nbt.getString("dim")) : null;
            this.world = dim != null ? server.getLevel(ResourceKey.create(Registries.DIMENSION, dim)) : server.overworld();
        }
        this.setCasterUUID(nbt.hasUUID("casterUUID") ? nbt.getUUID("casterUUID") : NIL);
        this.power = nbt.getFloat("power");
        this.complexity = nbt.getInt("complexity");
        ListTag nodelist = nbt.getList("nodes", Tag.TAG_COMPOUND);
        this.nodes.clear();
        for (int x = 0; x < nodelist.size(); x++) {
            CompoundTag nodenbt = nodelist.getCompound(x);
            EnumUnitType ut;
            try {
                ut = EnumUnitType.valueOf(nodenbt.getString("type"));
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (ut == EnumUnitType.PACKAGE) {
                FocusPackage fp = new FocusPackage();
                fp.deserialize(nodenbt.getCompound("package"));
                this.nodes.add(fp);
                break;
            }
            IFocusElement fn = FocusEngine.getElement(nodenbt.getString("key"));
            if (fn != null) {
                if (fn instanceof FocusNode node) {
                    node.initialize();
                    for (String ns : node.getSettingList()) {
                        node.getSetting(ns).setValue(nodenbt.getInt("setting." + ns));
                    }
                    if (fn instanceof FocusModSplit split) {
                        split.deserialize(nodenbt.getCompound("packages"));
                    }
                }
                this.addNode(fn);
            }
        }
    }

    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        if (this.uid != null) {
            nbt.putUUID("uid", this.uid);
        }
        nbt.putInt("index", this.index);
        if (this.getCasterUUID() != null) {
            nbt.putUUID("casterUUID", this.getCasterUUID());
        }
        if (this.world != null) {
            nbt.putString("dim", this.world.dimension().location().toString());
        }
        nbt.putFloat("power", this.power);
        nbt.putInt("complexity", this.complexity);
        ListTag nodelist = new ListTag();
        synchronized (this.nodes) {
            for (IFocusElement node : this.nodes) {
                CompoundTag nodenbt = new CompoundTag();
                nodenbt.putString("type", node.getType().name());
                nodenbt.putString("key", node.getKey());
                if (node.getType() == EnumUnitType.PACKAGE) {
                    nodenbt.put("package", ((FocusPackage) node).serialize());
                    nodelist.add(nodenbt);
                    break;
                }
                if (node instanceof FocusNode fn) {
                    for (String ns : fn.getSettingList()) {
                        nodenbt.putInt("setting." + ns, fn.getSettingValue(ns));
                    }
                }
                if (node instanceof FocusModSplit split) {
                    nodenbt.put("packages", split.serialize());
                }
                nodelist.add(nodenbt);
            }
        }
        nbt.put("nodes", nodelist);
        return nbt;
    }

    public float getPower() {
        return this.power;
    }

    public void multiplyPower(float pow) {
        this.power *= pow;
    }

    public FocusPackage copy(LivingEntity caster) {
        FocusPackage fp = new FocusPackage(caster);
        fp.deserialize(this.serialize());
        return fp;
    }

    public void initialize(LivingEntity caster) {
        this.world = caster.level();
        IFocusElement node = this.nodes.get(0);
        if (node instanceof FocusMediumRoot root && root.supplyTargets() == null) {
            root.setupFromCaster(caster);
        }
    }

    public int getSortingHelper() {
        StringBuilder s = new StringBuilder();
        for (IFocusElement k : this.nodes) {
            s.append(k.getKey());
        }
        return s.toString().hashCode();
    }
}
