package com.github.manasmods.cloudsettings.mixin.forge;

import com.github.manasmods.cloudsettings.handler.SettingsLoadingHandler;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions {
    @Inject(method = "save()V", at = @At("RETURN"))
    private void onSave(CallbackInfo ci) {
        SettingsLoadingHandler.checkForUpdate((Options) (Object) this);
    }

    @Inject(method = "load()V", at = @At("HEAD"))
    private void onSaved(CallbackInfo ci) {
        SettingsLoadingHandler.checkForLoad((Options) (Object) this);
    }
}
