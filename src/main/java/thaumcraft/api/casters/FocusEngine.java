package thaumcraft.api.casters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Движок фокусов (порт FocusEngine): реестр элементов (ключ → фабрика, значок, цвет) и выполнение пакета.
 * Реестр заполняется при запуске и дальше не меняется; выполнение — только на сервере (список защиты от урона — серверный).
 */
public class FocusEngine {
    public static final Map<String, Supplier<? extends IFocusElement>> elements = new HashMap<>();
    private static final Map<String, ResourceLocation> elementIcons = new HashMap<>();
    private static final Map<String, Integer> elementColor = new HashMap<>();
    /** Защита от повторного урона одним заклинанием по одной цели (id цели + uid пакета). Только серверный поток. */
    private static final ArrayList<String> damageResistList = new ArrayList<>();

    /** Регистрация элемента (в 1.12 — по классу через newInstance; здесь — фабрика). */
    public static void registerElement(Supplier<? extends IFocusElement> element, ResourceLocation icon, int color) {
        try {
            IFocusElement fe = element.get();
            elements.put(fe.getKey(), element);
            elementIcons.put(fe.getKey(), icon);
            elementColor.put(fe.getKey(), color);
        } catch (Exception ignored) {
        }
    }

    public static IFocusElement getElement(String key) {
        try {
            return elements.get(key).get();
        } catch (Exception e) {
            return null;
        }
    }

    public static ResourceLocation getElementIcon(String key) {
        return elementIcons.get(key);
    }

    public static int getElementColor(String key) {
        return elementColor.get(key);
    }

    public static boolean doesPackageContainElement(FocusPackage focusPackage, String key) {
        for (IFocusElement node : focusPackage.nodes) {
            if (node.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static void castFocusPackage(LivingEntity caster, FocusPackage focusPackage, boolean nocopy) {
        FocusPackage focusPackageCopy = nocopy ? focusPackage : focusPackage.copy(caster);
        focusPackageCopy.initialize(caster);
        focusPackageCopy.setUniqueID(UUID.randomUUID());
        for (FocusEffect effect : focusPackageCopy.getFocusEffects()) {
            effect.onCast(caster);
        }
        runFocusPackage(focusPackageCopy, null, null);
    }

    public static void castFocusPackage(LivingEntity caster, FocusPackage focusPackage) {
        castFocusPackage(caster, focusPackage, false);
    }

    public static void runFocusPackage(FocusPackage focusPackage, Trajectory[] trajectories, HitResult[] targets) {
        Trajectory[] prevTrajectories = trajectories;
        HitResult[] prevTargets = targets;
        synchronized (focusPackage.nodes) {
            if (!(focusPackage.nodes.get(0) instanceof FocusMediumRoot)) {
                focusPackage.nodes.add(0, new FocusMediumRoot(trajectories, targets));
            }
            for (int idx = 0; idx < focusPackage.nodes.size(); idx++) {
                focusPackage.setExecutionIndex(idx);
                IFocusElement node = focusPackage.nodes.get(idx);
                if (idx > 0 && node instanceof FocusNode fn && fn.getParent() == null
                        && focusPackage.nodes.get(idx - 1) instanceof FocusNode nodePrev) {
                    fn.setParent(nodePrev);
                }
                if (node instanceof FocusNode fn && fn.getPackage() == null) {
                    fn.setPackage(focusPackage);
                }
                if (node instanceof FocusNode fn) {
                    focusPackage.multiplyPower(fn.getPowerMultiplier());
                }
                if (node instanceof FocusPackage fp) {
                    runFocusPackage(fp, prevTrajectories, prevTargets);
                    break;
                }
                if (node instanceof FocusMedium medium) {
                    if (prevTrajectories != null) {
                        for (Trajectory trajectory : prevTrajectories) {
                            medium.execute(trajectory);
                        }
                    }
                    if (medium.hasIntermediary()) {
                        break;
                    }
                } else if (node instanceof FocusMod mod) {
                    if (mod instanceof FocusModSplit split) {
                        for (FocusPackage sp : split.getSplitPackages()) {
                            split.setPackage(sp);
                            sp.multiplyPower(focusPackage.getPower());
                            split.execute();
                            runFocusPackage(sp, split.supplyTrajectories(), split.supplyTargets());
                        }
                        break;
                    }
                    mod.execute();
                } else if (node instanceof FocusEffect effect) {
                    if (prevTargets != null) {
                        int num = 0;
                        for (HitResult target : prevTargets) {
                            if (target instanceof EntityHitResult eh && eh.getEntity() != null) {
                                String k = eh.getEntity().getId() + focusPackage.getUniqueID().toString();
                                if (damageResistList.contains(k) && eh.getEntity().invulnerableTime > 0) {
                                    eh.getEntity().invulnerableTime = 0;
                                } else {
                                    if (damageResistList.size() > 10) {
                                        damageResistList.remove(0);
                                    }
                                    damageResistList.add(k);
                                }
                            }
                            Trajectory tra = prevTrajectories != null
                                    ? (prevTrajectories.length == prevTargets.length ? prevTrajectories[num] : prevTrajectories[0])
                                    : null;
                            effect.execute(target, tra, focusPackage.getPower(), num);
                            num++;
                        }
                    }
                }
                if (node instanceof FocusNode fn) {
                    prevTrajectories = fn.supplyTrajectories();
                    prevTargets = fn.supplyTargets();
                }
            }
        }
    }

    /** Очистка серверного состояния при остановке сервера. */
    public static void clearServerState() {
        damageResistList.clear();
    }
}
