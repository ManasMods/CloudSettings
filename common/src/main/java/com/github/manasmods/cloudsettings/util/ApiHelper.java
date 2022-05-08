package com.github.manasmods.cloudsettings.util;

import com.github.manasmods.cloudsettings.CloudSettings;
import com.github.manasmods.cloudsettings.handler.SettingsLoadingHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.stream.Stream;

public class ApiHelper {
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

    public static String checkAutoLogin() {
        if (!SettingsLoadingHandler.getLoginKeyFile().get().exists()) return null;
        try (Stream<String> stream = Files.lines(SettingsLoadingHandler.getLoginKeyFile().get().toPath(), StandardCharsets.UTF_8)) {
            String storedToken = stream.findFirst().orElse(null);
            if (storedToken == null) return null;
            HttpGet request = new HttpGet("http://134.255.220.120:8800/cloudsettings/autologin");
            request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
            request.addHeader("user-token", storedToken);

            CloseableHttpResponse response = HTTP_CLIENT.execute(request);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            if (!result.startsWith("Login failed.")) {
                return result;
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getUserApiToken(String uuid, String password) {
        HttpGet request = new HttpGet("http://134.255.220.120:8800/cloudsettings/login/" + uuid);
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-password", password);
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            HttpEntity entity = response.getEntity();
            JsonObject resultObject = new JsonParser().parse(EntityUtils.toString(entity)).getAsJsonObject();
            if (!resultObject.has("result")) {
                CloudSettings.getLogger().fatal("Got an empty response from API in getUserApiToken.");
                return null;
            }

            String result = resultObject.get("result").getAsString();

            if (!result.startsWith("Login failed.")) {
                return result;
            } else {
                CloudSettings.getLogger().info("Login Failed.");
                TinyFileDialogs.tinyfd_notifyPopup("Cloud Settings - Authentication",
                    "Login failed.\nPlease enter the correct password.\nTo reset the password please message the ManasMods support team.",
                    "error");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getUserApiToken(uuid, password);
    }

    private static HttpGet authorizedGet(String path) {
        HttpGet request = new HttpGet("http://134.255.220.120:8800/" + path);
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-token", SettingsLoadingHandler.apiToken.get());
        return request;
    }

    public static boolean sendSetting(String key, String value) {
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(authorizedGet(String.format("store/%s/%s/%s", getUserId(), key, value)))) {
            HttpEntity entity = response.getEntity();
            JsonObject resultObject = new JsonParser().parse(EntityUtils.toString(entity)).getAsJsonObject();

        } catch (IOException e) {
            CloudSettings.getLogger().error("Failed to send setting {} to Cloud.", key);
            return false;
        }
    }

    private static String getUserId() {
        return Minecraft.getInstance().getUser().getUuid();
    }
}
