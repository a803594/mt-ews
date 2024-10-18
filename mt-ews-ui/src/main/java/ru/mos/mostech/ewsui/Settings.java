package ru.mos.mostech.ewsui;

import ru.mos.mostech.ewsui.http.HttpEwsClient;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Settings {

    private static Map<String, String> settings = new HashMap<>();

    public static void load() {
        settings = HttpEwsClient.getEwsSettings();
    }

    public static String printAll() {
        return settings.entrySet()
                .stream()
                .map(e -> String.format("%s = %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }
}
