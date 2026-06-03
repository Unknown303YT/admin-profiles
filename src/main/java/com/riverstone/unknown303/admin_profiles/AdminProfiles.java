package com.riverstone.unknown303.admin_profiles;

import com.riverstone.unknown303.admin_profiles.command.ProfileCommand;
import com.riverstone.unknown303.admin_profiles.profiles.PlayerDataEvents;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminProfiles implements ModInitializer {
	public static final String MOD_ID = "admin_profiles";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        LOGGER.info("OathProfiles loading...");

        // Register commands
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        ProfileCommand.register(dispatcher)
        );

        // Init player data on join, load NBT
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) ->
                        PlayerDataEvents.onPlayerJoin(handler.player)
        );

        // Save NBT on disconnect
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) ->
                        PlayerDataEvents.onPlayerLeave(handler.player)
        );
	}
}