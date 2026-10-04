package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.ParticleEngine;

/** Клиентские регистрации (шейдеры, рендереры…). */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ClientRegistration {
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "particle"), DefaultVertexFormat.PARTICLE),
                shader -> ParticleEngine.particleShader = shader);
    }
}
