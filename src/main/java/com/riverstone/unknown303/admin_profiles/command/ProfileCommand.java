package com.riverstone.unknown303.admin_profiles.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.riverstone.unknown303.admin_profiles.profiles.ProfileManager;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;

import java.util.List;

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
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");

                                            ProfileManager manager = ProfileManager.get(player.level());

                                            if (!manager.hasMultiProfile(player.getUUID())) {
                                                player.sendSystemMessage(Component.literal("[AdminProfiles] You don't have multiple profiles."), false);
                                                return 0;
                                            }

                                            manager.switchProfile(player, name);
                                            return 1;
                                        })))
                        // /profile list
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ProfileManager manager = ProfileManager.get(p.level());
                                    List<String> list = manager.listProfiles(p.getUUID());
                                    String active = manager.getActiveProfileName(p.getUUID());
                                    p.sendSystemMessage(Component.literal("[AdminProfiles] Your profiles: %s  (active: %s)".formatted(list, active)), false);
                                    return 1;
                                }).then(Commands.argument("player", EntityArgument.player())
                                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            ProfileManager manager = ProfileManager.get(p.level());
                                            List<String> list = manager.listProfiles(p.getUUID());
                                            String active = manager.getActiveProfileName(p.getUUID());
                                            p.sendSystemMessage(Component.literal("[AdminProfiles] Your profiles: %s  (active: %s)".formatted(list, active)), false);
                                            return 1;
                                        })))
                        // /profile grant <player> <profileName> <displayName> <isOp>
                        .then(Commands.literal("grant")
                                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("profileName", StringArgumentType.word())
                                                .then(Commands.argument("displayName", StringArgumentType.word())
                                                        .then(Commands.argument("isOp", BoolArgumentType.bool())
                                                                .executes(ctx -> {
                                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                                                    ProfileManager manager = ProfileManager.get(target.level());

                                                                    String pName = StringArgumentType.getString(ctx, "profileName");
                                                                    String dName = StringArgumentType.getString(ctx, "displayName");
                                                                    boolean op = BoolArgumentType.getBool(ctx, "isOp");

                                                                    manager.grantProfile(target.getUUID(), pName, dName, op);
                                                                    ctx.getSource().sendSuccess(() -> Component.literal("Granted profile '" + pName + "' to " + target.getPlainTextName()), true);
                                                                    return 1;
                                                                }))))))

                        // /profile revoke <player> <profileName>
                        .then(Commands.literal("revoke")
                                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("profileName", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                                    ProfileManager manager = ProfileManager.get(target.level());

                                                    String pName = StringArgumentType.getString(ctx, "profileName");
                                                    manager.revokeProfile(target.getUUID(), pName);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Revoked profile '" + pName + "' from " + target.getPlainTextName()), true);
                                                    return 1;
                                                }))))
        );
    }
}
