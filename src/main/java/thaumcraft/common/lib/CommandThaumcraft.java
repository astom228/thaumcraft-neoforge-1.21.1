package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * Команда /thaumcraft (/thaum, /tc), уровень прав 2. Порт CommandThaumcraft на Brigadier.
 * Тексты оригинала были захардкожены по-английски — здесь это ключи перевода thaumcraft.command.* (en — тот же текст).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class CommandThaumcraft {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("thaumcraft")
                .requires(s -> s.hasPermission(2))
                .executes(CommandThaumcraft::invalid)
                .then(Commands.literal("help").executes(CommandThaumcraft::help))
                .then(Commands.literal("reload").executes(CommandThaumcraft::reload))
                .then(Commands.literal("research").executes(CommandThaumcraft::researchUsage)
                        .then(Commands.literal("list").executes(CommandThaumcraft::listResearch))
                        .then(Commands.argument("player", EntityArgument.player()).executes(CommandThaumcraft::researchUsage)
                                .then(Commands.literal("list").executes(ctx -> listAllResearch(ctx, EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.literal("all").executes(ctx -> giveAllResearch(ctx, EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.literal("reset").executes(ctx -> resetResearch(ctx, EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.argument("research", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(allResearchKeys(), b))
                                        .executes(ctx -> giveResearch(ctx, EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "research"))))))
                .then(Commands.literal("warp").executes(CommandThaumcraft::warpUsage)
                        .then(Commands.argument("player", EntityArgument.player()).executes(CommandThaumcraft::warpUsage)
                                .then(warpAction("set", 0, Integer.MAX_VALUE))
                                .then(warpAction("add", -100, 100)))));
        dispatcher.register(Commands.literal("thaum").requires(s -> s.hasPermission(2)).redirect(root));
        dispatcher.register(Commands.literal("tc").requires(s -> s.hasPermission(2)).redirect(root));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> warpAction(String action, int min, int max) {
        boolean set = action.equals("set");
        return Commands.literal(action).executes(CommandThaumcraft::warpUsage)
                .then(Commands.argument("amount", IntegerArgumentType.integer(min, max))
                        .executes(ctx -> warp(ctx, set, ""))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"PERM", "TEMP"}, b))
                                .executes(ctx -> warp(ctx, set, StringArgumentType.getString(ctx, "type")))));
    }

    private static int invalid(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.invalid"));
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.usehelp"));
        return 0;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        for (int i = 1; i <= 8; i++) {
            ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.help." + i));
        }
        return 1;
    }

    private static int warpUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.invalid"));
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.warp.usage"));
        return 0;
    }

    private static int warp(CommandContext<CommandSourceStack> ctx, boolean set, String type) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int i = IntegerArgumentType.getInteger(ctx, "amount");
        IPlayerWarp.EnumWarpType warpType = type.equalsIgnoreCase("PERM") ? IPlayerWarp.EnumWarpType.PERMANENT
                : type.equalsIgnoreCase("TEMP") ? IPlayerWarp.EnumWarpType.TEMPORARY : IPlayerWarp.EnumWarpType.NORMAL;
        IPlayerWarp pw = ThaumcraftCapabilities.getWarp(player);
        String sender = ctx.getSource().getTextName();
        if (set) {
            pw.set(warpType, i);
            pw.sync(player);
            player.sendSystemMessage(Component.translatable("thaumcraft.command.warp.set", sender, i));
        } else {
            pw.add(warpType, i);
            pw.sync(player);
            PacketDistributor.sendToPlayer(player, new PacketWarpMessage(i, (byte) 0));
            player.sendSystemMessage(Component.translatable("thaumcraft.command.warp.add", sender, i));
        }
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.success"));
        return 1;
    }

    // --- исследования --------------------------------------------------------------------------------------------------

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ResearchManager.parseAllResearch();
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.success"));
        return 1;
    }

    private static int researchUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.invalid"));
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.research.usage"));
        return 0;
    }

    private static List<String> allResearchKeys() {
        List<String> keys = new ArrayList<>();
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            keys.addAll(cat.research.keySet());
        }
        return keys;
    }

    private static int listResearch(CommandContext<CommandSourceStack> ctx) {
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.research.list", ri.getKey()));
            }
        }
        return 1;
    }

    private static int giveResearch(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String research) {
        if (ResearchCategories.getResearch(research) != null) {
            giveRecursiveResearch(player, research);
            ThaumcraftCapabilities.getKnowledge(player).sync(player);
            player.sendSystemMessage(Component.translatable("thaumcraft.command.research.gave", ctx.getSource().getTextName(), research));
            ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.success"));
            return 1;
        }
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.research.notexist"));
        return 0;
    }

    /** Исследование вместе с родителями, исследованиями, требуемыми стадиями, и «братьями». */
    public static void giveRecursiveResearch(Player player, String research) {
        if (research.contains("@")) {
            research = research.substring(0, research.indexOf("@"));
        }
        ResearchEntry res = ResearchCategories.getResearch(research);
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge.isResearchComplete(research)) {
            return;
        }
        if (res != null && res.getParents() != null) {
            for (String rsi : res.getParentsStripped()) {
                giveRecursiveResearch(player, rsi);
            }
        }
        if (res != null && res.getStages() != null) {
            for (ResearchStage page : res.getStages()) {
                if (page.getResearch() != null) {
                    for (String gr : page.getResearch()) {
                        ResearchManager.completeResearch(player, gr);
                    }
                }
            }
        }
        ResearchManager.completeResearch(player, research);
        for (ResearchCategory rc : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : rc.research.values()) {
                if (ri.getStages() != null) {
                    for (ResearchStage stage : ri.getStages()) {
                        if (stage.getResearch() != null && Arrays.asList(stage.getResearch()).contains(research)) {
                            knowledge.setResearchFlag(ri.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE);
                            break;
                        }
                    }
                }
            }
        }
        if (res != null && res.getSiblings() != null) {
            for (String rsi : res.getSiblings()) {
                giveRecursiveResearch(player, rsi);
            }
        }
    }

    private static int listAllResearch(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String ss = String.join(", ", ThaumcraftCapabilities.getKnowledge(player).getResearchList());
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.research.listfor", player.getName()));
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.research.list", ss));
        return 1;
    }

    private static int giveAllResearch(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                giveRecursiveResearch(player, ri.getKey());
            }
        }
        player.sendSystemMessage(Component.translatable("thaumcraft.command.research.giveall", ctx.getSource().getTextName()));
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.success"));
        return 1;
    }

    private static int resetResearch(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        ThaumcraftCapabilities.getKnowledge(player).clear();
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                if (ri.hasMeta(ResearchEntry.EnumResearchMeta.AUTOUNLOCK)) {
                    ResearchManager.completeResearch(player, ri.getKey(), false);
                }
            }
        }
        player.sendSystemMessage(Component.translatable("thaumcraft.command.research.reset", ctx.getSource().getTextName()));
        ctx.getSource().sendSystemMessage(Component.translatable("thaumcraft.command.success"));
        ThaumcraftCapabilities.getKnowledge(player).sync(player);
        return 1;
    }
}
