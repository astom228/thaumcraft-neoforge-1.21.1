package thaumcraft.common.tiles.crafting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.IFocusElement;

/** Узел дерева фокусного манипулятора (порт FocusElementNode): позиция, связи, элемент с настройками. */
public class FocusElementNode {
    /** Предел числа узлов (в пакете оригинала число писалось байтом). */
    public static final int MAX_NODES = 100;
    public int x;
    public int y;
    public int id;
    public boolean target = false;
    public boolean trajectory = false;
    public int parent = -1;
    public int[] children = new int[0];
    public float complexityMultiplier = 1.0F;
    public FocusNode node = null;

    public float getPower(HashMap<Integer, FocusElementNode> data) {
        if (this.node == null) {
            return 1.0F;
        }
        float pow = this.node.getPowerMultiplier();
        FocusElementNode p = data.get(this.parent);
        if (p != null && p.node != null) {
            pow *= p.getPower(data);
        }
        return pow;
    }

    public void deserialize(CompoundTag nbt) {
        this.x = nbt.getInt("x");
        this.y = nbt.getInt("y");
        this.id = nbt.getInt("id");
        this.target = nbt.getBoolean("target");
        this.trajectory = nbt.getBoolean("trajectory");
        this.parent = nbt.getInt("parent");
        this.children = nbt.getIntArray("children");
        this.complexityMultiplier = nbt.getFloat("complexity");
        IFocusElement fe = FocusEngine.getElement(nbt.getString("key"));
        if (fe instanceof FocusNode fn) {
            this.node = fn;
            fn.initialize();
            for (String ns : fn.getSettingList()) {
                fn.getSetting(ns).setValue(nbt.getInt("setting." + ns));
            }
        }
    }

    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("x", this.x);
        nbt.putInt("y", this.y);
        nbt.putInt("id", this.id);
        nbt.putBoolean("target", this.target);
        nbt.putBoolean("trajectory", this.trajectory);
        nbt.putInt("parent", this.parent);
        nbt.putIntArray("children", this.children);
        nbt.putFloat("complexity", this.complexityMultiplier);
        if (this.node != null) {
            nbt.putString("key", this.node.getKey());
            for (String ns : this.node.getSettingList()) {
                nbt.putInt("setting." + ns, this.node.getSettingValue(ns));
            }
        }
        return nbt;
    }

    /**
     * Проверка дерева, пришедшего от клиента (раздел 7; в оригинале не было): корень 0 — «ROOT», ссылки родитель ↔ потомок
     * согласованы, каждый узел достижим ровно один раз (без циклов и сирот), ветвиться может только разделение (2 ветки),
     * не больше MAX_NODES узлов, координаты в разумных пределах.
     */
    public static boolean isStructureValid(Map<Integer, FocusElementNode> data) {
        if (data == null || data.isEmpty() || data.size() > MAX_NODES) {
            return false;
        }
        FocusElementNode root = data.get(0);
        if (root == null || root.parent != -1 || !(root.node instanceof FocusMediumRoot)) {
            return false;
        }
        for (Map.Entry<Integer, FocusElementNode> e : data.entrySet()) {
            FocusElementNode fn = e.getValue();
            if (fn == null || fn.id != e.getKey() || Math.abs(fn.x) > 200 || Math.abs(fn.y) > 200 || fn.children == null || fn.children.length > 2) {
                return false;
            }
            if (fn.children.length == 2 && !(fn.node instanceof FocusModSplit)) {
                return false;
            }
            if (fn.id != 0) {
                FocusElementNode p = data.get(fn.parent);
                if (p == null || !contains(p.children, fn.id)) {
                    return false;
                }
            }
            for (int c : fn.children) {
                FocusElementNode ch = data.get(c);
                if (ch == null || ch.parent != fn.id) {
                    return false;
                }
            }
        }
        Set<Integer> seen = new HashSet<>();
        return visit(data, 0, seen) && seen.size() == data.size();
    }

    private static boolean visit(Map<Integer, FocusElementNode> data, int id, Set<Integer> seen) {
        if (!seen.add(id) || seen.size() > MAX_NODES) {
            return false;
        }
        for (int c : data.get(id).children) {
            if (!visit(data, c, seen)) {
                return false;
            }
        }
        return true;
    }

    /** Правила экрана (gatherPartsList): узел получает от родителя хотя бы одно нужное; исключительный носитель — один. */
    public static boolean isCompositionValid(Map<Integer, FocusElementNode> data) {
        int mediums = 0;
        boolean exclusive = false;
        for (FocusElementNode fn : data.values()) {
            if (fn.node == null) {
                return false;
            }
            if (fn.node instanceof FocusMedium m && !(fn.node instanceof FocusMediumRoot)) {
                mediums++;
                exclusive |= m.isExclusive();
            }
            if (fn.id == 0) {
                continue;
            }
            if (fn.node instanceof FocusMediumRoot) {
                return false;
            }
            FocusElementNode p = data.get(fn.parent);
            boolean ok = false;
            FocusNode.EnumSupplyType[] need = fn.node.mustBeSupplied();
            if (need != null && p.node != null) {
                for (FocusNode.EnumSupplyType t : need) {
                    if (p.node.canSupply(t)) {
                        ok = true;
                        break;
                    }
                }
            }
            if (!ok) {
                return false;
            }
        }
        return !(exclusive && mediums > 1);
    }

    private static boolean contains(int[] a, int v) {
        for (int i : a) {
            if (i == v) {
                return true;
            }
        }
        return false;
    }
}
