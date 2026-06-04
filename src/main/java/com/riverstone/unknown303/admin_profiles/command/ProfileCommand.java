package com.riverstone.unknown303.admin_profiles.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.riverstone.unknown303.admin_profiles.profiles.ProfileManager;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ProfileCommand {
    /**
     * Registers all /profile subcommands.
     *
     * Guarding strategy:
     *   /profile switch <name>  — any player, but only works if
     *                             they have >1 allowed profile
     *   /profile list           — any player (shows own profiles)
     *   /profile grant  ...     — requires OP level 4
     *   /profile revoke ...     — requires OP level 4
     *   /profile addprofile ... — requires OP level 4
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("profile")

                        // /profile switch <profileName>
                        .then(Commands.literal("switch")
                                .then(Commands.argument("name",
                                                StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player =
                                                    ctx.getSource().getPlayerOrException();
                                            String name =
                                                    StringArgumentType.getString(ctx, "name");

                                            ProfileManager manager = ProfileManager.get(player.level());

                                            if (!manager.hasMultiProfile(player.getUUID())) {
                                                player.sendSystemMessage(Component.literal(
                                                        "[OathProfiles] You don't have"
                                                                + " multiple profiles."), false);
                                                return 0;
                                            }

                                            manager.switchProfile(player, name);
                                            return 1;
                                        })))

                        // /profile list
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer p =
                                            ctx.getSource().getPlayerOrException();
                                    ProfileManager manager = ProfileManager.get(p.level());
                                    var list =
                                            manager.listProfiles(p.getUUID());
                                    String active =
                                            manager.getActiveProfileName(
                                                    p.getUUID());
                                    p.sendSystemMessage(Component.literal(
                                            "[OathProfiles] Your profiles: "
                                                    + list + "  (active: " + active
                                                    + ")"), false);
                                    return 1;
                                }))

                        // /profile grant <player> <profileName> <displayName> <isOp>
                        .then(Commands.literal("grant")
                                .requires(src -> src.getServer().getPlayerList().isOp(src.getPlayer().nameAndId()))
                                .then(Commands.argument("player",
                                                StringArgumentType.word())
                                        .then(Commands.argument("profileName",
                                                        StringArgumentType.word())
                                                .then(Commands.argument("displayName",
                                                                StringArgumentType.word())
                                                        .then(Commands.argument("isOp",
                                                                        BoolArgumentType.bool())
                                                                .executes(ctx -> {
                                                                    String targetName =
                                                                            StringArgumentType.getString(
                                                                                    ctx, "player");
                                                                    ServerPlayer target =
                                                                            ctx.getSource().getServer()
                                                                                    .getPlayerList()
                                                                                    .getPlayer(targetName);

                                                                    if (target == null) {
                                                                        ctx.getSource().sendFailure(
                                                                                Component.literal("Player not found."));
                                                                        return 0;
                                                                    }

                                                                    ProfileManager manager = ProfileManager.get(target.level());

                                                                    String pName =
                                                                            StringArgumentType.getString(
                                                                                    ctx, "profileName");
                                                                    String dName =
                                                                            StringArgumentType.getString(
                                                                                    ctx, "displayName");
                                                                    boolean op =
                                                                            BoolArgumentType.getBool(
                                                                                    ctx, "isOp");

                                                                    manager.grantProfile(
                                                                            target.getUUID(), pName, dName, op);
                                                                    ctx.getSource().sendSuccess(
                                                                            () -> Component.literal("Granted profile '"
                                                                                    + pName + "' to " + targetName),
                                                                            true);
                                                                    return 1;
                                                                }))))))

                        // /profile revoke <player> <profileName>
                        .then(Commands.literal("revoke")
                                .requires(src -> src.hasPermissionLevel(4))
                                .then(Commands.argument("player",
                                                StringArgumentType.word())
                                        .then(Commands.argument("profileName",
                                                        StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String targetName =
                                                            StringArgumentType.getString(
                                                                    ctx, "player");
                                                    ServerPlayer target =
                                                            ctx.getSource().getServer()
                                                                    .getPlayerManager()
                                                                    .getPlayer(targetName);
                                                    if (target == null) {
                                                        ctx.getSource().sendError(
                                                                Component.literal("Player not found."));
                                                        return 0;
                                                    }
                                                    String pName =
                                                            StringArgumentType.getString(
                                                                    ctx, "profileName");
                                                    ProfileManager.revokeProfile(
                                                            target.getUUID(), pName);
                                                    ctx.getSource().sendFeedback(
                                                            () -> Component.literal("Revoked profile '"
                                                                    + pName + "' from " + targetName),
                                                            true);
                                                    return 1;
                                                }))))
        );
    }
}
