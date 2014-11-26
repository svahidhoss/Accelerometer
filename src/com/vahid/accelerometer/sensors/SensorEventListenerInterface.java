package com.vahid.accelerometer.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public interface SensorEventListenerInterface extends SensorEventListener {
	
	/**
	 * Method that initializes the sensors, after we're connected to the server.
	 * 
	 * @param mSensorManager
	 */
	public void initializeSensors(SensorManager mSensorManager);
	
	/**
	 * Register this class as sensor listener with the current delay rate.
	 */
	public void registerSensors(int curDelayRate);
	
	/**
	 * Unregisters the sensors on this activity.
	 */
	public void unregisterSensors();
	
	@Override
	public void onSensorChanged(SensorEvent event);

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy);

	
}
