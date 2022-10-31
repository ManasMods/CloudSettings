package com.github.manasmods.cloudsettings.lwjgl;

import com.github.manasmods.cloudsettings.handler.SettingsLoadingHandler;
import com.github.manasmods.cloudsettings.util.ApiHelper;
import com.github.manasmods.cloudsettings.util.LogHelper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
public class AuthenticationWindow {
    public static final Path disableFile = Paths.get("").resolve("disable.cloudsettings");
    private final String uuid;

    public void show() {
        String apiToken = ApiHelper.checkAutoLogin();
        if (apiToken == null) {
            while (apiToken == null) {
                String password = requestPassword();
                if (password != null) {
                    apiToken = ApiHelper.getUserApiToken(this.uuid, password);
                } else {
                    if (disableFile.toFile().exists()) {
                        return;
                    }
                }
            }
        } else {
            LogHelper.getLogger().info("Token login was successful. Skipping login request.");
        }
        try {
            if (SettingsLoadingHandler.getLoginKeyFile().getParentFile().mkdirs()) {
                LogHelper.getLogger().info("Created storage directory.");
            }

            if (SettingsLoadingHandler.getLoginKeyFile().exists()) {
                //Delete old login file if it exists
                SettingsLoadingHandler.getLoginKeyFile().delete();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(SettingsLoadingHandler.getLoginKeyFile()));
            writer.write(apiToken);
            writer.close();
            SettingsLoadingHandler.apiToken.set(apiToken);
        } catch (IOException e) {
            LogHelper.getLogger().trace("Exception while writing the new API Token into the login.key file.", e);
        }
    }

    @Nullable
    private String requestPassword() {
        String result = TinyFileDialogs.tinyfd_inputBox("Cloud Settings - Authentication", "Please enter a password to secure your Settings.", null);
        // Dialog cancel
        if (result == null) {
            boolean disableMod = TinyFileDialogs.tinyfd_messageBox("Cloud Settings", "Are you sure you want to disable CloudSettings?", "yesno", "warning", false);
            if (disableMod) {
                TinyFileDialogs.tinyfd_notifyPopup("Cloud Settings", "Cloud Settings has been disabled.", "info");
                disableCloudSettings();
                return null;
            } else {
                // Ask for Password again
                return requestPassword();
            }
        }

        // Retry on empty input
        if (result.isEmpty() || result.isBlank()) return requestPassword();

        return result;
    }


    private void disableCloudSettings() {
        if (!disableFile.toFile().exists()) {
            try {
                Files.write(disableFile, List.of(""), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
