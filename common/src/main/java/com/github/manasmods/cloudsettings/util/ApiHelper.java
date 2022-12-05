package com.github.manasmods.cloudsettings.util;

import com.github.manasmods.cloudsettings.handler.SettingsLoadingHandler;
import com.github.manasmods.cloudsettings.lwjgl.AuthenticationWindow;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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
    private static final Gson GSON = new Gson();
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    private static final String apiEndpointUrl = "https://cloudservice.blutmondgilde.de:2053";

    public static String checkAutoLogin() {
        if (!SettingsLoadingHandler.getLoginKeyFile().exists()) {
            LogHelper.getLogger().info("Couldn't find a login token at: {}", SettingsLoadingHandler.getLoginKeyFile().getAbsolutePath());
            return null;
        }
        LogHelper.getLogger().info("Found existing login token.");
        try (Stream<String> stream = Files.lines(SettingsLoadingHandler.getLoginKeyFile().toPath(), StandardCharsets.UTF_8)) {
            String storedToken = stream.findFirst().orElse(null);
            if (storedToken == null) {
                LogHelper.getLogger().warn("Could not load Token from file!");
                return null;
            }
            LogHelper.getLogger().info("Trying login with the Token...");
            HttpGet request = new HttpGet(apiEndpointUrl + "/cloudsettings/autologin");
            request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
            request.addHeader("user-token", storedToken);

            CloseableHttpResponse response = HTTP_CLIENT.execute(request);
            HttpEntity entity = response.getEntity();
            JsonObject resultObject = new JsonParser().parse(EntityUtils.toString(entity)).getAsJsonObject();
            String result = getResultValue(resultObject);
            if (request == null) return null;

            if (!result.startsWith("Login failed")) {
                LogHelper.getLogger().info("Received new token: {}", result);
                return result;
            } else {
                LogHelper.getLogger().info("Login with Token failed.");
                return null;
            }
        } catch (IOException e) {
            LogHelper.getLogger().trace("Exception while trying to log in.", e);
            return null;
        }
    }

    public static String getUserApiToken(String uuid, String password) {
        HttpGet request = new HttpGet(apiEndpointUrl + "/cloudsettings/login/" + uuid);
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
        HttpGet request = new HttpGet(apiEndpointUrl + "/cloudsettings/" + path);
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-token", SettingsLoadingHandler.apiToken.get());
        return request;
    }

    private static HttpPost authorizedPost(String path) {
        HttpPost request = new HttpPost(apiEndpointUrl + "/cloudsettings/" + path);
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-token", SettingsLoadingHandler.apiToken.get());
        request.addHeader("Content-type", "application/json");
        return request;
    }

    public static boolean sendSetting(String key, String value) {
        try {
            HttpPost request = authorizedPost("store/" + getUserId());

            String body = GSON.toJson(new Setting(key, value));
            request.setEntity(new StringEntity(body));

            HTTP_CLIENT.execute(request);
            CloseableHttpResponse response = HTTP_CLIENT.execute(request);

            HttpEntity entity = response.getEntity();
            JsonReader reader = new JsonReader(new StringReader(EntityUtils.toString(entity)));
            reader.setLenient(true);
            JsonObject resultObject = new JsonParser().parse(reader).getAsJsonObject();

            String resultValue = getResultValue(resultObject);

            if (resultValue == null) return false;
            if (resultValue.startsWith("Login failed")) {
                if (SettingsLoadingHandler.getLoginKeyFile().delete()) {
                    LogHelper.getLogger().info("Login Token is outdated. Requesting new a new one...");
                }
                AuthenticationWindow window = new AuthenticationWindow(Minecraft.getInstance().getUser().getUuid());
                window.show();
                return sendSetting(key, value);
            }

            return resultValue.equals("Value saved");
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
                if (SettingsLoadingHandler.getLoginKeyFile().delete()) {
                    LogHelper.getLogger().info("Login Token is outdated. Requesting new a new one...");
                }
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

                settings.add(jsonObject.get("value").getAsString());
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
