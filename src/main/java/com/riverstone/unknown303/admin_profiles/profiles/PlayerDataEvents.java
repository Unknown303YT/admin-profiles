package com.riverstone.unknown303.admin_profiles.profiles;

import net.minecraft.server.level.ServerPlayer;

public class PlayerDataEvents {
    /**
     * Called when a player joins.
     * Reads their profile data.
     */
    public static void onPlayerJoin(
            ServerPlayer player) {

        ProfileManager.get(player.level()).initPlayer(player);
    }
}
