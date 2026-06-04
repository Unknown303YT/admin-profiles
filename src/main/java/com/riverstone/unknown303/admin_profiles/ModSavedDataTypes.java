package com.riverstone.unknown303.admin_profiles;

import com.riverstone.unknown303.admin_profiles.profiles.ProfileManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedDataType;

public class ModSavedDataTypes {
    public static final SavedDataType<ProfileManager> PROFILE_MANAGER = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(AdminProfiles.MOD_ID, "profile_manager"),
            ProfileManager::new, ProfileManager.CODEC, null);
}
