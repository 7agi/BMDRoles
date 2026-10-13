package com.example.bmdroles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /bmd <players> <blind|deaf|mute|none>   assign a role to any number of players
 * /bmd rotate                             Blind -> Deaf -> Mute -> Blind for everyone who has a role
 * /bmd distribute                         spread the 3 roles evenly over all online players
 * /bmd clear                              remove all roles
 * /bmd list                               show who has which role
 */
public final class BmdCommand {
    /** 2 = operators only. Change to 0 to let every player use the command. */
    private static final int PERMISSION_LEVEL = 2;

    private BmdCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var targets = Commands.argument("targets", EntityArgument.players());
        for (Role role : Role.values()) {
            targets.then(Commands.literal(role.id()).executes(ctx -> assign(ctx, role)));
        }
        targets.then(Commands.literal("none").executes(ctx -> assign(ctx, null)));

        dispatcher.register(Commands.literal("bmd")
                .requires(src -> src.hasPermission(PERMISSION_LEVEL))
                .then(targets)
                .then(Commands.literal("rotate").executes(BmdCommand::rotate))
                .then(Commands.literal("distribute").executes(BmdCommand::distribute))
                .then(Commands.literal("clear").executes(BmdCommand::clear))
                .then(Commands.literal("list").executes(BmdCommand::list)));
    }

    private static int assign(CommandContext<CommandSourceStack> ctx, @Nullable Role role) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : players) {
            RoleManager.assign(player, role);
        }
        String what = role == null ? "no role" : role.displayName();
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + players.size() + " player(s) to " + what + "."), true);
        return players.size();
    }

    private static int rotate(CommandContext<CommandSourceStack> ctx) {
        int count = RoleManager.rotateAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("Rotated roles of " + count + " player(s)."), true);
        return count;
    }

    private static int distribute(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        players.sort(Comparator.comparing((ServerPlayer p) -> p.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER));
        Role[] roles = Role.values();
        for (int i = 0; i < players.size(); i++) {
            RoleManager.assign(players.get(i), roles[i % roles.length]);
        }
        int count = players.size();
        ctx.getSource().sendSuccess(() -> Component.literal("Distributed roles over " + count + " player(s)."), true);
        return count;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        int count = RoleManager.clearAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + count + " role(s)."), true);
        return count;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        Map<UUID, Role> all = RoleManager.snapshot();
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("Nobody has a role."), false);
            return 0;
        }
        for (Map.Entry<UUID, Role> e : all.entrySet()) {
            String name = nameOf(server, e.getKey());
            Role role = e.getValue();
            ctx.getSource().sendSuccess(() -> Component.literal(name + ": ")
                    .append(Component.literal(role.displayName()).withStyle(role.color())), false);
        }
        return all.size();
    }

    private static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        if (server.getProfileCache() != null) {
            var profile = server.getProfileCache().get(id);
            if (profile.isPresent()) {
                return profile.get().getName();
            }
        }
        return id.toString();
    }
}
