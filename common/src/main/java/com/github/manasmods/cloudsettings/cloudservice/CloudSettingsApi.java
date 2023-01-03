package com.github.manasmods.cloudsettings.cloudservice;

import com.github.manasmods.cloudsettings.AuthHandler;
import com.github.manasmods.cloudsettings.cloudservice.pojo.Setting;
import com.github.manasmods.cloudsettings.exception.AuthenticationFailedException;
import com.github.manasmods.cloudsettings.lwjgl.AuthenticationWindow;
import com.github.manasmods.cloudsettings.util.Constants;
import com.github.manasmods.cloudsettings.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CloudSettingsApi {
    private static final Gson GSON = new Gson();
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

    /**
     * @return api token
     */
    @Nullable
    public static String autoLogin(String userToken) {
        HttpGet request = new HttpGet(Constants.CLOUD_SERVER + "/cloudsettings/autologin");
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-token", userToken);

        Constants.logger.debug("Sending {} get request", "/cloudsettings/autologin");
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            String token = resolveResultValue(response);
            if (token == null) {
                Constants.logger.error("Got no result from auto login request.");
                return null;
            }
            return token;
        } catch (Exception e) {
            Constants.logger.error("Failed to read auto login response.", e);
        }

        return null;
    }

    /**
     * @return api token
     */
    @Nullable
    public static String login(String userId, String password, int retries) {
        if (retries-- <= 0) {
            Constants.logger.warn("Ran out of retries in login request");
            return " ";
        }

        HttpGet request = new HttpGet(Constants.CLOUD_SERVER + "/cloudsettings/login/" + userId);
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-password", password);

        Constants.logger.debug("Sending {} get request", "/cloudsettings/login/" + userId);
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            String token = resolveResultValue(response);
            if (token != null) return token;
        } catch (AuthenticationFailedException e) {
            Constants.logger.warn("Login failed.");
            AuthenticationWindow.logInFailedNotification();
            return null;
        } catch (Exception e) {
            Constants.logger.error("Failed to log in. Exception:", e);
        }
        // retry
        return login(userId, password, retries);
    }

    @Nullable
    public static String login(String userId, String password) {
        return login(userId, password, 5);
    }

    public static List<String> getUserSettings(String userId, AuthHandler authHandler) {
        ArrayList<String> loadedSettings = new ArrayList<>();
        HttpGet request = authHandler.authorizeGetRequest("store/" + userId);
        // Return none if user is not authenticated
        if (request == null) return loadedSettings;

        Constants.logger.debug("Sending {} get request", "store/" + userId);
        try {
            CloseableHttpResponse response = HTTP_CLIENT.execute(request);

            String resultValue = resolveResultValue(response);
            if (!"Success".equals(resultValue)) return loadedSettings;
            JsonObject resultObject = new JsonParser().parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (!resultObject.has("entries")) return loadedSettings;

            JsonArray storedSettings = resultObject.get("entries").getAsJsonArray();
            storedSettings.forEach(jsonElement -> {
                if (!jsonElement.isJsonObject()) return;
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (!jsonObject.has("key") || !jsonObject.has("value")) return;
                loadedSettings.add(jsonObject.get("value").getAsString());
            });
            Constants.logger.info("Loaded {} Settings from Cloud Service", loadedSettings.size());
            response.close();
        } catch (IOException e) {
            Constants.logger.error("Error on loading user settings from cloud service", e);
        }

        return loadedSettings;
    }

    public static boolean sendSetting(AuthHandler authHandler, String userId, String key, String value) {
        HttpPost request = authHandler.authorizedPostRequest("store/" + userId);
        if (request == null) {
            Constants.logger.warn("Unauthorized post request canceled.");
            return false;
        }

        try {
            String body = GSON.toJson(new Setting(key, value));
            request.setEntity(new StringEntity(body));

            Constants.logger.debug("Sending {} post request", "store/" + userId);
            try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
                String resultValue = resolveResultValue(response);

                if (resultValue == null) return false;
                return resultValue.equals("Value saved");
            } catch (Exception e) {
                Constants.logger.trace("Exception while trying to send the update to the cloud.", e);
                return false;
            }
        } catch (Exception e) {
            Constants.logger.trace("Exception while trying to prepare request entity.", e);
            return false;
        }
    }

    @Nullable
    private static String resolveResultValue(CloseableHttpResponse response) throws IOException, IllegalStateException {
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        Constants.logger.debug("Got Response:\n{}", responseString);

        JsonObject responseObject = Utils.readJson(responseString);
        if (!responseObject.has("result")) {
            Constants.logger.warn("Got no response object from Server");
            return null;
        }

        if (responseObject.get("result").isJsonObject()) {
            Constants.logger.debug("resolving inner result object {}", responseObject.toString());
            responseObject = responseObject.get("result").getAsJsonObject();
            Constants.logger.debug("resolved inner result object to {}", responseObject.toString());
        }

        String result = responseObject.get("result").getAsString();
        if (result.startsWith("Login failed")) throw new AuthenticationFailedException(EntityUtils.toString(response.getEntity()));
        return result;
    }
}
