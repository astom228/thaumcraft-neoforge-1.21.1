package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import thaumcraft.api.crafting.Part;
import thaumcraft.common.lib.crafting.Matrix;

/**
 * Схема многоблочной конструкции для книги (порт GuiResearchPage.BlueprintBlockAccess + renderBluePrint): блоки-источники
 * (страница рецепта) или блоки-результаты (значок в списке рецептов); sliceLine скрывает верхние слои по очереди.
 */
public class BlueprintBlockAccess {
    private final Part[][][] data;
    private final BlockState[][][] structure;
    public int sliceLine = 0;

    public BlueprintBlockAccess(Part[][][] data, boolean target) {
        this.data = new Part[data.length][data[0].length][data[0][0].length];
        for (int y = 0; y < data.length; y++) {
            for (int x = 0; x < data[0].length; x++) {
                System.arraycopy(data[y][x], 0, this.data[y][x], 0, data[0][0].length);
            }
        }
        this.structure = new BlockState[data.length][data[0].length][data[0][0].length];
        if (target) {
            for (int y = 0; y < this.data.length; y++) {
                Matrix matrix = new Matrix(this.data[y]);
                matrix.Rotate90DegRight(3);
                this.data[y] = matrix.getMatrix();
            }
        }
        for (int y = 0; y < data.length; y++) {
            for (int x = 0; x < data[0].length; x++) {
                for (int z = 0; z < data[0][0].length; z++) {
                    this.structure[data.length - y - 1][x][z] = target ? this.convertTarget(x, y, z) : this.convert(x, y, z);
                }
            }
        }
    }

    private static BlockState toState(Object o) {
        if (o instanceof ItemStack is && Block.byItem(is.getItem()) != Blocks.AIR) {
            return Block.byItem(is.getItem()).defaultBlockState();
        }
        if (o instanceof Block b) {
            return b.defaultBlockState();
        }
        if (o instanceof BlockState st) {
            return st;
        }
        if (o instanceof TagKey<?> tag) {
            if (tag.equals(FluidTags.LAVA)) {
                return Blocks.LAVA.defaultBlockState();
            }
            if (tag.equals(FluidTags.WATER)) {
                return Blocks.WATER.defaultBlockState();
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    private BlockState convert(int x, int y, int z) {
        Part p = this.data[y][x][z];
        return p == null || p.getSource() == null ? Blocks.AIR.defaultBlockState() : toState(p.getSource());
    }

    private BlockState convertTarget(int x, int y, int z) {
        Part p = this.data[y][x][z];
        if (p == null) {
            return Blocks.AIR.defaultBlockState();
        }
        if (p.getTarget() == null) {
            return this.convert(x, y, z);
        }
        return toState(p.getTarget());
    }

    public BlockState getBlockState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (this.sliceLine > this.structure.length) {
            this.sliceLine = 0;
        }
        return y >= 0 && y < this.structure.length - this.sliceLine && x >= 0 && x < this.structure[y].length && z >= 0 && z < this.structure[y][x].length
                ? this.structure[y][x][z]
                : Blocks.AIR.defaultBlockState();
    }

    /**
     * renderBluePrint: конструкция с поворотом 25° / −45°, блоки полной яркости; блоки, рисуемые только рендером BlockEntity,
     * рисуются им. Ингредиенты — рядом снизу (рисует вызывающий). TODO этап 14: жидкости (лава адской печи) не рисуются.
     */
    public void render(GuiGraphics g, int x, int y, float scale, Part[][][] blueprint, float rotX, float rotY) {
        Minecraft mc = Minecraft.getInstance();
        int ySize = blueprint.length;
        int xSize = blueprint[0].length;
        int zSize = blueprint[0][0].length;
        float transX = x - xSize / 2;
        float transY = y - (float) Math.sqrt(ySize * ySize + xSize * xSize + zSize * zSize) / 2.0F;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(transX, transY, Math.max(ySize, Math.max(xSize, zSize)) + 100.0F);
        pose.scale(scale, -scale, 1.0F);
        pose.mulPose(Axis.XP.rotationDegrees(rotX));
        pose.mulPose(Axis.YP.rotationDegrees(rotY));
        pose.translate(zSize / -2.0F, ySize / -2.0F, xSize / -2.0F);
        Lighting.setupForFlatItems();
        MultiBufferSource.BufferSource buffers = g.bufferSource();
        for (int h = 0; h < ySize; h++) {
            for (int l = 0; l < xSize; l++) {
                for (int w = 0; w < zSize; w++) {
                    BlockPos pos = new BlockPos(l, h, w);
                    BlockState state = this.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    pose.pushPose();
                    pose.translate(l, h, w);
                    if (state.getRenderShape() == RenderShape.MODEL) {
                        mc.getBlockRenderer().renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY,
                                null);
                    } else if (state.getBlock() instanceof EntityBlock eb) {
                        try {
                            BlockEntity te = eb.newBlockEntity(BlockPos.ZERO, state);
                            if (te != null) {
                                mc.getBlockEntityRenderDispatcher().renderItem(te, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                            }
                        } catch (Exception ignored) {
                            // как в оригинале: блок, который не удаётся нарисовать, пропускается
                        }
                    }
                    pose.popPose();
                }
            }
        }
        buffers.endBatch();
        Lighting.setupFor3DItems();
        pose.popPose();
    }
}
