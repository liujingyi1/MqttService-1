package com.android.server.mqttservice;

import android.app.Application;
import android.content.Context;

public class MqttApplication extends Application {

    private static MqttApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        Logger.enable(true);
    }

    public static MqttApplication getApplication() {
        return mInstance;
    }

    public static Context getAppContext() {
        return mInstance.getApplicationContext();
    }
}
