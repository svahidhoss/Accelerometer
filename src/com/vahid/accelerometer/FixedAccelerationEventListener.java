package com.vahid.accelerometer;

import java.util.Date;

import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.CsvFileWriter;
import com.vahid.accelerometer.util.CsvListenerInterface;
import com.vahid.accelerometer.util.MathUtil;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

public class FixedAccelerationEventListener implements SensorEventListenerInterface,
		CsvListenerInterface {
	private Handler mHandler;

	/**** Sensor related Fields ****/
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	// Sensor Values: it's important to initialize them.
	private float[] mAccelerationValues = new float[] { 0, 0, 0 };

	private double accelerationMagnitude;

	/**** Save to file view fields ****/
	private boolean savingToFile = false;
	private CsvFileWriter mCsvFile;

	public FixedAccelerationEventListener(Handler mHandler) {
		this.mHandler = mHandler;
	}

	@Override
	public void initializeSensors(SensorManager mSensorManager) {
		this.mSensorManager = mSensorManager;

		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		if (Constants.DEBUG)
			Log.d(Constants.LOG_TAG, "Sensors initialized");

	}

	@Override
	public void registerSensors(int curDelayRate) {
		mSensorManager.unregisterListener(this);

		mSensorManager.registerListener(this, mAccelerometer,
				Constants.DELAY_RATES[curDelayRate]);

		if (Constants.DEBUG)
			Log.d(Constants.LOG_TAG, "sensors registered");
	}

	@Override
	public void unregisterSensors() {
		try {
			// Be sure that this work in wake_lock (permission to work with
			// screen off)
			if (mSensorManager != null)
				mSensorManager.unregisterListener(this);
			if (Constants.DEBUG)
				Log.d(Constants.LOG_TAG, "sensor unregistered");
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	// Don't block the onSensorChanged() method
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {

		case Sensor.TYPE_ACCELEROMETER:
			getLinearAcceleration(event);
			break;

		case Sensor.TYPE_LINEAR_ACCELERATION:
			getLinearAcceleration(event);

			break;
		default:
			break;
		}
	}

	private void getLinearAcceleration(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
			mAccelerationValues = event.values;

		if (mAccelerationValues != null) {

			accelerationMagnitude = MathUtil
					.getVectorMagnitude(mAccelerationValues);

			// If check box for saving the file has been checked.
			if (savingToFile && mCsvFile != null) {
				// current time stamp
				Date date = new Date();
				mCsvFile.writeToFile(Long.toString(date.getTime()), false);

				mCsvFile.writeToFile(mAccelerationValues, false);

				mCsvFile.writeToFile((float) accelerationMagnitude, true);
			}

			mHandler.obtainMessage(Constants.FIXED_ACCEL_VALUE_MSG,
					mAccelerationValues).sendToTarget();

		}

	}

	@Override
	public void enableSaveToFile() {
		this.savingToFile = true;
	}

	@Override
	public void disableSaveToFile() {
		this.savingToFile = false;
	}

	@Override
	public void setCsvFile(CsvFileWriter csvFile) {
		this.mCsvFile = csvFile;
	}
}
