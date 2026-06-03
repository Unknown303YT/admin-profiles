package com.riverstone.unknown303.admin_profiles.profiles;

import net.minecraft.server.level.ServerPlayer;

public class PlayerDataEvents {
    private static final String NBT_KEY =
            "oathprofiles";

    /**
     * Called when a player joins.
     * Reads their saved profile data from Minecraft's
     * built-in player .dat file via getPersistentData()
     * (the custom data compound Fabric/Minecraft exposes).
     */
    public static void onPlayerJoin(
            ServerPlayer player) {

        NbtCompound persist =
                player.getPersistentData();

        // Always init defaults first (handles new players)
        ProfileManager.initPlayer(
                player.getUUID(),
                player.getGameProfile().getName());

        if (persist.contains(NBT_KEY)) {
            ProfileManager.loadFromNbt(
                    player.getUUID(),
                    persist.get(NBT_KEY));
        }
    }

    /**
     * Called when a player disconnects.
     * Saves current inventory into their active profile,
     * then writes everything into the persistent data
     * compound — Minecraft flushes this to disk itself.
     */
    public static void onPlayerLeave(
            ServerPlayer player) {

        NbtCompound profileTag = new NbtCompound();
        ProfileManager.saveToNbt(
                player.getUUID(), profileTag);

        player.getSave()
                .put(NBT_KEY, profileTag);
    }
}
