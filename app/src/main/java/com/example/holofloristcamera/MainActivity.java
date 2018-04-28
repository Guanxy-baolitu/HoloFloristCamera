package com.example.holofloristcamera;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter mBAdapter;
    BluetoothManager mBManager;
    BluetoothLeAdvertiser mBLEAdvertiser;
    static final int BEACON_ID = 1223;
    // 01/08, the followings are direction sensor
    private SensorManager sensorManager;
    private float[] accelerometerValues = new float[3];
    private float[] magneticValues = new float[3];
    private FrameLayout layout;
    private Button startBtn;
    private Thread thread;
    private TextView x;
    private TextView y;
    private boolean startAdvertising=false;
    private float xValue,yValue;
    private int plainColor, greenColor;
    private DecimalFormat dformat;
    private int thresholdAngle=18;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
        }
        mBManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        mBAdapter = mBManager.getAdapter();
        mBLEAdvertiser = mBAdapter.getBluetoothLeAdvertiser();
        // 01/08, the followings are direction sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor. TYPE_ACCELEROMETER);
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor. TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(listener, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
        initView();
    }

    private void initView()
    {
        layout=findViewById(R.id.control);
        x=findViewById(R.id.x);
        y=findViewById(R.id.y);
        dformat = new DecimalFormat("###.0");
        plainColor=getColor(R.color.pureWhite);
        greenColor=getColor(R.color.colorAccent);
        startBtn=findViewById(R.id.StartControllBtn);startBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(startAdvertising==false){
                restartAdvertising();
                waitToRunNewActivity();
                startAdvertising=true;
            }
            else{
                startAdvertising=false;
                stopAdvertising();
                x.setText("");
                y.setText("");
            }
        }
    });
    }

    private void waitToRunNewActivity(){
        thread = new Thread() {
            @Override
            public void run() {
                    try {
                        Thread.sleep(800);
                        Intent intent = new Intent(MainActivity.this, WaitActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        };
        thread.start();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(mBAdapter == null || !mBAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
                ||!mBAdapter.isMultipleAdvertisementSupported())
        {
            layout.setBackgroundResource(R.drawable.no_ble);
            x.setVisibility(View.GONE);
            y.setVisibility(View.GONE);
            startBtn.setVisibility(View.GONE);
            return;
        }
        return;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopAdvertising();
    }

    // 01/08, the followings are direction sensor
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(listener);
        }
    }
    private void startAdvertising()
    {
        if(mBLEAdvertiser == null) return;
        if(Math.abs(xValue)>thresholdAngle||Math.abs(yValue)>thresholdAngle) return;
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .addManufacturerData(BEACON_ID,buildPacket())
                .build();
        mBLEAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }
    private void stopAdvertising()
    {
        if(mBLEAdvertiser == null) return;
        mBLEAdvertiser.stopAdvertising(mAdvertiseCallback);
    }
    private void restartAdvertising()
    {
        stopAdvertising();
        startAdvertising();
    }
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            String msg = "Service Running";
            mHandler.sendMessage(Message.obtain(null,0,msg));
        }
        @Override
        public void onStartFailure(int errorCode)
        {
            if(errorCode != ADVERTISE_FAILED_ALREADY_STARTED)
            {
                String msg = "Service failed to start: " + errorCode;
                mHandler.sendMessage(Message.obtain(null,0,msg));
            }
            else
            {
                restartAdvertising();
            }
        }
    };
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
/*
UI feedback to the user would go here.
*/
        }
    };

    byte[] buildPacket(){
        byte[] packet = new byte[8];
        try {
            byte[] buffer = ByteBuffer.allocate(4).putFloat(xValue).array();
            for (int i = 0, j =3; i < 4; i++, j--)
                packet[i] = buffer[j];
            buffer = ByteBuffer.allocate(4).putFloat(yValue).array();
            for (int i = 4, j =3; i < 8; i++, j--)
                packet[i] = buffer[j];
            //buffer = ByteBuffer.allocate(4).putFloat(yValue).array();
            //for (int i = 8, j =3; i < 12; i++, j--)
            //    packet[i] = buffer[j];
        } catch (NumberFormatException e) {
            packet = new byte[8];
        }
        Log.d("packet",packet.toString());
        return packet;
    }

    // 01/08, the followings are direction sensor
    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            //if(startAdvertising){
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { // 注意赋值时要调用clone()方法
                    accelerometerValues = event.values.clone();
                }
                else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) { // 注意赋值时要调用clone()方法
                    magneticValues = event.values.clone();
                }
                float[] R = new float[9];
                float[] values = new float[3];
                SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticValues);
                SensorManager.getOrientation(R, values);
                // NOTE : transport with degree format
                // 0:z 1:x 2:y
                //zValue = (float)Math.toDegrees(values[0]);
                xValue = (float)Math.toDegrees(values[1]);
                yValue = (float)Math.toDegrees(values[2])+90;
                x.setText("水平:"+dformat.format(xValue));
                y.setText("垂直:"+dformat.format(yValue));
                if(xValue>thresholdAngle||xValue<-thresholdAngle){
                    x.setTextColor(plainColor);
                }
                else{
                    x.setTextColor(greenColor);
                }
                if(yValue>thresholdAngle||yValue<-thresholdAngle){
                    y.setTextColor(plainColor);
                }
                else{
                    y.setTextColor(greenColor);
                }
           // }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
}
