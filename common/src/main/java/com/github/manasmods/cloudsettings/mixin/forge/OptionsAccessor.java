package com.github.manasmods.cloudsettings.mixin.forge;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(Options.class)
public interface OptionsAccessor {
    @Accessor
    File getOptionsFile();
}
