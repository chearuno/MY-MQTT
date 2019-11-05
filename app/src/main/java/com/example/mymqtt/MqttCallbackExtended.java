package com.example.mymqtt;

import org.eclipse.paho.client.mqttv3.MqttCallback;

public interface MqttCallbackExtended extends MqttCallback {
    void connectComplete(boolean var1, String var2);
}
