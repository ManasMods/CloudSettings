package com.github.manasmods.cloudsettings.lwjgl;

import com.github.manasmods.cloudsettings.handler.SettingsLoadingHandler;
import com.github.manasmods.cloudsettings.util.ApiHelper;
import com.github.manasmods.cloudsettings.util.LogHelper;
import lombok.RequiredArgsConstructor;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@RequiredArgsConstructor
public class AuthenticationWindow {
    private final String uuid;

    public void show() {
        String apiToken = ApiHelper.checkAutoLogin();
        if (apiToken == null) {
            while (apiToken == null) {
                apiToken = ApiHelper.getUserApiToken(this.uuid, requestPassword());
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

    private String requestPassword() {
        String result = TinyFileDialogs.tinyfd_inputBox("Cloud Settings - Authentication", "Please enter a password to secure your Settings.", "");
        if (result == null || result.isEmpty()) {
            return requestPassword();
        } else {
            return result;
        }
    }
}
