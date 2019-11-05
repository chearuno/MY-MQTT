package com.example.mymqtt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;

import static android.content.ContentValues.TAG;


public class MainActivity extends Activity implements SensorEventListener, MqttCallback {

    MqttAndroidClient client;

    private TextView yText, textDevice, txtScore, txtMethod, txtConnection;
    private Sensor mySensor;
    private SensorManager SM;
    private String total;
    String topicSen = "topicSen";
    String topicButton = "topicButton";
    final Handler handler = new Handler();
    final Handler handlerNext = new Handler();
    final Handler senHandler = new Handler();
    String topic = "scr";
    int qos = 1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton playButton = (ImageButton) findViewById(R.id.imageButton_play);
        ImageButton pauseButton = (ImageButton) findViewById(R.id.imageButton_pause);
        ImageButton nextButton = (ImageButton) findViewById(R.id.imageButton_for);
        ImageButton preButton = (ImageButton) findViewById(R.id.imageButton_back);
        yText = (TextView) findViewById(R.id.yText);
        txtScore = (TextView) findViewById(R.id.txt_score);
        textDevice = (TextView) findViewById(R.id.txt_device);
        txtMethod = (TextView) findViewById(R.id.txt_method);
        txtConnection = (TextView) findViewById(R.id.txt_connection);


        ConnectNow();


        textDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (topicSen.equals("topicSen") && topicButton.equals("topicButton")) {
                    topicSen = "topicSen2";
                    topicButton = "topicButton2";
                    textDevice.setText("Device 2");

                } else if (topicSen.equals("topicSen2") && topicButton.equals("topicButton2")) {
                    topicSen = "topicSen";
                    topicButton = "topicButton";
                    textDevice.setText("Device 1");
                }
            }
        });

        txtMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txtMethod.getText().equals("Accelerometer ON")) {
                    senHandler.removeCallbacksAndMessages(null);
                    txtMethod.setText("Accelerometer OFF");
                    yText.setText("Accelerometer OFF");

                } else if (txtMethod.getText().equals("Accelerometer OFF")) {
                    final Runnable r = new Runnable() {
                        public void run() {
                            PublishNow();
                            senHandler.postDelayed(this, 10);
                        }
                    };

                    senHandler.postDelayed(r, 10);
                    txtMethod.setText("Accelerometer ON");
                }
            }
        });

        txtConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txtConnection.getText().equals("Disconnected")) {
                    ConnectNow();
                }
            }
        });


        playButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (textDevice.getText().equals("Device 1")) {
                    publishStart("YES1");
                } else {
                    publishStart("YES2");
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textDevice.getText().equals("Device 1")) {
                    publishStart("NO1");
                } else {
                    publishStart("NO2");
                }
            }
        });

        nextButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    // pressedd
                    Log.e("Button > ", " pressed");
                    final Runnable r = new Runnable() {
                        public void run() {
                            PublishZero("B");
                            handlerNext.postDelayed(this, 10);
                        }
                    };

                    handlerNext.postDelayed(r, 10);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    Log.e("Button > ", " realeased");
                    handlerNext.removeCallbacksAndMessages(null);
                    PublishZero("0");
                }
                return true;
            }
        });

        preButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.e("Button > ", " pressed");
                    final Runnable r = new Runnable() {
                        public void run() {
                            PublishZero("A");
                            handler.postDelayed(this, 10);
                        }
                    };

                    handler.postDelayed(r, 10);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    Log.e("Button > ", " realeased");
                    handler.removeCallbacksAndMessages(null);
                    PublishZero("0");
                }
                return true;
            }
        });

        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void ConnectNow() {

        String clientId = MqttClient.generateClientId();
        // String clientId = "clientId-1RKXAbDHaT";
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883",
                clientId);

        // MqttConnectOptions options = new MqttConnectOptions();
        // options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
//        options.setUserName("asd");
//        options.setPassword("asd".toCharArray());
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectionLost(Throwable cause) {
                txtConnection.setText("Disconnected");
                Toast.makeText(MainActivity.this, "Connection Fail", Toast.LENGTH_LONG).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                Log.e(TAG, "New message on " + topic + ":  " + new String(message.getPayload()));
                if(textDevice.getText().equals("Device 1")){
                    if(topic.equals("scr")){
                        txtScore.setText("Score: " + new String(message.getPayload()));
                    }
                }else{
                    if(topic.equals("scr2")){
                        txtScore.setText("Score: " + new String(message.getPayload()));
                    }
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }

            @Override
            public void connectComplete(boolean var1, String var2) {

            }
        });
        try {
            // IMqttToken token = client.connect(options);
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.e("main", "onSuccess");
                    txtConnection.setText("Connected");
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
                    Subscription();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.e("Main", "onFailure");
                    txtConnection.setText("Disconnected");
                    Toast.makeText(MainActivity.this, "Connection Fail", Toast.LENGTH_LONG).show();

                }


            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void Subscription() {

        if (txtConnection.getText().equals("Connected")) {
            try {
                Log.e("sub", "inside");
                IMqttToken subToken = client.subscribe(topic, qos);
                subToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // The message was published
                        Log.e("sub", "onSuccess");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
                                          Throwable exception) {
                        // The subscription could not be performed, maybe the user was not
                        // authorized to subscribe on the specified topic e.g. using wildcards
                        Log.e("sub", "fail");

                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }

            try {

                IMqttToken subToken = client.subscribe("scr2", qos);
                subToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
                                          Throwable exception) {


                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void PublishNow() {

        if (total != null) {
            String payload = total;
            byte[] encodedPayload = new byte[0];
            try {
                encodedPayload = payload.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                client.publish(topicSen, message);
            } catch (UnsupportedEncodingException | MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void PublishZero(String val) {

        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = val.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topicButton, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishStart(String val) {

        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = val.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish("startGame", message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // handler.removeCallbacksAndMessages(null);
        //senHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (txtMethod.getText().equals("Accelerometer ON")) {
            float yAxis = (float) event.values[1];
            yText.setText("Y: " + event.values[1]);

            if (yAxis > -1 && yAxis < 1) {
                total = "1";
            } else if (yAxis < -4) {
                total = "C";
            } else if (yAxis > 4) {
                total = "D";
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void connectionLost(Throwable cause) {
        txtConnection.setText("Disconnected");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.e(TAG, "New message on " + topic + ":  " + new String(message.getPayload()));
        txtScore.setText(new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

}
