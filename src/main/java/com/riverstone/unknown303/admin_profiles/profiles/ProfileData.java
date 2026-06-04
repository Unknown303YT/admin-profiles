package com.riverstone.unknown303.admin_profiles.profiles;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProfileData {
    public static final Codec<ProfileData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(Codec.STRING.fieldOf("displayName").orElse("PLAYER").forGetter(ProfileData::getDisplayName),
                                    Codec.BOOL.fieldOf("isOp").orElse(false).forGetter(ProfileData::isOp),
                                    ItemStack.CODEC.listOf().fieldOf("slots").orElse(new ArrayList<>(Collections.nCopies(41, ItemStack.EMPTY))).forGetter(data -> data.slots))
                            .apply(instance, ProfileData::new));

    private final String displayName;
    private final boolean isOp;
    private List<ItemStack> slots;

    public ProfileData(String displayName, boolean isOp) {
        this.displayName = displayName;
        this.isOp = isOp;
        slots = new ArrayList<>(Collections.nCopies(41, ItemStack.EMPTY));
    }

    public ProfileData(String displayName, boolean isOp, List<ItemStack> slots) {
        this.displayName = displayName;
        this.isOp = isOp;
        this.slots = new ArrayList<>(slots);
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOp() {
        return isOp;
    }

    public ItemStack getSlot(int slot) {
        return slots.get(slot).copy();
    }

    public void setSlot(int slot, ItemStack stack) {
        slots.set(slot, stack.copy());
    }

    public List<ItemStack> getAllSlots() {
        return slots.stream().map(ItemStack::copy).toList();
    }
}
