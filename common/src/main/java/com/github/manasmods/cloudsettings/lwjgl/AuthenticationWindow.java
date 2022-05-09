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
        while (apiToken == null) {
            apiToken = ApiHelper.getUserApiToken(this.uuid, requestPassword());
        }
        try {
            if (SettingsLoadingHandler.getLoginKeyFile().get().getParentFile().mkdirs()) {
                LogHelper.getLogger().info("Created storage directory.");
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(SettingsLoadingHandler.getLoginKeyFile().get()));
            writer.write(apiToken);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SettingsLoadingHandler.apiToken.set(apiToken);
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
