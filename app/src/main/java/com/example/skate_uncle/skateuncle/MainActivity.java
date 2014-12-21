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
import android.view.Surface;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.umeng.analytics.MobclickAgent;

import java.util.Arrays;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager_;
    private Sensor gyro_sensor_;
    private Sensor acceleration_sensor_;
    private Sensor orientation_sensor_;
    private int sensor_type_;
    private float[] angles = new float[3];
    public static SoundManager soundManager_;

    public float[][] gyro_samples_ = new float[3][5];
    public int gyro_samples_count_ = 0;
    public float[] gyro_samples_average_ = new float[3];

    public float[][] orientation_samples_ = new float[3][5];
    public int orientation_samples_count_ = 0;
    public float[] orientation_samples_average_ = new float[3];
    int rotation = 0;

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
        rotation = getWindowManager().getDefaultDisplay().getRotation();
        // Init sensors.
        sensorManager_ = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        gyro_sensor_ = sensorManager_.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        orientation_sensor_ = sensorManager_.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (gyro_sensor_ != null && orientation_sensor_ != null) {
            sensor_type_ = Sensor.TYPE_GYROSCOPE;
        } else if (orientation_sensor_ != null) {
            sensor_type_ = Sensor.TYPE_ORIENTATION;
        } else {
            gyro_sensor_ = null;
            orientation_sensor_ = null;
            acceleration_sensor_ = sensorManager_.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensor_type_ = Sensor.TYPE_ACCELEROMETER;
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
        if (gyro_sensor_ != null)
            sensorManager_.registerListener(this, gyro_sensor_, sensorManager_.SENSOR_DELAY_GAME);
        if (orientation_sensor_ != null)
            sensorManager_.registerListener(this, orientation_sensor_, sensorManager_.SENSOR_DELAY_GAME);
        if (acceleration_sensor_ != null)
            sensorManager_.registerListener(this, acceleration_sensor_, sensorManager_.SENSOR_DELAY_GAME);
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
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            onGyroSensorChanged(sensorEvent);
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            onAccelerationSensorChanged(sensorEvent);
        } else {
            onOrientationSensorChanged(sensorEvent);
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

        if (sensor_type_ == Sensor.TYPE_GYROSCOPE)
            x_acceleration = (float)(new_value / (Math.PI / 2));
        else if (sensor_type_ == Sensor.TYPE_ACCELEROMETER)
            x_acceleration = (float)(-new_value / 9.8);
        else
            x_acceleration = -new_value / 90;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void onAccelerationSensorChanged(SensorEvent sensorEvent) {
        angles[0] = sensorEvent.values[0];
        angles[1] = sensorEvent.values[1];
        angles[2] = sensorEvent.values[2];
    }

    private void onOrientationSensorChanged(SensorEvent event) {
        float axisX = event.values[2];
        float axisY = event.values[1];
        float axisZ = event.values[0];
//        Log.d("Orientation", axisX + " " + axisY + " " + axisZ);
        if (sensor_type_ == Sensor.TYPE_ORIENTATION &&
            orientation_samples_count_ > orientation_samples_[0].length) {
            angles[0] = axisX - orientation_samples_average_[0];
            angles[1] = axisY - orientation_samples_average_[1];
            angles[2] = axisZ - orientation_samples_average_[2];
        } else {
            if (orientation_samples_count_ < orientation_samples_[0].length) {
                orientation_samples_[0][orientation_samples_count_] = axisX;
                orientation_samples_[1][orientation_samples_count_] = axisY;
                orientation_samples_[2][orientation_samples_count_] = axisZ;
                ++orientation_samples_count_;
            } else if (orientation_samples_count_ == orientation_samples_[0].length) {
                for (int i = 0; i < 3; ++i) {
                    orientation_samples_average_[i] = 0;
                    Arrays.sort(orientation_samples_[i]);
                    orientation_samples_average_[i] = orientation_samples_[i][orientation_samples_[i].length / 2];
                }
                Log.d("OrientationCorrection", orientation_samples_average_[0] + " " +
                      orientation_samples_average_[1] + " " + orientation_samples_average_[2]);
                ++orientation_samples_count_;
            } else if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                        Math.abs(orientation_samples_average_[0] - axisX) < 0.5) ||
                       ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                        Math.abs(orientation_samples_average_[1] - axisY) < 0.5)
                      ) {
                Arrays.fill(angles, 0);
                Log.d("OrientationCorrection", "Reset angles");
            }
        }
    }

    private void onGyroSensorChanged(SensorEvent event) {
        if (timestamp != 0) {
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];
//            Log.d("GyroOrigin", axisX + " " + axisY + " " + axisZ);
            if (gyro_samples_count_ < gyro_samples_[0].length) {
                gyro_samples_[0][gyro_samples_count_] = axisX;
                gyro_samples_[1][gyro_samples_count_] = axisY;
                gyro_samples_[2][gyro_samples_count_] = axisZ;
                ++gyro_samples_count_;
            } else if (gyro_samples_count_ == gyro_samples_[0].length) {
                for (int i = 0; i < 3; ++i) {
                    gyro_samples_average_[i] = 0;
                    Arrays.sort(gyro_samples_[i]);
                    gyro_samples_average_[i] = gyro_samples_[i][gyro_samples_[i].length / 2];
                }
                Log.d("GyroCorrection", gyro_samples_average_[0] + " " + gyro_samples_average_[1] + " " + gyro_samples_average_[2]);
                ++gyro_samples_count_;
            } else {
//                Log.d("GyroCorrected", axisX + " " + axisY + " " + axisZ);
//                axisX -= gyro_samples_average_[0];
//                axisY -= gyro_samples_average_[1];
//                axisZ -= gyro_samples_average_[2];
                final float dT = (event.timestamp - timestamp) * NS2S;
                angles[0] += axisY * dT;
                angles[1] += -axisX * dT;
                angles[2] += axisZ * dT;
            }
        }
        timestamp = event.timestamp;
    }

    public void ResetAngles() {
        Arrays.fill(angles, 0);
        Arrays.fill(gyro_samples_average_, 0);
        Arrays.fill(orientation_samples_average_, 0);
        gyro_samples_count_ = 0;
        orientation_samples_count_ = 0;
    }
}
