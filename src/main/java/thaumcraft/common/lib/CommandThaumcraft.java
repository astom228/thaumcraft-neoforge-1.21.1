package thaumcraft.common.lib;

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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;

/**
 * Команда /thaumcraft (/thaum, /tc), уровень прав 2. Порт CommandThaumcraft на Brigadier.
 * Тексты оригинала были захардкожены по-английски — здесь это ключи перевода thaumcraft.command.* (en — тот же текст).
 * TODO этап 7: research <list|игрок> <list|all|reset|исследование>, reload.
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
}
