package com.github.manasmods.cloudsettings.mixin;

import com.github.manasmods.cloudsettings.CloudSettings;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Window.class)
public class MixinWindow {
    @Inject(method = "setPreferredFullscreenVideoMode(Ljava/util/Optional;)V", at = @At("RETURN"))
    private void onChange(Optional<VideoMode> optional, CallbackInfo ci) {
        if (optional.isPresent()) return;
        CloudSettings.disableResolution();
    }
}
