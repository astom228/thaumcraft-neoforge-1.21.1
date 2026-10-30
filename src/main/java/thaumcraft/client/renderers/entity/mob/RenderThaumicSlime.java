package thaumcraft.client.renderers.entity.mob;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;
import thaumcraft.Thaumcraft;

/** Таумический слизень (порт RenderThaumicSlime): ванильный рендер слизня со своей текстурой. */
public class RenderThaumicSlime extends SlimeRenderer {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/tslime.png");

    public RenderThaumicSlime(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(Slime entity) {
        return TEX;
    }
}
