package thaumcraft.client.lib.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.gui.ResearchToast;
import thaumcraft.client.lib.events.HudHandler;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;
import thaumcraft.common.lib.network.misc.PacketKnowledgeGain;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledgeDelta;
import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;

/** Клиентская обработка пакетов (onMessage оригинала). Вызывается в главном потоке клиента. */
public final class ClientPacketHandlers {
    public static void handleAura(PacketAuraToClient msg) {
        HudHandler.currentAura = new AuraChunk(null, msg.base(), msg.vis(), msg.flux());
    }

    public static void handleSyncWarp(PacketSyncWarp msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ThaumcraftCapabilities.getWarp(player).deserializeNBT(player.registryAccess(), msg.data());
        }
    }

    public static void handleSyncKnowledge(PacketSyncKnowledge msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IPlayerKnowledge pk = ThaumcraftCapabilities.getKnowledge(player);
        pk.deserializeNBT(player.registryAccess(), msg.data());
        for (String key : pk.getResearchList()) {
            showPopup(pk, key);
        }
    }

    /** Дельта знаний (PacketSyncKnowledgeDelta): то же, что полная синхронизация, только для изменившихся записей. */
    public static void handleSyncKnowledgeDelta(PacketSyncKnowledgeDelta msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(ThaumcraftCapabilities.getKnowledge(player) instanceof PlayerKnowledge pk)) {
            return;
        }
        for (PacketSyncKnowledgeDelta.ResearchDelta d : msg.research()) {
            pk.applyResearchDelta(d.key(), d.present(), d.stage(), d.flags());
        }
        for (PacketSyncKnowledgeDelta.KnowledgeDelta d : msg.knowledge()) {
            pk.applyKnowledgeDelta(d.key(), d.amount());
        }
        for (PacketSyncKnowledgeDelta.ResearchDelta d : msg.research()) {
            if (d.present()) {
                showPopup(pk, d.key());
            }
        }
    }

    /** Тост «Исследование завершено» для записи с флагом POPUP, затем флаг снимается. */
    private static void showPopup(IPlayerKnowledge pk, String key) {
        if (pk.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP)) {
            ResearchEntry ri = ResearchCategories.getResearch(key);
            if (ri != null) {
                Minecraft.getInstance().getToasts().addToast(new ResearchToast(ri));
            }
        }
        pk.clearResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
    }

    /** PacketFXInfusionSource: источник частиц наполнения у матрицы (пьедестал — 60 тиков, игрок — 15). */
    public static void handleInfusionSource(thaumcraft.common.lib.network.fx.PacketFXInfusionSource msg) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int tx = msg.x() - msg.dx();
        int ty = msg.y() - msg.dy();
        int tz = msg.z() - msg.dz();
        String key = tx + ":" + ty + ":" + tz + ":" + msg.color();
        if (level.getBlockEntity(new net.minecraft.core.BlockPos(msg.x(), msg.y(), msg.z())) instanceof thaumcraft.common.tiles.crafting.TileInfusionMatrix is) {
            int count = level.getBlockEntity(new net.minecraft.core.BlockPos(tx, ty, tz)) instanceof thaumcraft.common.tiles.crafting.TilePedestal ? 60 : 15;
            thaumcraft.common.tiles.crafting.TileInfusionMatrix.SourceFX sf = is.sourceFX.get(key);
            if (sf != null) {
                sf.ticks = count;
            } else {
                is.sourceFX.put(key, new thaumcraft.common.tiles.crafting.TileInfusionMatrix.SourceFX(new net.minecraft.core.BlockPos(tx, ty, tz), count, msg.color()));
            }
        }
    }

    /** PacketFXFocusPartImpact: вспышка эффектов в точке попадания. */
    public static void handleFocusPartImpact(thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact msg) {
        var world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        String[] partKeys = msg.parts().split("%");
        int amt = Math.max(1, 15 / partKeys.length);
        for (String k : partKeys) {
            if (thaumcraft.api.casters.FocusEngine.getElement(k) instanceof thaumcraft.api.casters.FocusEffect part) {
                for (int a = 0; a < amt; a++) {
                    part.renderParticleFX(world, msg.x(), msg.y(), msg.z(), world.random.nextGaussian() * 0.15, world.random.nextGaussian() * 0.15,
                            world.random.nextGaussian() * 0.15);
                }
            }
        }
    }

    /** PacketFXZap: разряд «Разряда». */
    /** PacketFXShield: вспышка рунической защиты, повёрнутая к источнику удара (−1 — сверху и снизу, −2 — снизу, −3 — сверху). */
    public static void handleShield(thaumcraft.common.lib.network.fx.PacketFXShield msg) {
        net.minecraft.client.multiplayer.ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        net.minecraft.world.entity.Entity p = world.getEntity(msg.source());
        if (p == null) {
            return;
        }
        if (msg.target() >= 0) {
            float pitch;
            float yaw;
            net.minecraft.world.entity.Entity t = world.getEntity(msg.target());
            if (t != null) {
                double d0 = p.getX() - t.getX();
                double d1 = (p.getBoundingBox().minY + p.getBoundingBox().maxY) / 2.0 - (t.getBoundingBox().minY + t.getBoundingBox().maxY) / 2.0;
                double d2 = p.getZ() - t.getZ();
                double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                yaw = (float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
                pitch = (float) (-(Math.atan2(d1, d3) * 180.0 / Math.PI));
            } else {
                pitch = 90.0F;
                yaw = 0.0F;
            }
            shield(world, p, yaw, pitch);
        } else if (msg.target() == -1) {
            shield(world, p, 0.0F, 90.0F);
            shield(world, p, 0.0F, 270.0F);
        } else if (msg.target() == -2) {
            shield(world, p, 0.0F, 270.0F);
        } else if (msg.target() == -3) {
            shield(world, p, 0.0F, 90.0F);
        }
    }

    private static void shield(net.minecraft.client.multiplayer.ClientLevel world, net.minecraft.world.entity.Entity p, float yaw, float pitch) {
        thaumcraft.client.fx.ParticleEngine.addEffect(world, new thaumcraft.client.fx.other.FXShieldRunes(world, p, 8, yaw, pitch));
    }

    public static void handleZap(thaumcraft.common.lib.network.fx.PacketFXZap msg) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        java.awt.Color c = new java.awt.Color(msg.color());
        IFXDispatcher.get().arcBolt(msg.source().x, msg.source().y, msg.source().z, msg.target().x, msg.target().y, msg.target().z,
                c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, msg.width());
    }

    /** PacketFXFocusEffect: по 10 / (число эффектов) частиц каждого эффекта. */
    public static void handleFocusEffect(thaumcraft.common.lib.network.fx.PacketFXFocusEffect msg) {
        var world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        String[] partKeys = msg.parts().split("%");
        int amt = Math.max(1, 10 / partKeys.length);
        for (String k : partKeys) {
            if (thaumcraft.api.casters.FocusEngine.getElement(k) instanceof thaumcraft.api.casters.FocusEffect part) {
                for (int a = 0; a < amt; a++) {
                    part.renderParticleFX(world, msg.x(), msg.y(), msg.z(), msg.mx() + world.random.nextGaussian() / 20.0,
                            msg.my() + world.random.nextGaussian() / 20.0, msg.mz() + world.random.nextGaussian() / 20.0);
                }
            }
        }
    }

    /** PacketFXPollute: клубы Выброса над блоком. */
    public static void handlePollute(thaumcraft.common.lib.network.fx.PacketFXPollute msg) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(msg.x(), msg.y(), msg.z());
        for (int a = 0; a < Math.min(40, msg.amount()); a++) {
            IFXDispatcher.get().drawPollutionParticles(p);
        }
    }

    /** PacketFXSlash: росчерк от существа к существу (середина высоты). */
    public static void handleSlash(thaumcraft.common.lib.network.fx.PacketFXSlash msg) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        net.minecraft.world.entity.Entity a = level.getEntity(msg.source());
        net.minecraft.world.entity.Entity b = level.getEntity(msg.target());
        if (a != null && b != null) {
            IFXDispatcher.get().drawSlash(a.getX(), a.getBoundingBox().minY + a.getBbHeight() / 2.0F, a.getZ(), b.getX(),
                    b.getBoundingBox().minY + b.getBbHeight() / 2.0F, b.getZ(), 8);
        }
    }

    /** PacketFXScanSource: подсветить группы руд вокруг блока — вспышка цвета руды, позже для дальних. */
    public static void handleScanSource(thaumcraft.common.lib.network.fx.PacketFXScanSource msg) {
        var world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(msg.loc());
        int range = 4 + msg.size() * 4;
        java.util.List<net.minecraft.core.BlockPos> positions = new java.util.ArrayList<>();
        for (int xx = -range; xx <= range; xx++) {
            for (int yy = -range; yy <= range; yy++) {
                for (int zz = -range; zz <= range; zz++) {
                    net.minecraft.core.BlockPos p = pos.offset(xx, yy, zz);
                    if (world.isLoaded(p) && thaumcraft.common.lib.utils.Utils.isOreBlock(world, p)) {
                        positions.add(p);
                    }
                }
            }
        }
        while (!positions.isEmpty()) {
            net.minecraft.core.BlockPos start = positions.remove(0);
            java.util.List<net.minecraft.core.BlockPos> coll = new java.util.ArrayList<>();
            coll.add(start);
            calcGroup(world, start, coll, positions);
            int c = getOreColor(world.getBlockState(start));
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            for (net.minecraft.core.BlockPos p : coll) {
                x += p.getX() + 0.5;
                y += p.getY() + 0.5;
                z += p.getZ() + 0.5;
            }
            x /= coll.size();
            y /= coll.size();
            z /= coll.size();
            double dis = Math.sqrt(pos.distToCenterSqr(x, y, z));
            thaumcraft.client.fx.particles.FXGeneric fb = new thaumcraft.client.fx.particles.FXGeneric(world, x, y, z, 0.0, 0.0, 0.0);
            fb.setMaxAge(44);
            java.awt.Color cc = new java.awt.Color(c);
            fb.setRBGColorF(cc.getRed() / 255.0F, cc.getGreen() / 255.0F, cc.getBlue() / 255.0F);
            float q = (cc.getRed() / 255.0F + cc.getGreen() / 255.0F + cc.getBlue() / 255.0F) / 3.0F;
            fb.setAlphaF(0.0F, 1.0F, 0.8F, 0.0F);
            fb.setParticles(240, 15, 1);
            fb.setGridSize(16);
            fb.setLoop(true);
            fb.setScale(9.0F);
            fb.setLayer(q < 0.25F ? 3 : 2);
            fb.setRotationSpeed(0.0F);
            thaumcraft.client.fx.ParticleEngine.addEffectWithDelay(world, fb, (int) (dis * 3.0));
        }
    }

    private static void calcGroup(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos start, java.util.List<net.minecraft.core.BlockPos> coll,
                                  java.util.List<net.minecraft.core.BlockPos> positions) {
        net.minecraft.world.level.block.state.BlockState bs = world.getBlockState(start);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    net.minecraft.core.BlockPos t = start.offset(x, y, z);
                    if (world.getBlockState(t).equals(bs) && positions.contains(t)) {
                        positions.remove(t);
                        coll.add(t);
                        if (positions.isEmpty()) {
                            return;
                        }
                        calcGroup(world, t, coll, positions);
                    }
                }
            }
        }
    }

    /** Цвет руды по имени OreDictionary (IRON, COAL, …) → по тегам c:ores/… того же смысла. */
    private static int getOreColor(net.minecraft.world.level.block.state.BlockState bi) {
        String[][] map = {{"iron", "14200723"}, {"coal", "1052688"}, {"redstone", "16711680"}, {"gold", "16576075"}, {"lapis", "1328572"},
                {"diamond", "6155509"}, {"emerald", "1564002"}, {"quartz", "15064789"}, {"silver", "14342653"}, {"tin", "15724539"},
                {"copper", "16620629"}, {"amber", "16626469"}, {"cinnabar", "10159368"}};
        for (String[] m : map) {
            if (bi.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ores/" + m[0])))) {
                return Integer.parseInt(m[1]);
            }
        }
        return 12632256;
    }

    /** PacketFXBlockArc: молния от цели к центру блока. */
    public static void handleBlockArc(thaumcraft.common.lib.network.fx.PacketFXBlockArc msg) {
        if (Minecraft.getInstance().level != null) {
            IFXDispatcher.get().arcLightning(msg.tx(), msg.ty(), msg.tz(), msg.x() + 0.5, msg.y() + 0.5, msg.z() + 0.5, msg.r(), msg.g(), msg.b(), 0.5F);
        }
    }

    /** PacketKnowledgeGain: эффект на HUD и звук. */
    public static void handleBlockBamf(PacketFXBlockBamf msg) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        if (msg.color() != -9999) {
            IFXDispatcher.get().drawBamf(msg.x(), msg.y(), msg.z(), msg.color(), msg.sound(), msg.flair(), msg.side());
        } else {
            IFXDispatcher.get().drawBamf(msg.x(), msg.y(), msg.z(), msg.sound(), msg.flair(), msg.side());
        }
    }

    public static void handleKnowledgeGain(PacketKnowledgeGain msg) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || msg.knowledgeType() < 0 || msg.knowledgeType() >= IPlayerKnowledge.EnumKnowledgeType.values().length) {
            return;
        }
        IPlayerKnowledge.EnumKnowledgeType type = IPlayerKnowledge.EnumKnowledgeType.values()[msg.knowledgeType()];
        ResearchCategory cat = !msg.cat().isEmpty() ? ResearchCategories.getResearchCategory(msg.cat()) : null;
        HudHandler.knowledgeGainTrackers.add(new HudHandler.KnowledgeGainTracker(type, cat, 40 + p.level().random.nextInt(20), p.level().random.nextLong()));
        p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), SoundsTC.learn.get(), SoundSource.AMBIENT, 1.0F, 1.0F, false);
    }

    /**
     * processMessage оригинала: постоянное (type 0) и обычное (1) — текст на панели действий и шёпот при получении;
     * для обычного (1) — текст на панели действий и шёпот; для временного (2) — только текст.
     */
    public static void handleWarpMessage(PacketWarpMessage msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || msg.amount() == 0) {
            return;
        }
        if (msg.warpType() == 0) {
            // в оригинале текст для постоянного Искажения формировался, но не выводился (DEVIATIONS.md)
            String key = "tc.addwarp";
            if (msg.amount() < 0) {
                key = "tc.removewarp";
            } else {
                player.playSound(SoundsTC.whispers.get(), 0.5F, 1.0F);
            }
            player.displayClientMessage(Component.translatable(key), true);
        } else if (msg.warpType() == 1) {
            String key = "tc.addwarpsticky";
            if (msg.amount() < 0) {
                key = "tc.removewarpsticky";
            } else {
                player.playSound(SoundsTC.whispers.get(), 0.5F, 1.0F);
            }
            player.displayClientMessage(Component.translatable(key), true);
        } else if (msg.amount() > 0) {
            player.displayClientMessage(Component.translatable("tc.addwarptemp"), true);
        }
    }
}
