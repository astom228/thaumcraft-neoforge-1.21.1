package thaumcraft.common.items.casters.foci;

import java.awt.Color;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.lib.network.fx.PacketFXZap;
import thaumcraft.common.lib.utils.EntityUtils;

/** Носитель «Разряд» (порт FocusMediumBolt): как касание, но на 16 блоков; разряд цвета эффектов виден всем в 64 блоках. */
public class FocusMediumBolt extends FocusMediumTouch {
    @Override
    public String getResearch() {
        return "FOCUSBOLT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.BOLT";
    }

    @Override
    public int getComplexity() {
        return 5;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ENERGY;
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        float range = 16.0F;
        Vec3 end = trajectory.direction.normalize();
        HitResult ray = EntityUtils.getPointedEntityRay(this.getPackage().world, this.getPackage().getCaster(), trajectory.source, end, 0.25, range, 0.25F,
                false);
        if (ray == null) {
            end = end.scale(range).add(trajectory.source);
            ray = EntityUtils.rayTraceBlocks(this.getPackage().world, trajectory.source, end);
            if (ray != null) {
                end = ray.getLocation();
            }
        } else if (ray instanceof EntityHitResult eh) {
            end = end.scale(trajectory.source.distanceTo(eh.getEntity().position())).add(trajectory.source);
        }
        FocusEffect[] effects = this.getPackage().getFocusEffects();
        if (effects.length == 0) {
            return true; // в оригинале — деление на ноль; без эффектов пакет не собрать
        }
        int r = 0;
        int g = 0;
        int b = 0;
        for (FocusEffect ef : effects) {
            Color c = new Color(FocusEngine.getElementColor(ef.getKey()));
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
        }
        Color c = new Color(r / effects.length, g / effects.length, b / effects.length);
        if (this.getPackage().world instanceof ServerLevel sl) {
            PacketDistributor.sendToPlayersNear(sl, null, trajectory.source.x, trajectory.source.y, trajectory.source.z, 64.0,
                    new PacketFXZap(trajectory.source, end, c.getRGB(), this.getPackage().getPower() * 0.66F));
        }
        return true;
    }
}
