package thaumcraft.client.fx.particles;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Крошка предмета или блока, летящая к цели (порт FXBoreParticles): значок частиц модели предмета из атласа блоков, кусочек
 * 1/4 значка со случайным сдвигом (как частицы разрушения). Ванильный движок частиц, как в оригинале.
 */
public class FXBoreParticles extends TextureSheetParticle {
    private BlockState blockInstance;
    private ItemStack itemInstance = ItemStack.EMPTY;
    private Entity target;
    private double targetX;
    private double targetY;
    private double targetZ;
    private final float uo;
    private final float vo;
    float particleScale;

    private FXBoreParticles(ClientLevel world, double x, double y, double z, double tx, double ty, double tz, ItemStack icon) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setSprite(Minecraft.getInstance().getItemRenderer().getModel(icon, world, null, 0).getParticleIcon());
        this.rCol = this.gCol = this.bCol = 0.6F;
        this.particleScale = this.random.nextFloat() * 0.3F + 0.4F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        double dx = tx - this.x;
        double dy = ty - this.y;
        double dz = tz - this.z;
        int base = (int) (Mth.sqrt((float) (dx * dx + dy * dy + dz * dz)) * 10.0F);
        if (base < 1) {
            base = 1;
        }
        this.lifetime = base / 2 + this.random.nextInt(base);
        this.gravity = 0.01F;
    }

    /** Блок с пьедестала. */
    public FXBoreParticles(ClientLevel world, double x, double y, double z, double tx, double ty, double tz, BlockState block) {
        this(world, x, y, z, tx, ty, tz, new ItemStack(block.getBlock()));
        this.blockInstance = block;
        float f3 = 0.01F;
        this.xd = (float) this.random.nextGaussian() * f3;
        this.yd = (float) this.random.nextGaussian() * f3;
        this.zd = (float) this.random.nextGaussian() * f3;
    }

    /** Предмет-не-блок с пьедестала. */
    public FXBoreParticles(ClientLevel world, double x, double y, double z, double tx, double ty, double tz, double sx, double sy, double sz, ItemStack item) {
        this(world, x, y, z, tx, ty, tz, item);
        this.itemInstance = item;
        float f3 = 0.01F;
        this.xd = sx + (float) this.random.nextGaussian() * f3;
        this.yd = sy + (float) this.random.nextGaussian() * f3;
        this.zd = sz + (float) this.random.nextGaussian() * f3;
        Entity view = Minecraft.getInstance().getCameraEntity();
        int visibleDistance = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST ? 32 : 64;
        if (view != null && view.distanceToSqr(this.x, this.y, this.z) > visibleDistance * visibleDistance) {
            this.lifetime = 0;
        }
    }

    public void setTarget(Entity target) {
        this.target = target;
    }

    public void setAlphaF(float a) {
        this.alpha = a;
    }

    /**
     * getObjectColor: окраска предмета (цвет слоя 0), если в pos стоит не этот блок; иначе — цвет блока.
     * pos — матрица, поэтому блоки с пьедестала, как и в оригинале, не окрашиваются.
     */
    public FXBoreParticles getObjectColor(BlockPos pos) {
        int c = -1;
        if (this.blockInstance == null || this.level.getBlockState(pos) != this.blockInstance) {
            if (!this.itemInstance.isEmpty()) {
                c = Minecraft.getInstance().getItemColors().getColor(this.itemInstance, 0);
            }
        } else {
            c = Minecraft.getInstance().getBlockColors().getColor(this.blockInstance, this.level, pos, 0);
        }
        if (c != -1) {
            this.rCol *= (c >> 16 & 0xFF) / 255.0F;
            this.gCol *= (c >> 8 & 0xFF) / 255.0F;
            this.bCol *= (c & 0xFF) / 255.0F;
        }
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.target != null) {
            this.targetX = this.target.getX();
            this.targetY = this.target.getY() + this.target.getEyeHeight();
            this.targetZ = this.target.getZ();
        }
        if (this.age++ < this.lifetime && (Mth.floor(this.x) != Mth.floor(this.targetX) || Mth.floor(this.y) != Mth.floor(this.targetY)
                || Mth.floor(this.z) != Mth.floor(this.targetZ))) {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.985;
            this.yd *= 0.95;
            this.zd *= 0.985;
            double dx = this.targetX - this.x;
            double dy = this.targetY - this.y;
            double dz = this.targetZ - this.z;
            double d11 = Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
            double clamp = Math.min(0.25, d11 / 15.0);
            if (d11 < 2.0) {
                this.particleScale *= 0.9F;
            }
            dx /= d11;
            dy /= d11;
            dz /= d11;
            this.xd += dx * clamp;
            this.yd += dy * clamp;
            this.zd += dz * clamp;
            this.xd = Mth.clamp((float) this.xd, -clamp, clamp);
            this.yd = Mth.clamp((float) this.yd, -clamp, clamp);
            this.zd = Mth.clamp((float) this.zd, -clamp, clamp);
            this.xd = this.xd + this.random.nextGaussian() * 0.005;
            this.yd = this.yd + this.random.nextGaussian() * 0.005;
            this.zd = this.zd + this.random.nextGaussian() * 0.005;
        } else {
            this.remove();
        }
    }

    /** Половина стороны квадрата — 0.1 · масштаб, как в renderParticle оригинала; непрозрачно (альфа вершин — 1). */
    @Override
    public float getQuadSize(float partialTicks) {
        return 0.1F * this.particleScale;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
