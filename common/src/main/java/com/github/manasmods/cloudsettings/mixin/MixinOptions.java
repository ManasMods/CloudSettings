package com.github.manasmods.cloudsettings.mixin;

import com.github.manasmods.cloudsettings.CloudSettings;
import com.github.manasmods.cloudsettings.util.Constants;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions {
    @Inject(method = "save()V", at = @At("RETURN"))
    private void onSave(CallbackInfo ci) {
        if (!CloudSettings.isInitialized()) return;
        if (!CloudSettings.isEnabled()) return;
        Constants.logger.info("Checking for updates");
        long start = System.currentTimeMillis();
        CloudSettings.updateSettings((Options) (Object) this);
        Constants.logger.info("Updated Settings in {}ms", System.currentTimeMillis() - start);
    }

    @Inject(method = "load()V", at = @At("HEAD"))
    private void onLoad(CallbackInfo ci) {
        if (CloudSettings.isInitialized()) return;
        if (!CloudSettings.isEnabled()) return;
        Constants.logger.info("Initializing Cloud Settings");
        long start = System.currentTimeMillis();
        CloudSettings.loadSettings((Options) (Object) this);
        Constants.logger.info("Initialized Cloud Settings in {}ms", System.currentTimeMillis() - start);
    }
}
