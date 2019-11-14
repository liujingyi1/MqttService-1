LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-common \
                               android-support-v4 \
                               android-support-v7-recyclerview \
                               mqtt-client-1.14 \
                               hawtbuf-1.11 \
                               hawtdispatch-1.22 \
                               hawtdispatch-transport-1.22
							   #paho-service-1.1.1 \
							   #paho-client-mqttv3-1.2.0

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES += \
	src/com/android/client/mqtt/IClientCallback.aidl \
	src/com/android/server/mqtt/IMqttService.aidl
	
LOCAL_AAPT_FLAGS := \
    --auto-add-overlay

LOCAL_PACKAGE_NAME := MqttService
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := paho-service-1.1.1:libs/org.eclipse.paho.android.service-1.1.1.jar \
#										paho-client-mqttv3-1.2.0:libs/org.eclipse.paho.client.mqttv3-1.2.0.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := mqtt-client-1.14:libs/mqtt-client-1.14.jar \
										hawtbuf-1.11:libs/hawtbuf-1.11.jar \
										hawtdispatch-1.22:libs/hawtdispatch-1.22.jar \
										hawtdispatch-transport-1.22:libs/hawtdispatch-transport-1.22.jar
include $(BUILD_MULTI_PREBUILT)
