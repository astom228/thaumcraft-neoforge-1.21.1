package thaumcraft.api.casters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;

/** Корень пакета (порт FocusMediumRoot): траектория — взгляд заклинателя от глаз, цель — сам заклинатель. */
public class FocusMediumRoot extends FocusMedium {
    Trajectory[] trajectories = null;
    HitResult[] targets = null;

    public FocusMediumRoot() {
    }

    public FocusMediumRoot(Trajectory[] trajectories, HitResult[] targets) {
        this.trajectories = trajectories;
        this.targets = targets;
    }

    @Override
    public String getResearch() {
        return "BASEAUROMANCY";
    }

    @Override
    public String getKey() {
        return "ROOT";
    }

    @Override
    public int getComplexity() {
        return 0;
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] {EnumSupplyType.TARGET, EnumSupplyType.TRAJECTORY};
    }

    @Override
    public HitResult[] supplyTargets() {
        return this.targets;
    }

    @Override
    public Trajectory[] supplyTrajectories() {
        return this.trajectories;
    }

    public void setupFromCaster(LivingEntity caster) {
        this.trajectories = new Trajectory[] {new Trajectory(this.generateSourceVector(caster), caster.getLookAngle())};
        this.targets = new HitResult[] {new EntityHitResult(caster)};
    }

    public void setupFromCasterToTarget(LivingEntity caster, Entity target, double offset) {
        Vec3 sv = this.generateSourceVector(caster);
        double d0 = target.getX() - sv.x;
        double d1 = target.getBoundingBox().minY + target.getBbHeight() / 2.0F - sv.y;
        double d2 = target.getZ() - sv.z;
        Vec3 lv = new Vec3(d0, d1 + offset, d2);
        this.trajectories = new Trajectory[] {new Trajectory(sv, lv.normalize())};
        this.targets = new HitResult[] {new EntityHitResult(caster)};
    }

    public void setupFromCasterToTargetLoc(LivingEntity caster, double x, double y, double z) {
        Vec3 sv = this.generateSourceVector(caster);
        Vec3 lv = new Vec3(x - sv.x, y - sv.y, z - sv.z);
        this.trajectories = new Trajectory[] {new Trajectory(sv, lv.normalize())};
        this.targets = new HitResult[] {new EntityHitResult(caster)};
    }

    private Vec3 generateSourceVector(LivingEntity e) {
        return e.position().add(0.0, e.getEyeHeight() - 0.1F, 0.0);
    }

    @Override
    public Aspect getAspect() {
        return null;
    }
}
