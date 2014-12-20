package com.example.skate_uncle.skateuncle;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Surface;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.Window;

import com.umeng.analytics.MobclickAgent;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager_;
    private Sensor sensor_;
    private boolean use_gyro = true;
    private float[] angles = new float[3];
    public static SoundManager soundManager_;

    public float[][] samples = new float[100][3];
    public int samples_count = 0;
    public float[] samples_average = new float[3];

    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private long timestamp;

    private PowerManager powerManager_;
    private WakeLock wakeLock_;

    public static volatile float x_acceleration;

    private static TapListener tap_listener_;
    public static int last_tap_x_;
    public static int last_tap_y_;

    private GestureDetectorCompat gesture_detector_;

    class TapListener extends SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d("Listener:", "onDown");
            tapped_ = true;
            last_tap_x_ = (int) e.getX();
            last_tap_y_ = (int) e.getY();
            return true;
        }

        public boolean ReceiveTapEvent() {
            boolean t = tapped_;
            tapped_ = false;
            return t;
        }

        private boolean tapped_ = false;
    }

    static public boolean ReceiveTapEvent() {
        return tap_listener_.ReceiveTapEvent();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init sensors.
        sensorManager_ = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor_ = sensorManager_.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (sensor_ == null) {
            use_gyro = false;
            sensor_ = sensorManager_.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Keep screen wake.
        powerManager_ = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock_ = powerManager_.newWakeLock(PowerManager.FULL_WAKE_LOCK, "FlappyBot");
        tap_listener_ = new TapListener();
        gesture_detector_ = new GestureDetectorCompat(this, tap_listener_);

        // Init sound manager
        soundManager_ = new SoundManager(this);

        // Init umeng
        MobclickAgent.updateOnlineConfig(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gesture_detector_.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        ResetAngles();
        sensorManager_.registerListener(this, sensor_, sensorManager_.SENSOR_DELAY_GAME);
        wakeLock_.acquire();
        soundManager_.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager_.unregisterListener(this);
        wakeLock_.release();
        soundManager_.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (use_gyro)
            onGyroSensorChanged(sensorEvent);
        else {
            onAccelerationSensorChanged(sensorEvent);
        }

        float new_value = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                new_value = angles[0];
                break;
            case Surface.ROTATION_90:
                new_value = -angles[1];
                break;
            case Surface.ROTATION_180:
                new_value = -angles[0];
                break;
            case Surface.ROTATION_270:
                new_value = angles[1];
                break;
        }

        if (use_gyro) {
            x_acceleration = (float)(new_value / (Math.PI / 2));
        } else
            x_acceleration = (float)(-new_value / 9.8);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void onAccelerationSensorChanged(SensorEvent sensorEvent) {
        angles[0] = sensorEvent.values[0];
        angles[1] = sensorEvent.values[1];
        angles[2] = sensorEvent.values[2];
    }

    private void onGyroSensorChanged(SensorEvent event) {
        if (timestamp != 0) {
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];
//            Log.d("GyroOrigin", axisX + " " + axisY + " " + axisZ);
            if (samples_count < samples.length) {
                samples[samples_count][0] = axisX;
                samples[samples_count][1] = axisY;
                samples[samples_count][2] = axisZ;
                ++samples_count;
            } else if (samples_count == samples.length) {
                for (int j = 0; j < 3; ++j) {
                    samples_average[j] = 0;
                    for (int i = 0; i < samples.length; ++i) {
                        samples_average[j] += samples[i][j];
                    }
                    samples_average[j] /= samples.length;
                }
                Log.d("GyroCorrection", samples_average[0] + " " + samples_average[1] + " " + samples_average[2]);
                ++samples_count;
            } else {
//                Log.d("GyroCorrected", axisX + " " + axisY + " " + axisZ);
//                axisX -= samples_average[0];
//                axisY -= samples_average[1];
//                axisZ -= samples_average[2];
                final float dT = (event.timestamp - timestamp) * NS2S;
                angles[0] += axisY * dT;
                angles[1] += -axisX * dT;
                angles[2] += axisZ * dT;
            }


        }
        timestamp = event.timestamp;
    }

    public void ResetAngles() {
        angles = new float[3];
    }
}
