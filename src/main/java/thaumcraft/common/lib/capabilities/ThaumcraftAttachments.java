package thaumcraft.common.lib.capabilities;

import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.aura.AuraChunk;

/**
 * Data Attachments вместо capabilities оригинала (thaumcraft:knowledge, thaumcraft:warp).
 * copyOnDeath — в оригинале PlayerEvent.Clone копировал данные всегда, в том числе после смерти.
 */
public class ThaumcraftAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Thaumcraft.MODID);

    public static final Supplier<AttachmentType<PlayerKnowledge>> KNOWLEDGE = ATTACHMENTS.register("knowledge",
            () -> AttachmentType.serializable(PlayerKnowledge::new).copyOnDeath().build());
    public static final Supplier<AttachmentType<PlayerWarp>> WARP = ATTACHMENTS.register("warp",
            () -> AttachmentType.serializable(PlayerWarp::new).copyOnDeath().build());
    /** Аура чанка (в оригинале — NBT чанка "Thaumcraft": base, vis, flux). */
    public static final Supplier<AttachmentType<AuraChunk>> AURA = ATTACHMENTS.register("aura",
            () -> AttachmentType.serializable(AuraChunk::new).build());
}
