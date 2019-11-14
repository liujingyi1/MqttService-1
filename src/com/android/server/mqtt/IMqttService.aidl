// ILyyService.aidl
package com.android.server.mqtt;

import com.android.client.mqtt.IClientCallback;

interface IMqttService {
    void publish(String topic, String content);
    void subscribeTopic(String topic, IClientCallback client);
    void subscribeTopics(in List<String> topics, IClientCallback client);
    void config(String host, String username, String password);
}
