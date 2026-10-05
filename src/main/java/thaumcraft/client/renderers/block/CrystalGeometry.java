package thaumcraft.client.renderers.block;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.obj.MeshLoader;

/**
 * Геометрия "thaumcraft:crystal" для models/block/crystal.json: OBJ-сетка models/obj/crystal.obj и текстура "crystal".
 * В оригинале CrystalModel подставлялся в реестр моделей в ModelBakeEvent (ProxyBlock).
 */
public class CrystalGeometry implements IUnbakedGeometry<CrystalGeometry> {
    public static final ResourceLocation LOADER_ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "crystal");
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "models/obj/crystal.obj");

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState, ItemOverrides overrides) {
        TextureAtlasSprite sprite = spriteGetter.apply(context.getMaterial("crystal"));
        try {
            return new CrystalModel(new MeshLoader().loadFromResource(Minecraft.getInstance().getResourceManager(), MESH), sprite);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить " + MESH, e);
        }
    }

    public static final class Loader implements IGeometryLoader<CrystalGeometry> {
        public static final Loader INSTANCE = new Loader();

        @Override
        public CrystalGeometry read(JsonObject json, JsonDeserializationContext ctx) {
            return new CrystalGeometry();
        }
    }
}
