package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.lib.network.fx.PacketFXFocusEffect;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Носитель «Касание» (порт FocusMediumTouch): цель — существо или блок на расстоянии досягаемости (у «Разряда» — 16 блоков).
 * Траектория дальше — из точки попадания в том же направлении.
 */
public class FocusMediumTouch extends FocusMedium {
    @Override
    public String getResearch() {
        return "BASEAUROMANCY";
    }

    @Override
    public String getKey() {
        return "thaumcraft.TOUCH";
    }

    @Override
    public int getComplexity() {
        return 2;
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] {EnumSupplyType.TRAJECTORY, EnumSupplyType.TARGET};
    }

    @Override
    public Aspect getAspect() {
        return Aspect.AVERSION;
    }

    /** RayTracer.getBlockReachDistance: у игрока — досягаемость; у других заклинателей в оригинале было приведение к игроку (сбой). */
    double range() {
        if (this instanceof FocusMediumBolt) {
            return 16.0;
        }
        return this.getPackage().getCaster() instanceof Player p ? p.blockInteractionRange() : 4.5;
    }

    @Override
    public Trajectory[] supplyTrajectories() {
        if (this.getParent() == null) {
            return new Trajectory[0];
        }
        ArrayList<Trajectory> trajectories = new ArrayList<>();
        double range = this.range();
        for (Trajectory sT : this.getParent().supplyTrajectories()) {
            Vec3 end = sT.direction.normalize();
            HitResult ray = EntityUtils.getPointedEntityRay(this.getPackage().world, this.getPackage().getCaster(), sT.source, end, 0.25, range, 0.25F, false);
            if (ray == null) {
                end = end.scale(range).add(sT.source);
                ray = EntityUtils.rayTraceBlocks(this.getPackage().world, sT.source, end);
                if (ray != null) {
                    end = ray.getLocation();
                }
            } else if (ray instanceof EntityHitResult eh) {
                end = end.scale(sT.source.distanceTo(eh.getEntity().position())).add(sT.source);
            }
            trajectories.add(new Trajectory(end, sT.direction.normalize()));
        }
        return trajectories.toArray(new Trajectory[0]);
    }

    @Override
    public HitResult[] supplyTargets() {
        if (this.getParent() == null || !(this.getPackage().getCaster() instanceof Player)) {
            return new HitResult[0];
        }
        ArrayList<HitResult> targets = new ArrayList<>();
        double range = this.range();
        for (Trajectory sT : this.getParent().supplyTrajectories()) {
            Vec3 end = sT.direction.normalize();
            HitResult ray = EntityUtils.getPointedEntityRay(this.getPackage().world, this.getPackage().getCaster(), sT.source, end, 0.25, range, 0.25F, false);
            if (ray == null) {
                end = end.scale(range).add(sT.source);
                ray = EntityUtils.rayTraceBlocks(this.getPackage().world, sT.source, end);
            }
            if (ray != null) {
                targets.add(ray);
            }
        }
        return targets.toArray(new HitResult[0]);
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        FocusEffect[] fe = this.getPackage().getFocusEffects();
        if (fe != null && fe.length > 0 && this.getPackage().world instanceof ServerLevel sl) {
            String[] effects = new String[fe.length];
            for (int a = 0; a < fe.length; a++) {
                effects[a] = fe[a].getKey();
            }
            PacketDistributor.sendToPlayersNear(sl, null, trajectory.source.x, trajectory.source.y, trajectory.source.z, 64.0,
                    new PacketFXFocusEffect((float) trajectory.source.x, (float) trajectory.source.y, (float) trajectory.source.z,
                            (float) trajectory.direction.x / 2.0F, (float) trajectory.direction.y / 2.0F, (float) trajectory.direction.z / 2.0F, effects));
        }
        return true;
    }
}
