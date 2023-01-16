package ch.dekuen.android.compassapp.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
public class CompassEventListenerTest {
    private CompassEventListener testee;
    private final List<Float> consumedFloats = new ArrayList<>();
    private AzimutService azimutService;
    private final Consumer<Float> floatConsumer = consumedFloats::add;

    @Before
    public void before() {
        azimutService = mock(AzimutService.class);
        consumedFloats.clear();
        testee = new CompassEventListener(azimutService, floatConsumer);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(azimutService);
        assertTrue(consumedFloats.isEmpty());
    }

    @Test
    public void onAccuracyChanged_AnyInput_consumeNothing() {
        // setup
        Sensor sensor = mock(Sensor.class);
        // act
        testee.onAccuracyChanged(sensor, -99);
        // assert
        verifyNoMoreInteractions(sensor);
    }

    @Test
    public void onSensorChanged_NullEvent_consumeNothing() {
        // setup
        // act & assert
        assertDoesNotThrow(() -> testee.onSensorChanged(null));
    }

    @Test
    public void onSensorChanged_NullSensor_consumeNothing() {
        // setup
        SensorEvent event = mock(SensorEvent.class);
        // act
        testee.onSensorChanged(event);
        // assert
        verifyNoMoreInteractions(event);
    }

    @Test
    public void onSensorChanged_UnknownSensorType_consumeNothing() {
        // setup
        Sensor sensor = mock(Sensor.class);
        SensorEvent event = mockEvent(sensor, -99, null);
        // act
        testee.onSensorChanged(event);
        //
        verifySensorEvent(event);
    }

    @Test
    public void onSensorChanged_OnlyDataFromAcceleration_consumeNothing() {
        onSensorChanged_OnlyDataFromOneSensor_consumeNothing(Sensor.TYPE_ACCELEROMETER);
    }

    @Test
    public void onSensorChanged_OnlyDataFromMagnetometer_consumeNothing() {
        onSensorChanged_OnlyDataFromOneSensor_consumeNothing(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void onSensorChanged_OnlyDataFromOneSensor_consumeNothing(int type) {
        // setup
        Sensor sensor = mock(Sensor.class);
        float[] values = new float[0];
        SensorEvent event = mockEvent(sensor, type, values);
        // act
        testee.onSensorChanged(event);
        //
        verifySensorEvent(event);
    }

    @Test
    public void onSensorChanged_BothSensorsPresentNoAzimut_consumeNothing() {
        // setup
        float[] acceleration = new float[1];
        float[] magneticField = new float[2];
        Sensor accelerometer = mock(Sensor.class);
        Sensor magnetometer = mock(Sensor.class);
        SensorEvent accelerometerEvent = mockEvent(accelerometer, Sensor.TYPE_ACCELEROMETER, acceleration);
        SensorEvent magnetometerEvent = mockEvent(magnetometer, Sensor.TYPE_MAGNETIC_FIELD, magneticField);
        // act
        testee.onSensorChanged(accelerometerEvent);
        testee.onSensorChanged(magnetometerEvent);
        // assert
        verifySensorEvent(accelerometerEvent);
        verifySensorEvent(magnetometerEvent);
        verify(azimutService).getAzimut(acceleration, magneticField);
    }

    @Test
    public void onSensorChanged_BothSensorsPresentAndAzimut_consumeAzimut() {
        // setup
        float[] acceleration = new float[1];
        float[] magneticField = new float[2];
        Sensor accelerometer = mock(Sensor.class);
        Sensor magnetometer = mock(Sensor.class);
        SensorEvent accelerometerEvent = mockEvent(accelerometer, Sensor.TYPE_ACCELEROMETER, acceleration);
        SensorEvent magnetometerEvent = mockEvent(magnetometer, Sensor.TYPE_MAGNETIC_FIELD, magneticField);
        float azimut = -98.76f;
        when(azimutService.getAzimut(acceleration, magneticField)).thenReturn(Optional.of(azimut));
        // act
        testee.onSensorChanged(accelerometerEvent);
        testee.onSensorChanged(magnetometerEvent);
        // assert
        verifySensorEvent(accelerometerEvent);
        verifySensorEvent(magnetometerEvent);
        verify(azimutService).getAzimut(acceleration, magneticField);
        assertEquals(Collections.singletonList(azimut), consumedFloats);
        consumedFloats.clear();
    }

    private static SensorEvent mockEvent(Sensor sensor, int type, float[] values) {
        SensorEvent event = mock(SensorEvent.class);
        when(sensor.getType()).thenReturn(type);
        event.sensor = sensor;
        try {
            Field field = SensorEvent.class.getDeclaredField("values");
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            field.set(event, values);
            field.setAccessible(accessible);
            return event;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifySensorEvent(SensorEvent event) {
        Sensor sensor = event.sensor;
        verify(sensor).getType();
        verifyNoMoreInteractions(sensor);
        verifyNoMoreInteractions(event);
    }
}