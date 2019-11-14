package com.android.server.mqttservice;

public class MqttTopics {
    /**
     * From server to client
     */
    public static final String DISPATCH_FILTER_ITEM = "/server/dispatchFilterItem/";
    public static final String DISPATCH_FILTER_LIST = "/server/dispatchFilterList/";
    public static final String SIP_ITEM = "/server/sipItem/";
    public static final String SIP_ITEM_LIST = "/server/sipItemList/";
    public static final String SIP_CONFIG_INFO = "/server/sipConfigInfo/";
    public static final String DISPATCH_OTP = "/server/dispatchOTP/";
    public static final String UPGRADE_REQUEST = "/server/upgradeRequest/";
    public static final String DEVICE_CONTROL_REQUEST = "/server/deviceControlRequest/";
    public static final String FACE_ADD = "/server/faceAdd/";
    public static final String FACE_DELETE = "/server/faceDelete/";
    //add new
    public static final String TIME_CALIBRATE = "/server/timeCalibrate/";
    //add new by v8.4
    public static final String DISPATCH_SIP_ITEM = "/server/dispatchSipItem/";
    public static final String DISPATCH_AD_URL = "/server/dispatchAdURL/";
    public static final String DISPATCH_DOWNLOAD_URL = "/server/dispatchDownLoadURL/";
    public static final String DISPATCH_GROUP_CODES = "/server/dispatchGroupCodes/";
    public static final String DISPATCH_READING_HEAD_PROGRAM_URL = "/server/dispatchReadingHeadProgramURL/";
    
	/**
     * From client to server
     */
    public static final String UPLOAD_ACCESS_LOG = "/client/uploadAccessLog/";
    public static final String UPLOAD_DOOR_SENSOR = "/client/uploadDoorSensor/";
    public static final String UPLOAD_DEVICE_INFO = "/client/uploadDeviceInfo/";
    public static final String UPLOAD_DEVICE_EVENT = "/client/uploadDeviceEvent/";
    public static final String UPLOAD_DEVICE_MAINTAIN = "/client/uploadDeviceMaintain/";
    //add new
    public static final String FILTER_REQUEST = "/client/filterRequest/";
    public static final String RESPONSE = "/client/response/";
    //add new by v8.4
    public static final String SIP_REQUEST = "/client/sipRequest/";
    public static final String KEEP_ALIVE = "/client/keepAlive/";
    public static final String FACE_REQUEST = "/client/faceRequest/";
    public static final String CAPTURE_UPLOAD = "/client/captureUpload/";
    public static final String READER_UPGRADE_RESULT_UPLOAD = "/client/readerUpgradeResultUpload/";
    public static final String UPLOAD_DEVICE_INFO_REQUEST = "/client/uploadDeviceInfoRequest/";

}
