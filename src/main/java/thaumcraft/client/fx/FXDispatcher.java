package thaumcraft.client.fx;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.client.fx.particles.FXBlockRunes;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.client.fx.particles.FXGenericGui;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

/** Создание эффектов (порт FXDispatcher; методы переносятся по мере надобности — тела и числа как в оригинале). */
public class FXDispatcher implements IFXDispatcher {
    public static final FXDispatcher INSTANCE = new FXDispatcher();

    public ClientLevel getWorld() {
        return Minecraft.getInstance().level;
    }

    @Override
    public void blockRunes(double x, double y, double z, float r, float g, float b, int dur, float grav) {
        FXBlockRunes fb = new FXBlockRunes(this.getWorld(), x + 0.5, y + 0.5, z + 0.5, r, g, b, dur);
        fb.setGravity(grav);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    /** Подсветка ещё не изученного объекта таумометром. Для блока — его рамка выделения (у пустой формы — ничего). */
    @Override
    public void scanHighlight(BlockPos p) {
        VoxelShape shape = this.getWorld().getBlockState(p).getShape(this.getWorld(), p);
        if (shape.isEmpty()) {
            return;
        }
        this.scanHighlight(shape.bounds().move(p));
    }

    @Override
    public void scanHighlight(Entity e) {
        this.scanHighlight(e.getBoundingBox());
    }

    public void scanHighlight(AABB bb) {
        int num = Mth.ceil(bb.getSize() * 2.0);
        double ax = (bb.minX + bb.maxX) / 2.0;
        double ay = (bb.minY + bb.maxY) / 2.0;
        double az = (bb.minZ + bb.maxZ) / 2.0;
        RandomSource rand = this.getWorld().random;
        for (Direction face : Direction.values()) {
            double mx = 0.5 + face.getStepX() * 0.51;
            double my = 0.5 + face.getStepY() * 0.51;
            double mz = 0.5 + face.getStepZ() * 0.51;
            for (int a = 0; a < num * 2; a++) {
                double x = mx + rand.nextGaussian() * (bb.maxX - bb.minX);
                double y = my + rand.nextGaussian() * (bb.maxY - bb.minY);
                double z = mz + rand.nextGaussian() * (bb.maxZ - bb.minZ);
                x = Mth.clamp(x, bb.minX - ax, bb.maxX - ax);
                y = Mth.clamp(y, bb.minY - ay, bb.maxY - ay);
                z = Mth.clamp(z, bb.minZ - az, bb.maxZ - az);
                float r = Mth.randomBetweenInclusive(rand, 16, 32) / 255.0F;
                float g = Mth.randomBetweenInclusive(rand, 132, 165) / 255.0F;
                float b = Mth.randomBetweenInclusive(rand, 223, 239) / 255.0F;
                this.drawSimpleSparkle(rand, ax + x, ay + y, az + z, 0.0, 0.0, 0.0, 0.4F + (float) rand.nextGaussian() * 0.1F, r, g, b,
                        rand.nextInt(10), 1.0F, 0.0F, 4);
            }
        }
    }

    public void drawSimpleSparkle(RandomSource rand, double x, double y, double z, double x2, double y2, double z2, float scale, float r, float g,
                                  float b, int delay, float decay, float grav, int baseAge) {
        boolean sp = rand.nextFloat() < 0.2;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        int age = baseAge * 4 + this.getWorld().random.nextInt(baseAge);
        fb.setMaxAge(age);
        fb.setRBGColorF(r, g, b);
        float[] alphas = new float[6 + rand.nextInt(age / 3)];
        for (int a = 1; a < alphas.length - 1; a++) {
            alphas[a] = rand.nextFloat();
        }
        fb.setAlphaF(alphas);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScale(scale, scale * 2.0F);
        fb.setLayer(0);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(5.0E-4F, 0.001F, 5.0E-4F);
        fb.setWind(5.0E-4);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
    }

    /** Искры над столом исследований во время теории (BlockResearchTable.randomDisplayTick). */
    @Override
    public void researchTableSparkle(BlockPos pos, RandomSource rand) {
        double xx = rand.nextGaussian() / 2.0;
        double zz = rand.nextGaussian() / 2.0;
        double yy = 1.5 + rand.nextFloat();
        int a = 40 + rand.nextInt(20);
        FXGeneric fb = new FXGeneric(this.getWorld(), pos.getX() + 0.5 + xx, pos.getY() + yy, pos.getZ() + 0.5 + zz, -xx / a, -(yy - 0.85) / a, -zz / a);
        fb.setMaxAge(a);
        fb.setRBGColorF(0.5F + rand.nextFloat() * 0.5F, 0.5F + rand.nextFloat() * 0.5F, 0.5F + rand.nextFloat() * 0.5F);
        fb.setAlphaF(0.0F, 0.25F, 0.5F, 0.75F, 0.0F);
        fb.setParticles(384 + rand.nextInt(16), 1, 1);
        fb.setScale(0.8F + rand.nextFloat() * 0.3F, 0.3F);
        fb.setLayer(0);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    /** Искра поверх интерфейса (слой 4). */
    public void drawSimpleSparkleGui(RandomSource rand, double x, double y, double x2, double y2, float scale, float r, float g, float b,
                                     int delay, float decay, float grav) {
        boolean sp = rand.nextFloat() < 0.2;
        FXGenericGui fb = new FXGenericGui(this.getWorld(), x, y, 0.0, x2, y2, 0.0);
        fb.setMaxAge(32 + this.getWorld().random.nextInt(8));
        fb.setRBGColorF(r, g, b);
        fb.setAlphaF(0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScale(scale, scale * 2.0F);
        fb.setNoClip(false);
        fb.setLayer(4);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(0.025F, 0.025F, 0.0F);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
    }

    public void drawWispyMotesOnBlock(BlockPos pp, int age, float grav) {
        this.drawWispyMotes(pp.getX() + this.getWorld().random.nextFloat(), pp.getY(), pp.getZ() + this.getWorld().random.nextFloat(),
                0.0, 0.0, 0.0, age,
                0.4F + this.getWorld().random.nextFloat() * 0.6F,
                0.6F + this.getWorld().random.nextFloat() * 0.4F,
                0.6F + this.getWorld().random.nextFloat() * 0.4F, grav);
    }

    public void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float grav) {
        this.drawWispyMotes(d, e, f, vx, vy, vz, age,
                0.25F + this.getWorld().random.nextFloat() * 0.75F,
                0.25F + this.getWorld().random.nextFloat() * 0.75F,
                0.25F + this.getWorld().random.nextFloat() * 0.75F, grav);
    }

    @Override
    public void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float r, float g, float b, float grav) {
        FXGeneric fb = new FXGeneric(this.getWorld(), d, e, f, vx, vy, vz);
        fb.setMaxAge((int) (age + age / 2 * this.getWorld().random.nextFloat()));
        fb.setRBGColorF(r, g, b);
        fb.setAlphaF(0.0F, 0.6F, 0.6F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(512, 16, 1);
        fb.setScale(1.0F, 0.5F);
        fb.setLoop(true);
        fb.setWind(0.001);
        fb.setGravity(grav);
        fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void drawAlumentum(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
        thaumcraft.client.fx.particles.FXFireMote glow = new thaumcraft.client.fx.particles.FXFireMote(this.getWorld(), x, y, z, vx, vy, vz, r, g, b, scale, 1);
        glow.setAlphaF(alpha);
        ParticleEngine.addEffect(this.getWorld(), glow);
    }

    @Override
    public void drawLevitatorParticles(double x, double y, double z, double x2, double y2, double z2) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.setMaxAge(200 + this.getWorld().random.nextInt(100));
        fb.setRBGColorF(0.5F, 0.5F, 0.2F);
        fb.setAlphaF(0.3F, 0.0F);
        fb.setGridSize(16);
        fb.setParticles(56, 1, 1);
        fb.setScale(2.0F, 5.0F);
        fb.setLayer(0);
        fb.setSlowDown(1.0);
        fb.setRotationSpeed(this.getWorld().random.nextFloat(), this.getWorld().random.nextBoolean() ? -1.0F : 1.0F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void purifyingFluidFx(double x, double y, double z) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(10 + this.getWorld().random.nextInt(10));
        fb.setScale(this.getWorld().random.nextFloat() * 0.3F + 0.3F);
        fb.setRBGColorF(1.0F, 1.0F, 1.0F);
        fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
        fb.setGravity(-0.01F);
        fb.setAlphaF(0.25F);
        fb.setParticle(64);
        fb.setFinalFrames(65, 66);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void slimyBubble(double x, double y, double z, float scale, float alpha, float r, float g, float b) {
        thaumcraft.client.fx.particles.FXSlimyBubble ef = new thaumcraft.client.fx.particles.FXSlimyBubble(this.getWorld(), x, y, z, scale);
        ef.setAlphaF(alpha);
        ef.setRBGColorF(r, g, b);
        ParticleEngine.addEffect(this.getWorld(), ef);
    }

    /** Брызги лавы из окна адской печи (ParticleLava.Factory оригинала — скорость частица задаёт сама). */
    @Override
    public void furnaceLavaFx(int x, int y, int z, int facingX, int facingZ) {
        var r = this.getWorld().random;
        float qx = facingX == 0 ? (r.nextFloat() - r.nextFloat()) * 0.5F : facingX * r.nextFloat();
        float qz = facingZ == 0 ? (r.nextFloat() - r.nextFloat()) * 0.5F : facingZ * r.nextFloat();
        this.getWorld().addParticle(net.minecraft.core.particles.ParticleTypes.LAVA, x + 0.5F + (r.nextFloat() - r.nextFloat()) * 0.3F + facingX,
                y + 0.3F, z + 0.5F + (r.nextFloat() - r.nextFloat()) * 0.3F + facingZ, 0.15F * qx, 0.2F * r.nextFloat(), 0.15F * qz);
    }

    @Override
    public void smokeSpiral(double x, double y, double z, float rad, int start, int miny, int color) {
        thaumcraft.client.fx.particles.FXSmokeSpiral fx = new thaumcraft.client.fx.particles.FXSmokeSpiral(this.getWorld(), x, y, z, rad, start, miny);
        Color c = new Color(color);
        fx.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
        ParticleEngine.addEffect(this.getWorld(), fx);
    }

    /** Туман над блоком (рост от мотыги стихий и т.п.): 8 клубов в границах формы блока. */
    @Override
    public void drawBlockMistParticles(net.minecraft.core.BlockPos p, int c) {
        net.minecraft.world.phys.AABB bs = this.getWorld().getBlockState(p).getShape(this.getWorld(), p).isEmpty()
                ? new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1) : this.getWorld().getBlockState(p).getShape(this.getWorld(), p).bounds();
        Color color = new Color(c);
        for (int a = 0; a < 8; a++) {
            double x = p.getX() + bs.minX + this.getWorld().random.nextFloat() * (bs.maxX - bs.minX);
            double y = p.getY() + bs.minY + this.getWorld().random.nextFloat() * (bs.maxY - bs.minY);
            double z = p.getZ() + bs.minZ + this.getWorld().random.nextFloat() * (bs.maxZ - bs.minZ);
            FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, this.getWorld().random.nextGaussian() * 0.01,
                    this.getWorld().random.nextFloat() * 0.075, this.getWorld().random.nextGaussian() * 0.01);
            fb.setMaxAge(50 + this.getWorld().random.nextInt(25));
            fb.setRBGColorF(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
            fb.setAlphaF(0.0F, 0.5F, 0.4F, 0.3F, 0.2F, 0.1F, 0.0F);
            fb.setGridSize(16);
            fb.setParticles(56, 1, 1);
            fb.setScale(5.0F, 1.0F);
            fb.setLayer(0);
            fb.setSlowDown(1.0);
            fb.setGravity(0.1F);
            fb.setWind(0.001);
            fb.setRotationSpeed(this.getWorld().random.nextFloat(), this.getWorld().random.nextBoolean() ? -1.0F : 1.0F);
            ParticleEngine.addEffect(this.getWorld(), fb);
        }
    }

    @Override
    public void drawNitorCore(double x, double y, double z, double x2, double y2, double z2) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.setMaxAge(10);
        fb.setRBGColorF(1.0F, 1.0F, 1.0F);
        fb.setAlphaF(1.0F);
        fb.setParticles(457, 1, 1);
        fb.setScale(1.0F, 1.0F + (float) this.getWorld().random.nextGaussian() * 0.1F, 1.0F);
        fb.setLayer(1);
        fb.setRandomMovementScale(2.0E-4F, 2.0E-4F, 2.0E-4F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void drawNitorFlames(double x, double y, double z, double x2, double y2, double z2, int color, int a) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.setMaxAge(10 + this.getWorld().random.nextInt(5));
        Color c = new Color(color);
        fb.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
        fb.setAlphaF(0.66F);
        fb.setLoop(true);
        fb.setGridSize(64);
        fb.setParticles(264, 8, 1);
        fb.setScale(3.0F + this.getWorld().random.nextFloat(), 0.05F);
        fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, a);
    }

    /** Искры по открытым граням блока, с задержкой по расстоянию от точки start (соль мира). */
    @Override
    public void drawBlockSparkles(BlockPos p, Vec3 start) {
        ClientLevel w = this.getWorld();
        VoxelShape shape = w.getBlockState(p).getShape(w, p);
        // в 1.12 — getBoundingBox (локальная рамка; у пустого блока — полный куб); результат grow(0.1) не использовался
        AABB bs = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        int num = (int) (bs.getSize() * 20.0);
        for (Direction face : Direction.values()) {
            BlockPos np = p.relative(face);
            BlockState state = w.getBlockState(np);
            if (!state.isSolidRender(w, np) && !state.isFaceSturdy(w, np, face.getOpposite())) {
                boolean rx = face.getStepX() == 0;
                boolean ry = face.getStepY() == 0;
                boolean rz = face.getStepZ() == 0;
                double mx = 0.5 + face.getStepX() * 0.51;
                double my = 0.5 + face.getStepY() * 0.51;
                double mz = 0.5 + face.getStepZ() * 0.51;
                for (int a = 0; a < num * 2; a++) {
                    double x = mx;
                    double y = my;
                    double z = mz;
                    if (rx) {
                        x += w.random.nextGaussian() * 0.6;
                    }
                    if (ry) {
                        y += w.random.nextGaussian() * 0.6;
                    }
                    if (rz) {
                        z += w.random.nextGaussian() * 0.6;
                    }
                    x = Mth.clamp(x, bs.minX, bs.maxX);
                    y = Mth.clamp(y, bs.minY, bs.maxY);
                    z = Mth.clamp(z, bs.minZ, bs.maxZ);
                    float r = Mth.randomBetweenInclusive(w.random, 255, 255) / 255.0F;
                    float g = Mth.randomBetweenInclusive(w.random, 189, 255) / 255.0F;
                    float b = Mth.randomBetweenInclusive(w.random, 64, 255) / 255.0F;
                    Vec3 v1 = new Vec3(p.getX() + x, p.getY() + y, p.getZ() + z);
                    double delay = w.random.nextInt(5) + v1.distanceTo(start) * 16.0;
                    this.drawSimpleSparkle(w.random, p.getX() + x, p.getY() + y, p.getZ() + z, 0.0, 0.0025, 0.0,
                            0.4F + (float) w.random.nextGaussian() * 0.1F, r, g, b, (int) delay, 1.0F, 0.01F, 16);
                }
            }
        }
    }

    @Override
    public void drawBamf(double x, double y, double z, int color, boolean sound, boolean flair, Direction side) {
        Color c = new Color(color);
        this.drawBamf(x, y, z, c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, sound, flair, side);
    }

    @Override
    public void drawBamf(double x, double y, double z, boolean sound, boolean flair, Direction side) {
        this.drawBamf(x, y, z, 0.5F, 0.1F, 0.6F, sound, flair, side);
    }

    /** Облачко дыма (превращение блока солью мира и т.п.). */
    @Override
    public void drawBamf(double x, double y, double z, float r, float g, float b, boolean sound, boolean flair, Direction side) {
        ClientLevel w = this.getWorld();
        if (sound) {
            w.playLocalSound(x, y, z, SoundsTC.poof.get(), SoundSource.BLOCKS, 0.4F, 1.0F + (float) w.random.nextGaussian() * 0.05F, false);
        }
        for (int a = 0; a < 6 + w.random.nextInt(3) + 2; a++) {
            double vx = (0.05F + w.random.nextFloat() * 0.05F) * (w.random.nextBoolean() ? -1 : 1);
            double vy = (0.05F + w.random.nextFloat() * 0.05F) * (w.random.nextBoolean() ? -1 : 1);
            double vz = (0.05F + w.random.nextFloat() * 0.05F) * (w.random.nextBoolean() ? -1 : 1);
            if (side != null) {
                vx += side.getStepX() * 0.1F;
                vy += side.getStepY() * 0.1F;
                vz += side.getStepZ() * 0.1F;
            }
            FXGeneric fb2 = new FXGeneric(w, x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx / 2.0, vy / 2.0, vz / 2.0);
            fb2.setMaxAge(20 + w.random.nextInt(15));
            fb2.setRBGColorF(Mth.clamp(r * (1.0F + (float) w.random.nextGaussian() * 0.1F), 0.0F, 1.0F),
                    Mth.clamp(g * (1.0F + (float) w.random.nextGaussian() * 0.1F), 0.0F, 1.0F),
                    Mth.clamp(b * (1.0F + (float) w.random.nextGaussian() * 0.1F), 0.0F, 1.0F));
            fb2.setAlphaF(1.0F, 0.1F);
            fb2.setGridSize(16);
            fb2.setParticles(123, 5, 1);
            fb2.setScale(3.0F, 4.0F + w.random.nextFloat() * 3.0F);
            fb2.setLayer(1);
            fb2.setSlowDown(0.7);
            fb2.setRotationSpeed(w.random.nextFloat(), w.random.nextBoolean() ? -1.0F : 1.0F);
            ParticleEngine.addEffect(w, fb2);
        }
        if (flair) {
            for (int a = 0; a < 2 + w.random.nextInt(3); a++) {
                double vx = (0.025F + w.random.nextFloat() * 0.025F) * (w.random.nextBoolean() ? -1 : 1);
                double vy = (0.025F + w.random.nextFloat() * 0.025F) * (w.random.nextBoolean() ? -1 : 1);
                double vz = (0.025F + w.random.nextFloat() * 0.025F) * (w.random.nextBoolean() ? -1 : 1);
                this.drawWispyMotes(x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx, vy, vz, 15 + w.random.nextInt(10), -0.01F);
            }
            FXGeneric fb = new FXGeneric(w, x, y, z, 0.0, 0.0, 0.0);
            fb.setMaxAge(10 + w.random.nextInt(5));
            fb.setRBGColorF(1.0F, 0.9F, 1.0F);
            fb.setAlphaF(1.0F, 0.0F);
            fb.setGridSize(16);
            fb.setParticles(77, 1, 1);
            fb.setScale(10.0F + w.random.nextFloat() * 2.0F, 0.0F);
            fb.setLayer(0);
            fb.setRotationSpeed(w.random.nextFloat(), (float) w.random.nextGaussian());
            ParticleEngine.addEffect(w, fb);
        }
        for (int a = 0; a < (flair ? 2 : 0) + w.random.nextInt(3); a++) {
            this.drawCurlyWisp(x, y, z, 0.0, 0.0, 0.0, 1.0F, (0.9F + w.random.nextFloat() * 0.1F + r) / 2.0F, (0.1F + g) / 2.0F,
                    (0.5F + w.random.nextFloat() * 0.1F + b) / 2.0F, 0.75F, side, a, 0, 0);
        }
    }

    public void drawCurlyWisp(double x, double y, double z, double vx, double vy, double vz, float scale, float r, float g, float b, float a,
                              Direction side, int seed, int layer, int delay) {
        ClientLevel w = this.getWorld();
        if (w != null) {
            vx += (0.0025F + w.random.nextFloat() * 0.005F) * (w.random.nextBoolean() ? -1 : 1);
            vy += (0.0025F + w.random.nextFloat() * 0.005F) * (w.random.nextBoolean() ? -1 : 1);
            vz += (0.0025F + w.random.nextFloat() * 0.005F) * (w.random.nextBoolean() ? -1 : 1);
            if (side != null) {
                vx += side.getStepX() * 0.025F;
                vy += side.getStepY() * 0.025F;
                vz += side.getStepZ() * 0.025F;
            }
            FXGeneric fb2 = new FXGeneric(w, x + vx * 5.0, y + vy * 5.0, z + vz * 5.0, vx, vy, vz);
            if (seed > 0 && w.random.nextBoolean()) {
                fb2.setAngles(90.0F * (float) w.random.nextGaussian(), 90.0F * (float) w.random.nextGaussian());
            }
            fb2.setMaxAge(25 + w.random.nextInt(20 + 20 * seed));
            fb2.setRBGColorF(r, g, b, 0.1F, 0.0F, 0.1F);
            fb2.setAlphaF(a, 0.0F);
            fb2.setGridSize(16);
            fb2.setParticles(60 + w.random.nextInt(4), 1, 1);
            fb2.setScale(5.0F * scale, (10.0F + w.random.nextFloat() * 4.0F) * scale);
            fb2.setLayer(layer);
            fb2.setRotationSpeed(w.random.nextFloat(), w.random.nextBoolean() ? -2.0F - w.random.nextFloat() * 2.0F : 2.0F + w.random.nextFloat() * 2.0F);
            ParticleEngine.addEffectWithDelay(w, fb2, delay);
        }
    }

    @Override
    public void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
        ClientLevel w = this.getWorld();
        FXGeneric fb = new FXGeneric(w, x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(15 + w.random.nextInt(10));
        fb.setScale(w.random.nextFloat() * 0.3F + 0.3F);
        fb.setRBGColorF(cr, cg, cb);
        fb.setRandomMovementScale(0.002F, 0.002F, 0.002F);
        fb.setGravity(-0.001F);
        fb.setParticle(64);
        fb.setFinalFrames(65, 66, 66);
        ParticleEngine.addEffect(w, fb);
    }

    /** Брызги при растворении (j — сила подъёма); в оригинале получал TileCrucible — здесь его высота жидкости и аспекты. */
    @Override
    public void crucibleBoil(BlockPos pos, float fluidHeight, thaumcraft.api.aspects.AspectList aspects, int j) {
        ClientLevel w = this.getWorld();
        for (int a = 0; a < 2; a++) {
            FXGeneric fb = new FXGeneric(w, pos.getX() + 0.2F + w.random.nextFloat() * 0.6F, pos.getY() + 0.1F + fluidHeight,
                    pos.getZ() + 0.2F + w.random.nextFloat() * 0.6F, 0.0, 0.002, 0.0);
            fb.setMaxAge((int) (7.0 + 8.0 / (Math.random() * 0.8 + 0.2)));
            fb.setScale(w.random.nextFloat() * 0.3F + 0.2F);
            if (aspects.size() == 0) {
                fb.setRBGColorF(1.0F, 1.0F, 1.0F);
            } else {
                Color color = new Color(aspects.getAspects()[w.random.nextInt(aspects.getAspects().length)].getColor());
                fb.setRBGColorF(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
            }
            fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
            fb.setGravity(-0.025F * j);
            fb.setParticle(64);
            fb.setFinalFrames(65, 66);
            ParticleEngine.addEffect(w, fb);
        }
    }

    @Override
    public void crucibleFroth(float x, float y, float z) {
        ClientLevel w = this.getWorld();
        FXGeneric fb = new FXGeneric(w, x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(4 + w.random.nextInt(3));
        fb.setScale(w.random.nextFloat() * 0.2F + 0.2F);
        fb.setRBGColorF(0.5F, 0.5F, 0.7F);
        fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
        fb.setGravity(0.1F);
        fb.setParticle(64);
        fb.setFinalFrames(65, 66);
        ParticleEngine.addEffect(w, fb);
    }

    @Override
    public void crucibleFrothDown(float x, float y, float z) {
        ClientLevel w = this.getWorld();
        FXGeneric fb = new FXGeneric(w, x, y, z, 0.0, 0.0, 0.0);
        fb.setMaxAge(12 + w.random.nextInt(12));
        fb.setScale(w.random.nextFloat() * 0.2F + 0.4F);
        fb.setRBGColorF(0.25F, 0.0F, 0.75F);
        fb.setAlphaF(0.8F);
        fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
        fb.setGravity(0.05F);
        fb.setNoClip(false);
        fb.setParticle(73);
        fb.setFinalFrames(65, 66);
        fb.setLayer(1);
        ParticleEngine.addEffect(w, fb);
    }

    /** Поток эссенции от p1 к p2 (count — фаза колебаний, ext — длина). */
    public void essentiaTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale, int ext) {
        thaumcraft.client.fx.other.FXEssentiaStream fb = new thaumcraft.client.fx.other.FXEssentiaStream(this.getWorld(), p1.getX() + 0.5,
                p1.getY() + 0.5, p1.getZ() + 0.5, p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, count, color, scale, ext, 0.0);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    public void essentiaDropFx(double x, double y, double z, float r, float g, float b, float alpha) {
        ClientLevel w = this.getWorld();
        FXGeneric fb = new FXGeneric(w, x, y, z, w.random.nextGaussian() * 0.005F, w.random.nextGaussian() * 0.005F, w.random.nextGaussian() * 0.005F);
        fb.setMaxAge(20 + w.random.nextInt(10));
        fb.setRBGColorF(r, g, b);
        fb.setAlphaF(alpha);
        fb.setLoop(false);
        fb.setParticles(25, 1, 1);
        fb.setScale(0.4F + w.random.nextFloat() * 0.2F, 0.2F);
        fb.setLayer(1);
        fb.setGravity(0.01F);
        fb.setRotationSpeed(0.0F);
        ParticleEngine.addEffect(w, fb);
    }

    @Override
    public void spark(double d, double e, double f, float size, float r, float g, float b, float a) {
        ClientLevel w = this.getWorld();
        FXGeneric fb = new FXGeneric(w, d, e, f, 0.0, 0.0, 0.0);
        fb.setMaxAge(5 + w.random.nextInt(5));
        fb.setAlphaF(a);
        fb.setRBGColorF(r, g, b);
        fb.setGridSize(16);
        fb.setParticles(8 + w.random.nextInt(3) * 16, 8, 1);
        fb.setScale(size);
        fb.setFlipped(w.random.nextBoolean());
        ParticleEngine.addEffect(w, fb);
    }

    @Override
    public void jarSplashFx(double x, double y, double z) {
        ClientLevel w = this.getWorld();
        FXGeneric fb = new FXGeneric(w, x + w.random.nextGaussian() * 0.075F, y, z + w.random.nextGaussian() * 0.075F, w.random.nextGaussian() * 0.015F,
                0.075F + w.random.nextFloat() * 0.05F, w.random.nextGaussian() * 0.015F);
        fb.setMaxAge(20 + w.random.nextInt(10));
        Color c = new Color(2650102);
        fb.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
        fb.setAlphaF(0.5F);
        fb.setLoop(false);
        fb.setParticles(73, 1, 1);
        fb.setScale(0.4F + w.random.nextFloat() * 0.3F, 0.0F);
        fb.setLayer(1);
        fb.setGravity(0.3F);
        fb.setRotationSpeed(0.0F);
        ParticleEngine.addEffect(w, fb);
    }

    /** Струя воды (неиссякаемая урна): тот же поток, что у эссенции, но с подъёмом 0.2 и без длины. */
    @Override
    public void waterTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale) {
        thaumcraft.client.fx.other.FXEssentiaStream fb = new thaumcraft.client.fx.other.FXEssentiaStream(this.getWorld(), p1.getX() + 0.5,
                p1.getY() + 0.66, p1.getZ() + 0.5, p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, count, color, scale, 0, 0.2);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color) {
        thaumcraft.client.fx.particles.FXVent fb = new thaumcraft.client.fx.particles.FXVent(this.getWorld(), x, y, z, x2, y2, z2, color);
        fb.setAlphaF(0.4F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        thaumcraft.client.fx.particles.FXVent fb = new thaumcraft.client.fx.particles.FXVent(this.getWorld(), x, y, z, x2, y2, z2, color);
        fb.setAlphaF(0.4F);
        fb.setScale(scale);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void drawInfusionParticles1(double x, double y, double z, net.minecraft.core.BlockPos pos, net.minecraft.world.item.ItemStack stack) {
        var r = this.getWorld().random;
        thaumcraft.client.fx.particles.FXBoreParticles fb = new thaumcraft.client.fx.particles.FXBoreParticles(this.getWorld(), x, y, z, pos.getX() + 0.5,
                pos.getY() - 0.5, pos.getZ() + 0.5, (float) r.nextGaussian() * 0.03F, (float) r.nextGaussian() * 0.03F, (float) r.nextGaussian() * 0.03F, stack)
                .getObjectColor(pos);
        fb.setAlphaF(0.3F);
        net.minecraft.client.Minecraft.getInstance().particleEngine.add(fb);
    }

    @Override
    public void drawInfusionParticles2(double x, double y, double z, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        thaumcraft.client.fx.particles.FXBoreParticles fb = new thaumcraft.client.fx.particles.FXBoreParticles(this.getWorld(), x, y, z, pos.getX() + 0.5,
                pos.getY() - 0.5, pos.getZ() + 0.5, state).getObjectColor(pos);
        fb.setAlphaF(0.3F);
        net.minecraft.client.Minecraft.getInstance().particleEngine.add(fb);
    }

    @Override
    public void drawInfusionParticles3(double x, double y, double z, int x2, int y2, int z2) {
        thaumcraft.client.fx.particles.FXBoreSparkle fb = new thaumcraft.client.fx.particles.FXBoreSparkle(this.getWorld(), x, y, z, x2 + 0.5, y2 - 0.5, z2 + 0.5);
        fb.setRGB(0.4F + this.getWorld().random.nextFloat() * 0.2F, 0.2F, 0.6F + this.getWorld().random.nextFloat() * 0.3F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void drawInfusionParticles4(double x, double y, double z, int x2, int y2, int z2) {
        thaumcraft.client.fx.particles.FXBoreSparkle fb = new thaumcraft.client.fx.particles.FXBoreSparkle(this.getWorld(), x, y, z, x2 + 0.5, y2 - 0.5, z2 + 0.5);
        fb.setRGB(0.2F, 0.6F + this.getWorld().random.nextFloat() * 0.3F, 0.3F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    /** В оригинале молния шла через ванильный движок частиц; здесь — через движок Thaumcraft (своя отрисовка, аддитивно). */
    @Override
    public void arcLightning(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float h) {
        if (h <= 0.0F) {
            h = 0.1F;
        }
        ParticleEngine.addEffect(this.getWorld(), new thaumcraft.client.fx.beams.FXArc(this.getWorld(), x, y, z, tx, ty, tz, r, g, b, h));
    }

    @Override
    public void drawSlash(double x, double y, double z, double x2, double y2, double z2, int dur) {
        ParticleEngine.addEffect(this.getWorld(), new thaumcraft.client.fx.particles.FXPlane(this.getWorld(), x, y, z, x2, y2, z2, dur));
    }

    @Override
    public void drawPollutionParticles(net.minecraft.core.BlockPos p) {
        var rand = this.getWorld().random;
        float x = p.getX() + 0.2F + rand.nextFloat() * 0.6F;
        float y = p.getY() + 0.2F + rand.nextFloat() * 0.6F;
        float z = p.getZ() + 0.2F + rand.nextFloat() * 0.6F;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, (rand.nextFloat() - rand.nextFloat()) * 0.005, 0.02,
                (rand.nextFloat() - rand.nextFloat()) * 0.005);
        fb.setMaxAge(400 + rand.nextInt(100));
        fb.setRBGColorF(1.0F, 0.3F, 0.9F);
        fb.setAlphaF(0.5F, 0.0F);
        fb.setGridSize(16);
        fb.setParticles(56, 1, 1);
        fb.setScale(2.0F, 5.0F);
        fb.setLayer(1);
        fb.setSlowDown(1.0);
        fb.setWind(0.001);
        fb.setRotationSpeed(rand.nextFloat(), rand.nextBoolean() ? -1.0F : 1.0F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void arcBolt(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
        ParticleEngine.addEffect(this.getWorld(), new thaumcraft.client.fx.beams.FXBolt(this.getWorld(), x, y, z, tx, ty, tz, r, g, b, width));
    }

    @Override
    public void drawFireMote(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
        boolean bb = this.getWorld().random.nextBoolean();
        thaumcraft.client.fx.particles.FXFireMote glow = new thaumcraft.client.fx.particles.FXFireMote(this.getWorld(), x, y, z, vx, vy, vz, r, g, b,
                bb ? scale / 3.0F : scale, bb ? 1 : 0);
        glow.setAlphaF(alpha);
        ParticleEngine.addEffect(this.getWorld(), glow);
    }

    @Override
    public void drawFocusCloudParticle(double x, double y, double z, double mx, double my, double mz, int c) {
        java.awt.Color color = new java.awt.Color(c);
        var rand = this.getWorld().random;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
        fb.setMaxAge(20 + rand.nextInt(10));
        fb.setRBGColorF(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
        fb.setAlphaF(0.0F, 0.66F, 0.0F);
        fb.setGridSize(16);
        fb.setParticles(56 + rand.nextInt(4), 1, 1);
        fb.setScale(5.0F + rand.nextFloat(), 10.0F + rand.nextFloat());
        fb.setLayer(0);
        fb.setSlowDown(0.99);
        fb.setWind(0.001);
        fb.setRotationSpeed(rand.nextFloat(), rand.nextBoolean() ? -0.25F : 0.25F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, thaumcraft.common.lib.fx.GenPart part) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
        fb.setMaxAge(part.age);
        fb.setRBGColorF(part.redStart, part.greenStart, part.blueStart, part.redEnd, part.greenEnd, part.blueEnd);
        fb.setAlphaF(part.alpha);
        fb.setLoop(part.loop);
        fb.setParticles(part.partStart, part.partNum, part.partInc);
        fb.setScale(part.scale);
        fb.setLayer(part.layer);
        fb.setRotationSpeed(part.rotstart, part.rot);
        fb.setSlowDown(part.slowDown);
        fb.setGravity(part.grav);
        fb.setGridSize(part.grid);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, part.delay);
    }

    @Override
    public void sparkle(float x, float y, float z, float r, float g, float b) {
        var rand = this.getWorld().random;
        if (rand.nextInt(6) < 4) {
            FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
            fb.setMaxAge(6 + rand.nextInt(4));
            fb.setRBGColorF(r, g, b);
            fb.setAlphaF(0.9F);
            fb.setLoop(true);
            fb.setParticles(320, 16, 1);
            fb.setScale(0.6F + rand.nextFloat() * 0.2F);
            fb.setLayer(0);
            fb.setRotationSpeed(0.0F);
            ParticleEngine.addEffectWithDelay(this.getWorld(), fb, 0);
        }
    }

    @Override
    public void focusFrostFX(double x, double y, double z, double mx, double my, double mz) {
        var rand = this.getWorld().random;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
        fb.setMaxAge(40 + rand.nextInt(40));
        fb.setAlphaF(1.0F, 0.0F);
        fb.setParticles(8, 1, 1);
        fb.setGravity(0.033F);
        fb.setSlowDown(0.8);
        fb.setRandomMovementScale(0.0025F, 1.0E-4F, 0.0025F);
        fb.setScale((float) (0.7F + rand.nextGaussian() * 0.3F));
        fb.setRotationSpeed(rand.nextFloat() * 3.0F, (float) rand.nextGaussian() / 4.0F);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, 0);
    }

    @Override
    public void focusBreakFX(double x, double y, double z, double mx, double my, double mz) {
        var rand = this.getWorld().random;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
        fb.setMaxAge(6 + rand.nextInt(6));
        int q = rand.nextInt(4);
        fb.setParticles(704 + q * 3, 3, 1);
        fb.setSlowDown(0.8);
        fb.setScale((float) (1.7F + rand.nextGaussian() * 0.3F));
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void focusHealFX(double x, double y, double z, double vx, double vy, double vz) {
        var rand = this.getWorld().random;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, vx + rand.nextGaussian() * 0.01, vy + rand.nextGaussian() * 0.01, vz + rand.nextGaussian() * 0.01);
        fb.setMaxAge((int) (10.0F + 10.0F * rand.nextFloat()));
        fb.setRBGColorF(1.0F, 1.0F, 1.0F);
        fb.setAlphaF(0.0F, 0.7F, 0.7F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(0, 1, 1);
        fb.setScale(rand.nextFloat() * 2.0F, rand.nextFloat());
        fb.setSlowDown(0.8);
        fb.setGravity((float) (rand.nextGaussian() * 0.1F));
        fb.setRandomMovementScale(0.0125F, 0.0125F, 0.0125F);
        fb.setRotationSpeed((float) rand.nextGaussian());
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, rand.nextInt(4));
    }

    @Override
    public void focusFluxFX(double x, double y, double z, double vx, double vy, double vz) {
        var rand = this.getWorld().random;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, vx + rand.nextGaussian() * 0.01, vy + rand.nextGaussian() * 0.01, vz + rand.nextGaussian() * 0.01);
        fb.setMaxAge((int) (15.0F + 10.0F * rand.nextFloat()));
        fb.setRBGColorF(0.25F + rand.nextFloat() * 0.25F, 0.0F, 0.25F + rand.nextFloat() * 0.25F);
        fb.setAlphaF(0.0F, 1.0F, 1.0F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(128, 14, 1);
        fb.setScale(2.0F + rand.nextFloat(), 0.25F + rand.nextFloat() * 0.25F);
        fb.setLoop(true);
        fb.setSlowDown(0.9);
        fb.setGravity((float) (rand.nextGaussian() * 0.1F));
        fb.setRandomMovementScale(0.0125F, 0.0125F, 0.0125F);
        fb.setRotationSpeed((float) rand.nextGaussian());
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, rand.nextInt(4));
    }

    @Override
    public void focusCurseFX(double x, double y, double z, double mx, double my, double mz) {
        var rand = this.getWorld().random;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
        fb.setMaxAge(8);
        fb.setRBGColorF(0.41F + rand.nextFloat() * 0.2F, 0.0F, 0.019F + rand.nextFloat() * 0.2F);
        fb.setAlphaF(0.0F, rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 0.0F);
        fb.setGridSize(16);
        fb.setParticles(72 + rand.nextInt(4), 1, 1);
        fb.setScale(2.0F + rand.nextFloat() * 4.0F);
        fb.setLoop(false);
        fb.setSlowDown(0.9);
        fb.setGravity(0.0F);
        fb.setRotationSpeed(rand.nextFloat(), 0.0F);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, rand.nextInt(4));
    }

    @Override
    public void focusExchangeFX(double x, double y, double z, double vx, double vy, double vz) {
        var rand = this.getWorld().random;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, vx + rand.nextGaussian() * 0.01, vy + rand.nextGaussian() * 0.01, vz + rand.nextGaussian() * 0.01);
        fb.setMaxAge(9);
        fb.setRBGColorF(0.25F + rand.nextFloat() * 0.25F, 0.25F + rand.nextFloat() * 0.25F, 0.25F + rand.nextFloat() * 0.25F);
        fb.setAlphaF(0.0F, 0.6F, 0.6F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(448, 9, 1);
        fb.setScale(0.5F, 0.25F);
        fb.setGravity((float) (rand.nextGaussian() * 0.01F));
        fb.setRandomMovementScale(0.0025F, 0.0025F, 0.0025F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }

    @Override
    public void focusRiftFX(double x, double y, double z, double mx, double my, double mz) {
        var rand = this.getWorld().random;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
        fb.setMaxAge(16 + rand.nextInt(16));
        fb.setParticles(384 + rand.nextInt(16), 1, 1);
        fb.setSlowDown(0.75);
        fb.setAlphaF(1.0F, 0.0F);
        fb.setScale((float) (0.7F + rand.nextGaussian() * 0.3F));
        fb.setRBGColorF(0.25F, 0.25F, 1.0F);
        fb.setRandomMovementScale(0.01F, 0.01F, 0.01F);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, 0);
    }

    @Override
    public void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
        var rand = this.getWorld().random;
        thaumcraft.client.fx.particles.FXVisSparkle fb = new thaumcraft.client.fx.particles.FXVisSparkle(this.getWorld(), x + rand.nextFloat(),
                y + rand.nextFloat(), z + rand.nextFloat(), x2 + 0.4 + rand.nextFloat() * 0.2F, y2 + 0.4 + rand.nextFloat() * 0.2F,
                z2 + 0.4 + rand.nextFloat() * 0.2F);
        java.awt.Color c = new java.awt.Color(color);
        fb.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
        ParticleEngine.addEffect(this.getWorld(), fb);
    }
}
