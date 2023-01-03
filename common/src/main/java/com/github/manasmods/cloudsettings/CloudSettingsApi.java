package com.github.manasmods.cloudsettings;

import com.github.manasmods.cloudsettings.lwjgl.AuthenticationWindow;
import com.github.manasmods.cloudsettings.util.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.IOException;

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

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            String token = resolveResultValue(response);
            if (token == null) {
                Constants.logger.error("Got no result from auto login request.");
                return null;
            }
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
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            String token = resolveResultValue(response);

            if (token != null && !token.startsWith("Login failed")) {
                return token;
            } else {
                Constants.logger.warn("Login failed. Login Response: {}", token);
                AuthenticationWindow.logInFailedNotification();
                return null;
            }
        } catch (Exception e) {
            Constants.logger.error("Failed to log in.", e);
        }
        // retry
        return login(userId, password, retries);
    }

    @Nullable
    public static String login(String userId, String password) {
        return login(userId, password, 5);
    }

    @Nullable
    private static String resolveResultValue(CloseableHttpResponse response) throws IOException, IllegalStateException {
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        Constants.logger.debug("Got Response:\n{}", responseString);
        JsonObject responseObject = new JsonParser().parse(responseString).getAsJsonObject();
        if (!responseObject.has("result")) {
            Constants.logger.warn("Got no response object from Server");
            return null;
        }
        return responseObject.get("result").getAsString();
    }
}
