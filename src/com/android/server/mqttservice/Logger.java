package com.android.server.mqttservice;

import android.util.Log;

public class Logger {

    private static boolean ENABLE_LOG = false;

    public static void enable(boolean enable) {
        ENABLE_LOG = enable;
    }

    public static void i(String tag, String message) {
        if (ENABLE_LOG) {
            Log.i(tag, message);
        }
    }

    public static void d(String tag, String message) {
        if (ENABLE_LOG) {
            Log.d(tag, message);
        }
    }

}
