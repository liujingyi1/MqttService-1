// IClientCallback.aidl
package com.android.client.mqtt;

// Declare any non-default types here with import statements

interface IClientCallback {
    void dispatch(String topic, String content);
}
