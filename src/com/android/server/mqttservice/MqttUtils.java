package com.android.server.mqttservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Nv;
import com.android.internal.lock.ILockManager;

public class MqttUtils {
    private static final String HOST_KEY = "host_key";
    private static final String USERNAME_KEY = "username_key";
    private static final String PASSWORD_KEY = "password_key";

    public static boolean isConnectIsNormal() {
        ConnectivityManager connectivityManager = (ConnectivityManager)MqttApplication.getApplication()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null) {
            return false;
        }

        boolean isConnected = info.isConnected();
        return isConnected;
    }

    public static String getDeviceID() {
        ILockManager lockManager = (ILockManager)MqttApplication.getApplication()
                .getSystemService("lock");
        String deviceId = "";
        try {
            deviceId = lockManager.readNvStr(1);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        //String deviceId = Nv.readNvStr(1);
        if (TextUtils.isEmpty(deviceId) || "unknown".equalsIgnoreCase(deviceId)
                || "0123456789ABCDEF".equalsIgnoreCase(deviceId)) {
            SharedPreferences sp = MqttApplication.getApplication().getSharedPreferences("mqttservice", Context.MODE_PRIVATE);
            deviceId = sp.getString("deviceid", "");

            if (TextUtils.isEmpty(deviceId)) {
                String time = Long.toString(System.currentTimeMillis());
                deviceId = time.substring(time.length() - 12);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("deviceid", deviceId);
                editor.commit();
            }
        }

        return deviceId;
    }

    public static void setMqttConfig(String host, String username, String password) {
        SharedPreferences sp = MqttApplication.getApplication().getSharedPreferences("mqttservice", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(HOST_KEY, host == null ? "" : host);
        editor.putString(USERNAME_KEY, username == null ? "" : username);
        editor.putString(PASSWORD_KEY, password == null ? "" : password);
        editor.commit();
    }

    public static String getHost() {
        SharedPreferences sp = MqttApplication.getApplication().getSharedPreferences("mqttservice", Context.MODE_PRIVATE);
        return sp.getString(HOST_KEY, "ssl://220.248.34.75:8443");
    }

    public static String getUserName() {
        SharedPreferences sp = MqttApplication.getApplication().getSharedPreferences("mqttservice", Context.MODE_PRIVATE);
        return sp.getString(USERNAME_KEY, "shhx");
    }

    public static String getPassword() {
        SharedPreferences sp = MqttApplication.getApplication().getSharedPreferences("mqttservice", Context.MODE_PRIVATE);
        return sp.getString(PASSWORD_KEY, "7ed98d7018caf009c60008521274ceb4");
    }
}
