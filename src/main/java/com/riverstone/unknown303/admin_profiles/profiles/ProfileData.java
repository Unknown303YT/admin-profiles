package com.riverstone.unknown303.admin_profiles.profiles;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileData {
    public static final Codec<ProfileData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(Codec.STRING.fieldOf("displayName").orElse("PLAYER").forGetter(data -> data.getDisplayName()),
                                    Codec.BOOL.fieldOf("isOp").orElse(false).forGetter(data -> data.isOp()),
                                    ItemStack.CODEC.listOf().fieldOf("inventory").orElse(new ArrayList<>()).forGetter(data -> data.getInventory()),
                                    ItemStack.CODEC.listOf().fieldOf("armor").orElse(new ArrayList<>()).forGetter(data -> data.getArmor()),
                                    ItemStack.CODEC.fieldOf("offhand").orElse(ItemStack.EMPTY).forGetter(data -> data.getOffhand()))
                            .apply(instance, ProfileData::new));

    private final String displayName;
    private final boolean isOp;
    private final List<ItemStack> inventory;
    private final List<ItemStack> armor;
    private ItemStack offhand = ItemStack.EMPTY;

    public ProfileData(String displayName, boolean isOp) {
        this.displayName = displayName;
        this.isOp = isOp;
        inventory = new ArrayList<>();
        armor = new ArrayList<>();
    }

    public ProfileData(String displayName, boolean isOp, List<ItemStack> inventory, List<ItemStack> armor, ItemStack offhand) {
        this.displayName = displayName;
        this.isOp = isOp;
        this.inventory = new ArrayList<>(inventory);
        this.armor = new ArrayList<>(armor);
        this.offhand = offhand.copy();
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOp() {
        return isOp;
    }

    public List<ItemStack> getInventory() {
        return new ArrayList<>(inventory);
    }

    public List<ItemStack> getArmor() {
        return new ArrayList<>(armor);
    }

    public ItemStack getOffhand() {
        return offhand.copy();
    }
}
