package com.riverstone.unknown303.admin_profiles.profiles;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;

import java.util.*;

public class ProfileManager {
    private static final Map<UUID, Map<String, ProfileData>>
            profiles = new HashMap<>();

    // UUID -> currently active profile name
    private static final Map<UUID, String>
            activeProfile = new HashMap<>();

    // UUID -> set of profiles they are allowed to use
    private static final Map<UUID, Set<String>>
            allowedProfiles = new HashMap<>();

    // ── Access control ──────────────────────────────────────────
    public static boolean hasMultiProfile(UUID uuid) {
        Set<String> allowed = allowedProfiles.get(uuid);
        return allowed != null && allowed.size() > 1;
    }

    public static void grantProfile(
            UUID uuid, String profileName,
            String displayName, boolean isOp) {

        profiles.computeIfAbsent(uuid, k -> new HashMap<>())
                .putIfAbsent(profileName,
                        new ProfileData(displayName, isOp));

        allowedProfiles.computeIfAbsent(uuid, k -> new HashSet<>())
                .add(profileName);
    }

    public static void revokeProfile(UUID uuid, String profileName) {
        Set<String> allowed = allowedProfiles.get(uuid);
        if (allowed != null) allowed.remove(profileName);
    }

    // ── Init on player join ─────────────────────────────────────
    public static void initPlayer(UUID uuid, String realName) {
        profiles.computeIfAbsent(uuid, k -> new HashMap<>())
                .putIfAbsent("default",
                        new ProfileData(realName, false));

        allowedProfiles.computeIfAbsent(uuid, k -> {
            Set<String> s = new HashSet<>();
            s.add("default");
            return s;
        });
        activeProfile.putIfAbsent(uuid, "default");
    }

    // ── Switch ──────────────────────────────────────────────────
    public static void switchProfile(
            ServerPlayer player, String profileName) {

        UUID uuid = player.getUUID();
        Set<String> allowed = allowedProfiles.get(uuid);

        if (allowed == null || !allowed.contains(profileName)) {
            player.sendMessage(Component.literal(
                    "[OathProfiles] You don't have a profile called '"
                            + profileName + "'."), false);
            return;
        }

        saveInventory(player);
        activeProfile.put(uuid, profileName);
        ProfileData next = profiles.get(uuid).get(profileName);

        loadInventory(player, next);
        applyDisplayName(player, next.getDisplayName());
        applyOpStatus(player, next.isOp);

        player.sendMessage(Component.literal(
                "[OathProfiles] Switched to " + profileName
                        + " (" + next.getDisplayName() + ")"), false);
    }

    public static String getDisplayName(UUID uuid) {
        String active = activeProfile.get(uuid);
        if (active == null) return null;
        Map<String, ProfileData> pMap = profiles.get(uuid);
        if (pMap == null) return null;
        ProfileData d = pMap.get(active);
        return d != null ? d.displayName : null;
    }

    public static List<String> listProfiles(UUID uuid) {
        Set<String> allowed = allowedProfiles.get(uuid);
        return allowed == null
                ? List.of()
                : List.copyOf(allowed);
    }

    public static String getActiveProfileName(UUID uuid) {
        return activeProfile.getOrDefault(uuid, "default");
    }

    // ── Inventory helpers ───────────────────────────────────────
    private static void saveInventory(ServerPlayer player) {
        String cur = activeProfile.get(player.getUUID());
        ProfileData data = profiles
                .getOrDefault(player.getUUID(), Map.of())
                .get(cur);
        if (data == null) return;

        var inv = player.getInventory();
        for (int i = 0; i < 36; i++)
            data.getInventory()[i] = inv.getStack(i).copy();
        for (int i = 0; i < 4; i++)
            data.getArmor()[i] = inv.armor.get(i).copy();
        data.offhand      = inv.offHand.get(0).copy();
    }

    private static void loadInventory(
            ServerPlayer player, ProfileData data) {
        var inv = player.getInventory();
        inv.clear();
        for (int i = 0; i < 36; i++)
            inv.setStack(i, data.inventory[i].copy());
        for (int i = 0; i < 4; i++)
            inv.armor.set(i, data.armor[i].copy());
        inv.offHand.set(0, data.offhand.copy());
        player.playerScreenHandler.sendContentUpdates();
    }

    private static void applyDisplayName(
            ServerPlayer player, String name) {
        // Handled via the Mixin; just force a tab-list refresh
        player.level().getServer().getPlayerManager()
                .sendToAll(player.createPlayerListEntry());
    }

    private static void applyOpStatus(
            ServerPlayer player, boolean isOp) {
        MinecraftServer server = player.level().getServer();
        var ops    = server.getPlayerList().getOps();
        var profile = player.getGameProfile();

        if (isOp) {
            ops.add(new ServerOpListEntry(
                    profile,
                    server.getOpPermissionLevel(),
                    false));
        } else {
            ops.remove(profile);
        }
        server.getPlayerManager().sendCommandTree(player);
    }

    // ── NBT save / load ─────────────────────────────────────────
    public static void saveToNbt(UUID uuid, NbtCompound root) {
        Map<String, ProfileData> pMap = profiles.get(uuid);
        if (pMap == null) return;

        NbtCompound profilesTag = new NbtCompound();
        pMap.forEach((name, data) ->
                profilesTag.put(name, data.toNbt()));
        root.put("profiles", profilesTag);

        NbtList allowedTag = new NbtList();
        Set<String> allowed =
                allowedProfiles.getOrDefault(uuid, Set.of());
        allowed.forEach(n ->
                allowedTag.add(NbtString.of(n)));
        root.put("allowedProfiles", allowedTag);

        root.putString("activeProfile",
                activeProfile.getOrDefault(uuid, "default"));
    }

    public static void loadFromNbt(UUID uuid, NbtCompound root) {
        if (!root.contains("profiles")) return;

        NbtCompound profilesTag =
                root.getCompound("profiles");
        Map<String, ProfileData> pMap =
                new HashMap<>();
        profilesTag.getKeys().forEach(name ->
                pMap.put(name,
                        ProfileData.fromNbt(
                                profilesTag.getCompound(name))));
        profiles.put(uuid, pMap);

        NbtList allowedTag =
                root.getList("allowedProfiles", 8);
        Set<String> allowed = new HashSet<>();
        allowedTag.forEach(el ->
                allowed.add(el.asString()));
        allowedProfiles.put(uuid, allowed);

        activeProfile.put(uuid,
                root.getString("activeProfile"));
    }
}
