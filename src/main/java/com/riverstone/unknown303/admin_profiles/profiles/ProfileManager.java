package com.riverstone.unknown303.admin_profiles.profiles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.riverstone.unknown303.admin_profiles.ModSavedDataTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class ProfileManager extends SavedData {
    public static final Codec<ProfileManager> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(
                            Codec.STRING.xmap(UUID::fromString, UUID::toString),
                                    Codec.unboundedMap(Codec.STRING, ProfileData.CODEC))
                            .fieldOf("profiles")
                            .orElse(new HashMap<>())
                            .forGetter(manager -> manager.profiles),
                            Codec.unboundedMap(
                                    Codec.STRING.xmap(UUID::fromString, UUID::toString), Codec.STRING)
                                    .fieldOf("activeProfile")
                                    .orElse(new HashMap<>())
                                    .forGetter(manager -> manager.activeProfile),
                            Codec.unboundedMap(
                                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                                            (Codec<Set<String>>) Codec.STRING.listOf().xmap(
                                                    list -> new HashSet(list),
                                                    set -> new ArrayList<>(set)))
                                    .fieldOf("allowedProfiles")
                                    .orElse(new HashMap<>())
                                    .forGetter(manager -> manager.allowedProfiles))
                    .apply(instance, ProfileManager::new));

    public static ProfileManager get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(ModSavedDataTypes.PROFILE_MANAGER);
    }

    private final Map<UUID, Map<String, ProfileData>> profiles;

    // UUID -> currently active profile name
    private final Map<UUID, String> activeProfile;

    // UUID -> set of profiles they are allowed to use
    private final Map<UUID, Set<String>> allowedProfiles;

    public ProfileManager() {
        profiles = new HashMap<>();
        activeProfile = new HashMap<>();
        allowedProfiles = new HashMap<>();
    }

    public ProfileManager(Map<UUID, Map<String, ProfileData>> profiles, Map<UUID, String> activeProfile, Map<UUID, Set<String>> allowedProfiles) {
        this.profiles = new HashMap<>(profiles);
        this.activeProfile = new HashMap<>(activeProfile);
        this.allowedProfiles = new HashMap<>(allowedProfiles);
    }

    // ── Access control ──────────────────────────────────────────
    public boolean hasMultiProfile(UUID uuid) {
        Set<String> allowed = allowedProfiles.get(uuid);
        return allowed != null && allowed.size() > 1;
    }

    public void grantProfile(
            UUID uuid, String profileName,
            String displayName, boolean isOp) {

        profiles.computeIfAbsent(uuid, k -> new HashMap<>())
                .putIfAbsent(profileName,
                        new ProfileData(displayName, isOp));

        allowedProfiles.computeIfAbsent(uuid, k -> new HashSet<>())
                .add(profileName);

        setDirty();
    }

    public void revokeProfile(UUID uuid, String profileName) {
        Set<String> allowed = allowedProfiles.get(uuid);
        if (allowed != null) allowed.remove(profileName);

        setDirty();
    }

    // ── Init on player join ─────────────────────────────────────
    public void initPlayer(ServerPlayer player) {
        profiles.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .putIfAbsent("default",
                        new ProfileData(player.getPlainTextName(), false));

        allowedProfiles.computeIfAbsent(player.getUUID(), k -> {
            Set<String> s = new HashSet<>();
            s.add("default");
            return s;
        });
        activeProfile.putIfAbsent(player.getUUID(), "default");

        setDirty();
    }

    // ── Switch ──────────────────────────────────────────────────
    public void switchProfile(
            ServerPlayer player, String profileName) {

        UUID uuid = player.getUUID();
        Set<String> allowed = allowedProfiles.get(uuid);

        if (allowed == null || !allowed.contains(profileName)) {
            player.sendSystemMessage(Component.literal(
                    "[OathProfiles] You don't have a profile called '"
                            + profileName + "'."), false);
            return;
        }

        saveInventory(player);
        activeProfile.put(uuid, profileName);
        ProfileData next = profiles.get(uuid).get(profileName);

        loadInventory(player, next);
        applyDisplayName(player, next.getDisplayName());
        applyOpStatus(player, next.isOp());

        player.sendSystemMessage(Component.literal(
                "[OathProfiles] Switched to " + profileName
                        + " (" + next.getDisplayName() + ")"), false);

        setDirty();
    }

    public String getDisplayName(UUID uuid) {
        String active = activeProfile.get(uuid);
        if (active == null) return null;
        Map<String, ProfileData> pMap = profiles.get(uuid);
        if (pMap == null) return null;
        ProfileData d = pMap.get(active);
        return d != null ? d.getDisplayName() : null;
    }

    public void applyDisplayName(ServerPlayer player, String name) {
        player.setCustomName(Component.literal(name));
        player.setCustomNameVisible(false);
    }

    public List<String> listProfiles(UUID uuid) {
        Set<String> allowed = allowedProfiles.get(uuid);
        return allowed == null
                ? List.of()
                : List.copyOf(allowed);
    }

    public String getActiveProfileName(UUID uuid) {
        return activeProfile.getOrDefault(uuid, "default");
    }

    // ── Inventory helpers ───────────────────────────────────────
    private void saveInventory(ServerPlayer player) {
        String cur = activeProfile.get(player.getUUID());
        ProfileData data = profiles
                .getOrDefault(player.getUUID(), Map.of())
                .get(cur);
        if (data == null) return;



        Inventory inventory = player.getInventory();

        for (int i = 0; i < 36; i++)
            data.setSlot(i, inventory.getItem(i));

        data.setSlot(EquipmentSlot.FEET.getIndex(36), player.getItemBySlot(EquipmentSlot.FEET));
        data.setSlot(EquipmentSlot.LEGS.getIndex(36), player.getItemBySlot(EquipmentSlot.LEGS));
        data.setSlot(EquipmentSlot.CHEST.getIndex(36), player.getItemBySlot(EquipmentSlot.CHEST));
        data.setSlot(EquipmentSlot.HEAD.getIndex(36), player.getItemBySlot(EquipmentSlot.HEAD));
        data.setSlot(40, player.getOffhandItem());

        setDirty();
    }

    private void loadInventory(
            ServerPlayer player, ProfileData data) {
        Inventory inv = player.getInventory();

        inv.clearContent();

        // Load main inventory
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, data.getSlot(i));
        }

        // Load armor
        player.setItemSlot(EquipmentSlot.FEET,  data.getSlot(36));
        player.setItemSlot(EquipmentSlot.LEGS,  data.getSlot(37));
        player.setItemSlot(EquipmentSlot.CHEST, data.getSlot(38));
        player.setItemSlot(EquipmentSlot.HEAD,  data.getSlot(39));

        // Load offhand
        player.setItemSlot(EquipmentSlot.OFFHAND, data.getSlot(40));

        player.containerMenu.broadcastChanges();

        setDirty();
    }

    private void applyOpStatus(
            ServerPlayer player, boolean isOp) {
        MinecraftServer server = player.level().getServer();

        if (isOp) {
            server.getPlayerList().op(player.nameAndId(),
                    Optional.of(server.operatorUserPermissions()),
                    Optional.of(true));
        } else {
            server.getPlayerList().deop(player.nameAndId());
        }

        setDirty();
    }
}
