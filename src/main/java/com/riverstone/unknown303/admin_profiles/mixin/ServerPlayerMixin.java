package com.riverstone.unknown303.admin_profiles.mixin;

import com.riverstone.unknown303.admin_profiles.profiles.ProfileManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /**
     * Intercepts getDisplayName() so the custom profile name
     * appears in chat, tab-list, death messages, etc.
     */
    @Inject(
            method = "getTabListDisplayName",
            at = @At("HEAD"),
            cancellable = true
    )
    private void injectProfileName(
            CallbackInfoReturnable<Component> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        String overrideName = ProfileManager.get(self.level())
                .getDisplayName(self.getUUID());

        if (overrideName != null)
            cir.setReturnValue(Component.literal(overrideName));
    }
}