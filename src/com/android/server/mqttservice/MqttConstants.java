package com.android.server.mqttservice;

public class MqttConstants {

//    public static String HOST = "tcp://220.248.34.75:3001";
//    public static String USERNAME = "shhx";
//    public static String PASSWORD = "7ed98d7018caf009c60008521274ceb4";

    public static int QOS_QUALITY_0 = 0;   //最多一次，有可能重复或丢失
    public static int QOS_QUALITY_1 = 1;   //最少一次，有可能丢失
    public static int QOS_QUALITY_2 = 2;   //只有一次，确保消息只能到达一次

    public static boolean RETAINED_ENABLE = true;     //whether or not this message should be retained by the server.
    public static boolean RETAINED_DISABLE = false;   //whether or not this message should be retained by the server.

    public static final String CLIENT_NAME_FACE = "face";

    public static final String BKS_PASSWORD = "Tsl@2019";
    public static final String BKS_PASSWORD_1 = "123456";

}
