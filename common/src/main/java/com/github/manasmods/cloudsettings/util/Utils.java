package com.github.manasmods.cloudsettings.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import lombok.experimental.UtilityClass;

import java.io.StringReader;

@UtilityClass
public class Utils {
    public String getKeyFromOptionLine(String line) {
        return line.substring(0, line.indexOf(':'));
    }

    public JsonObject readJson(String string) {
        JsonReader reader = new JsonReader(new StringReader(string));
        reader.setLenient(true);
        return new JsonParser().parse(reader).getAsJsonObject();
    }
}
