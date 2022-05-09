package com.github.manasmods.cloudsettings.util;

import com.github.manasmods.cloudsettings.handler.SettingsLoadingHandler;
import com.github.manasmods.cloudsettings.lwjgl.AuthenticationWindow;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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
            if (!result.startsWith("Login failed")) {
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
                LogHelper.getLogger().fatal("Got an empty response from API in getUserApiToken.");
                return null;
            }

            String result = resultObject.get("result").getAsString();

            if (!result.startsWith("Login failed")) {
                return result;
            } else {
                LogHelper.getLogger().info("Login Failed");
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
        HttpGet request = new HttpGet("http://134.255.220.120:8800/cloudsettings/" + path);
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-token", SettingsLoadingHandler.apiToken.get());
        return request;
    }

    public static boolean sendSetting(String key, String value) {
        try {
            CloseableHttpResponse response = HTTP_CLIENT.execute(authorizedGet(String.format("store/%s/%s/%s", getUserId(), encode(key), encode(value))));

            HttpEntity entity = response.getEntity();
            JsonReader reader = new JsonReader(new StringReader(EntityUtils.toString(entity)));
            reader.setLenient(true);
            JsonObject resultObject = new JsonParser().parse(reader).getAsJsonObject();

            String resultValue = getResultValue(resultObject);

            if (resultValue.startsWith("Login failed")) {
                SettingsLoadingHandler.getLoginKeyFile().get().delete();
                AuthenticationWindow window = new AuthenticationWindow(Minecraft.getInstance().getUser().getUuid());
                window.show();
                return sendSetting(key, value);
            }
            return resultValue != null && resultValue.equals("Value saved");
        } catch (Exception e) {
            LogHelper.getLogger().trace("Exception while trying to send the update to the cloud.", e);
            return false;
        }
    }

    public static List<String> loadSettings() {
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(authorizedGet(String.format("store/%s", getUserId())))) {
            HttpEntity entity = response.getEntity();
            JsonObject resultObject = new JsonParser().parse(EntityUtils.toString(entity)).getAsJsonObject();
            String resultValue = getResultValue(resultObject.get("result").getAsJsonObject());
            //Exit if no status has been sent
            if (resultValue == null) return Lists.newArrayList();
            if (resultValue.startsWith("Login failed")) {
                SettingsLoadingHandler.getLoginKeyFile().get().delete();
                AuthenticationWindow window = new AuthenticationWindow(Minecraft.getInstance().getUser().getUuid());
                window.show();
                return loadSettings();
            }
            if (!resultValue.equals("Success")) return Lists.newArrayList();
            //Exit if no values has been sent
            if (!resultObject.has("entries")) return Lists.newArrayList();
            JsonArray storedSettings = resultObject.get("entries").getAsJsonArray();

            ArrayList<String> settings = new ArrayList<>();
            storedSettings.forEach(jsonElement -> {
                if (!jsonElement.isJsonObject()) return;

                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (!jsonObject.has("key") || !jsonObject.has("value")) return;

                String settingLine = String.format("%s:%s", jsonObject.get("key").getAsString(), jsonObject.get("value").getAsString());
                settings.add(settingLine);
            });

            LogHelper.getLogger().info("Loaded {} settings from Cloud.", settings.size());
            return settings;
        } catch (IOException e) {
            LogHelper.getLogger().trace("Failed to load settings from Cloud.", e);
            return Lists.newArrayList();
        }
    }

    private static String getUserId() {
        return Minecraft.getInstance().getUser().getUuid();
    }

    @Nullable
    private static String getResultValue(JsonObject jsonObject) {
        if (!jsonObject.has("result")) return null;
        return jsonObject.get("result").getAsString();
    }

    private static String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }
}
