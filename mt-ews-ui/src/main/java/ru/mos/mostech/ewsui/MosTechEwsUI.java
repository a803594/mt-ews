package ru.mos.mostech.ewsui;

import ru.mos.mostech.ewsui.ui.SimpleUi;

import java.io.IOException;

public class MosTechEwsUI {

    public static void main(String[] args) throws IOException {
        Settings.load();
        SimpleUi.start();
    }
}