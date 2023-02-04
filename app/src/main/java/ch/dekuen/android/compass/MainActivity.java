package ch.dekuen.android.compass;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import ch.dekuen.android.compass.sensor.AzimutCalculator;
import ch.dekuen.android.compass.sensor.CompassSensorEventListener;
import ch.dekuen.android.compass.sensor.CoordinatesLowPassFilter;
import ch.dekuen.android.compass.sensor.OrientationCalculator;
import ch.dekuen.android.compass.sensor.RotationMatrixCalculator;
import ch.dekuen.android.compass.view.CompassImageViewService;
import ch.dekuen.android.compass.view.CompassTextViewService;

public class MainActivity extends Activity {

    // SENSOR_DELAY_GAME for fast response, alternatively use SENSOR_DELAY_UI or SENSOR_DELAY_NORMAL
    private static final int SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_GAME;

    private CompassSensorEventListener compassSensorEventListener;

    // device sensor manager
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AzimutListener azimutListener = getAzimutListener();
        AzimutCalculator azimutCalculator = new AzimutCalculator(azimutListener);
        OrientationCalculator orientationCalculator = new OrientationCalculator(azimutCalculator::onOrientationChanged);
        RotationMatrixCalculator rotationMatrixCalculator = new RotationMatrixCalculator(orientationCalculator::calculate);
        CoordinatesLowPassFilter accelerationLPF = new CoordinatesLowPassFilter(rotationMatrixCalculator::onAccelerationSensorChanged);
        CoordinatesLowPassFilter magneticLPF = new CoordinatesLowPassFilter(rotationMatrixCalculator::onMagneticSensorChanged);
        compassSensorEventListener = new CompassSensorEventListener(
                accelerationLPF::onSensorChanged,
                magneticLPF::onSensorChanged);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @NonNull
    private AzimutListener getAzimutListener() {
        // ImageView for compass image
        ImageView compassImageView = findViewById(R.id.compassImageView);
        // TextView that will display the azimut in degrees
        TextView azimutTextView = findViewById(R.id.azimutTextView);
        CompassTextViewService textViewService = new CompassTextViewService(azimutTextView);
        CompassImageViewService imageViewService = new CompassImageViewService(compassImageView);
        return azimut -> runOnUiThread(() -> {
            textViewService.onNewAzimut(azimut);
            imageViewService.onNewAzimut(azimut);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // to stop the listener and save battery
        sensorManager.unregisterListener(compassSensorEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(compassSensorEventListener, accelerometer, SAMPLING_PERIOD_US);
        sensorManager.registerListener(compassSensorEventListener, magnetometer, SAMPLING_PERIOD_US);
    }
}